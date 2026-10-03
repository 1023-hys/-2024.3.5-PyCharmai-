package dev.localai.pycharmassistant

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import java.util.UUID

@State(
    name = "AiAssistantChatHistory",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class ChatHistoryService : PersistentStateComponent<ChatHistoryState> {
    private var persistedState = ChatHistoryState()

    override fun getState(): ChatHistoryState = persistedState

    override fun loadState(state: ChatHistoryState) {
        persistedState = state
    }

    companion object {
        fun getInstance(project: Project): ChatHistoryService =
            project.getService(ChatHistoryService::class.java)
    }
}

data class ChatHistoryState(
    var sessions: MutableList<ChatSessionRecord> = mutableListOf()
)

data class ChatSessionRecord(
    var id: String = UUID.randomUUID().toString(),
    var title: String = "New chat",
    var messages: MutableList<ChatMessageRecord> = mutableListOf()
)

data class ChatMessageRecord(
    var fromUser: Boolean = false,
    var text: String = ""
)
