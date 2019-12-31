# common-pipeline

## Overview
**Common Pipeline Lib** could be useful for CI/DI tasks.

It contains useful tasks:

- **setupBuildVersion** - to define the current release version, update sources appropriately and add the new git release tag if needed

- **currentVersion** - to get release version for git tag or from the appropriated source file in case of feature, SNAPSHOT etc build

- **gitAskPass** - to do remote (origin) git operations using the appropriated credentials

- **copyFilteredFile** - to change the source file by pattern. Kind of Unix `sed`

- **printStatus** - just sample step. Prints some job status.

## setupBuildVersion

### Description
It is used to check the current git branch found in the workspace for the new commits since the last tagged release. If new commits exist them it generate the new version, update the required source files and pushes the new release build tagging it with the new version.

It is smart enough to use higher version either from the appropriated source file or from the previous release.

Now it is supporting 2 kind of release version format.

If you need another kind of version feel free to create a new class similar to `src/com/engilyin/pipeline/FourPartReleaseVersion.groovy` or `src/com/engilyin/pipeline/ThreePartReleaseVersion.groovy`

### Parameters
 - `versionFormat` - default: `3` - Release Version Format. Currently supported 2 kinds of versions: `3` for MAJOR_VERSION.BUILD_NO.TIMESTAMP and `4` for MAJOR_VERSION.BUILD_NO.YY.MMDD.
 - `gitRepoPath` - default: `.` - Path where the repository. Sometimes you could load the repository manually somewhere into the workspace
 - `branch` - default: `master` - the used git branch. 
 - `versionFiles` - comma-separated list of source files to be updated with the new version. Path is relative to the `gitRepoPath`
 - `extractVersionExpression` - the RegExt pattern for the Java `String.replaceAll` which will be used on all `versionFiles`. It must have 3 groups. The group in the middle will be replaced with the new version. You may use empty groups if you do not have surrounding text.
 - `gitCredentials` - the git username/password credential Id
 - `customChanger` - you can define any function to chenge the codebase before the new version commit. It must have one string parameter for the new version. E.g.
 
 ```groovy
 def updateReadme(String newVersion) {
    String file = 'README.txt'
    String fileContents = steps.readFile(file)
    String updatedFile = fileContents.replaceAll('\\$\\{latestVersion\\}', newVersion)
    steps.writeFile file: file, text: updatedFile
 }
 ```
 - `release` - the existed release version could be defined for artifact regeneration. [Not yet ready at the time]
 
 
### Example

```groovy
@Library("CommonPipeline")_

pipeline {
    agent any

    environment {
            
        PRJ_CODE_PATH = 'code/myproj'

        versionSourceFile = 'src/java/com/myproj/consts/Consts.java,src/java/com/myproj/consts/AnotherConsts.java'
        versionPattern = '(public\\s+static\\s+final\\s+String\\s+PROJ_VERSION\\s*=\\s*\\")(\\d+\\.\\d+\\.\\d+)(\\";)'
    }
    
    parameters {
       choice(choices: ['master', 'sandbox'], description: 'What type of release do you need?', name: 'RELEASE_TYPE')
       string(name: 'custom_branch', defaultValue: '', description: 'The feature branch?')
    }
    
    stage('Loading code') {
            agent {
                label 'master'
            } 
                       
            steps {
                
                echo 'Loading code...'
                echo "Using branch: ${getBranch()}"
                
                dir("$PRJ_CODE_PATH") {
                    git url: 'https://shell.berendosolutions.com/nj/NJTicket',
                    branch: getBranch(),
                    credentialsId: 'MyGitCredentials'
                }
            }
        }
    
    stage('Define version') {
        agent {
            label 'master'
        }
        when {
            environment name: 'RELEASE_TYPE', value: 'master'
        }
                       
        steps {
            echo 'Getting release version....'
            
            setupBuildVersion versionFormat: '3', gitRepoPath: "$PRJ_CODE_PATH", versionFiles: "$versionSourceFile", extractVersionExpression: "$versionPattern", gitCredentials: 'MyGitCredentials', customChanger: this.&updateReadme 
        }
    }
    
    stage('Config') {

        steps {
            echo 'Set config variables'
            script {
                def isRelease = (env.RELEASE_TYPE == 'master')
                    
                currentVer = currentVersion(gitRepoPath: "$PRJ_CODE_PATH", release: isRelease, versionFiles: "$versionSourceFile", extractVersionExpression: "$versionPattern").substring(1)
                    
            }
            echo "Now you can use currentVer variable in the below stages. currentVer = $currentVer"
        }    
}

def updateReadme(String newVersion) {
    String file = 'docs/README.txt'
    String fileContents = steps.readFile(file)
    String updatedFile = fileContents.replaceAll('\\$\\{latestVersion\\}', newVersion)
    steps.writeFile file: file, text: updatedFile
}



```

## currentVersion

### Description
Gets the current version from the last release tag or from the source file.

### Parameters
 - `versionFormat` - default: `3` - Release Version Format. Currently supported 2 kinds of versions: `3` for MAJOR_VERSION.BUILD_NO.TIMESTAMP and `4` for MAJOR_VERSION.BUILD_NO.YY.MMDD.
 - `gitRepoPath` - default: `.` - Path where the repository. Sometimes you could load the repository manually somewhere into the workspace
 - `release` - `true` or `false`. Where to get the version. If `true` - from the TAG, if false - from the first source file.
 - `versionFiles` - comma-separated list of source files to be updated with the new version. Path is relative to the `gitRepoPath`
 - `extractVersionExpression` - the RegExt pattern for the Java `String.replaceAll` which will be used on all `versionFiles`. It must have 3 groups. The group in the middle will be replaced with the new version. You may use empty groups if you do not have surrounding text.

### Example

```groovy
currentVer = currentVersion(gitRepoPath: "$PRJ_CODE_PATH", release: isRelease, versionFormat: '4', versionFiles: "$versionSourceFile", extractVersionExpression: "$versionPattern").substring(1)

```

## gitAskPass
### Description
Allows to do remote operations on git origin.

### Parameters

- **credentialsId** - the git username/password credential Id
- **gitCommand** - git command. E.g. `git push origin HEAD`

### Example

```
gitAskPass('MyGitCredentials', "git push origin HEAD")
```

## copyFilteredFile

### Description
Reads the source file into the string and do the Java `String.replaceAll` on it and if the `destination` available write it into the new file. Otherwise updates the source.

### Parameters
- **source** - source file
- **destination** - destination file if you need to create a new file for the changed contents
- **pattern** - Regex pattern for the Java `String.replaceAll`
- **replacement** - replacement for the found substring

### Example

```groovy
copyFilteredFile source: "templates/${currentEnvFolder}/webapp.xml", destination: "release/${bundleName}/myproj/webapp-${bundleName}.xml", pattern: '\\$\\{bundleName\\}', replacement: "${bundleName}"


copyFilteredFile source: "release/${bundleName}/${bundleName}/MyCSharpApp.exe.config", pattern: '(<setting\\s+name="MasterHost".*>\\s*<value>)(.*)(<\\/value>)', replacement: '$1' + "${masterHost}" + '$3'

```

## printStatus
### Description

It is very simple and does not have parameters. You can use it to test whether this library is set up properly and available for your pipeline.

### Example

```groovy
@Library("CommonPipeline")_

pipeline {
    agent none
    stages {
        stage('Test') {
            steps {
                printStatus()
            }
        }
    }
}
```