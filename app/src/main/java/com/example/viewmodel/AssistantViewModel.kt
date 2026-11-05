package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.assistant.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AssistantStore(application)
    private val client = AssistantClient()
    private val loaded = runCatching { store.read() }
    private val _data = MutableStateFlow(loaded.getOrDefault(AssistantData()))
    val data = _data.asStateFlow()
    private val _error = MutableStateFlow<String?>(if (loaded.isFailure) "Could not unlock assistant storage on this device. Existing data was not overwritten." else null)
    val error = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _using = MutableStateFlow<String?>(null)
    val using = _using.asStateFlow()
    private val available = loaded.isSuccess

    private fun save(next: AssistantData): Boolean {
        if (!available) return false
        return try { store.write(next); _data.value = next; _error.value = null; true }
        catch (_: Exception) { _error.value = "Could not save securely on this device. Nothing was replaced."; false }
    }

    fun setKey(provider: AssistantProvider, key: String): Boolean {
        if (_busy.value) return false
        val cleaned = key.trim()
        if (cleaned.any { it.isWhitespace() } || cleaned.length > 512) { _error.value = "Check the key: it should not contain spaces."; return false }
        val keys = _data.value.keys.toMutableMap()
        if (cleaned.isEmpty()) keys.remove(provider) else keys[provider] = cleaned
        return save(_data.value.copy(keys = keys))
    }
    fun select(provider: AssistantProvider) { if (!_busy.value) save(_data.value.copy(selected = provider)) }
    fun setFallback(value: Boolean) { if (!_busy.value) save(_data.value.copy(fallback = value)) }
    fun setShareContext(value: Boolean) { if (!_busy.value) save(_data.value.copy(shareContext = value)) }
    fun setMemory(value: String) { if (!_busy.value) save(_data.value.copy(memory = value.take(4000))) }
    fun clearChat() { if (!_busy.value) save(_data.value.copy(messages = emptyList())) }

    /** Local answers are saved alongside chat without contacting any configured provider. */
    fun replyFromSavedDay(question: String, snapshot: AssistantDaySnapshot): Boolean {
        if (_busy.value || question.isBlank()) return false
        val messages = (_data.value.messages + AssistantMessage("user", question.trim().take(4000)) +
            AssistantMessage("assistant", AssistantDay.summary(snapshot), "On device")).takeLast(30)
        return save(_data.value.copy(messages = messages))
    }

    fun send(question: String, context: String): Boolean {
        val text = question.trim().take(4000)
        if (text.isEmpty() || _busy.value) return false
        val state = _data.value
        val ordered = AssistantRouting.providers(state)
        if (ordered.isEmpty()) { _error.value = "Add an API key in Assistant settings first."; return false }
        val conversation = (state.messages + AssistantMessage("user", text)).takeLast(30)
        if (!save(state.copy(messages = conversation))) return false
        _busy.value = true
        viewModelScope.launch {
            var lastError = "No provider returned a response."
            var answer: AssistantReply? = null
            var answeredBy: AssistantProvider? = null
            for (provider in ordered) {
                _using.value = provider.label
                val result = withContext(Dispatchers.IO) {
                    runCatching { client.ask(provider, state.keys.getValue(provider), conversation,
                        if (state.shareContext) context + "\nPersonal memory explicitly saved by the user: " + state.memory else "The user chose not to share saved app context or memory.") }
                }
                if (result.isSuccess) { answer = result.getOrNull(); answeredBy = provider; break }
                val exception = result.exceptionOrNull()
                lastError = if (exception is AssistantHttpException) "${provider.label} returned HTTP ${exception.status}. Check the key, model availability or quota."
                    else "${provider.label} could not be reached or returned an unreadable response."
            }
            if (answer != null && answeredBy != null) {
                save(_data.value.copy(messages = (conversation + AssistantMessage("assistant", answer.text, answeredBy.label, answer.truncated)).takeLast(30)))
            } else _error.value = "$lastError Your message is kept; you can retry."
            _using.value = null
            _busy.value = false
        }
        return true
    }
}
