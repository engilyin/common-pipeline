#!/usr/bin/env groovy

/**
 * awsValidateServiceCreation - check whether ECS service has been created and clean up it in case of issues
 *
 * returns false if the service was not created well and has been cleaned up. Otherwise return true
**/
def call(Map vars) {
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String servicePrefix = vars.get("servicePrefix", null)
    boolean deploymentSuccess = vars.get("deploymentSuccess", false)
    int waitMins = vars.get("waitMins", 3)
    boolean noCleanupOnFailure = vars.get("noCleanupOnFailure", false)

    if (deploymentSuccess) {
        try {
            timeout(time: waitMins, unit: 'MINUTES') {
                echo "⏳ Waiting for service to stabilize..."
                sh "aws ecs wait services-stable --cluster ${clusterName} --services ${serviceName}"
            }
            echo "✅ Service is up and running!"
            return true
        } catch (Exception e) {
            echo "❌ Deployment failed during stabilization! Proceeding to cleanup..."
            if(noCleanupOnFailure) {
                echo "💤Skipping cleanup. Please clean it up manually (E.g. use awsFullServiceRemove)..."
            } else {
                awsCleanupFailedDeployment clusterName: clusterName, serviceName: serviceName, servicePrefix: servicePrefix
            }
            return false
        }
    } else {
        echo "❌ Service creation failed! Proceeding to cleanup..."
        if(noCleanupOnFailure) {
            echo "💤Skipping cleanup. Please clean it up manually (E.g. use awsFullServiceRemove)..."
        } else {
            awsCleanupFailedDeployment clusterName: clusterName, serviceName: serviceName, servicePrefix: servicePrefix
        }
        return false
    }
}