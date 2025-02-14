#!/usr/bin/env groovy

/**
 * awsExistedServcieArn - check whether ECS service exists
 *
 * Returns the ARN of the existed service
**/
def call(Map vars) {
    String clusterName = vars.get("clusterName", null)
    String servicePrefix = vars.get("servicePrefix", null)
    String serviceName = vars.get("serviceName", null)
    boolean otherVersions = vars.get("otherVersions", false) 

    def grepCommand = otherVersions ? "-v ${serviceName}" : "${serviceName}"

    return sh(script: """
        aws ecs list-services --cluster ${clusterName} --query "serviceArns[?contains(@, '${servicePrefix}-')]" --output text | tr ' ' '\\n' | grep ${grepCommand} || true
    """,  returnStdout: true).trim()
}