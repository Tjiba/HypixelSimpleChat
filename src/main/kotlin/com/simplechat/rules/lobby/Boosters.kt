package com.simplechat.rules.lobby

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Boosters, générosité, ticket de temps de jeu, lien de récompense. */
object Boosters {

    val BOOSTER = Group("boosterActivated", "Booster activated", Category.LOBBY, "", RuleAction.COMPACT)
    val GENEROSITY = Group("radiatingGenerosity", "Radiating generosity", Category.LOBBY, "", RuleAction.COMPACT)
    val PLAYTIME = Group("playtimeTicket", "Playtime ticket", Category.LOBBY, "", RuleAction.COMPACT)
    val REWARD_LINK = Group("rewardLink", "Reward website link", Category.LOBBY, "", RuleAction.COMPACT)

    val rules =
        rules(BOOSTER) {
            // La durée est écrite en toutes lettres et reste optionnelle selon le type de booster.
            rule("booster-activated", RuleAction.COMPACT,
                "^Activated your booster\\. You now have .* of ([0-9.]+)x coins\\.",
                compact = {
                    val hours = Regex("have (\\w+) hours?").find(it.clean)?.groupValues?.get(1) ?: ""
                    "§eBooster §f${it[1]}x§e coins §7· ${Fmt.wordToHours(hours)}"
                },
                sample = "Activated your booster. You now have three hours of 4.0x coins.")
        } +
        rules(GENEROSITY) {
            rule("radiating-generosity", RuleAction.COMPACT,
                "^You are still radiating with ",
                compact = { "§eRadiating generosity" },
                sample = "You are still radiating with §bGenerosity!")
        } +
        rules(PLAYTIME) {
            rule("playtime-ticket", RuleAction.COMPACT,
                "^PLAYTIME! You gained (.+)$",
                compact = {
                    "§ePlaytime §7· §a+" + it[1].removeSuffix("!").removePrefix("a ").removePrefix("Playtime ").trim()
                },
                sample = "§6PLAYTIME! §eYou gained a Playtime Chest!")
        } +
        rules(REWARD_LINK) {
            rule("reward-link", RuleAction.COMPACT,
                "Click the link to visit our website and claim your reward(?:: (\\S+))?",
                compact = { if (it[1].isEmpty()) "§6Claim reward" else "§6Claim reward: §b${it[1]}" },
                sample = "Click the link to visit our website and claim your reward: hypixel.net/link")
        }
}
