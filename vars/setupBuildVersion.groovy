#!/usr/bin/env groovy

/**
 * Calculate and ensure we have the right version number for further processing
 *
**/
def call(Map vars) {

    def releaseVersion = com.engilyin.pipeline.ReleaseVersionFactory.create(vars)

    releaseVersion.init(this, vars)
    releaseVersion.setup()
}

