package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Quêtes slayer. La sonnerie Abiphone (« ✆ RING ») n'est pas là : elle est actionnable. */
object Slayer {

    val SLAYER = Group(
        id = "slayer",
        title = "Slayer",
        category = Category.SKYBLOCK,
        section = "COMBAT",
        default = RuleAction.HIDE,
        description = "Quest started/complete, slay lines",
    )

    val rules = rules(SLAYER) {
        rule("slayer-quest-started", RuleAction.HIDE,
            "^SLAYER QUEST STARTED!",
            compact = { "§5Slayer §7· started" },
            sample = "  §5§lSLAYER QUEST STARTED!",
            title = "Quest started")
        rule("slayer-quest-complete", RuleAction.HIDE,
            "^SLAYER QUEST COMPLETE!",
            compact = { "§5Slayer §a✔ §fcomplete" },
            sample = "  §5§lSLAYER QUEST COMPLETE!",
            title = "Quest complete")
        rule("slayer-objective", RuleAction.HIDE,
            "^» Slay ([\\d,]+) Combat XP worth of (.+?)\\.?$",
            compact = { "§7Slay §f${it[1]} XP §7of ${it[2]}" },
            sample = "  §5§l» §7Slay §c20,000 Combat XP §7worth of Zombies.",
            title = "Slay objective")
    }
}
