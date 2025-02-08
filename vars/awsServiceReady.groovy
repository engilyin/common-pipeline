#!/usr/bin/env groovy

/**
 * awsServiceReady - wait when ECS service will be ready
 *
**/
def call(Map vars) {
    String awsCredentials = vars.get("awsCredentials", null)
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String servicePrefix = vars.get("servicePrefix", null)
    String checkLogStarts = vars.get("checkLogStarts", null)
    String checkLogsSince = vars.get("checkLogsSince", '5m')
    int waitMins = vars.get("waitMins", 3)


    withCredentials([[
        $class: 'AmazonWebServicesCredentialsBinding',
        credentialsId: awsCredentials,
        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
    ]]) {
        try {
            timeout(time: waitMins, unit: 'MINUTES') {
                echo "🔍 Checking application logs from CloudWatch..."
                sh """
                    aws logs tail ${servicePrefix} --since ${checkLogsSince} | grep "${checkLogStarts}"
                """
            }

            echo "✅ Service ${serviceName} is ready"
            return true
        } catch (Exception e) {
            echo "❌ Application failed to start within timeout!"
            return false
        }
    }

}