package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Gains : sacs, loot share, GEXP, coffres rares. */
object Economy {

    val SACKS = Group("sacks", "Sacks notifications", Category.SKYBLOCK, "ECONOMY", RuleAction.GREY)
    val LOOT_SHARE = Group("lootShare", "Loot share", Category.SKYBLOCK, "ECONOMY", RuleAction.COMPACT)
    val GEXP = Group("gexp", "Guild EXP earned", Category.SKYBLOCK, "ECONOMY", RuleAction.COMPACT)
    val RARE_REWARD = Group("rareReward", "Rare reward (chest)", Category.SKYBLOCK, "ECONOMY", RuleAction.COMPACT)

    val rules =
        rules(SACKS) {
            rule("sacks", RuleAction.GREY,
                "^\\[Sacks] \\+",
                compact = { "§a" + it.clean.removePrefix("[Sacks] ") },
                sample = "§6[Sacks] §a+64 Cobblestone")
        } +
        rules(LOOT_SHARE) {
            rule("loot-share", RuleAction.COMPACT,
                "^LOOT SHARE You received loot for assisting (.+?)!*$",
                compact = { "§6Loot share §7· §f${it[1]}" },
                sample = "§eLOOT SHARE §fYou received loot for assisting §b__Anoteros__§f!")
        } +
        rules(GEXP) {
            rule("gexp", RuleAction.COMPACT,
                "^You earned ([\\d,]+) GEXP \\+ ([\\d,]+) Event EXP from playing",
                compact = { "§2${it[1]} GEXP §a+ §e${it[2]} Event EXP" },
                sample = "You earned 211 GEXP + 633 Event EXP from playing SkyBlock!")
        } +
        rules(RARE_REWARD) {
            // Item pris dans le raw pour garder sa couleur de rareté.
            rule("rare-reward", RuleAction.COMPACT,
                "^RARE REWARD! (.+?) found a (.+?) in their (.+?) Chest!",
                compact = {
                    val item = Fmt.rawItem(it.raw, "in their") ?: "§f${it[2]}"
                    "§6§lRARE REWARD §r§f${it[1]} §7· §r$item §fin ${it[3]} Chest"
                },
                sample = "§6§lRARE REWARD! §fTioLDK §efound a §9Fuming Potato Book §ein their §aBedrock Chest§e!")
        }
}
