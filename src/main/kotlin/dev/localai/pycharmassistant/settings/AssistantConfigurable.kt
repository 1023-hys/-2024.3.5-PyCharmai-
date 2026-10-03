package dev.localai.pycharmassistant.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class AssistantConfigurable : Configurable {
    private lateinit var baseUrlField: JBTextField
    private lateinit var modelField: JBTextField
    private lateinit var promptTemplateField: JBTextArea
    private lateinit var apiKeyField: JPasswordField
    private lateinit var timeoutField: JSpinner
    private lateinit var enableContextCheckBox: JBCheckBox
    private lateinit var maxContextCharsField: JSpinner
    private lateinit var autoCompleteCheckBox: JBCheckBox
    private lateinit var autoCompleteDelayField: JSpinner
    private lateinit var stableCaretCheckBox: JBCheckBox
    private lateinit var includeSelectionCheckBox: JBCheckBox
    private lateinit var includeFullFileCheckBox: JBCheckBox
    private lateinit var ollamaCheckBox: JBCheckBox
    private lateinit var ollamaBaseUrlField: JBTextField
    private lateinit var ollamaModelField: JBTextField
    private lateinit var ollamaTimeoutField: JSpinner
    private lateinit var clearApiKeyCheckBox: JBCheckBox
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "OpenAI Code Assistant"

    override fun createComponent(): JComponent {
        baseUrlField = JBTextField()
        modelField = JBTextField()
        promptTemplateField = JBTextArea(AssistantSettings.DEFAULT_PROMPT_TEMPLATE, 4, 40).apply {
            lineWrap = true
            wrapStyleWord = true
        }
        apiKeyField = JPasswordField()
        timeoutField = JSpinner(SpinnerNumberModel(30, 1, 300, 1))
        enableContextCheckBox = JBCheckBox("Enable context for AI requests")
        maxContextCharsField = JSpinner(SpinnerNumberModel(4000, 200, 20000, 100))
        autoCompleteCheckBox = JBCheckBox("Enable automatic AI completion")
        autoCompleteDelayField = JSpinner(SpinnerNumberModel(2, 1, 30, 1))
        stableCaretCheckBox = JBCheckBox("Wait until the caret stays still for the delay")
        includeSelectionCheckBox = JBCheckBox("Include selected text in completion requests")
        includeFullFileCheckBox = JBCheckBox("Include the entire current file in completion requests")
        ollamaCheckBox = JBCheckBox("Use Ollama instead of OpenAI-compatible API")
        ollamaBaseUrlField = JBTextField()
        ollamaModelField = JBTextField()
        ollamaTimeoutField = JSpinner(SpinnerNumberModel(300, 30, 1800, 30))
        clearApiKeyCheckBox = JBCheckBox("Remove saved API key")

        return JPanel(GridBagLayout()).also { form ->
            panel = form
            addRow(form, 0, "API base URL", baseUrlField)
            addRow(form, 1, "Model", modelField)
            addRow(form, 2, "Prompt template", JScrollPane(promptTemplateField).apply {
                border = JBUI.Borders.empty()
            })
            addRow(form, 3, "API key", apiKeyField)
            addRow(form, 4, "Request timeout (seconds)", timeoutField)
            addRow(form, 5, "Max context chars", maxContextCharsField)
            val enableConstraints = GridBagConstraints().apply {
                gridx = 1
                gridy = 6
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 0, 4, 0)
            }
            form.add(enableContextCheckBox, enableConstraints)
            val selectionConstraints = GridBagConstraints().apply {
                gridx = 1
                gridy = 7
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 0, 4, 0)
            }
            form.add(includeSelectionCheckBox, selectionConstraints)
            val fullFileConstraints = GridBagConstraints().apply {
                gridx = 1
                gridy = 8
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 0, 4, 0)
            }
            form.add(includeFullFileCheckBox, fullFileConstraints)
            val autoConstraints = GridBagConstraints().apply {
                gridx = 1
                gridy = 9
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 0, 4, 0)
            }
            form.add(autoCompleteCheckBox, autoConstraints)
            addRow(form, 10, "Auto-completion delay (seconds)", autoCompleteDelayField)
            val stableCaretConstraints = GridBagConstraints().apply {
                gridx = 1
                gridy = 11
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 0, 4, 0)
            }
            form.add(stableCaretCheckBox, stableCaretConstraints)
            ollamaCheckBox.addActionListener { updateOllamaFields() }
            val ollamaConstraints = GridBagConstraints().apply {
                gridx = 1
                gridy = 12
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 0, 4, 0)
            }
            form.add(ollamaCheckBox, ollamaConstraints)
            addRow(form, 13, "Ollama base URL", ollamaBaseUrlField)
            addRow(form, 14, "Ollama model", ollamaModelField)
            addRow(form, 15, "Ollama response timeout (seconds)", ollamaTimeoutField)
            val clearConstraints = GridBagConstraints().apply {
                gridx = 1
                gridy = 16
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 8, 4, 0)
            }
            form.add(clearApiKeyCheckBox, clearConstraints)
            reset()
        }
    }

    override fun isModified(): Boolean {
        val state = AssistantSettings.getInstance().state
        return baseUrlField.text != state.baseUrl ||
            modelField.text != state.model ||
            promptTemplateField.text != state.promptTemplate ||
            timeoutField.value != state.timeoutSeconds ||
            enableContextCheckBox.isSelected != state.enableContext ||
            maxContextCharsField.value != state.maxContextChars ||
            autoCompleteCheckBox.isSelected != state.autoCompleteEnabled ||
            autoCompleteDelayField.value != state.autoCompleteDelaySeconds ||
            stableCaretCheckBox.isSelected != state.requireStableCaretForAutoComplete ||
            includeSelectionCheckBox.isSelected != state.includeSelectionInCompletion ||
            includeFullFileCheckBox.isSelected != state.includeFullFileInCompletion ||
            ollamaCheckBox.isSelected != state.ollamaEnabled ||
            ollamaBaseUrlField.text != state.ollamaBaseUrl ||
            ollamaModelField.text != state.ollamaModel ||
            ollamaTimeoutField.value != state.ollamaTimeoutSeconds ||
            apiKeyField.password.isNotEmpty() ||
            clearApiKeyCheckBox.isSelected
    }

    override fun apply() {
        val settings = AssistantSettings.getInstance()
        settings.state.baseUrl = baseUrlField.text.trim()
        settings.state.model = modelField.text.trim()
        settings.state.promptTemplate = promptTemplateField.text.trim()
        settings.state.timeoutSeconds = timeoutField.value as Int
        settings.state.enableContext = enableContextCheckBox.isSelected
        settings.state.maxContextChars = maxContextCharsField.value as Int
        settings.state.autoCompleteEnabled = autoCompleteCheckBox.isSelected
        settings.state.autoCompleteDelaySeconds = autoCompleteDelayField.value as Int
        settings.state.requireStableCaretForAutoComplete = stableCaretCheckBox.isSelected
        settings.state.includeSelectionInCompletion = includeSelectionCheckBox.isSelected
        settings.state.includeFullFileInCompletion = includeFullFileCheckBox.isSelected
        settings.state.ollamaEnabled = ollamaCheckBox.isSelected
        settings.state.ollamaBaseUrl = ollamaBaseUrlField.text.trim()
        settings.state.ollamaModel = ollamaModelField.text.trim()
        settings.state.ollamaTimeoutSeconds = ollamaTimeoutField.value as Int
        when {
            clearApiKeyCheckBox.isSelected -> settings.setApiKey(null)
            apiKeyField.password.isNotEmpty() -> settings.setApiKey(String(apiKeyField.password))
        }
        reset()
    }

    override fun reset() {
        val settings = AssistantSettings.getInstance()
        baseUrlField.text = settings.state.baseUrl
        modelField.text = settings.state.model
        promptTemplateField.text = settings.state.promptTemplate.ifBlank { AssistantSettings.DEFAULT_PROMPT_TEMPLATE }
        timeoutField.value = settings.state.timeoutSeconds
        enableContextCheckBox.isSelected = settings.state.enableContext
        maxContextCharsField.value = settings.state.maxContextChars
        autoCompleteCheckBox.isSelected = settings.state.autoCompleteEnabled
        autoCompleteDelayField.value = settings.state.autoCompleteDelaySeconds.coerceIn(1, 30)
        stableCaretCheckBox.isSelected = settings.state.requireStableCaretForAutoComplete
        includeSelectionCheckBox.isSelected = settings.state.includeSelectionInCompletion
        includeFullFileCheckBox.isSelected = settings.state.includeFullFileInCompletion
        ollamaCheckBox.isSelected = settings.state.ollamaEnabled
        ollamaBaseUrlField.text = settings.state.ollamaBaseUrl
        ollamaModelField.text = settings.state.ollamaModel
        ollamaTimeoutField.value = settings.state.ollamaTimeoutSeconds.coerceIn(30, 1800)
        updateOllamaFields()
        apiKeyField.text = ""
        clearApiKeyCheckBox.isSelected = false
    }

    override fun disposeUIResources() {
        panel = null
    }

    private fun addRow(form: JPanel, row: Int, label: String, component: JComponent) {
        form.add(JLabel(label), GridBagConstraints().apply {
            gridx = 0
            gridy = row
            anchor = GridBagConstraints.WEST
            insets = Insets(4, 0, 4, 8)
        })
        form.add(component, GridBagConstraints().apply {
            gridx = 1
            gridy = row
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(4, 0, 4, 0)
        })
    }

    private fun updateOllamaFields() {
        val enabled = ollamaCheckBox.isSelected
        baseUrlField.isEnabled = !enabled
        modelField.isEnabled = !enabled
        apiKeyField.isEnabled = !enabled
        clearApiKeyCheckBox.isEnabled = !enabled
        ollamaBaseUrlField.isEnabled = enabled
        ollamaModelField.isEnabled = enabled
        ollamaTimeoutField.isEnabled = enabled
    }
}