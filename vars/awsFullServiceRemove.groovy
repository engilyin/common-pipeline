#!/usr/bin/env groovy

/**
 * awsFullServiceRemove - Full service removal
 *
**/
def call(Map vars) {
    String awsCredentials = vars.get("awsCredentials", null)
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String targetGroupArn = vars.get("targetGroupArn", null)


    withCredentials([[
            $class: 'AmazonWebServicesCredentialsBinding',
            credentialsId: awsCredentials,
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
        ]]) {

        echo "🔍 Finding the service..."
        def serviceArn = awsExistedServcieArn(clusterName: clusterName, serviceName: serviceName)
        echo "Found service: ${serviceArn}"

        if(!serviceArn.isEmpty()) {
            def foundServiceName = serviceArn.tokenize('/').last()

            awsDeregisterTasksFromTargetGroup clusterName: clusterName, serviceName: foundServiceName, targetGroupArn: targetGroupArn
            
            echo "🔴 Deleting the service: ${serviceArn}"
            awsCleanupFailedDeployment clusterName: clusterName, serviceName: foundServiceName

        }
    }
}