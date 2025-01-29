#!/usr/bin/env groovy

/**
 * Bumps the current gradle project version
 *
**/
def call(Map vars) {
    def gitCredentials = vars.get("gitCredentials", null)

    def baseVersion = sh(script: "./gradlew properties | grep '^baseVersion:' | awk '{print \$2}'", returnStdout: true).trim()
    def (major, minor, buildNo) = baseVersion.tokenize('.')*.toInteger()
    def branch = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
    def isHotfix = branch.startsWith('hotfix/')
    
    if (branch != 'main' && !isHotfix) {
        return "${baseVersion}-SNAPSHOT"
    }
    
    def tagPattern = isHotfix ? 'v\d+\.\d+\.\d+\.\d+' : 'v\d+\.\d+\.\d+'
    def lastTag = sh(script: "git for-each-ref --sort=-v:refname --count=1 --format='%(refname:short)' refs/tags/${tagPattern}", returnStdout: true).trim()
    
    buildNo = 1
    
    if (lastTag) {
        def versionParts = lastTag.replace('v', '').tokenize('.')*.toInteger()
        
        if (isHotfix) {
            if (versionParts[0..2] != [major, minor, buildNo]) {
                error("Hotfix version mismatch with latest release!")
            }
            versionParts[3]++
        } else {
            if (major > versionParts[0] || minor > versionParts[1]) {
                buildNo = 1
            } else {
                buildNo = versionParts[2] + 1
            }
        }
    }
    
    def newVersion = isHotfix ? "v${major}.${minor}.${buildNo}.${versionParts[3]}" : "v${major}.${minor}.${buildNo}"
    
    sh "git tag -a ${newVersion} -m 'Release ${newVersion}'"
    gitAskPass(gitCredentials, "git push origin ${newVersion}")

    env.VERSION = newVersion
}
