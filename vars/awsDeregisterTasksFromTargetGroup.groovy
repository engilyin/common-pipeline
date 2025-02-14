#!/usr/bin/env groovy

/**
 * awsDeregisterTasksFromTargetGroup - Deregister service tasks from target group
 *
**/
def call(Map vars) {
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String targetGroupArn = vars.get("targetGroupArn", null)

    sh """
        echo "🔴 Deregistering ${serviceName} service..."

        ARN_TASKS=\$(aws ecs list-tasks --cluster ${clusterName} --service-name ${serviceName} --query "taskArns[0]" --output text )

        for task_arn in \$ARN_TASKS; do
            echo "Processing task: \$task_arn"
        
            # Get the network interface ID
            network_interface_id=\$(aws ecs describe-tasks --cluster ${clusterName} --tasks \$task_arn --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text)
        
            if [ -z "\$network_interface_id" ] || [ "\$network_interface_id" == "None" ]; then
                echo "No network interface found for task: \$task_arn"
                continue
            fi
        
            # Get the private IP address
            private_ip=\$(aws ec2 describe-network-interfaces --network-interface-ids \$network_interface_id --query "NetworkInterfaces[0].PrivateIpAddresses[0].PrivateIpAddress" --output text)
        
            if [ -z "\$private_ip" ] || [ "\$private_ip" == "None" ]; then
                echo "No private IP found for task: \$task_arn"
                continue
            fi
        
            echo "Deregistering target: \$private_ip from Target Group: $targetGroupArn"
            aws elbv2 deregister-targets --target-group-arn $targetGroupArn --targets Id=\$private_ip

        done

    """

}