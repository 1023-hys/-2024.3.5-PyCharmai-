package dev.localai.pycharmassistant.actions

internal object CompletionText {
    fun buildCompletionPrompt(
        promptTemplate: String,
        contextBefore: String,
        contextAfter: String,
        selectedText: String?,
        fullFileText: String?
    ): String = buildString {
        append(promptTemplate)
        append("\n\nContinue the code at the cursor. Return only the text to insert; do not repeat the existing prefix or add markdown fences.")
        fullFileText?.let {
            append("\n\nComplete current file:\n")
            append(it)
        }
        selectedText?.let {
            append("\n\nSelected code to reference:\n")
            append(it)
        }
        append("\n\nCode before cursor:\n")
        append(contextBefore)
        append("\n<CURSOR>\nCode after cursor:\n")
        append(contextAfter)
    }

    fun mergePrefix(prefix: String, suggestion: String): String =
        if (prefix.isNotEmpty() && suggestion.startsWith(prefix)) suggestion else prefix + suggestion

    fun normalizeSuggestion(contextBefore: String, contextAfter: String, prefix: String, suggestion: String): String {
        val normalizedBefore = normalizeLineEndings(contextBefore)
        var normalizedSuggestion = normalizeLineEndings(suggestion)
        val echoedLength = longestEchoedContextLength(normalizedBefore, normalizedSuggestion)
        if (echoedLength > 0) {
            normalizedSuggestion = normalizedSuggestion.drop(echoedLength)
            normalizedSuggestion = removeEchoedSuffix(normalizedSuggestion, normalizeLineEndings(contextAfter))
        }
        return mergePrefix(prefix, normalizedSuggestion)
    }

    private fun longestEchoedContextLength(context: String, suggestion: String): Int {
        var longest = 0
        for (contextStart in context.indices) {
            var matched = 0
            while (contextStart + matched < context.length && matched < suggestion.length &&
                context[contextStart + matched] == suggestion[matched]
            ) {
                matched++
            }
            if (context.substring(contextStart, contextStart + matched).contains('\n')) {
                longest = maxOf(longest, matched)
            }
        }
        return longest
    }

    private fun removeEchoedSuffix(suggestion: String, contextAfter: String): String {
        var longest = 0
        for (length in 1..minOf(suggestion.length, contextAfter.length)) {
            if (suggestion.regionMatches(suggestion.length - length, contextAfter, 0, length) &&
                contextAfter.take(length).contains('\n')
            ) {
                longest = length
            }
        }
        return suggestion.dropLast(longest)
    }

    private fun normalizeLineEndings(text: String): String =
        text.replace("\r\n", "\n").replace('\r', '\n')

    fun prefixStart(text: String, offset: Int): Int {
        var start = offset.coerceIn(0, text.length)
        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_' || text[start - 1] == '$')) {
            start--
        }
        return start
    }

    fun presentableText(text: String): String {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        return normalized.split('\n')
            .take(3)
            .joinToString("\n") { line ->
                if (line.length <= 80) line else line.take(77) + "..."
            }
    }

    fun removeCodeFence(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```") || !trimmed.endsWith("```")) return text
        return trimmed.substringAfter('\n').substringBeforeLast("```").trimEnd()
    }
}