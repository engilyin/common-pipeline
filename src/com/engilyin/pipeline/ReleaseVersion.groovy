package com.engilyin.pipeline

abstract class ReleaseVersion implements Serializable {

    def steps

    String gitRepoPath
    String branch
    String release
    String versionFiles
    String extractVersionExpression
    String originHttpsUrl
    Closure customChanger 

    def gitCredentials

    abstract def versionStringFromMatch(def newVersion)
    abstract def buildCommitMessagePattern()
    abstract def versionPartsFromString(String versionString)
    abstract def unknownVersion()
    abstract def versionPartsFromMatch(def match, int startIndex)
    abstract def buildNewVersion(String latestRelease)
    abstract def getStringVersion(def newVersion)

    def init(steps, Map vars) {
        this.steps = steps

        this.gitRepoPath = vars.get("gitRepoPath", '.') 
        this.branch = vars.get("branch", 'master')
        this.release = vars.get("release", '')
        this.versionFiles = vars.get("versionFiles", '').trim()
        this.extractVersionExpression = vars.get("extractVersionExpression", '')

        this.gitCredentials = vars.get("gitCredentials", null)
        this.originHttpsUrl = vars.get("originHttpsUrl", null)

        this.customChanger = vars.get("customChanger", null)
    }

    def setup() {
        steps.echo "Building with params gitRepoPath: $gitRepoPath, branch: $branch, release: $release,  versionFiles: $versionFiles, extractVersionExpression: $extractVersionExpression"
        steps.dir(gitRepoPath) {
            if(release.length() == 0) {
                steps.echo 'Building the current version'

                setupLatestRelease()
            } else {
                steps.echo "Custom by release build for $release"
                switchToRelease()
            }
        }
    }

    def getFeatureBranchVersion() {
        steps.echo "Get the Feature Branch Version form source file gitRepoPath: $gitRepoPath, branch: $branch, versionFiles: $versionFiles, extractVersionExpression: $extractVersionExpression"
        steps.dir(gitRepoPath) {
            def sourcesVersion = extractCurrentSourceVersion()
            return 'v' + getStringVersion(sourcesVersion)
        }
    }

    def switchToRelease() {
        steps.sh("git checkout $release")
    } 

    String lastRelease(String projectPath = '.') {
        steps.dir(projectPath) {
            try {
                return steps.sh(returnStdout: true, script: "git describe --abbrev=0 --tags").trim()
            } catch(Exception e) {
                return null
            }
        }
    }

    def haveNewCommits(String latestRelease) {
        def currentCommit = steps.sh(returnStdout: true, script: "git rev-parse HEAD")
        steps.echo "Current commit: $currentCommit"

        try {
            def haveNewCommits = steps.sh(returnStdout: true, script: "git describe --contains $currentCommit")
            steps.echo "haveNewCommits = $haveNewCommits. Means we do not have new commits"

            return false
        } catch(hudson.AbortException e) {
            def fullError = e.toString()
            steps.echo "Full error: $fullError"

            steps.echo "Means we have new commits and need new version"
            return true
        }
    }

    def updateVersionSourceFiles(String newVersion) {
        if(this.versionFiles.length() > 0) {
            String[] files = this.versionFiles.split(',')
            for(f in files) {
                steps.echo "Updating the file $f"
                updateVersionSourceFile(f, newVersion)
               
                try {
                     steps.sh "git add $f"
                } catch(Exception e) {
                    steps.echo "Git unable to add $f. Just ignore it."
                }
            }
            def changedFiles = steps.sh(returnStdout: true, script: "git status")
            steps.echo "Changed files: $changedFiles"
        } else {
            steps.echo "There are no version source files to change"
        }

        if(this.customChanger != null) {
            this.customChanger(newVersion)
        }
    }

    def updateVersionSourceFile(String file, String newVersion) {
        String fileContents = steps.readFile(file)
        String updatedFile = fileContents.replaceAll(extractVersionExpression, '$1' + newVersion + '$3')
        steps.writeFile file: file, text: updatedFile
    }

    def tagCurrentCommitAsRelease(def existedReleaseVersion) {
        steps.echo "Just tag existed release $existedReleaseVersion"

        steps.sh("git tag -a v$existedReleaseVersion -m 'Releasing version v$existedReleaseVersion'")
        steps.gitAskPass(gitCredentials, "git push origin v$existedReleaseVersion")
    }

    def notTaggedReleaseVersion() {
        def currentCommitMessage = steps.sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
        return (currentCommitMessage =~ buildCommitMessagePattern()).with {it ->  it.matches() ? it[0][1]: null  }
    }

    def tagNewVersion(String latestRelease) {
        def existedReleaseVersion = notTaggedReleaseVersion()
        if(existedReleaseVersion == null) {
            steps.echo "Tag a new version $latestRelease"

            def newVersion = buildNewVersion(latestRelease)
            updateVersionSourceFiles(versionStringFromMatch(newVersion))

            steps.echo "new version will be: $newVersion"

            releaseLatestChanges(newVersion)
        } else {
            tagCurrentCommitAsRelease(existedReleaseVersion)
        }
    }

    def releaseLatestChanges(def newVersion) {
        def stringVersion = getStringVersion(newVersion)
        steps.sh "git remote get-url origin"
        steps.echo "Using credentials: $gitCredentials"

        if(versionFiles.length() > 0) {
            steps.sh("""
                        #!/usr/bin/env bash

                        git add .
                        git status
                        git commit -m 'build $stringVersion'
                    """)
            steps.gitAskPass(gitCredentials, "git push origin HEAD")

        } 
        steps.sh "git tag -a v$stringVersion -m 'Releasing version v$stringVersion'"
        steps.gitAskPass(gitCredentials, "git push origin v$stringVersion")
    }

    def versionParts(String versionString) {
        steps.echo "version from git to be converted '$versionString'"
        if(versionString != null) {
            return versionPartsFromString(versionString)
        }
        return unknownVersion()
    }

    def extractCurrentSourceVersion() {
        if(versionFiles.length() > 0) {

            String[] files = versionFiles.split(',')
            String mainVersionFile = steps.readFile(files[0])
        
            def versionMatchPattern = java.util.regex.Pattern.compile(extractVersionExpression)
            def versionMatcher = versionMatchPattern.matcher(mainVersionFile)

            def version = versionPartsFromString(versionMatcher[0][2])

            steps.echo "Found source version: $version"

            return version
        } else {
            steps.echo 'No version files'
        }

        return unknownVersion()
    }

    def setupLatestRelease() {
        def latestRelease = lastRelease()
        steps.echo "Found the latest release: $latestRelease"
        steps.echo "Do we have new commits?"
    
        if(latestRelease == null) {
            steps.echo "We never had the releases for this project or error occured with the code: $latestRelease"
        
            tagNewVersion(null)

        } else if(haveNewCommits(latestRelease)) {
            steps.echo "Yes we do and we need to tag a new version $latestRelease"

            tagNewVersion(latestRelease)

        } else {
            steps.echo "We have already the latest version: $latestRelease"
        }
    }
}