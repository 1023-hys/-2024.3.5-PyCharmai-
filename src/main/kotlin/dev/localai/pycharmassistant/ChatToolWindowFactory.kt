package dev.localai.pycharmassistant

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import dev.localai.pycharmassistant.service.OpenAiClient
import dev.localai.pycharmassistant.settings.AssistantSettings
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.Insets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ListSelectionModel

class ChatToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val history = project.getService(ChatHistoryService::class.java)
        val busySessionIds = mutableSetOf<String>()
        val requestFutures = ConcurrentHashMap<String, Future<*>>()
        if (history.state.sessions.isEmpty()) history.state.sessions.add(ChatSessionRecord(title = "New chat"))

        val sessionListModel = DefaultListModel<ChatSessionRecord>()
        history.state.sessions.forEach(sessionListModel::addElement)
        val sessionList = JBList(sessionListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component = super.getListCellRendererComponent(
                    list,
                    (value as? ChatSessionRecord)?.title ?: "",
                    index,
                    isSelected,
                    cellHasFocus
                )
            }
        }
        val messagesArea = JBTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            margin = Insets(8, 8, 8, 8)
        }
        val inputArea = JBTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            rows = 4
            margin = Insets(8, 8, 8, 8)
        }
        val newButton = JButton("New chat")
        val renameChatButton = JButton("Rename chat")
        val deleteChatButton = JButton("Delete chat")
        val deleteMessageButton = JButton("Delete message")
        val copyButton = JButton("Copy chat")
        val sendButton = JButton("Send")
        val attachFileButton = JButton("Attach file")
        val sessionToolbar = JPanel(GridLayout(0, 1, 0, 4)).apply {
            add(newButton)
            add(renameChatButton)
            add(deleteChatButton)
        }
        val messageToolbar = JPanel(GridLayout(1, 0, 4, 0)).apply {
            add(copyButton)
            add(deleteMessageButton)
        }
        val transcriptPanel = JPanel(BorderLayout(0, 4)).apply {
            add(messageToolbar, BorderLayout.NORTH)
            add(JBScrollPane(messagesArea), BorderLayout.CENTER)
        }
        val sessionPanel = JPanel(BorderLayout(0, 4)).apply {
            preferredSize = Dimension(150, 100)
            add(sessionToolbar, BorderLayout.NORTH)
            add(JBScrollPane(sessionList), BorderLayout.CENTER)
        }
        val bottomPanel = JPanel(BorderLayout()).apply {
            add(JPanel().apply { add(attachFileButton) }, BorderLayout.NORTH)
            add(JBScrollPane(inputArea), BorderLayout.CENTER)
            add(sendButton, BorderLayout.EAST)
        }
        val chatPanel = JPanel(BorderLayout()).apply {
            add(sessionPanel, BorderLayout.WEST)
            add(transcriptPanel, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }

        fun selectedSession(): ChatSessionRecord? = sessionList.selectedValue

        fun renderSession(session: ChatSessionRecord?) {
            messagesArea.text = session?.transcript().orEmpty()
            messagesArea.caretPosition = messagesArea.document.length
            renameChatButton.isEnabled = session != null
            deleteChatButton.isEnabled = session != null
            deleteMessageButton.isEnabled = session?.messages?.isNotEmpty() == true
            copyButton.isEnabled = session?.messages?.isNotEmpty() == true
            sendButton.text = if (session?.id in busySessionIds) "Stop" else "Send"
            sendButton.isEnabled = session != null
        }

        fun createSession() {
            val session = ChatSessionRecord(title = "New chat")
            history.state.sessions.add(session)
            sessionListModel.addElement(session)
            sessionList.selectedIndex = sessionListModel.size() - 1
            renderSession(session)
        }

        fun renderCurrentSession() = renderSession(selectedSession())

        fun deleteMessageAt(offset: Int) {
            val session = selectedSession() ?: return
            if (session.messages.isEmpty()) return
            val messageIndex = session.messageIndexAt(offset.coerceIn(0, messagesArea.document.length))
            if (messageIndex >= 0) session.messages.removeAt(messageIndex)
            renderSession(session)
        }

        sessionList.addListSelectionListener {
            if (!it.valueIsAdjusting) renderCurrentSession()
        }
        newButton.addActionListener { createSession() }
        renameChatButton.addActionListener {
            val session = selectedSession() ?: return@addActionListener
            val newTitle = Messages.showInputDialog(
                project,
                "Enter a new title for this chat:",
                "Rename Chat",
                Messages.getQuestionIcon(),
                session.title,
                null
            )?.trim()
            if (!newTitle.isNullOrEmpty() && newTitle != session.title) {
                session.title = newTitle
                sessionList.repaint()
            }
        }
        deleteChatButton.addActionListener {
            val session = selectedSession() ?: return@addActionListener
            val confirmed = Messages.showYesNoDialog(
                project,
                "Delete this chat and its saved messages?",
                "Delete Chat",
                Messages.getQuestionIcon()
            ) == Messages.YES
            if (!confirmed) return@addActionListener

            requestFutures.remove(session.id)?.cancel(true)
            busySessionIds.remove(session.id)
            val index = sessionList.selectedIndex
            history.state.sessions.removeAll { it.id == session.id }
            sessionListModel.removeElement(session)
            if (sessionListModel.isEmpty) createSession()
            else sessionList.selectedIndex = index.coerceAtMost(sessionListModel.size() - 1)
        }
        deleteMessageButton.addActionListener {
            val session = selectedSession() ?: return@addActionListener
            if (session.messages.isEmpty()) return@addActionListener
            val selectedText = messagesArea.selectedText
            val offset = if (!selectedText.isNullOrEmpty()) messagesArea.selectionStart else messagesArea.caretPosition
            deleteMessageAt(offset)
        }
        copyButton.addActionListener {
            val session = selectedSession() ?: return@addActionListener
            val transcript = session.transcript()
            if (transcript.isNotBlank()) {
                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(java.awt.datatransfer.StringSelection(transcript), null)
            }
        }
        attachFileButton.addActionListener {
            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            FileChooser.chooseFile(descriptor, project, null) { file ->
                ApplicationManager.getApplication().executeOnPooledThread {
                    val content = runCatching { VfsUtil.loadText(file) }.getOrNull()
                    ApplicationManager.getApplication().invokeLater {
                        if (project.isDisposed) return@invokeLater
                        if (content == null) {
                            Messages.showErrorDialog(project, "Could not read ${file.name}.", "Attach File")
                            return@invokeLater
                        }
                        val clipped = if (content.length > MAX_ATTACH_CHARS) {
                            content.take(MAX_ATTACH_CHARS) + "\n... (truncated)"
                        } else {
                            content
                        }
                        val prefix = if (inputArea.text.isNullOrBlank()) "" else "\n\n"
                        inputArea.text = "${inputArea.text}$prefix[Reference file: ${file.path}]\n```\n$clipped\n```"
                        inputArea.caretPosition = inputArea.document.length
                    }
                }
            }
        }
        sendButton.addActionListener {
            val session = selectedSession() ?: return@addActionListener
            if (session.id in busySessionIds) {
                requestFutures.remove(session.id)?.cancel(true)
                busySessionIds.remove(session.id)
                sendButton.text = "Send"
                sendButton.isEnabled = true
                return@addActionListener
            }
            val userText = inputArea.text.trim()
            if (userText.isBlank()) return@addActionListener

            session.messages.add(ChatMessageRecord(fromUser = true, text = userText))
            if (session.title == "New chat") {
                session.title = userText.lineSequence().first().take(32)
                sessionList.repaint()
            }
            inputArea.text = ""
            busySessionIds.add(session.id)
            sendButton.text = "Stop"
            renderSession(session)
            val sessionId = session.id
            val conversation = session.messages.toList()
            val future = ApplicationManager.getApplication().executeOnPooledThread {
                val reply = runCatching {
                    val settings = AssistantSettings.getInstance()
                    val promptTemplate = settings.getPromptTemplate()
                    val contextPrefix = if (settings.isContextEnabled()) {
                        "Context limit: ${settings.getMaxContextChars()} chars\n\n"
                    } else {
                        "Context disabled\n\n"
                    }
                    val transcript = conversation.joinToString("\n\n") { message ->
                        "${if (message.fromUser) "User" else "Assistant"}: ${message.text}"
                    }
                    OpenAiClient.complete(
                        "$promptTemplate\n\n$contextPrefix Continue this conversation. Answer clearly and concisely.\n\n$transcript\n\nAssistant:"
                    )
                }
                ApplicationManager.getApplication().invokeLater {
                    if (sessionId !in busySessionIds) return@invokeLater
                    val targetSession = history.state.sessions.firstOrNull { it.id == sessionId }
                        ?: return@invokeLater
                    requestFutures.remove(sessionId)
                    busySessionIds.remove(sessionId)
                    reply.onSuccess { targetSession.messages.add(ChatMessageRecord(fromUser = false, text = it)) }
                        .onFailure {
                            if (it !is java.util.concurrent.CancellationException && !Thread.currentThread().isInterrupted) {
                                targetSession.messages.add(
                                    ChatMessageRecord(fromUser = false, text = it.message ?: "Request failed.")
                                )
                            }
                        }
                    if (selectedSession()?.id == sessionId) renderSession(targetSession)
                    else sessionList.repaint()
                }
            }
            requestFutures[sessionId] = future
        }

        sessionList.selectedIndex = 0
        renderSession(sessionList.selectedValue)
        val content = ContentFactory.getInstance().createContent(chatPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private companion object {
        const val MAX_ATTACH_CHARS = 20_000
    }
}
