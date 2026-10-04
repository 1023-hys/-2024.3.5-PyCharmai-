package dev.localai.pycharmassistant.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import dev.localai.pycharmassistant.settings.AssistantSettings
import dev.localai.pycharmassistant.settings.AssistantSettings.CustomPrompt
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class PromptLibraryAction : AiEditorAction() {
    override fun perform(
        project: Project,
        editor: Editor?,
        virtualFile: VirtualFile?,
        selection: String?,
        fileText: String?
    ) {
        val dialog = PromptLibraryDialog(project)
        if (!dialog.showAndGet()) return
        val entry = dialog.selectedEntry ?: return
        val context = codeContext(virtualFile, selection, fileText)
        val prompt = if (context != null) "${entry.instruction}\n\n$context" else entry.instruction
        run(project, editor, "Prompt: ${entry.name}", prompt)
    }
}

internal class PromptLibraryDialog(project: Project) : DialogWrapper(project) {

    internal class Entry(var name: String, var instruction: String, val custom: CustomPrompt?)

    private val settings = AssistantSettings.getInstance()
    private val listModel = DefaultListModel<Entry>()
    private val list = JBList(listModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = object : javax.swing.DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): java.awt.Component {
                val entry = value as? Entry
                val label = if (entry?.custom != null) "* ${entry.name}" else entry?.name.orEmpty()
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus)
            }
        }
    }
    private val nameField = JBTextField()
    private val instructionArea = JBTextArea(8, 44).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val newButton = JButton("New")
    private val saveButton = JButton("Save")
    private val deleteButton = JButton("Delete")

    val selectedEntry: Entry? get() = list.selectedValue

    init {
        title = "Prompt Library"
        setOKButtonText("Run")

        BuiltInPrompts.LIBRARY.forEach { (name, instruction) ->
            listModel.addElement(Entry(name, instruction, null))
        }
        settings.state.customPrompts.forEach { custom ->
            listModel.addElement(Entry(custom.name, custom.instruction, custom))
        }

        list.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) showSelectedEntry()
        }
        newButton.addActionListener { createCustomPrompt() }
        saveButton.addActionListener { saveSelectedEntry() }
        deleteButton.addActionListener { deleteSelectedEntry() }

        init()
        list.selectedIndex = 0
        showSelectedEntry()
    }

    override fun createCenterPanel(): JComponent {
        val editorPanel = JPanel(BorderLayout(0, 4)).apply {
            add(JLabel("Name"), BorderLayout.NORTH)
            add(nameField, BorderLayout.CENTER)
        }
        val rightPanel = JPanel(BorderLayout(0, 4)).apply {
            add(editorPanel, BorderLayout.NORTH)
            add(JBScrollPane(instructionArea), BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                add(newButton)
                add(saveButton)
                add(deleteButton)
            }, BorderLayout.SOUTH)
        }
        val leftPanel = JBScrollPane(list).apply {
            preferredSize = Dimension(240, 320)
        }
        return JPanel(BorderLayout(8, 0)).apply {
            border = JBUI.Borders.empty(8)
            add(leftPanel, BorderLayout.WEST)
            add(rightPanel, BorderLayout.CENTER)
            preferredSize = Dimension(720, 360)
        }
    }

    override fun doOKAction() {
        val entry = list.selectedValue ?: return
        if (entry.custom != null && !saveSelectedEntry(showWarning = false)) return
        super.doOKAction()
    }

    private fun showSelectedEntry() {
        val entry = list.selectedValue
        val editable = entry?.custom != null
        nameField.text = entry?.name.orEmpty()
        instructionArea.text = entry?.instruction.orEmpty()
        nameField.isEnabled = editable
        instructionArea.isEditable = editable
        saveButton.isEnabled = editable
        deleteButton.isEnabled = editable
    }

    private fun createCustomPrompt() {
        val custom = CustomPrompt(name = "New prompt", instruction = "")
        settings.state.customPrompts.add(custom)
        val entry = Entry(custom.name, custom.instruction, custom)
        listModel.addElement(entry)
        list.selectedIndex = listModel.size() - 1
        nameField.requestFocusInWindow()
    }

    private fun saveSelectedEntry(): Boolean = saveSelectedEntry(showWarning = true)

    private fun saveSelectedEntry(showWarning: Boolean): Boolean {
        val entry = list.selectedValue
        val custom = entry?.custom ?: return true
        val name = nameField.text.trim()
        val instruction = instructionArea.text.trim()
        if (name.isEmpty() || instruction.isEmpty()) {
            if (showWarning) {
                Messages.showWarningDialog(null, "A custom prompt needs both a name and instructions.", "Prompt Library")
            }
            return false
        }
        custom.name = name
        custom.instruction = instruction
        entry.name = name
        entry.instruction = instruction
        list.repaint()
        return true
    }

    private fun deleteSelectedEntry() {
        val entry = list.selectedValue ?: return
        val custom = entry.custom ?: return
        settings.state.customPrompts.remove(custom)
        val index = list.selectedIndex
        listModel.removeElement(entry)
        if (!listModel.isEmpty) {
            list.selectedIndex = index.coerceAtMost(listModel.size() - 1)
        }
    }
}
