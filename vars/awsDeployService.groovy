#!/usr/bin/env groovy

/**
 * awsDeployService - deploy new version of the ECS service
 *
**/
def call(Map vars) {
    String awsCredentials = vars.get("awsCredentials", null)
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String taskJson = vars.get("taskJson", null)
    String deployedVersion = vars.get("deployedVersion", null)
    String subnets = vars.get("subnets", null)
    String securityGroups = vars.get("securityGroups", null)
    String envir = vars.get("envir", null)
    boolean assignPublicIp = vars.get("assignPublicIp", false)
    boolean noCleanupOnFailure = vars.get("noCleanupOnFailure", false)

    withCredentials([[
        $class: 'AmazonWebServicesCredentialsBinding',
        credentialsId: awsCredentials,
        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
    ]]) {
        echo "🚀 Starting deployment process..."

        def existedServiceArn = awsExistedServcieArn(clusterName: clusterName, serviceName: serviceName)

        if (existedServiceArn.isEmpty()) {
            
            def success = awsCreateEcsService(
                clusterName: clusterName,
                serviceName: serviceName,
                deployedVersion: deployedVersion,
                subnets: subnets,
                securityGroups: securityGroups,
                envir: envir,
                taskJson: taskJson,
                assignPublicIp: assignPublicIp,
                noCleanupOnFailure: noCleanupOnFailure
            )

            if (!awsValidateServiceCreation(deploymentSuccess: success,
                    clusterName: clusterName,
                    serviceName: serviceName,
                    noCleanupOnFailure: noCleanupOnFailure)) {
                echo "❌ ECS Service deployment failed!"
                return false
            }

            echo "✅ Service ${serviceName} is successfully created"

            return true
        } else {
            error "❌ Service ${serviceName} already exists. Skipping deployment."
            retrun false
        }
    }
}
