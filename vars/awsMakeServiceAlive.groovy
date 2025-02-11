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

            sh """
                echo "🔴 Deregistering old service..."

                OLD_ENI_ID=\$(aws ecs describe-tasks --cluster ${clusterName} --tasks ${oldServiceArn} --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text || true)
                echo "OLD_ENI_ID = \$OLD_ENI_ID"
                
                OLD_ENI_IP=\$(aws ec2 describe-network-interfaces --network-interface-ids \$OLD_ENI_ID --query "NetworkInterfaces[0].PrivateIpAddress" --output text || true)
                echo "OLD_ENI_IP = \$OLD_ENI_IP"

                echo "TG=${targetGroupArn}"

                if [[ ! -z "\$OLD_ENI_IP" ]]; then
                    aws elbv2 deregister-targets --target-group-arn ${targetGroupArn} --targets Id=\$OLD_ENI_IP,Port=8080
                fi

            """
            
            echo "🔴 Deleting old service: ${oldServiceArn}"
            awsCleanupFailedDeployment clusterName: clusterName, serviceName: oldServiceArn.tokenize('/').last(), servicePrefix: servicePrefix

        } else {
            echo "⚠️No old service to clean up: ${oldServiceArn}"
        }
    }
}