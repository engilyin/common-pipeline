#!/usr/bin/env groovy

def call() {

    def status = "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' Url: ${env.BUILD_URL}"
    print(status)
    
    return status

}