package dev.localai.pycharmassistant

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.util.Alarm
import dev.localai.pycharmassistant.actions.GenerateCompletionAction
import dev.localai.pycharmassistant.settings.AssistantSettings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class AiAutoCompleteService : Disposable {
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val requestSequence = AtomicLong()
    private val pendingRequests = ConcurrentHashMap<Editor, Long>()
    private val inFlightEditors = ConcurrentHashMap.newKeySet<Editor>()

    init {
        val multicaster = EditorFactory.getInstance().eventMulticaster
        multicaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                EditorFactory.getInstance().allEditors.filter {
                    it.document === event.document && !it.isDisposed && it.project != null
                }.forEach { scheduleCompletion(it, caretMoved = false) }
            }
        }, this)
        multicaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                scheduleCompletion(event.editor, caretMoved = true)
            }
        }, this)
    }

    private fun scheduleCompletion(editor: Editor, caretMoved: Boolean) {
        val settings = AssistantSettings.getInstance()
        if (editor.isDisposed || !settings.isAutoCompleteEnabled()) return
        val project = editor.project ?: return
        if (project.isDisposed || hasCompletionPopup(editor)) return
        if (caretMoved && !settings.state.requireStableCaretForAutoComplete && pendingRequests.containsKey(editor)) {
            return
        }

        val requestId = requestSequence.incrementAndGet()
        pendingRequests[editor] = requestId
        alarm.addRequest({
            if (!pendingRequests.remove(editor, requestId) || editor.isDisposed) return@addRequest
            if (!settings.isAutoCompleteEnabled() ||
                project.isDisposed || hasCompletionPopup(editor) ||
                !inFlightEditors.add(editor)
            ) {
                return@addRequest
            }
            val documentStamp = editor.document.modificationStamp
            val caretOffset = editor.caretModel.offset
            GenerateCompletionAction.requestCompletion(project, editor, reportErrors = false) {
                inFlightEditors.remove(editor)
                ApplicationManager.getApplication().invokeLater {
                    if (editor.isDisposed || project.isDisposed) return@invokeLater
                    val documentChanged = editor.document.modificationStamp != documentStamp
                    val caretMoved = editor.caretModel.offset != caretOffset
                    if (documentChanged || caretMoved) {
                        scheduleCompletion(editor, caretMoved)
                    }
                }
            }
        }, settings.state.autoCompleteDelaySeconds.coerceIn(1, 30) * MILLIS_PER_SECOND)
    }

    private fun hasCompletionPopup(editor: Editor): Boolean =
        LookupManager.getActiveLookup(editor) != null || AiMultilineCompletionPopup.isActive(editor)

    override fun dispose() {
        pendingRequests.clear()
        inFlightEditors.clear()
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1000
    }
}
