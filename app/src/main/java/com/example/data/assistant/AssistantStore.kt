package com.example.data.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class AssistantProvider(val label: String, val endpoint: String, val model: String, val signup: String) {
    GROQ("Groq", "https://api.groq.com/openai/v1/chat/completions", "openai/gpt-oss-20b", "https://console.groq.com/keys"),
    CEREBRAS("Cerebras", "https://api.cerebras.ai/v1/chat/completions", "qwen-3.8-27b", "https://cloud.cerebras.ai/"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "openrouter/free", "https://openrouter.ai/keys"),
    NVIDIA("NVIDIA", "https://integrate.api.nvidia.com/v1/chat/completions", "meta/llama-3.3-70b-instruct", "https://build.nvidia.com/")
}

data class AssistantMessage(val role: String, val text: String, val provider: String = "")
data class AssistantData(
    val keys: Map<AssistantProvider, String> = emptyMap(),
    val selected: AssistantProvider = AssistantProvider.GROQ,
    val fallback: Boolean = true,
    val shareContext: Boolean = true,
    val memory: String = "",
    val messages: List<AssistantMessage> = emptyList()
)

/** Device-bound keys, memory and chat are encrypted together; excluded from Android backups. */
class AssistantStore(context: Context) {
    private val prefs = context.getSharedPreferences("assistant_private", Context.MODE_PRIVATE)
    private val alias = "kindle.assistant.v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    fun read(): AssistantData {
        val raw = prefs.getString("payload", null) ?: return AssistantData()
        val payload = JSONObject(raw)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(payload.getString("iv"), Base64.NO_WRAP)))
        val plain = String(cipher.doFinal(Base64.decode(payload.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8)
        val json = JSONObject(plain)
        val keys = json.optJSONObject("keys") ?: JSONObject()
        val messages = json.optJSONArray("messages") ?: JSONArray()
        return AssistantData(
            keys = AssistantProvider.entries.mapNotNull { provider -> keys.optString(provider.name).takeIf { it.isNotBlank() }?.let { provider to it } }.toMap(),
            selected = AssistantProvider.entries.firstOrNull { it.name == json.optString("selected") } ?: AssistantProvider.GROQ,
            fallback = json.optBoolean("fallback", true), shareContext = json.optBoolean("shareContext", true),
            memory = json.optString("memory"), messages = (0 until messages.length()).mapNotNull { i ->
                messages.optJSONObject(i)?.let { AssistantMessage(it.optString("role"), it.optString("text"), it.optString("provider")) }
            }.filter { it.role in listOf("user", "assistant") }.takeLast(30)
        )
    }

    fun write(data: AssistantData) {
        val keys = JSONObject().apply { data.keys.forEach { (provider, value) -> put(provider.name, value) } }
        val messages = JSONArray().apply { data.messages.takeLast(30).forEach { put(JSONObject().put("role", it.role).put("text", it.text).put("provider", it.provider)) } }
        val plain = JSONObject().put("keys", keys).put("selected", data.selected.name).put("fallback", data.fallback)
            .put("shareContext", data.shareContext).put("memory", data.memory).put("messages", messages).toString()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val payload = JSONObject().put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)).toString()
        check(prefs.edit().putString("payload", payload).commit()) { "Could not save assistant settings" }
    }
}
