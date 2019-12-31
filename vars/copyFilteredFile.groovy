#!/usr/bin/env groovy

def call(Map vars) {
    String srcFile = vars.get('source', '')
    String destFile = vars.get('destination', srcFile)
    String pattern = vars.get('pattern', '')
    String replacement = vars.get('replacement', '')

    if(srcFile != '' && pattern != '') {
        echo "Copy $srcFile to $destFile replacing all occurences by pattern $pattern to $replacement"

        String fileContents = readFile(srcFile)
        String updatedFile = fileContents.replaceAll(pattern, replacement)
        writeFile file: destFile, text: updatedFile
    } else {
        echo "To do filtered file copying set at lease srcFile and pattern parameters"
    }
}