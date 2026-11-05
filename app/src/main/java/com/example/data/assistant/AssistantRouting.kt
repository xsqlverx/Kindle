package com.example.data.assistant

/** Only configured providers can receive a request; fallback order is visible and deterministic. */
object AssistantRouting {
    fun providers(data: AssistantData): List<AssistantProvider> =
        (listOf(data.selected) + if (data.fallback) AssistantProvider.entries.filter { it != data.selected } else emptyList())
            .filter { !data.keys[it].isNullOrBlank() }

    fun turns(messages: List<AssistantMessage>): List<AssistantMessage> {
        val result = mutableListOf<AssistantMessage>()
        messages.takeLast(12).dropWhile { it.role != "user" }.forEach { message ->
            if (result.lastOrNull()?.role == message.role) {
                val previous = result.removeAt(result.lastIndex)
                result.add(previous.copy(text = (previous.text + "\n" + message.text).takeLast(4000)))
            } else result.add(message)
        }
        return result
    }

    fun cloudTurns(messages: List<AssistantMessage>): List<AssistantMessage> = turns(messages.filterNot { it.provider == "On device" })
}
