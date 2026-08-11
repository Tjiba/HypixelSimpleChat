package com.simplechat.rules.system

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Lignes techniques du serveur : identifiant de profil, routage. */
object Server {

    val PROFILE_ID = Group("profileId", "Profile ID line", Category.SYSTEM, "", RuleAction.COMPACT)
    val ROUTING = Group("serverRouting", "Server routing / warping", Category.SYSTEM, "", RuleAction.COMPACT,
        description = "OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove")

    val rules =
        rules(PROFILE_ID) {
            rule("profile-id", RuleAction.COMPACT,
                "^Profile ID: (.+)$",
                compact = { "§8${it[1]}" },
                sample = "Profile ID: ceccda75-3780-4791-b93c-87d1e7bc397f")
        } +
        rules(ROUTING) {
            rule("server-routing", RuleAction.COMPACT,
                "^(?:Sending to server (.+?)\\.\\.\\.|Warping\\.\\.\\.|Request join for Hub #\\d+ \\(.+\\)\\.\\.\\.|Sending you to .+!)",
                compact = { if (it[1].isEmpty()) "§7Warping..." else "§8→ §7${it[1]}" },
                sample = "Sending to server mega8E...")
        }
}
