#!/usr/bin/env groovy

/**
 * awsMakeServiceAlive - Makes the service alive and clean up the older version
 *
**/
def call(Map vars) {
    String awsCredentials = vars.get("awsCredentials", null)
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String servicePrefix = vars.get("servicePrefix", null)
    String targetGroupArn = vars.get("targetGroupArn", null)


    withCredentials([[
            $class: 'AmazonWebServicesCredentialsBinding',
            credentialsId: awsCredentials,
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
        ]]) {

        echo "🔍 Finding old service..."
        def oldServiceArn = awsExistedServcieArn(otherVersions: true, clusterName: clusterName, servicePrefix: servicePrefix, serviceName: serviceName)
        echo "Found the old service(s): ${oldServiceArn}"

        sh """
            echo "📄 Registering new service to Target Group..."
            NEW_TASK_ID=\$(aws ecs list-tasks --cluster ${clusterName} --service-name ${serviceName} --query "taskArns[0]" --output text)
            ENI_ID=\$(aws ecs describe-tasks --cluster ${clusterName} --tasks \$NEW_TASK_ID --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text)
            ENI_IP=\$(aws ec2 describe-network-interfaces --network-interface-ids \$ENI_ID --query "NetworkInterfaces[0].PrivateIpAddress" --output text)

            aws elbv2 register-targets --target-group-arn ${targetGroupArn} --targets Id=\$ENI_IP,Port=8080
        """

        if(!oldServiceArn.isEmpty()) {
            def oldServiceName = oldServiceArn.tokenize('/').last()

            awsDeregisterTasksFromTargetGroup clusterName: clusterName, serviceName: oldServiceName, targetGroupArn: targetGroupArn
            
            echo "🔴 Deleting old service: ${oldServiceArn}"
            awsCleanupFailedDeployment clusterName: clusterName, serviceName: oldServiceName, servicePrefix: servicePrefix

        } else {
            echo "⚠️No old service to clean up: ${oldServiceArn}"
        }
    }
}