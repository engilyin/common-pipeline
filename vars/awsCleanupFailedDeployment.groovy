#!/usr/bin/env groovy

/**
 * awsCleanupFailedDeployment - cleanup ASW ECS service and its related resources
 *
**/
def call(Map vars) {
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String servicePrefix = vars.get("servicePrefix", null)

    echo "🧹 Starting cleanup process..."

    def existedServiceArn = awsExistedServcieArn(clusterName: clusterName, servicePrefix: servicePrefix, serviceName: serviceName)
    def latestTaskDefinition = sh(
        script: """
            aws ecs list-task-definitions --family-prefix ${serviceName} --sort DESC --query "taskDefinitionArns[0]" --output text || echo ""
        """,
        returnStdout: true
    ).trim()

    if (existedServiceArn.isEmpty() && latestTaskDefinition.isEmpty()) {
        echo "✅ No service or task definition found. Cleanup not needed."
    } else {
        // Remove Auto Scaling Policies
        echo "🔴 Checking and removing auto-scaling policies..."
        def scalingPolicies = sh(
            script: """
                aws application-autoscaling describe-scaling-policies \
                    --service-namespace ecs --resource-id service/${clusterName}/${serviceName} \
                    --query "ScalingPolicies[*].PolicyName" --output text || echo ""
            """,
            returnStdout: true
        ).trim().split("\\s+")

        if (scalingPolicies.size() > 0 && scalingPolicies[0]) {
            scalingPolicies.each { policyName ->
                echo "🛑 Removing scaling policy: ${policyName}"
                sh "aws application-autoscaling delete-scaling-policy \
                    --service-namespace ecs --resource-id service/${clusterName}/${serviceName} \
                    --policy-name ${policyName} || true"
            }
        } else {
            echo "⚠️ No scaling policies found for service ${serviceName}."
        }

        // Delete Auto Scaling Target
        echo "🔴 Checking and removing auto-scaling target..."
        try {
            sh "aws application-autoscaling deregister-scalable-target \
                --service-namespace ecs --resource-id service/${clusterName}/${serviceName} \
                --scalable-dimension ecs:service:DesiredCount || true"
            echo "✅ Auto-scaling target removed."
        } catch (Exception e) {
            echo "⚠️ Failed to remove auto-scaling target: ${e.getMessage()}."
        }


        // Proceed with existing ECS service deletion
        if (!existedServiceArn.isEmpty()) {
            echo "🔴 Service exists. Proceeding with deletion..."
            try {
                sh "aws ecs update-service --cluster ${clusterName} --service ${serviceName} --desired-count 0 || true"
                sh "aws ecs delete-service --cluster ${clusterName} --service ${serviceName} --force || true"
                echo "✅ ECS service deleted successfully."
            } catch (Exception e) {
                echo "⚠️ Service deletion failed: ${e.getMessage()}. Proceeding with task definition cleanup."
            }
        } else {
            echo "⚠️ Service was never created or already deleted."
        }

        // Deregister Task Definition
        if (!latestTaskDefinition.isEmpty()) {
            echo "🔴 Task definition exists. Proceeding with cleanup..."
            try {
                sh "aws ecs deregister-task-definition --task-definition ${latestTaskDefinition} || true"
                echo "✅ Task definition deregistered successfully."
            } catch (Exception e) {
                echo "⚠️ Task definition cleanup failed: ${e.getMessage()}."
            }
        } else {
            echo "⚠️ No task definition found. Skipping deregistration."
        }
    }

    echo "🧹 Cleanup process completed."
}