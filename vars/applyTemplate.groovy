#!/usr/bin/env groovy

/**
 * Apply the values from the map over the template
 *
**/
def call(String text, Map<String, String> vars) {
    if (!text) return ''
    if (!vars) return text

    def pattern = /\$\{([^}]+)\}/

    return text.replaceAll(pattern) { fullMatch ->
        def key = fullMatch[1]
        if (!vars.containsKey(key)) {
         error "Missing placeholder: ${key}"
        }
        vars[key]
    }
}