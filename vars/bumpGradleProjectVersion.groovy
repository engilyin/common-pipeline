#!/usr/bin/env groovy

/**
 * Bumps the current gradle project version
 *
**/
def call(Map vars) {
    def gitCredentials = vars.get("gitCredentials", null)

    // Get baseVersion from Gradle
    def baseVersion = sh(script: "./gradlew properties | grep '^baseVersion:' | awk '{print \$2}'", returnStdout: true).trim()
    def (major, minor, buildNo) = baseVersion.tokenize('.').collect { it.toInteger() }

    // Get the current Git branch
    def branch = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
    def isHotfix = branch.startsWith('hotfix/')

    // Return snapshot version if not main/hotfix
    if (branch != 'main' && !isHotfix) {
        env.VERSION = "${baseVersion}-SNAPSHOT"
        return env.VERSION
    }

    // Define the tag regex pattern properly
    def tagPattern = isHotfix ? "v\\d+\\.\\d+\\.\\d+\\.\\d+" : "v\\d+\\.\\d+\\.\\d+"

    // Get last matching tag
    def lastTag = sh(script: "git for-each-ref --sort=-v:refname --count=1 --format='%(refname:short)' refs/tags/${tagPattern}", returnStdout: true).trim()

    // Start new build number at 1
    buildNo = 1

    if (lastTag) {
        def versionParts = lastTag.replace('v', '').tokenize('.').collect { it.toInteger() }

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

    def newVersion = isHotfix ? "v${major}.${minor}.${buildNo}.${versionParts[3]}" : "v${major}.${minor}.${buildNo}"

    // Tag and push new version
    sh "git tag -a ${newVersion} -m 'Release ${newVersion}'"
    gitAskPass(gitCredentials, "git push origin ${newVersion}")

    env.VERSION = newVersion
    return newVersion
}

