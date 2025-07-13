#!/usr/bin/env groovy

/**
 * Bumps the current gradle project version and save it as tag if it is release
 * 
 *
**/
def call(Map args = [:]) {
    def gitCredentials = args.gitCredentials
    def versionTagPrefix = args.versionTagPrefix ?: "v"
    def release = args.release.toBoolean()

    def baseVersion = readFile('version.txt').trim() // e.g., "2.1.0"
    def (baseMajor, baseMinor, basePatch) = baseVersion.tokenize('.').collect { it.toInteger() }

    if (!release) {
        // Development build — bump patch without tagging
        def bumpedVersion = "${baseMajor}.${baseMinor}.${basePatch + 1}"
        echo "⚙️ Development mode — using bumped version: ${bumpedVersion}"
        env.VERSION = bumpedVersion
        writeFile file: 'defined-version.txt', text: bumpedVersion

        return bumpedVersion
    }

    echo "🚀 Production mode — generating version tag"

    def tagPrefix = "${versionTagPrefix}${baseMajor}.${baseMinor}."

    // Fetch tags
    gitAskPass(gitCredentials, "git fetch --tags --quiet")

    def lastTag = sh(
        script: "git tag --list '${tagPrefix}*' --sort=-v:refname | head -n1",
        returnStdout: true
    ).trim()

    int buildNumber
    if (lastTag) {
        echo "🕵️ Last matching tag: ${lastTag}"
        def lastBuild = lastTag.replace(tagPrefix, "")
        buildNumber = lastBuild.toInteger() + 1
    } else {
        echo "⚠️ No tags matching ${tagPrefix} — fallback to base version. Using patch from version.txt: ${basePatch}"
        buildNumber = basePatch + 1
    }

    def fullVersion = "${baseMajor}.${baseMinor}.${buildNumber}"
    def versionTag = "${versionTagPrefix}${fullVersion}"

    echo "🏷️ Tagging commit as: ${versionTag}"
    sh "git tag -a ${versionTag} -m \"Release ${versionTag}\""
    gitAskPass(gitCredentials, "git push origin ${versionTag}")

    writeFile file: 'defined-version.txt', text: fullVersion
    env.VERSION = fullVersion

    return newVersion
}