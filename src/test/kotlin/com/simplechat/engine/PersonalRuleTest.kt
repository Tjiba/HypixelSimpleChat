package com.simplechat.engine

import com.simplechat.config.PersonalRuleSettings
import com.simplechat.config.PersonalRules
import com.simplechat.config.HscConfig
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.Optional

class PersonalRuleTest {
    private class Config(dir: Path) : HscConfig("personal-test", dir) {
        init { category(PersonalRules) }
    }

    @Test fun `personal rules persist through the settings file`(@TempDir dir: Path) {
        val previous = PersonalRules.rules
        val rules = listOf(PersonalRule(text = "hello, world", color = 0xFEDCBA, bold = true, italic = true, underlined = true),
            PersonalRule(text = "spam", action = PersonalAction.HIDE, enabled = false))
        try {
            PersonalRules.replace(rules)
            Config(dir).save()
            PersonalRules.replace(emptyList())
            Config(dir).load()
            assertEquals(rules, PersonalRules.list())
        } finally {
            PersonalRules.rules = previous
        }
    }

    @Test fun `matching ignores colors and case but treats punctuation literally`() {
        val rule = PersonalRule(text = "[Rare]")
        assertTrue(rule.matches("§a[RARE] item", Channel.SYSTEM))
        assertFalse(rule.matches("Rare item", Channel.SYSTEM))
        assertFalse(rule.copy(caseSensitive = true).matches("[RARE] item", Channel.SYSTEM))
        assertFalse(rule.copy(enabled = false).matches("[Rare] item", Channel.SYSTEM))
        assertFalse(rule.copy(text = " ").matches("anything", Channel.SYSTEM))
    }

    @Test fun `channel scope includes officer and excludes unrelated messages`() {
        val rule = PersonalRule(text = "hello", channel = PersonalChannel.GUILD)
        assertTrue(rule.matches("Officer > Timo: hello", Channel.OFFICER))
        assertTrue(rule.matches("Guild > Timo: hello", Channel.GUILD))
        assertFalse(rule.matches("Party > Timo: hello", Channel.PARTY))
    }

    @Test fun `rules survive serialization with commas quotes and custom colors`() {
        val rules = listOf(PersonalRule(text = "hello, \"Timo\"", color = 0x123456, bold = true, italic = true, underlined = true),
            PersonalRule(text = "spam", enabled = false, action = PersonalAction.HIDE,
                channel = PersonalChannel.PUBLIC, caseSensitive = true))
        assertEquals(rules, PersonalRule.decode(PersonalRule.encode(rules)))
        assertEquals(rules.first(), PersonalRuleSettings(rules.first()).rule())
    }

    @Test fun `malformed entries do not discard other valid rules`() {
        assertEquals(listOf(PersonalRule(text = "valid")), PersonalRule.decode("""[{"text":"valid"},{"action":"unknown"}]"""))
        assertTrue(PersonalRule.decode("broken json").isEmpty())
    }

    @Test fun `highlight preserves inherited clicks hover and formatting`() {
        val click = ClickEvent.SuggestCommand("/msg Timo ")
        val hover = HoverEvent.ShowText(Component.literal("Details"))
        val message = Component.empty().withStyle { it.withClickEvent(click).withHoverEvent(hover).withBold(true) }
            .append(Component.literal("Hello ").withStyle { it.withColor(0x00FF00) })
            .append(Component.literal("Timo").withStyle { it.withColor(0xFF0000) })
        val result = SegRender.highlight(message, PersonalRule(text = "Timo", color = 0x123456))
        assertEquals("Hello Timo", result.string)
        var parts = 0
        result.visit({ style, text ->
            if (text.isNotEmpty()) {
                parts++
                assertEquals(if (text == "Timo") 0x123456 else 0x00FF00, style.color?.value)
                assertEquals(click, style.clickEvent)
                assertEquals(hover, style.hoverEvent)
                assertTrue(style.isBold)
            }
            Optional.empty<Unit>()
        }, Style.EMPTY)
        assertEquals(2, parts)
    }

    @Test fun `highlight finds literal occurrences across components and respects case`() {
        val message = Component.empty()
            .append(Component.literal("Before [Ra").withStyle { it.withColor(0x00FF00) })
            .append(Component.literal("re] and [RARE] after Rare").withStyle { it.withColor(0xFF0000) })
        for (caseSensitive in listOf(false, true)) {
            val result = SegRender.highlight(message, PersonalRule(text = "[Rare]", color = 0x123456,
                caseSensitive = caseSensitive))
            assertEquals(message.string, result.string)
            val selected = StringBuilder()
            result.visit({ style, text ->
                if (style.color?.value == 0x123456) selected.append(text)
                else if (text.isNotEmpty()) assertEquals(if (text == "Before ") 0x00FF00 else 0xFF0000, style.color?.value)
                Optional.empty<Unit>()
            }, Style.EMPTY)
            assertEquals(if (caseSensitive) "[Rare]" else "[Rare][RARE]", selected.toString())
        }
    }

