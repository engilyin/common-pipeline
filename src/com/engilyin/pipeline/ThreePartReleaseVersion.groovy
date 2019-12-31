package com.engilyin.pipeline

class ThreePartReleaseVersion extends ReleaseVersion {

    def versionStringFromMatch(def newVersion) {
        return newVersion[0] + '.' + newVersion[1] + '.' + newVersion[2]
    }

    def buildCommitMessagePattern() {
        def r = /build\s+(\d+\.\d+\.\d+)/
        return r
    }
    
    def versionPartsFromString(String versionString) {
        return (versionString =~ /\S*(\d+)\.(\d+)\.(\d+).*/).with {it ->  it.matches() ? versionPartsFromMatch(it, 1) : unknownVersion() }
    }

    def unknownVersion() {
        return [0, 0, 0]
    }

    def versionPartsFromMatch(def match, int startIndex) {
        return [match[0][startIndex] as Integer, match[0][startIndex + 1] as Integer, match[0][startIndex + 2] as Integer]
    }

    def buildNewVersion(String latestRelease) {
        def sourcesVersion = extractCurrentSourceVersion()
        def gitVersion = versionParts(latestRelease)

        steps.echo "sourcesVersion=$sourcesVersion gitVersion=$gitVersion"
        if(sourcesVersion[0] == 0 && sourcesVersion[1] == 0) {
            steps.echo "The version was never set on this repo. Setting it to 1.1"
            sourcesVersion[0] = 1
            sourcesVersion[1] = 1
        }

        if(sourcesVersion[0] > gitVersion[0]) {
            gitVersion[0] = sourcesVersion[0]
        }

        if(sourcesVersion[1] > gitVersion[1]) {
            gitVersion[1] = sourcesVersion[1]
        } else {
            gitVersion[1] = gitVersion[1] + 1
        }

        def now = new Date()
        gitVersion[2] = now.format("yyMMdd")

        return gitVersion
    }

    def getStringVersion(def newVersion) {
        def major = newVersion[0]
        def build = newVersion[1]
        def timestamp = newVersion[2]
        return "$major.$build.$timestamp"
    }

}