#!/usr/bin/env groovy

/**
 * awsDeployService - deploy new version of the ECS service
 *
**/
def call(Map vars) {
    String awsCredentials = vars.get("awsCredentials", null)
    String clusterName = vars.get("clusterName", null)
    String serviceName = vars.get("serviceName", null)
    String servicePrefix = vars.get("servicePrefix", null)
    String deployedVersion = vars.get("deployedVersion", null)
    String subnets = vars.get("subnets", null)
    String securityGroups = vars.get("securityGroups", null)
    String appImage = vars.get("appImage", null)
    String envir = vars.get("envir", null)

    withCredentials([[
        $class: 'AmazonWebServicesCredentialsBinding',
        credentialsId: awsCredentials,
        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
    ]]) {
        echo "🚀 Starting deployment process..."

        def existedServiceArn = awsServcieExists(clusterName: clusterName, servicePrefix: servicePrefix, serviceName: serviceName)

        if (existedServiceArn.isEmpty()) {
            
            String baseTaskJson = readFile('base-task.json').trim()

            def success = awsCreateEcsService(
                clusterName: clusterName,
                serviceName: serviceName,
                servicePrefix: servicePrefix,
                deployedVersion: deployedVersion,
                subnets: subnets,
                securityGroups: securityGroups,
                appImage: appImage,
                envir: envir,
                baseTaskJson: baseTaskJson
            )

            if (!awsValidateServiceCreation(deploymentSuccess: success,
                    clusterName: clusterName,
                    serviceName: serviceName,
                    servicePrefix: servicePrefix)) {
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
