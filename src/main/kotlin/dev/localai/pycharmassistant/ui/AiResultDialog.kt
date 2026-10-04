package dev.localai.pycharmassistant.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class AiResultDialog(
    private val project: Project,
    private val editor: Editor?,
    dialogTitle: String,
    private val resultText: String,
    codeOutput: Boolean
) : DialogWrapper(project) {

    private val textArea = JBTextArea(resultText).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        margin = JBUI.insets(8)
        caretPosition = 0
    }

    private val canInsert = codeOutput && editor != null && !editor.isDisposed

    init {
        title = dialogTitle
        setOKButtonText("Close")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val buttonRow = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            add(JButton("Copy").apply {
                addActionListener {
                    Toolkit.getDefaultToolkit().systemClipboard
                        .setContents(StringSelection(resultText), null)
                }
            })
            if (canInsert) {
                add(JButton("Insert into Editor").apply {
                    addActionListener { insertIntoEditor() }
                })
            }
        }
        return JPanel(BorderLayout(0, 6)).apply {
            add(JBScrollPane(textArea), BorderLayout.CENTER)
            add(buttonRow, BorderLayout.SOUTH)
            preferredSize = Dimension(780, 440)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = textArea

    override fun createActions(): Array<Action> = arrayOf(okAction)

    private fun insertIntoEditor() {
        val targetEditor = editor?.takeUnless { it.isDisposed } ?: return
        val document = targetEditor.document
        val selection = targetEditor.selectionModel
        var insertOffset = targetEditor.caretModel.offset
        WriteCommandAction.runWriteCommandAction(project) {
            if (selection.hasSelection()) {
                val start = selection.selectionStart
                val end = selection.selectionEnd
                document.replaceString(start, end, resultText)
                insertOffset = start + resultText.length
            } else {
                val offset = targetEditor.caretModel.offset
                document.insertString(offset, resultText)
                insertOffset = offset + resultText.length
            }
        }
        targetEditor.caretModel.moveToOffset(insertOffset)
        close(OK_EXIT_CODE)
    }
}
