#!/usr/bin/env groovy

/**
 * awsFullServiceRemove - Full service removal
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

        echo "🔍 Finding the service..."
        def serviceArn = awsExistedServcieArn(clusterName: clusterName, servicePrefix: servicePrefix, serviceName: serviceName)
        echo "Found service: ${serviceArn}"

        if(!serviceArn.isEmpty()) {

            sh """
                echo "🔴 Deregistering ${serviceArn} service..."
                OLD_ENI_ID=\$(aws ecs describe-tasks --cluster ${clusterName} --tasks ${serviceArn} --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text || true)
                OLD_ENI_IP=\$(aws ec2 describe-network-interfaces --network-interface-ids \$OLD_ENI_ID --query "NetworkInterfaces[0].PrivateIpAddress" --output text || true)

                echo "OLD_ENI_IP = \$OLD_ENI_IP"

                if [[ ! -z "\$OLD_ENI_IP" ]]; then
                    echo "Deregistering from target group"
                    aws elbv2 deregister-targets --target-group-arn ${targetGroupArn} --targets Id=\$OLD_ENI_IP,Port=8080
                fi

            """
            
            echo "🔴 Deleting the service: ${serviceArn}"
            awsCleanupFailedDeployment clusterName: clusterName, serviceName: serviceArn.tokenize('/').last(), servicePrefix: servicePrefix

        }
    }
}