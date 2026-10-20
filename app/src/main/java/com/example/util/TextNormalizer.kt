package com.example.util

import java.text.Normalizer
import java.util.Locale

object TextNormalizer {
    /**
     * Normalizes text for alarm dismissal validation:
     * 1. Strips diacritics / accents (ex: "não" -> "nao")
     * 2. Lowercases
     * 3. Removes punctuation symbols
     * 4. Collapses multiple whitespace
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val withoutAccents = normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutAccents
            .lowercase(Locale.ROOT)
            .replace("[^\\p{L}\\p{N}\\s]".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun isPhraseMatch(typed: String?, target: String?): Boolean {
        val normTyped = normalize(typed)
        val normTarget = normalize(target)
        if (normTarget.isEmpty()) return false
        return normTyped == normTarget
    }
}
