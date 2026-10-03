package dev.localai.pycharmassistant.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import dev.localai.pycharmassistant.service.OpenAiClient
import dev.localai.pycharmassistant.settings.AssistantSettings

class ExplainSelectionAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val selectedCode = editor.selectionModel.selectedText?.takeIf(String::isNotBlank) ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val settings = AssistantSettings.getInstance()
                val contextPrefix = if (settings.isContextEnabled()) {
                    val maxContext = settings.getMaxContextChars()
                    "Context window: ${maxContext} chars\n\n"
                } else {
                    "Context disabled\n\n"
                }
                val promptTemplate = settings.getPromptTemplate()
                val explanation = OpenAiClient.complete(
                    "$promptTemplate\n\n$contextPrefix Explain the following code clearly and concisely.\n\n$selectedCode"
                )
                ApplicationManager.getApplication().invokeLater {
                    if (!project.isDisposed) {
                        Messages.showInfoMessage(project, explanation, "AI Code Explanation")
                    }
                }
            } catch (error: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    if (!project.isDisposed) {
                        Messages.showErrorDialog(project, error.message ?: "AI request failed.", "AI Assistant")
                    }
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible =
            event.project != null && event.getData(CommonDataKeys.EDITOR)?.selectionModel?.hasSelection() == true
    }
}