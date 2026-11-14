package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.Section
import com.simplechat.rules.rules

/** Dialogues PNJ et sonnerie Abiphone. */
object Npc {

    val NPC_DIALOG = Group("npcDialog", "NPC dialog", Category.SKYBLOCK, Section.WORLD, RuleAction.GREY,
        description = "OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove")
    val ABIPHONE = Group("abiphoneRing", "Abiphone ring", Category.SKYBLOCK, Section.GENERAL, RuleAction.HIDE,
        description = "'✆ RING…' lines. The clickable pickup line always stays")
    val WEATHER_ALERTS = Group("weatherAlerts", "Weather alerts", Category.SKYBLOCK, Section.WORLD, RuleAction.COMPACT,
        description = "Professor Wynd's automatic alerts. Compact keeps weather and location, skips the introduction",
        split = false)

    val rules =
        rules(WEATHER_ALERTS) {
            rule("weather-intro", RuleAction.COMPACT,
                "^(?:\\[NPC] )?Professor Wynd: ✆ Hello! This is an automatic update brought to you by the Weather Alert System\\.$",
                compact = { "" },
                sample = "§eProfessor Wynd: §b✆ §fHello! This is an automatic update brought to you by the §6Weather Alert System§f.",
                title = "Weather alert introduction")
            rule("weather-detected", RuleAction.COMPACT,
                "^(?:\\[NPC] )?Professor Wynd: ✆ (?:Our systems have detected|\\.\\.\\. They have also detected) (.+?) on the (.+?)\\.$",
                compact = { "§bWeather §8· ${Fmt.rawSpan(it.raw, it[1])} §8· ${Fmt.rawSpan(it.raw, it[2])}" },
                sample = "§eProfessor Wynd: §b✆ §fOur systems have detected §7Rockfall §2on the §5Crystal Hollows§2.",
                title = "Weather detected")
        } +
        rules(NPC_DIALOG) {
            rule("npc-dialog", RuleAction.GREY,
                "^\\[NPC] ",
                compact = { Fmt.stripNpc(it.raw) },
                sample = "§e[NPC] Simon§f: We hope you enjoy the festivities this year!")
        } +
        rules(ABIPHONE) {
            // Le bouton [PICK UP] est réattaché au compact par le mixin.
            rule("abiphone-ring", RuleAction.HIDE,
                "^✆ RING",
                compact = { "§a✆ §7Ring…" },
                sample = "§a✆ RING... RING...")
        }
}
