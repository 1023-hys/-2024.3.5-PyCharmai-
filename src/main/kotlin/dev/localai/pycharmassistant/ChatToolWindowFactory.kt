package dev.localai.pycharmassistant

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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
import javax.swing.DefaultListModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ListSelectionModel

class ChatToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val history = project.getService(ChatHistoryService::class.java)
        val busySessionIds = mutableSetOf<String>()
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
        val deleteButton = JButton("Delete chat")
        val sendButton = JButton("Send")
        val sessionToolbar = JPanel(GridLayout(0, 1, 0, 4)).apply {
            add(newButton)
            add(deleteButton)
        }
        val sessionPanel = JPanel(BorderLayout(0, 4)).apply {
            preferredSize = Dimension(150, 100)
            add(sessionToolbar, BorderLayout.NORTH)
            add(JBScrollPane(sessionList), BorderLayout.CENTER)
        }
        val bottomPanel = JPanel(BorderLayout()).apply {
            add(JBScrollPane(inputArea), BorderLayout.CENTER)
            add(sendButton, BorderLayout.EAST)
        }
        val chatPanel = JPanel(BorderLayout()).apply {
            add(sessionPanel, BorderLayout.WEST)
            add(JBScrollPane(messagesArea), BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }

        fun selectedSession(): ChatSessionRecord? = sessionList.selectedValue

        fun renderSession(session: ChatSessionRecord?) {
            messagesArea.text = session?.messages?.joinToString("\n\n") { message ->
                "${if (message.fromUser) "You" else "Assistant"}: ${message.text}"
            }.orEmpty()
            messagesArea.caretPosition = messagesArea.document.length
            deleteButton.isEnabled = session != null
            sendButton.isEnabled = session != null && session.id !in busySessionIds
        }

        fun createSession() {
            val session = ChatSessionRecord(title = "New chat")
            history.state.sessions.add(session)
            sessionListModel.addElement(session)
            sessionList.selectedIndex = sessionListModel.size() - 1
            renderSession(session)
        }

        sessionList.addListSelectionListener {
            if (!it.valueIsAdjusting) renderSession(selectedSession())
        }
        newButton.addActionListener { createSession() }
        deleteButton.addActionListener {
            val session = selectedSession() ?: return@addActionListener
            val confirmed = Messages.showYesNoDialog(
                project,
                "Delete this chat and its saved messages?",
                "Delete Chat",
                Messages.getQuestionIcon()
            ) == Messages.YES
            if (!confirmed) return@addActionListener

            val index = sessionList.selectedIndex
            history.state.sessions.removeAll { it.id == session.id }
            busySessionIds.remove(session.id)
            sessionListModel.removeElement(session)
            if (sessionListModel.isEmpty) {
                createSession()
            } else {
                sessionList.selectedIndex = index.coerceAtMost(sessionListModel.size() - 1)
            }
        }
        sendButton.addActionListener {
            val session = selectedSession() ?: return@addActionListener
            val userText = inputArea.text.trim()
            if (userText.isBlank() || session.id in busySessionIds) return@addActionListener

            session.messages.add(ChatMessageRecord(fromUser = true, text = userText))
            if (session.title == "New chat") {
                session.title = userText.lineSequence().first().take(32)
                sessionList.repaint()
            }
            inputArea.text = ""
            busySessionIds.add(session.id)
            renderSession(session)
            val sessionId = session.id
            val conversation = session.messages.toList()

            ApplicationManager.getApplication().executeOnPooledThread {
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
                    val targetSession = history.state.sessions.firstOrNull { it.id == sessionId }
                        ?: return@invokeLater
                    busySessionIds.remove(sessionId)
                    reply.onSuccess { targetSession.messages.add(ChatMessageRecord(fromUser = false, text = it)) }
                        .onFailure {
                            targetSession.messages.add(
                                ChatMessageRecord(fromUser = false, text = it.message ?: "Request failed.")
                            )
                        }
                    if (selectedSession()?.id == sessionId) renderSession(targetSession)
                    else sessionList.repaint()
                }
            }
        }

        sessionList.selectedIndex = 0
        renderSession(sessionList.selectedValue)
        val content = ContentFactory.getInstance().createContent(chatPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
