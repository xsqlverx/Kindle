package com.example

import com.example.data.assistant.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantRoutingTest {
    @Test fun onlyConfiguredProvidersReceiveFallback() {
        val data = AssistantData(selected = AssistantProvider.CEREBRAS,
            keys = mapOf(AssistantProvider.GROQ to "test-key", AssistantProvider.CEREBRAS to "test-key", AssistantProvider.NVIDIA to ""))
        assertEquals(listOf(AssistantProvider.CEREBRAS, AssistantProvider.GROQ), AssistantRouting.providers(data))
        assertEquals(listOf(AssistantProvider.CEREBRAS), AssistantRouting.providers(data.copy(fallback = false)))
        assertTrue(AssistantRouting.providers(data.copy(keys = emptyMap())).isEmpty())
    }

    @Test fun retriesAndTruncationKeepValidAlternatingTurns() {
        val messages = listOf(AssistantMessage("assistant", "old"), AssistantMessage("user", "first"),
            AssistantMessage("user", "retry"), AssistantMessage("assistant", "answer"), AssistantMessage("user", "next"))
        val turns = AssistantRouting.turns(messages)
        assertEquals(listOf("user", "assistant", "user"), turns.map { it.role })
        assertEquals("first\nretry", turns.first().text)
    }

    @Test fun openRouterUsesOnlyItsFreeRouter() {
        assertEquals("openrouter/free", AssistantProvider.OPENROUTER.model)
        assertTrue(AssistantProvider.entries.all { it.endpoint.startsWith("https://") })
    }
}
