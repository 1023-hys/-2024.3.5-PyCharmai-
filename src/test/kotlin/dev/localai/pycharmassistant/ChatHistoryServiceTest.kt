package dev.localai.pycharmassistant

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatHistoryServiceTest {
    @Test
    fun formatsWholeConversationForCopy() {
        val session = ChatSessionRecord(
            title = "test",
            messages = mutableListOf(
                ChatMessageRecord(fromUser = true, text = "hello"),
                ChatMessageRecord(fromUser = false, text = "world")
            )
        )

        assertEquals("You: hello\n\nAssistant: world", session.transcript())
    }

    @Test
    fun findsMessageContainingCaretOffset() {
        val session = ChatSessionRecord(
            messages = mutableListOf(
                ChatMessageRecord(fromUser = true, text = "first"),
                ChatMessageRecord(fromUser = false, text = "second")
            )
        )
        val transcript = session.transcript()

        assertEquals(0, session.messageIndexAt(transcript.indexOf("first")))
        assertEquals(1, session.messageIndexAt(transcript.indexOf("second")))
    }
}
