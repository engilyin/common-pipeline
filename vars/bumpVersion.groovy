#!/usr/bin/env groovy

/**
 * Bumps the current gradle project version
 *
**/
def call(Map vars) {
    def gitCredentials = vars.get("gitCredentials", null)
    def versionTagPrefix = vars.get("versionTagPrefix", "ver")
    def baseVersionFile = vars.get("baseVersionFile", "version.txt")
    def noSnapshots = vars.get("noSnapshots", false)


    // Get baseVersion from Gradle
    def baseVersion = sh(script: "cat ${baseVersionFile} | grep '^baseVersion=' | awk -F'=' '{print \$2}'", returnStdout: true).trim()
    echo "baseVersion=${baseVersion}"

    def (major, minor, buildNo) = baseVersion.tokenize('.').collect { it.toInteger() }

    def branch = env.BRANCH_NAME
    echo "The current Git branch: ${branch}"

    def isHotfix = branch.startsWith('hotfix/')
    echo "Is Hotfix: ${isHotfix}"

    // Return snapshot version if not main/hotfix
    if (!noSnapshots && branch != 'main' && !isHotfix) {
        echo "It is not a release or hotfix. Fallback to SNAPSHOT"
        env.VERSION = "${baseVersion}-SNAPSHOT"
        return env.VERSION
    }

    // Define the tag regex pattern properly
    def tagPattern = isHotfix ? "v\\d+\\.\\d+\\.\\d+\\.\\d+" : "v\\d+\\.\\d+\\.\\d+"

    // Get last matching tag

    gitAskPass(gitCredentials, "git fetch --tags --depth=5")
    def lastTag = sh(script: "git for-each-ref --sort=-v:refname --count=1 --format='%(refname:short)' 'refs/tags/${versionTagPrefix}*'", returnStdout: true).trim()
    
    //new way based on ls-remote
 //   def lastTag = gitAskPass(gitCredentials, "git ls-remote --tags --sort=creatordate | grep refs/tags/${versionTagPrefix} | awk -F'/' '/refs\\/tags\\/${versionTagPrefix}/{print \$3}' | tail -n 1")
    
    echo "Last Tag: ${lastTag}"

    // Start new build number at 1
    buildNo = 1

    if (lastTag) {
        def versionParts = lastTag.replace("${versionTagPrefix}", '').tokenize('.').collect { it.toInteger() }

        if (isHotfix) {
            if (versionParts[0..2] != [major, minor, buildNo]) {
                error("Hotfix version mismatch with latest release!")
            }
            versionParts[3]++  // Increment patch number for hotfix
        } else {
            if (major > versionParts[0] || minor > versionParts[1]) {
                buildNo = 1
            } else {
                buildNo = versionParts[2] + 1
            }
        }
    }

    def newVersion = isHotfix ? "${major}.${minor}.${buildNo}.${versionParts[3]}" : "${major}.${minor}.${buildNo}"

    echo "Define a new version: ${newVersion}"

    // Tag and push new version
    sh "git tag -a ${versionTagPrefix}${newVersion} -m 'Release for ${versionTagPrefix} ${newVersion}'"
    gitAskPass(gitCredentials, "git push origin ${versionTagPrefix}${newVersion}")

    env.VERSION = newVersion
    return newVersion
}

