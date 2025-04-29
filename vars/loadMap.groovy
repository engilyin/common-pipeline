#!/usr/bin/env groovy

/**
 * Loads the Map<String, String> from the file
 *
**/
def call(Map vars) {

    def filename = vars.get('filename', '')

    Map<String, String> result = readFile(filename)
        .split('\n')
        .findAll { it && !it.startsWith('#') }  // skip empty lines and comments
        .collectEntries { line ->
            def (key, value) = line.split('=', 2)
            [(key.trim()): value.trim()]
        }

    return result
}