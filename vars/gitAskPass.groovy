#!/usr/bin/env groovy

@NonCPS
String generateTempLocation(String path) {
  String tmpDir = pwd tmp: true
  return tmpDir + '/' + path
}

String createGitAskPassScript() {
  String destPath = generateTempLocation('git_ask_pass.sh')

  String scriptBody = '''#!/bin/sh
                         case "$1" in
                         Username*) echo $GIT_USERNAME ;;
                         Password*) echo $GIT_PASSWORD ;;
                         esac
                      '''

  writeFile file: destPath, text: scriptBody
  echo "createGitAskPassScript: created at ${destPath}"
  return destPath
}

def call(credentialsId, gitCommand) {
    String gitAskPassScriptPath = createGitAskPassScript()
    echo "Prepare git ask pass script at $gitAskPassScriptPath"

    sh("chmod +x $gitAskPassScriptPath")
    def output = ""
    withCredentials([usernamePassword(credentialsId: credentialsId, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {            
        output = sh(script: "GIT_ASKPASS=$gitAskPassScriptPath $gitCommand", returnStdout: true).trim()
    }
    sh("rm $gitAskPassScriptPath")

    return output
}