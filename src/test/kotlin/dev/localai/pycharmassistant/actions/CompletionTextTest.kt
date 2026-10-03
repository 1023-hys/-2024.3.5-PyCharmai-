package dev.localai.pycharmassistant.actions

import org.junit.Assert.assertEquals
import org.junit.Test

class CompletionTextTest {
    @Test
    fun doesNotDuplicatePrefixWhenSuggestionContainsFullIdentifier() {
        assertEquals("print('')", CompletionText.mergePrefix("prin", "print('')"))
    }

    @Test
    fun appendsSuggestionWhenItContainsOnlyTheSuffix() {
        assertEquals("print('')", CompletionText.mergePrefix("prin", "t('')"))
    }

    @Test
    fun removesRepeatedContextFromMultilineCompletion() {
        val contextBefore = "print('开始游戏')\ni"
        val suggestion = "print('开始游戏')\nii=1"

        assertEquals("i=1", CompletionText.normalizeSuggestion(contextBefore, "", "i", suggestion))
    }

    @Test
    fun removesRepeatedContextBeforeAndAfterCursor() {
        val contextBefore = "if ready:\n    i"
        val contextAfter = "\nnext_line()"
        val suggestion = "if ready:\n    i=1\nnext_line()"

        assertEquals("i=1", CompletionText.normalizeSuggestion(contextBefore, contextAfter, "i", suggestion))
    }

    @Test
    fun findsPartialIdentifierAtCaret() {
        assertEquals(6, CompletionText.prefixStart("print(value", 10))
    }

    @Test
    fun handlesCaretOutsideDocumentBounds() {
        assertEquals(0, CompletionText.prefixStart("name", -4))
        assertEquals(0, CompletionText.prefixStart("name", 20))
    }

    @Test
    fun removesSurroundingMarkdownCodeFence() {
        assertEquals("return value", CompletionText.removeCodeFence("```python\nreturn value\n```"))
    }

    @Test
    fun leavesPlainTextUnchanged() {
        assertEquals("return value", CompletionText.removeCodeFence("return value"))
    }

    @Test
    fun includesSelectedCodeAndFullFileWhenProvided() {
        val prompt = CompletionText.buildCompletionPrompt(
            "Instructions",
            "before",
            "after",
            "selected block",
            "whole file"
        )

        org.junit.Assert.assertTrue(prompt.contains("Complete current file:\nwhole file"))
        org.junit.Assert.assertTrue(prompt.contains("Selected code to reference:\nselected block"))
        org.junit.Assert.assertTrue(prompt.contains("Code before cursor:\nbefore\n<CURSOR>\nCode after cursor:\nafter"))
    }

    @Test
    fun omitsDisabledSelectionAndFullFileContext() {
        val prompt = CompletionText.buildCompletionPrompt("Instructions", "before", "after", null, null)

        org.junit.Assert.assertFalse(prompt.contains("Selected code to reference:"))
        org.junit.Assert.assertFalse(prompt.contains("Complete current file:"))
    }
}