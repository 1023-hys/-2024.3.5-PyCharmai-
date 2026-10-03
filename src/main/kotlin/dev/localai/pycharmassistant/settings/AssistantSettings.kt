package dev.localai.pycharmassistant.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "OpenAiAssistantSettings", storages = [Storage("openAiAssistant.xml")])
class AssistantSettings : PersistentStateComponent<AssistantSettings.SettingsState> {
    data class SettingsState(
        var baseUrl: String = "https://api.openai.com/v1",
        var model: String = "gpt-4o-mini",
        var timeoutSeconds: Int = 30,
        var promptTemplate: String = DEFAULT_PROMPT_TEMPLATE,
        var enableContext: Boolean = true,
        var maxContextChars: Int = 4000,
        var autoCompleteEnabled: Boolean = true,
        var ollamaEnabled: Boolean = false,
        var ollamaBaseUrl: String = "http://localhost:11434",
        var ollamaModel: String = "qwen2.5-coder:7b",
        var ollamaTimeoutSeconds: Int = 300,
        var includeSelectionInCompletion: Boolean = true,
        var includeFullFileInCompletion: Boolean = true,
        var autoCompleteDelaySeconds: Int = 2,
        var requireStableCaretForAutoComplete: Boolean = true
    )

    private var settingsState = SettingsState()

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        settingsState = state
    }

    fun getApiKey(): String? = PasswordSafe.instance
        .getPassword(CredentialAttributes(API_KEY_CREDENTIAL_ID))

    fun getPromptTemplate(): String = state.promptTemplate.ifBlank { DEFAULT_PROMPT_TEMPLATE }

    fun isContextEnabled(): Boolean = state.enableContext

    fun getMaxContextChars(): Int = state.maxContextChars.coerceAtLeast(200).coerceAtMost(20000)

    fun isAutoCompleteEnabled(): Boolean = state.autoCompleteEnabled

    fun isOllamaEnabled(): Boolean = state.ollamaEnabled

    fun setApiKey(apiKey: String?) {
        PasswordSafe.instance.setPassword(
            CredentialAttributes(API_KEY_CREDENTIAL_ID),
            apiKey?.takeIf(String::isNotBlank)
        )
    }

    companion object {
        private const val API_KEY_CREDENTIAL_ID = "dev.localai.pycharmassistant.apiKey"
        const val DEFAULT_PROMPT_TEMPLATE = "You are an expert coding assistant. Keep the answer concise and return only the code needed for the task."

        fun getInstance(): AssistantSettings =
            ApplicationManager.getApplication().getService(AssistantSettings::class.java)
    }
}