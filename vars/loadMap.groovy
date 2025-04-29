#!/usr/bin/env groovy

/**
 * Loads the Map<String, String> from the file
 *
**/
def call(String content) {
    return content
        .trim()
        .split('\n')
        .findAll { it && !it.startsWith('#') }
        .collectEntries { line ->
            def (key, value) = line.split('=', 2)
            [(key.trim()): value.trim()]
        }
}