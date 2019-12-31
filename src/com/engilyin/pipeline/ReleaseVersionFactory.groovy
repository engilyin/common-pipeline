package com.engilyin.pipeline

class ReleaseVersionFactory {
    
    static ReleaseVersion create(Map vars) {
        def versionFormat = vars.get("versionFormat", '3') 
        return createReleaseVersion(versionFormat)
    }

    static ReleaseVersion createReleaseVersion(String versionFormat) {
        if(versionFormat == '4') {
            return new com.engilyin.pipeline.FourPartReleaseVersion()
        } else {
            return new com.engilyin.pipeline.ThreePartReleaseVersion()
        }
    }
}