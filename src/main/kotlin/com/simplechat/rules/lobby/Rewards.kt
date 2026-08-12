package com.simplechat.rules.lobby

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Cartes de récompense quotidienne et butins réclamés. */
object Rewards {

    val MYSTERY = Group("mysteryReward", "Daily/mystery reward", Category.LOBBY, "", RuleAction.COMPACT)
    val CLAIMED = Group("claimedCurrency", "Claimed rewards", Category.LOBBY, "", RuleAction.COMPACT)

    val rules =
        rules(MYSTERY) {
            rule("mystery-reward", RuleAction.COMPACT,
                "^You have claimed a (\\[.+?]) reward card!",
                compact = { "§6Daily reward §7· ${Fmt.rawSpan(it.raw, it[1])}" },
                sample = "You have claimed a [Legendary Mystery Dust] reward card!")
        } +
        rules(CLAIMED) {
            rule("claimed-currency", RuleAction.COMPACT,
                "^You have successfully claimed (.+ and .+)!",
                compact = { "§aClaimed §f" + it[1].replace(" and ", "§a, §f") },
                sample = "You have successfully claimed 2,200 Hypixel Experience and 3,000 Arcade Coins!")
        }
}
