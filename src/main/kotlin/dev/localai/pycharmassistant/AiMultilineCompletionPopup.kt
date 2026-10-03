package dev.localai.pycharmassistant

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.util.WeakHashMap
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.JTextArea

object AiMultilineCompletionPopup {
    private val activePopups = WeakHashMap<Editor, JBPopup>()

    @Synchronized
    fun isActive(editor: Editor): Boolean = activePopups[editor]?.isDisposed == false

    fun show(project: Project, editor: Editor, replaceStartOffset: Int, caretOffset: Int, text: String) {
        clear(editor)
        val document = editor.document
        val modificationStamp = document.modificationStamp
        val textArea = JTextArea(text).apply {
            isEditable = false
            lineWrap = false
            font = previewFont(editor.colorsScheme.getFont(EditorFontType.PLAIN), text)
            foreground = editor.colorsScheme.defaultForeground
            background = editor.colorsScheme.defaultBackground
            border = JBUI.Borders.empty(8)
            caretPosition = 0
            setFocusTraversalKeysEnabled(false)
        }
        val lineCount = text.count { it == '\n' } + 1
        val scrollPane = JBScrollPane(textArea).apply {
            preferredSize = Dimension(560, (lineCount * textArea.getFontMetrics(textArea.font).height + 20).coerceIn(64, 280))
        }
        lateinit var popup: JBPopup
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("TAB"), "accept-ai-completion")
        textArea.actionMap.put("accept-ai-completion", object : AbstractAction() {
            override fun actionPerformed(event: java.awt.event.ActionEvent) {
                popup.cancel()
                if (project.isDisposed || editor.isDisposed ||
                    document.modificationStamp != modificationStamp || editor.caretModel.offset != caretOffset
                ) return
                WriteCommandAction.runWriteCommandAction(project) {
                    document.replaceString(replaceStartOffset, caretOffset, text)
                }
                editor.caretModel.moveToOffset(replaceStartOffset + text.length)
            }
        })
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "dismiss-ai-completion")
        textArea.actionMap.put("dismiss-ai-completion", object : AbstractAction() {
            override fun actionPerformed(event: java.awt.event.ActionEvent) {
                popup.cancel()
            }
        })
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, textArea)
            .setRequestFocus(true)
            .setFocusable(true)
            .setResizable(true)
            .setMovable(false)
            .setCancelOnClickOutside(true)
            .setCancelKeyEnabled(false)
            .createPopup()
        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                synchronized(this@AiMultilineCompletionPopup) {
                    if (activePopups[editor] === popup) activePopups.remove(editor)
                }
            }
        })
        synchronized(this) {
            activePopups[editor] = popup
        }
        popup.showInBestPositionFor(editor)
    }

    @Synchronized
    private fun clear(editor: Editor) {
        activePopups.remove(editor)?.cancel()
    }

    private fun previewFont(editorFont: Font, text: String): Font {
        if (editorFont.canDisplayUpTo(text) == -1) return editorFont
        val fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
        val preferredFamilies = listOf(
            "Microsoft YaHei UI",
            "Microsoft YaHei",
            "SimSun",
            "SimHei",
            "Noto Sans CJK SC",
            "PingFang SC",
            "WenQuanYi Micro Hei"
        )
        val availableFamilies = preferredFamilies.filter { it in fontFamilies } + "Dialog"
        return availableFamilies
            .map { Font(it, editorFont.style, editorFont.size) }
            .firstOrNull { it.canDisplayUpTo(text) == -1 }
            ?: Font("Dialog", editorFont.style, editorFont.size)
    }
}
