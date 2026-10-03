package dev.localai.pycharmassistant.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.ui.Messages
import dev.localai.pycharmassistant.AiMultilineCompletionPopup
import dev.localai.pycharmassistant.service.OpenAiClient
import dev.localai.pycharmassistant.settings.AssistantSettings

class GenerateCompletionAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        requestCompletion(project, editor)
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible =
            event.project != null && event.getData(CommonDataKeys.EDITOR) != null
    }

    companion object {
        fun requestCompletion(
            project: com.intellij.openapi.project.Project,
            editor: com.intellij.openapi.editor.Editor,
            reportErrors: Boolean = true,
            onFinished: (() -> Unit)? = null
        ) {
            val document = editor.document
            val caretOffset = editor.caretModel.offset
            val documentText = document.text
            val settings = AssistantSettings.getInstance()
            val selectedText = if (settings.state.includeSelectionInCompletion) {
                editor.selectionModel.selectedText?.takeIf(String::isNotBlank)
            } else {
                null
            }
            val prefixStart = CompletionText.prefixStart(documentText, caretOffset)
            val prefix = documentText.substring(prefixStart, caretOffset)
            val includedSelection = selectedText
            val fullFileText = if (settings.state.includeFullFileInCompletion) documentText else null
            val maxContext = if (settings.isContextEnabled()) settings.getMaxContextChars() else 0
            val contextStart = (prefixStart - maxContext).coerceAtLeast(0)
            val contextEnd = (caretOffset + maxContext).coerceAtMost(documentText.length)
            val contextBefore = documentText.substring(contextStart, caretOffset)
            val contextAfter = documentText.substring(caretOffset, contextEnd)
            val modificationStamp = document.modificationStamp

            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val promptTemplate = settings.getPromptTemplate()
                    val suggestion = OpenAiClient.complete(
                        CompletionText.buildCompletionPrompt(
                            promptTemplate,
                            contextBefore,
                            contextAfter,
                            includedSelection,
                            fullFileText
                        )
                    ).trimEnd().let(CompletionText::removeCodeFence)
                    if (suggestion.isBlank()) return@executeOnPooledThread

                    ApplicationManager.getApplication().invokeLater {
                        if (project.isDisposed || editor.isDisposed ||
                            document.modificationStamp != modificationStamp ||
                            editor.caretModel.offset != caretOffset
                        ) {
                            return@invokeLater
                        }
                        val completionText = CompletionText.normalizeSuggestion(
                            contextBefore,
                            contextAfter,
                            prefix,
                            suggestion
                        )
                        if (completionText.contains('\n') || completionText.contains('\r')) {
                            AiMultilineCompletionPopup.show(project, editor, prefixStart, caretOffset, completionText)
                            return@invokeLater
                        }
                        val lookupText = prefix + suggestion
                        val item = LookupElementBuilder.create(suggestion)
                            .withLookupString(lookupText)
                            .withPresentableText(CompletionText.presentableText(completionText))
                            .withTypeText("AI completion")
                            .withInsertHandler { context, _ ->
                                context.document.replaceString(context.startOffset, context.tailOffset, completionText)
                            }
                        LookupManager.getInstance(project).showLookup(editor, item)
                    }
                } catch (error: Exception) {
                    if (reportErrors) {
                        ApplicationManager.getApplication().invokeLater {
                            if (!project.isDisposed) {
                                Messages.showErrorDialog(project, error.message ?: "AI request failed.", "AI Assistant")
                            }
                        }
                    }
                } finally {
                    onFinished?.invoke()
                }
            }
        }
    }
}