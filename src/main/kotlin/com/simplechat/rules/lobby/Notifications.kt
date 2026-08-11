package com.simplechat.rules.lobby

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Notifications de fond : pourboires, intérêts, captures, succès. */
object Notifications {

    val NOTIFICATIONS = Group("notifications", "Notifications", Category.LOBBY, "", RuleAction.HIDE,
        description = "Tipped players, plasmaflux, bank interest, hunting catches, achievements, unclaimed rewards reminder")

    val rules = rules(NOTIFICATIONS) {
        rule("tipped-players", RuleAction.HIDE,
            "^You tipped (\\d+) players? in (\\d+) .+!",
            compact = { "§aTipped §f${it[1]} §7(${it[2]} games)" },
            sample = "§aYou tipped 12 players in 4 games!",
            title = "Players tipped")
        rule("plasmaflux-removed", RuleAction.HIDE,
            "Your previous Plasmaflux Power Orb was removed!",
            sample = "§cYour previous Plasmaflux Power Orb was removed!",
            title = "Plasmaflux orb removed")
        rule("bank-interest", RuleAction.HIDE,
            "^You have just received ([\\d,]+) coins as interest",
            compact = { "§6+${Fmt.shortNum(it[1])} coins §7(interest)" },
            sample = "You have just received 12,000 coins as interest in your personal bank account!",
            title = "Bank interest")
        rule("invisibug", RuleAction.HIDE,
            "^You caught yourself an invisibug!",
            compact = { "§a+ §fInvisibug" },
            sample = "§aYou caught yourself an invisibug!",
            title = "Invisibug caught")
        rule("mochibear", RuleAction.HIDE,
            "^Mochibear ate too much",
            compact = { "§a+ §fMochibear" },
            sample = "Mochibear ate too much and passed out! You caught it!",
            title = "Mochibear caught")
        rule("achievement", RuleAction.HIDE,
            "^Achievement Unlocked: (.+)$",
            compact = { "§eAchievement §7· §f${it[1]}" },
            sample = "§eAchievement Unlocked: Bedwars Banker",
            title = "Achievement unlocked")
        rule("kernel-donation", RuleAction.HIDE,
            "Thanks for the donation! I've added a Kernel to your purse\\.",
            sample = "§aThanks for the donation! I've added a Kernel to your purse.",
            title = "Kernel donation")
        rule("unclaimed-rewards", RuleAction.HIDE,
            "You haven't claimed your .+ Rewards yet!",
            sample = "§cYou haven't claimed your Daily Rewards yet!",
            title = "Unclaimed rewards reminder")
    }
}
