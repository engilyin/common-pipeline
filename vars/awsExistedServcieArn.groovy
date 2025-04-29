#!/usr/bin/env groovy

/**
 * awsExistedServcieArn - check whether ECS service exists
 *
 * Returns the ARN of the existed service
**/
def call(Map vars) {
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    boolean otherVersions = vars.get("otherVersions", false) 

    if (!serviceName) {
        error "serviceName must be provided"
    }

    // Extract servicePrefix by removing the version suffix
    String servicePrefix = serviceName.replaceFirst(/-v[0-9]+(-[0-9]+)*$/, '')

    def grepCommand = otherVersions ? "-v ${serviceName}" : "${serviceName}"

    return sh(script: """
        aws ecs list-services --cluster ${clusterName} --query "serviceArns[?contains(@, '${servicePrefix}-')]" --output text | awk '{for (i=1; i<=NF; i++) print \$i}' | grep ${grepCommand} || true
    """,  returnStdout: true).trim()
}