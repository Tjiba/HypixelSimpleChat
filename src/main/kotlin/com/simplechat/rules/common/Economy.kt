package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Gains : sacs, loot share, GEXP, coffres rares. */
object Economy {

    val SACKS = Group("sacks", "Sacks notifications", Category.SKYBLOCK, "ECONOMY", RuleAction.GREY)
    val LOOT_SHARE = Group("lootShare", "Loot share", Category.SKYBLOCK, "ECONOMY", RuleAction.COMPACT,
        description = "Loot earned by assisting someone", split = false)
    val GEXP = Group("gexp", "Guild EXP earned", Category.SKYBLOCK, "ECONOMY", RuleAction.COMPACT)
    val RARE_REWARD = Group("rareReward", "Rare reward (chest)", Category.SKYBLOCK, "ECONOMY", RuleAction.COMPACT)

    val rules =
        rules(SACKS) {
            // Les sacs se vident aussi : un retrait arrive en "-12 items", en rouge plutôt qu'en vert.
            rule("sacks", RuleAction.GREY,
                "^\\[Sacks] [-+]",
                compact = {
                    val body = it.clean.removePrefix("[Sacks] ")
                    (if (body.startsWith("-")) "§c" else "§a") + body
                },
                sample = "§6[Sacks] §a+64 Cobblestone")
        } +
        rules(LOOT_SHARE) {
            // Hypixel dit soit « loot », soit ce qui est tombé (« 2 Puck Shards », des coins…).
            rule("loot-share", RuleAction.COMPACT,
                "^LOOT SHARE!? You received (.+?) for assisting (.+?)!*$",
                compact = {
                    val who = Fmt.rawSpan(it.raw, it[2])
                    if (it[1] == "loot") "§6Loot share §7· $who"
                    else "§6Loot share §7· ${Fmt.rawSpan(it.raw, it[1])} §8· $who"
                },
                sample = "§eLOOT SHARE §fYou received §b2 §9Puck §fShards for assisting §b__Anoteros__§f!",
                title = "Loot share")
            // Forme chasse : « … from <joueur> catching a <mob> ». Le mob est déjà dans le nom du
            // shard, on ne le répète pas.
            rule("loot-share-catch", RuleAction.COMPACT,
                "^LOOT SHARE!? You received (?:an? )?(.+?) from (.+?) catching an? .+?!*$",
                compact = { "§6Loot share §7· ${Fmt.rawSpan(it.raw, it[1])} §8· ${Fmt.rawSpan(it.raw, it[2])}" },
                sample = "§e§lLOOT SHARE! §7You received a §9Rockmite Shard§7 from §aROTOTO1213§7 catching a §9Rockmite§7!",
                title = "Loot share (catch)")
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
