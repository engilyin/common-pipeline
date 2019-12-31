/**
 * Calculate and ensure we have the right version number for further processing
 *
**/
def call(Map vars) {
    
    def projectPath = vars.get('gitRepoPath', '.')

    def releaseVersion = com.engilyin.pipeline.ReleaseVersionFactory.create(vars)

    releaseVersion.init(this, vars)

    def release = vars.get("release", false)

    if(release) {
        return releaseVersion.lastRelease(projectPath)
    } else {
        return releaseVersion.getFeatureBranchVersion()
    }
}