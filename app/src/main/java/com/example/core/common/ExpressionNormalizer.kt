package com.example.core.common

object ExpressionNormalizer {
    fun normalize(raw: String): String {
        return raw.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    /**
     * Extracts the sentence containing [targetWord] from [paragraphText],
     * falling back to the full paragraph trimmed to 280 chars if sentence boundaries are unclear.
     */
    fun extractSurroundingSentence(paragraphText: String, targetWord: String): String {
        val cleaned = paragraphText.trim()
        if (cleaned.isEmpty()) return ""
        val sentences = cleaned.split(Regex("(?<=[.!?])\\s+"))
        val match = sentences.firstOrNull { sentence ->
            sentence.contains(targetWord, ignoreCase = true)
        }
        return (match ?: cleaned).take(320).trim()
    }

    fun tokenizeWords(text: String): List<String> {
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    fun cleanSelectedWord(token: String): String {
        return token.trim { !it.isLetterOrDigit() && it != '\'' && it != '-' }
    }
}
