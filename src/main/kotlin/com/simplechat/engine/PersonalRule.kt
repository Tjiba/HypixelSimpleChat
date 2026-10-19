package com.simplechat.engine

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

enum class PersonalAction { HIGHLIGHT, HIDE }
enum class PersonalChannel { ALL, PUBLIC, PARTY, GUILD, WHISPER, SYSTEM }

data class PersonalRule(
    val text: String = "",
    val enabled: Boolean = true,
    val action: PersonalAction = PersonalAction.HIGHLIGHT,
    val color: Int = 0xFFAA00,
    val channel: PersonalChannel = PersonalChannel.ALL,
    val caseSensitive: Boolean = false,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underlined: Boolean = false,
) {
    fun matches(raw: String, source: Channel): Boolean {
        if (!enabled || text.isBlank()) return false
        val actual = if (source == Channel.OFFICER) "GUILD" else source.name
        if (channel != PersonalChannel.ALL && channel.name != actual) return false
        return CustomHide.stripColors(raw).contains(text, ignoreCase = !caseSensitive)
    }

    companion object {
        fun decode(json: String): List<PersonalRule> = runCatching {
            JsonParser.parseString(json).asJsonArray.mapNotNull { item ->
                runCatching {
                    val row = item.asJsonObject
                    PersonalRule(
                        text = row.get("text").asString,
                        enabled = row.get("enabled")?.asBoolean ?: true,
                        action = row.get("action")?.asString?.let { PersonalAction.valueOf(it) } ?: PersonalAction.HIGHLIGHT,
                        color = (row.get("color")?.asInt ?: 0xFFAA00) and 0xFFFFFF,
                        channel = row.get("channel")?.asString?.let { PersonalChannel.valueOf(it) } ?: PersonalChannel.ALL,
                        caseSensitive = row.get("caseSensitive")?.asBoolean ?: false,
                        bold = row.get("bold")?.asBoolean ?: false,
                        italic = row.get("italic")?.asBoolean ?: false,
                        underlined = row.get("underlined")?.asBoolean ?: false,
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())

        fun encode(rules: List<PersonalRule>): String = JsonArray().apply {
            for (rule in rules) add(JsonObject().apply {
                addProperty("text", rule.text)
                addProperty("enabled", rule.enabled)
                addProperty("action", rule.action.name)
                addProperty("color", rule.color)
                addProperty("channel", rule.channel.name)
                addProperty("caseSensitive", rule.caseSensitive)
                addProperty("bold", rule.bold)
                addProperty("italic", rule.italic)
                addProperty("underlined", rule.underlined)
            })
        }.toString()
    }
}
