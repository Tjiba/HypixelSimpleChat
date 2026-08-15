package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Dialogues PNJ et sonnerie Abiphone. */
object Npc {

    val NPC_DIALOG = Group("npcDialog", "NPC dialog", Category.SKYBLOCK, "WORLD & EVENTS", RuleAction.GREY,
        description = "OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove")
    val ABIPHONE = Group("abiphoneRing", "Abiphone ring", Category.SKYBLOCK, "GENERAL", RuleAction.HIDE,
        description = "'✆ RING…' lines. The clickable pickup line always stays")

    val rules =
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
