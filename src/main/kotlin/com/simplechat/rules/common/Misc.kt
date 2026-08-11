package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Rappels sans action possible : inventaire plein, items legacy, grenouille fatiguée. */
object Misc {

    val MISC = Group(
        id = "misc",
        title = "Misc",
        category = Category.SKYBLOCK,
        section = "WORLD & EVENTS",
        default = RuleAction.HIDE,
        description = "Inventory full, Legacy Items notice, exhausted Frog",
    )

    val rules = rules(MISC) {
        rule("inventory-full", RuleAction.HIDE,
            "^(?:Inventory full\\? Don't forget to check out your Storage inside the SkyBlock Menu!|You don't have any inventory space!)",
            sample = "§cInventory full? Don't forget to check out your Storage inside the SkyBlock Menu!",
            title = "Inventory full")
        rule("legacy-items", RuleAction.HIDE,
            "^One or more Legacy Items in your inventory",
            sample = "§cOne or more Legacy Items in your inventory have been removed.",
            title = "Legacy items notice")
        rule("frog-exhausted", RuleAction.HIDE,
            "^The Frog is exhausted",
            sample = "§eThe Frog is exhausted and needs to rest.",
            title = "Frog exhausted")
    }
}
