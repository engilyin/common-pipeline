#!/usr/bin/env groovy

/**
 * awsMakeServiceAliveInternal - For services not fronted by ALB (Cloud Map)
 * Finds older versions of the service and deletes them without touching load balancers.
 */
def call(Map vars) {
    String awsCredentials = vars.get("awsCredentials", null)
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)

    withCredentials([[
            $class: 'AmazonWebServicesCredentialsBinding',
            credentialsId: awsCredentials,
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
        ]]) {

        echo "🔍 Finding old service..."
        def oldServiceArn = awsExistedServcieArn(otherVersions: true, clusterName: clusterName, serviceName: serviceName)
        echo "Found the old service(s): ${oldServiceArn}"

        if(!oldServiceArn.isEmpty()) {
            def oldServiceName = oldServiceArn.tokenize('/').last()
            echo "🔴 Deleting old service: ${oldServiceArn}"
            awsCleanupFailedDeployment clusterName: clusterName, serviceName: oldServiceName
        } else {
            echo "⚠️No old service to clean up: ${oldServiceArn}"
        }
    }
}
