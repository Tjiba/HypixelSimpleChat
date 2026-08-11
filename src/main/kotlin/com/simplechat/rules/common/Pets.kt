package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Invocation de familiers : même logique +/- côté lobby et côté SkyBlock. */
object Pets {

    val PET_SPAWN = Group("petSpawn", "Pet spawn/despawn", Category.LOBBY, "", RuleAction.COMPACT)
    val PET_SUMMON = Group("petSummon", "Pet summon/despawn", Category.SKYBLOCK, "GENERAL", RuleAction.COMPACT,
        description = "'You summoned your …' — compact keeps the pet's rarity color")

    val rules =
        rules(PET_SPAWN) {
            rule("pet-spawn", RuleAction.COMPACT,
                "^(Spawned|Despawned) your (.+)$",
                compact = {
                    val name = it[2].removeSuffix("!").trim()
                    if (it[1] == "Spawned") "§a+ §f$name" else "§c- §f$name"
                },
                sample = "§aSpawned your §6Bat§a!")
        } +
        rules(PET_SUMMON) {
            // Nom pris dans le raw pour garder la couleur de rareté du familier.
            rule("pet-summon", RuleAction.COMPACT,
                "^You (summoned|despawned) your .+",
                compact = {
                    val name = it.raw.substringAfter("your ").trim().replace(Regex("(?:§.)*!(?:§.)*\\s*$"), "")
                    if (it[1] == "summoned") "§a+ §r$name" else "§c- §r$name"
                },
                sample = "§aYou summoned your §6Baby Yeti§a!")
        }
}
