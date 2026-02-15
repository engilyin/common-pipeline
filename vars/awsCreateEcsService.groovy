#!/usr/bin/env groovy

/**
 * awsCreateEcsService - creates the new ECS service
 *
**/
def call(Map vars) {
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String subnets = vars.get("subnets", null)
    String securityGroups  = vars.get("securityGroups", null)
    String taskJson = vars.get("taskJson", null)
    boolean noCleanupOnFailure = vars.get("noCleanupOnFailure", false)
    String assignPublicIp = vars.get("assignPublicIp", false) ? 'ENABLED' : 'DISABLED' 
    int minCapacity = vars.get("minCapacity", 1) // Minimum number of tasks
    int maxCapacity = vars.get("maxCapacity", 10) // Maximum number of tasks
    int cpuTarget = vars.get("cpuTarget", 80) // Target CPU utilization percentage
    int memTarget = vars.get("memTarget", 75) // Target Memory utilization percentage   
    // fargateType controls Fargate vs Fargate Spot behaviour. Expected values: FARGATE or FARGATE_SPOT
    String fargateType = (vars.get("fargateType") ?: 'FARGATE_SPOT').toString()

    try {

        echo "📄 Updated Task Definition:\n${taskJson}"

        echo "🚀 Registering ECS Task Definition..."
        sh """
            aws ecs register-task-definition --cli-input-json '${taskJson}'
        """

        echo "🔧 Creating ECS Service..."

        // validate fargateType
        if(!(fargateType in ['FARGATE','FARGATE_SPOT'])) {
            error "Unsupported fargateType: ${fargateType}. Allowed: FARGATE, FARGATE_SPOT"
        }

        def launchTypeFlag = ''
        def capacityProviderFlag = ''

        echo "Selected fargateType: ${fargateType}"

        if(fargateType == 'FARGATE') {
            capacityProviderFlag = "--launch-type FARGATE"
        } else {
            // use capacity provider strategy for Spot
            capacityProviderFlag = "--capacity-provider-strategy capacityProvider=FARGATE_SPOT,weight=1"
        }

        sh """
            aws ecs create-service --cluster ${clusterName} --service-name ${serviceName} ${launchTypeFlag} ${capacityProviderFlag} \
                --task-definition ${serviceName} --desired-count ${minCapacity} \
                --network-configuration "awsvpcConfiguration={subnets=[${subnets}],securityGroups=[${securityGroups}],assignPublicIp=${assignPublicIp}}" \
                --scheduling-strategy REPLICA
        """

        echo "📈 Setting up Auto Scaling..."
        sh """
            aws application-autoscaling register-scalable-target \
                --service-namespace ecs \
                --scalable-dimension ecs:service:DesiredCount \
                --resource-id service/${clusterName}/${serviceName} \
                --min-capacity ${minCapacity} --max-capacity ${maxCapacity}
        """

        sh """
            aws application-autoscaling put-scaling-policy \
                --service-namespace ecs \
                --scalable-dimension ecs:service:DesiredCount \
                --resource-id service/${clusterName}/${serviceName} \
                --policy-name ${serviceName}-cpu-scaling \
                --policy-type TargetTrackingScaling \
                --target-tracking-scaling-policy-configuration '{
                    "TargetValue": ${cpuTarget},
                    "PredefinedMetricSpecification": {
                        "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
                    },
                    "ScaleInCooldown": 120,
                    "ScaleOutCooldown": 60
                }'
        """

        sh """
            aws application-autoscaling put-scaling-policy \
                --service-namespace ecs \
                --scalable-dimension ecs:service:DesiredCount \
                --resource-id service/${clusterName}/${serviceName} \
                --policy-name ${serviceName}-memory-scaling \
                --policy-type TargetTrackingScaling \
                --target-tracking-scaling-policy-configuration '{
                    "TargetValue": ${memTarget},
                    "PredefinedMetricSpecification": {
                        "PredefinedMetricType": "ECSServiceAverageMemoryUtilization"
                    },
                    "ScaleInCooldown": 120,
                    "ScaleOutCooldown": 60
                }'
        """
        
        echo "✅ ECS Service and Auto Scaling policies created successfully!"
        return true
    } catch (Exception e) {
        echo "❌ Deployment failed: ${e.getMessage()}"
        if(noCleanupOnFailure) {
            echo "💤Skipping cleanup. Please clean it up manually (E.g. use awsFullServiceRemove)..."
        } else {
            echo "🧹 Triggering cleanup..."
            awsCleanupFailedDeployment clusterName: clusterName, serviceName: serviceName
        }
        return false
    }

}