    @Test fun `highlight ignores legacy colors and leaves absent or blank matches unchanged`() {
        val message = Component.literal("Before §aTimo§r after").withStyle { it.withColor(0x00FF00) }
        val result = SegRender.highlight(message, PersonalRule(text = "Timo", color = 0x123456))
        assertEquals("Before Timo after", result.string)
        result.visit({ style, text ->
            if (text.isNotEmpty()) assertEquals(if (text == "Timo") 0x123456 else 0x00FF00, style.color?.value)
            Optional.empty<Unit>()
        }, Style.EMPTY)
        for (text in listOf("missing", "", " ")) {
            assertTrue(message === SegRender.highlight(message, PersonalRule(text = text)))
        }
    }

    @Test fun `highlight keeps show item colors and hover outside the matching name`() {
        val click = ClickEvent.SuggestCommand("/msg LaMeteo ")
        val hover = HoverEvent.ShowText(Component.literal("Item details"))
        val message = Component.empty().withStyle { it.withClickEvent(click) }
            .append(Component.literal("§7[§6425§7] §6⚒ §b[MVP§a+§b] LaMeteo §7is holding "))
            .append(Component.literal("§6[Fabled Sting ✪✪✪✪✪]").withStyle { it.withHoverEvent(hover) })
        val result = SegRender.highlight(message, PersonalRule(text = "Meteo", color = 0x0088FF, underlined = true))
        val parts = mutableListOf<Pair<String, Int?>>()
        result.visit({ style, text ->
            if (text.isNotEmpty()) {
                parts.add(text to style.color?.value)
                assertEquals(click, style.clickEvent)
                assertEquals(text == "Meteo", style.isUnderlined)
                assertEquals(if (text.startsWith("[Fabled")) hover else null, style.hoverEvent)
            }
            Optional.empty<Unit>()
        }, Style.EMPTY)
        assertEquals(listOf("[" to 0xAAAAAA, "425" to 0xFFAA00, "] " to 0xAAAAAA,
            "⚒ " to 0xFFAA00, "[MVP" to 0x55FFFF, "+" to 0x55FF55, "] La" to 0x55FFFF,
            "Meteo" to 0x0088FF, " " to 0x55FFFF, "is holding " to 0xAAAAAA,
            "[Fabled Sting ✪✪✪✪✪]" to 0xFFAA00), parts)
    }

    @Test fun `highlight preserves legacy formatting and resets to inherited style`() {
        val message = Component.literal("§6§lGold §oTimo§r base §aGreen")
            .withStyle { it.withColor(0x123456).withUnderlined(true) }
        val result = SegRender.highlight(message, PersonalRule(text = "Timo", color = 0x0088FF))
        val parts = mutableListOf<Pair<String, Int?>>()
        result.visit({ style, text ->
            if (text.isNotEmpty()) {
                parts.add(text to style.color?.value)
                assertEquals(text == "Gold " || text == "Timo", style.isBold)
                assertEquals(text == "Timo", style.isItalic)
                assertEquals(text == " base ", style.isUnderlined)
            }
            Optional.empty<Unit>()
        }, Style.EMPTY)
        assertEquals(listOf("Gold " to 0xFFAA00, "Timo" to 0x0088FF,
            " base " to 0x123456, "Green" to 0x55FF55), parts)
    }

    @Test fun `highlight keeps unicode and literal ampersands around legacy text`() {
        val message = Component.literal("§6💎 R&D §bTi").append(Component.literal("mo §a✪"))
        val result = SegRender.highlight(message, PersonalRule(text = "Timo", color = 0x0088FF))
        assertEquals("💎 R&D Timo ✪", result.string)
        val selected = StringBuilder()
        result.visit({ style, text ->
            if (style.color?.value == 0x0088FF) selected.append(text)
            Optional.empty<Unit>()
        }, Style.EMPTY)
        assertEquals("Timo", selected.toString())
    }

    @Test fun `highlight adds each style independently and keeps interactions`() {
        val click = ClickEvent.SuggestCommand("/msg Timo ")
        val hover = HoverEvent.ShowText(Component.literal("Details"))
        val message = Component.literal("Hello Timo!").withStyle { it.withClickEvent(click).withHoverEvent(hover) }
        for (flags in 0..7) {
            val bold = flags and 1 != 0
            val italic = flags and 2 != 0
            val underlined = flags and 4 != 0
            val result = SegRender.highlight(message, PersonalRule(text = "Timo", color = 0xABCDEF,
                bold = bold, italic = italic, underlined = underlined))
            result.visit({ style, text ->
                if (text.isNotEmpty()) {
                    assertEquals(text == "Timo" && bold, style.isBold)
                    assertEquals(text == "Timo" && italic, style.isItalic)
                    assertEquals(text == "Timo" && underlined, style.isUnderlined)
                    assertEquals(click, style.clickEvent)
                    assertEquals(hover, style.hoverEvent)
                }
                Optional.empty<Unit>()
            }, Style.EMPTY)
        }
    }
}
