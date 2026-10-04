package dev.localai.pycharmassistant.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import dev.localai.pycharmassistant.service.OpenAiClient
import dev.localai.pycharmassistant.settings.AssistantSettings
import dev.localai.pycharmassistant.ui.AiResultDialog

abstract class AiEditorAction : AnAction() {
    final override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR)
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val selection = editor?.selectionModel?.selectedText?.takeIf(String::isNotBlank)
        val fileText = editor?.document?.text
        perform(project, editor, virtualFile, selection, fileText)
    }

    protected abstract fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    )

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    protected fun askInput(
        project: Project,
        title: String,
        label: String,
        initial: String = ""
    ): String? = Messages.showInputDialog(
        project,
        label,
        title,
        Messages.getQuestionIcon(),
        initial,
        null
    )

    protected fun showMessage(project: Project, message: String, title: String = "AI Assistant") {
        Messages.showWarningDialog(project, message, title)
    }

    /**
     * References the file for an action: an explicit selection is always sent
     * (it is the action's direct target), while falling back to the whole file
     * honors the "Enable context" setting and the configured context character limit.
     */
    protected fun codeContext(
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ): String? {
        val settings = AssistantSettings.getInstance()
        val fileName = virtualFile?.name
        if (!selection.isNullOrBlank()) {
            return "Selected code${fileName?.let { " from $it" }.orEmpty()}:\n$selection"
        }
        if (!settings.isContextEnabled()) return null
        if (!fileText.isNullOrBlank()) {
            val limit = settings.getMaxContextChars()
            val truncated = if (fileText.length > limit) fileText.take(limit) else fileText
            return "Current file${fileName?.let { ": $it" }.orEmpty()}:\n$truncated"
        }
        return null
    }

    protected fun run(
        project: Project,
        editor: Editor?,
        title: String,
        prompt: String,
        codeOutput: Boolean = false
    ) {
        val fullPrompt = "${AssistantSettings.getInstance().getPromptTemplate()}\n\n$prompt"
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                var answer = OpenAiClient.complete(fullPrompt).trim()
                if (codeOutput) answer = CompletionText.removeCodeFence(answer).trim()
                ApplicationManager.getApplication().invokeLater {
                    if (!project.isDisposed) {
                        AiResultDialog(project, editor, title, answer, codeOutput).show()
                    }
                }
            } catch (error: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    if (!project.isDisposed) {
                        Messages.showErrorDialog(
                            project,
                            error.message ?: "AI request failed.",
                            "AI Assistant"
                        )
                    }
                }
            }
        }
    }
}
