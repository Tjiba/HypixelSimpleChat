package com.simplechat.rules.lobby

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Morphs et garde-robe des lobbies. Une seule règle pour cinq tournures différentes. */
object Morphs {

    val MORPH = Group("morphWardrobe", "Morph / wardrobe", Category.LOBBY, "", RuleAction.COMPACT)

    val rules = rules(MORPH) {
        rule("morph-wardrobe", RuleAction.COMPACT,
            "^(You are now morphed|You selected .+ Cloak|Reset your (Cloak|Morph)|Morph reset\\.|Right-Click with the .* to activate)",
            compact = { m ->
                val c = m.clean
                when {
                    c.startsWith("You are now morphed") -> {
                        // "morphed into a Cow!" ou "morphed as a Cow Morph for 5 minutes."
                        val name = Regex("morphed (?:into|as) (?:an? )?(.+?)(?: Morph)?(?: for .+)?[!.]*$")
                            .find(c)?.groupValues?.get(1) ?: c
                        "§7Morphed §8→ §f$name"
                    }
                    c.startsWith("You selected") -> {
                        val name = c.substringAfter("You selected ").removeSuffix("!")
                            .removePrefix("the ").removeSuffix(" Cloak").trim()
                        "§7Cloak §8→ §f$name"
                    }
                    c.startsWith("Morph reset") -> "§7Reset morph"
                    c.startsWith("Reset your") ->
                        "§7Reset " + c.substringAfter("Reset your ").removeSuffix("!").trim().lowercase()
                    else -> {
                        val item = Regex("Right-Click with the (.+?)(?: selected)? to activate").find(c)?.groupValues?.get(1)
                        if (item != null) "§7Right-click §f$item §7to activate" else "§7$c"
                    }
                }
            },
            sample = "§aYou are now morphed into a §6Cow§a!")
    }
}
