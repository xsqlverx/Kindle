package com.example.data.assistant

enum class AnswerBlockKind { PARAGRAPH, HEADING, STEP, TABLE_ROW, CODE }
data class AnswerBlock(val kind: AnswerBlockKind, val title: String = "", val body: String = "", val label: String = "")

/** A display parser, never a source of calendar facts or automatic mutations. Raw text is retained. */
object AssistantAnswer {
    private val bullet = Regex("^\\s*(?:[-*•]|\\d+[.)])\\s+(.+)$")
    private val heading = Regex("^\\s*#{1,6}\\s+(.+)$")
    private val separator = Regex("^:?-{3,}:?$")
    private val bold = Regex("\\*\\*(\\S(?:[^\\n]*?\\S)?)\\*\\*")
    private val italic = Regex("(?<![\\w*])\\*([^*\\s](?:[^*\\n]*?[^*\\s])?)\\*(?![\\w*])")

    fun clean(text: String): String = italic.replace(bold.replace(text, "$1"), "$1").trim()
    private fun cells(line: String) = line.trim().removePrefix("|").removeSuffix("|").split('|').map(::clean)
    private fun tableDivider(line: String): Boolean {
        val parts = cells(line)
        return parts.size >= 2 && parts.all { separator.matches(it.replace(" ", "")) }
    }
    private fun splitStep(value: String): Pair<String, String> {
        val text = clean(value)
        val colon = text.indexOf(": ")
        return if (colon in 1..65) text.substring(0, colon).trim() to text.substring(colon + 1).trim()
            else text to ""
    }

    fun parse(raw: String): List<AnswerBlock> {
        val lines = raw.replace("\r\n", "\n").split('\n')
        val blocks = mutableListOf<AnswerBlock>()
        val paragraph = mutableListOf<String>()
        fun flush() { if (paragraph.isNotEmpty()) { blocks.add(AnswerBlock(AnswerBlockKind.PARAGRAPH, body = clean(paragraph.joinToString("\n")))); paragraph.clear() } }
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                flush()
                val language = trimmed.removePrefix("```").trim()
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) { code.add(lines[i]); i++ }
                blocks.add(AnswerBlock(AnswerBlockKind.CODE, body = code.joinToString("\n"), label = language))
            } else if (i + 1 < lines.size && '|' in line && tableDivider(lines[i + 1])) {
                flush()
                val headers = cells(line)
                i += 2
                while (i < lines.size && '|' in lines[i] && lines[i].isNotBlank()) {
                    val row = cells(lines[i])
                    val label = row.firstOrNull().orEmpty()
                    val title = row.getOrNull(1).orEmpty().ifBlank { label }
                    val detail = row.drop(2).mapIndexed { index, value ->
                        val name = headers.getOrNull(index + 2).orEmpty()
                        if (name.isBlank() || value.isBlank()) value else "$name: $value"
                    }.filter(String::isNotBlank).joinToString("\n")
                    blocks.add(AnswerBlock(AnswerBlockKind.TABLE_ROW, title = title, body = detail, label = if (row.size > 1) label else ""))
                    i++
                }
                continue
            } else if (trimmed.isEmpty()) flush()
            else {
                val h = heading.matchEntire(line)
                val b = bullet.matchEntire(line)
                when {
                    h != null -> { flush(); blocks.add(AnswerBlock(AnswerBlockKind.HEADING, title = clean(h.groupValues[1]))) }
                    b != null -> { flush(); val (title, body) = splitStep(b.groupValues[1]); blocks.add(AnswerBlock(AnswerBlockKind.STEP, title, body)) }
                    else -> paragraph.add(line.trim())
                }
            }
            i++
        }
        flush()
        return blocks.filter { it.title.isNotBlank() || it.body.isNotBlank() }
    }
}
