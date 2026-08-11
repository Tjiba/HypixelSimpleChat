package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Dialogues PNJ et sonnerie Abiphone. */
object Npc {

    val NPC_DIALOG = Group("npcDialog", "NPC dialog", Category.SKYBLOCK, "WORLD & EVENTS", RuleAction.GREY,
        description = "OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove")
    val ABIPHONE = Group("abiphoneRing", "Abiphone ring", Category.SKYBLOCK, "GENERAL", RuleAction.HIDE,
        description = "'✆ RING…' lines — the clickable pickup line always stays")

    val rules =
        rules(NPC_DIALOG) {
            rule("npc-dialog", RuleAction.GREY,
                "^\\[NPC] ",
                compact = { it.raw.replaceFirst(Regex("\\[NPC]\\s*"), "") },
                sample = "§e[NPC] Simon§f: We hope you enjoy the festivities this year!")
        } +
        rules(ABIPHONE) {
            // La ligne du NPC avec le bouton cliquable n'est jamais touchée
            // (garde-fou ClickEvent dans ChatHudMixin).
            rule("abiphone-ring", RuleAction.HIDE,
                "^✆ RING",
                compact = { "§a✆ §7Ring…" },
                sample = "§a✆ RING... RING...")
        }
}
