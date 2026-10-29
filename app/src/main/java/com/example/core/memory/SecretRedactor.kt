package com.example.core.memory

/** Redacts common provider credentials before text is persisted in local memory or conversation history. */
object SecretRedactor {
    private val patterns = listOf(
        Regex("(?i)\\bsk-(?:ant-|or-v1-)?[A-Za-z0-9_-]{16,}\\b"),
        Regex("(?i)\\bAIza[0-9A-Za-z_-]{20,}\\b"),
        Regex("(?i)\\bgsk_[A-Za-z0-9_-]{16,}\\b"),
        Regex("(?i)\\bhf_[A-Za-z0-9]{16,}\\b"),
        Regex("(?i)\\bxai-[A-Za-z0-9_-]{16,}\\b"),
        Regex("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]{16,}"),
        Regex("(?i)(\\b(?:api[_-]?key|access[_-]?token|secret[_-]?key)\\b\\s*[:=]\\s*)['\\\"]?([A-Za-z0-9._~+/=-]{12,})['\\\"]?")
    )

    fun redact(text: String): String {
        var result = text
        patterns.forEachIndexed { index, pattern ->
            result = if (index == patterns.lastIndex) {
                pattern.replace(result) { match -> match.groupValues[1] + "[REDACTED_SECRET]" }
            } else {
                pattern.replace(result, "[REDACTED_SECRET]")
            }
        }
        return result
    }
}