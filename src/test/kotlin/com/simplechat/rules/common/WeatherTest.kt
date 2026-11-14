package com.simplechat.rules.common

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeatherTest {

    private val intro = "Professor Wynd: ✆ Hello! This is an automatic update brought to you by the Weather Alert System."
    private val alerts = listOf(
        "§eProfessor Wynd: §b✆ §fOur systems have detected §7Rockfall §2on the §5Crystal Hollows§2." to
            "§bWeather §8· §7Rockfall §8· §5Crystal Hollows",
        "§eProfessor Wynd: §b✆ §f... They have also detected §dBlossoming §2on the §3Lotus Atoll§2." to
            "§bWeather §8· §dBlossoming §8· §3Lotus Atoll",
        "§eProfessor Wynd: §b✆ §f... They have also detected §cHellstorm §2on the §cCrimson Isle§2." to
            "§bWeather §8· §cHellstorm §8· §cCrimson Isle",
    )

    @Test fun `compact removes intro and preserves weather and location colors`() {
        assertEquals(Verdict.Hide, ChatRules.evaluate(intro, RuleConfig.DEFAULT))
        for ((raw, compact) in alerts) {
            assertEquals(Verdict.Replace(compact), ChatRules.evaluate(raw, RuleConfig.DEFAULT))
        }
    }

    @Test fun `all modes apply to the complete weather alert`() {
        for (action in RuleAction.entries) {
            val cfg = RuleConfig.DEFAULT.copy(groupActions = mapOf(Npc.WEATHER_ALERTS.id to action))
            for ((raw, compact) in alerts + (intro to "")) {
                val expected = when (action) {
                    RuleAction.OFF -> Verdict.Pass
                    RuleAction.HIDE -> Verdict.Hide
                    RuleAction.GREY -> Verdict.Replace("§8" + ChatRules.clean(raw))
                    RuleAction.COMPACT -> if (compact.isEmpty()) Verdict.Hide else Verdict.Replace(compact)
                    RuleAction.COMPACT_GREY -> if (compact.isEmpty()) Verdict.Hide
                        else Verdict.Replace("§8" + ChatRules.clean(compact))
                }
                assertEquals(expected, ChatRules.evaluate(raw, cfg), action.name)
            }
        }
    }

    @Test fun `disabled mod or skyblock leaves alerts untouched`() {
        for (cfg in listOf(RuleConfig.DEFAULT.copy(masterEnabled = false), RuleConfig.DEFAULT.copy(skyblockEnabled = false))) {
            for (raw in alerts.map { it.first } + intro) assertEquals(Verdict.Pass, ChatRules.evaluate(raw, cfg))
        }
    }

    @Test fun `weather setting takes priority over generic npc dialog`() {
        val cfg = RuleConfig.DEFAULT.copy(groupActions = mapOf(Npc.WEATHER_ALERTS.id to RuleAction.OFF,
            Npc.NPC_DIALOG.id to RuleAction.HIDE))
        assertEquals(Verdict.Pass, ChatRules.evaluate("[NPC] " + alerts.first().first, cfg))
        assertEquals(Verdict.Replace(alerts.first().second),
            ChatRules.evaluate("[NPC] " + alerts.first().first, RuleConfig.DEFAULT))
    }

    @Test fun `quest dialogue and incoming call remain untouched`() {
        val cfg = RuleConfig.DEFAULT.copy(groupActions = mapOf(Npc.WEATHER_ALERTS.id to RuleAction.HIDE))
        for (raw in listOf("✆ Professor Wynd ✆", "Professor Wynd: ✆ What can I help you with today?",
            "Professor Wynd: ✆ Since we're buddy-buddy now, I'll grant you access to my Weather Alert System.",
            "Someone else: ✆ Our systems have detected Rockfall on the Crystal Hollows.")) {
            assertEquals(Verdict.Pass, ChatRules.evaluate(raw, cfg))
        }
    }

    @Test fun `player quoting an alert stays a player message`() {
        val cfg = RuleConfig.DEFAULT.copy(groupActions = mapOf(Npc.WEATHER_ALERTS.id to RuleAction.HIDE),
            partyStyle = RuleConfig.DEFAULT.partyStyle.copy(compact = false))
        assertEquals(Verdict.Pass, ChatRules.evaluate("Party > Timo: $intro", cfg))
    }
}
