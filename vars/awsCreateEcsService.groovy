#!/usr/bin/env groovy

/**
 * awsCreateEcsService - creates the new ECS service
 *
**/
def call(Map vars) {
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String servicePrefix = vars.get("servicePrefix", null)
    String deployedVersion = vars.get("deployedVersion", null)
    String subnets = vars.get("subnets", null)
    String securityGroups  = vars.get("securityGroups", null)
    String appImage = vars.get("appImage", null)
    String envir = vars.get("envir", null)
    String baseTaskJson = vars.get("baseTaskJson", null)
    boolean noCleanupOnFailure = vars.get("noCleanupOnFailure", false)

    try {
        echo "📄 Preparing ECS Task Definition..."
        
        def taskJson = sh(
            script: """
                echo '${baseTaskJson}' | jq '
                    .family = "${serviceName}" |
                    .containerDefinitions[0].image = "${appImage}:${deployedVersion}" |
                    .containerDefinitions[0].environment |= map(if .name == "APP_ENV" then .value = "${envir}" else . end)
                '
            """,
            returnStdout: true
        ).trim()

        echo "📄 Updated Task Definition:\n${taskJson}"

        echo "🚀 Registering ECS Task Definition..."
        sh """
            aws ecs register-task-definition --cli-input-json '${taskJson}'
        """

        echo "🔧 Creating ECS Service..."
        sh """
            aws ecs create-service --cluster ${clusterName} --service-name ${serviceName} \
                --task-definition ${serviceName} --desired-count 1 \
                --network-configuration "awsvpcConfiguration={subnets=[${subnets}],securityGroups=[${securityGroups}],assignPublicIp=DISABLED}"
        """
        return true
    } catch (Exception e) {
        echo "❌ Deployment failed: ${e.getMessage()}"
        if(noCleanupOnFailure) {
            echo "💤Skipping cleanup. Please clean it up manually (E.g. use awsFullServiceRemove)..."
        } else {
            echo "🧹 Triggering cleanup..."
            awsCleanupFailedDeployment clusterName: clusterName, serviceName: serviceName, servicePrefix: servicePrefix
        }
        return false
    }

}