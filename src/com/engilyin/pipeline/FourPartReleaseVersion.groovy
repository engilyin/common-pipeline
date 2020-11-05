package com.engilyin.pipeline

class FourPartReleaseVersion extends ReleaseVersion {

    def versionStringFromMatch(def newVersion) {
        return newVersion[0] + '.' + newVersion[1] + '.' + newVersion[2] + '.' + newVersion[3]
    }

    def buildCommitMessagePattern() {
        def r = /build\s+(\d+\.\d+\.\d+\.\d+)/
        return r
    }
    
    def versionPartsFromString(String versionString) {
        return (versionString =~ /\S*(\d+)\.(\d+)\.(\d+)\.(\d+).*/).with {it ->  it.matches() ? versionPartsFromMatch(it, 1) : unknownVersion() }
    }

    def unknownVersion() {
        return [0, 0, 0, 0]
    }

    def versionPartsFromMatch(def match, int startIndex) {
        return [match[0][startIndex] as Integer, match[0][startIndex + 1] as Integer, match[0][startIndex + 2] as Integer, match[0][startIndex + 3] as Integer]
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
        gitVersion[2] = now.format("yy") as int
        gitVersion[3] = now.format("MMdd") as int

        return gitVersion
    }

    def getStringVersion(def newVersion) {
        def major = newVersion[0]
        def build = newVersion[1]
        def year = newVersion[2]
        def timestamp = String.format("%04d", newVersion[3])
        return "$major.$build.$year.$timestamp"
    }

}