#!/usr/bin/env groovy

/**
 * awsServiceRestart - Restart service. E.g. to apply the new params
 *
**/
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

        echo "🚀 Restarting ECS Service ${serviceName}..."
        sh """  
            aws ecs update-service --cluster ${clusterName} --service ${serviceName} --force-new-deployment
        """

        if (!awsValidateServiceCreation(deploymentSuccess: true,
                    clusterName: clusterName,
                    serviceName: serviceName,
                    noCleanupOnFailure: false)) {
                echo "❌ ECS Service restart failed!"
                return false
        }

        echo "✅ Service ${serviceName} is successfully restarted"
    }
}