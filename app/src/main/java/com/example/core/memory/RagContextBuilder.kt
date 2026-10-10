package com.example.core.memory

/** Small, deterministic local-first lexical retrieval. Embedding/vector retrieval can be layered on later. */
object RagContextBuilder {
    private val stopWords = setOf("the", "a", "an", "is", "are", "was", "were", "to", "of", "in", "on", "for", "and", "or", "it", "i", "me", "my", "you", "your", "what", "when", "where", "how", "do", "does", "did", "about", "please", "jarvis", "remember", "tell")

    data class Snippet(val title: String, val content: String, val source: String, val timestamp: Long)

    fun build(query: String, snippets: List<Snippet>, maxItems: Int = 6, maxChars: Int = 3200): String {
        val queryTokens = tokens(query).filterNot { it in stopWords }.toSet()
        if (queryTokens.isEmpty() || snippets.isEmpty()) return ""
        val ranked = snippets.mapNotNull { item ->
            val contentTokens = tokens(item.title + " " + item.content).toSet()
            val overlap = queryTokens.intersect(contentTokens).size
            val phraseBonus = if ((item.title + " " + item.content).contains(query.trim(), ignoreCase = true) && query.isNotBlank()) 3 else 0
            val score = overlap + phraseBonus
            if (score == 0) null else item to score
        }.sortedWith(compareByDescending<Pair<Snippet, Int>> { it.second }.thenByDescending { it.first.timestamp })
        val output = StringBuilder()
        for ((item, score) in ranked.take(maxItems)) {
            val block = "[${item.source}; relevance=$score; title=${item.title}]\n${item.content.trim().take(900)}\n"
            if (output.length + block.length > maxChars) break
            output.append(block).append('\n')
        }
        return output.toString().trim()
    }

    private fun tokens(text: String): List<String> = Regex("[\\p{L}\\p{N}]{2,}").findAll(text.lowercase()).map { it.value }.toList()
}
