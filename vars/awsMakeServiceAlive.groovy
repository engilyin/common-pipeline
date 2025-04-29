#!/usr/bin/env groovy

/**
 * awsMakeServiceAlive - Makes the service alive and clean up the older version
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

        echo "🔍 Finding old service..."
        def oldServiceArn = awsExistedServcieArn(otherVersions: true, clusterName: clusterName, serviceName: serviceName)
        echo "Found the old service(s): ${oldServiceArn}"

        echo "📄 Updating ECS Service to use the Target Group..."
        sh """
            aws ecs update-service --cluster ${clusterName} --service ${serviceName} --load-balancers targetGroupArn=${targetGroupArn},containerName=${serviceName},containerPort=8080
        """

        if(!oldServiceArn.isEmpty()) {
            def oldServiceName = oldServiceArn.tokenize('/').last()
            
            echo "🔴 Deleting old service: ${oldServiceArn}"
            awsCleanupFailedDeployment clusterName: clusterName, serviceName: oldServiceName

        } else {
            echo "⚠️No old service to clean up: ${oldServiceArn}"
        }
    }
}