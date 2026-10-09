package com.example.core.nlp

object InputNormalizer {
    fun normalize(input: String?): String {
        if (input == null) return ""
        return input.trim()
            .lowercase()
            .replace(Regex("[\\p{Punct}&&[^%]]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
