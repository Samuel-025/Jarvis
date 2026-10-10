package com.example.core.memory

import kotlin.math.ln

/**
 * Deterministic on-device hybrid lexical retrieval: weighted title/body term frequency,
 * inverse document frequency, exact phrase boost, and a small recency tie-breaker.
 * No embeddings, network calls, or third-party index are required.
 */
object RagContextBuilder {
    private val stopWords = setOf(
        "the", "a", "an", "is", "are", "was", "were", "to", "of", "in", "on", "for",
        "and", "or", "it", "i", "me", "my", "you", "your", "what", "when", "where",
        "how", "do", "does", "did", "about", "please", "jarvis", "remember", "tell",
        "का", "की", "के", "है", "था", "थे", "और", "में", "मेरा", "मेरी", "क्या", "कैसे"
    )

    data class Snippet(val title: String, val content: String, val source: String, val timestamp: Long)

    fun build(query: String, snippets: List<Snippet>, maxItems: Int = 6, maxChars: Int = 3200): String {
        val queryTerms = tokens(query).filterNot { it in stopWords }.distinct()
        if (queryTerms.isEmpty() || snippets.isEmpty() || maxItems <= 0 || maxChars <= 0) return ""

        val documents = snippets.map { snippet ->
            Document(snippet, tokens(snippet.title), tokens(snippet.content))
        }
        val now = documents.maxOfOrNull { it.snippet.timestamp } ?: 0L
        val ranked = documents.mapNotNull { document ->
            var score = 0.0
            queryTerms.forEach { term ->
                val titleFrequency = document.titleTokens.count { it == term }
                val bodyFrequency = document.bodyTokens.count { it == term }
                val documentFrequency = documents.count { doc ->
                    term in doc.titleTokens || term in doc.bodyTokens
                }
                if (titleFrequency + bodyFrequency > 0) {
                    val idf = ln(1.0 + (documents.size - documentFrequency + 0.5) / (documentFrequency + 0.5))
                    val weightedFrequency = titleFrequency * 2.5 + bodyFrequency
                    score += idf * weightedFrequency / (1.0 + 0.35 * weightedFrequency)
                }
            }
            val fullText = document.snippet.title + " " + document.snippet.content
            if (query.isNotBlank() && fullText.contains(query.trim(), ignoreCase = true)) score += 3.0
            if (score <= 0.0) return@mapNotNull null
            val age = (now - document.snippet.timestamp).coerceAtLeast(0L)
            val recency = if (now > 0L) 0.15 / (1.0 + age.toDouble() / (30L * 24 * 60 * 60 * 1000)) else 0.0
            document to (score + recency)
        }.sortedWith(compareByDescending<Pair<Document, Double>> { it.second }.thenByDescending { it.first.snippet.timestamp })

        val output = StringBuilder()
        for ((document, score) in ranked.take(maxItems)) {
            val item = document.snippet
            val block = "[${item.source}; relevance=${"%.2f".format(java.util.Locale.ROOT, score)}; title=${item.title.take(120)}]\n${item.content.trim().take(900)}\n"
            if (output.length + block.length > maxChars) break
            output.append(block).append('\n')
        }
        return output.toString().trim()
    }

    private data class Document(val snippet: Snippet, val titleTokens: List<String>, val bodyTokens: List<String>)

    private fun tokens(text: String): List<String> =
        Regex("[\\p{L}\\p{N}]{2,}").findAll(text.lowercase()).map { it.value }.toList()
}
