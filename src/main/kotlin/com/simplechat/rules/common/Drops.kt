package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Essences, bonus d'XP, rappels de récompenses, radio, charmes. */
object Drops {

    val REWARDS = Group("rewards", "Rewards / drops", Category.SKYBLOCK, "WORLD & EVENTS", RuleAction.HIDE,
        description = "Essence finds, Event EXP bonuses, unclaimed rewards, radio signal, shard charms, expired combo")
    val SKYBLOCK_XP = Group("skyblockXp", "SkyBlock XP gains", Category.SKYBLOCK, "WORLD & EVENTS", RuleAction.COMPACT,
        description = "Every '+N SkyBlock XP' line — shortened, never hidden")

    /**
     * Déclarée avant tout le reste : un gain d'XP SkyBlock est traité ici et par aucune autre règle.
     * Le milestone reste, la progression (69/100) saute. Ancrée des deux côtés : un pavé multi-ligne
     * (COLLECTION LEVEL UP) qui contient un gain d'XP n'est pas un gain d'XP.
     */
    val skyblockXp = rules(SKYBLOCK_XP) {
        rule("skyblock-xp", RuleAction.COMPACT,
            "^\\+([\\d,.]+) SkyBlock XP(?: \\((.+?)\\))?(?: \\([\\d,]+/[\\d,]+\\))?$",
            compact = { "§b+${it[1]} SB XP" + if (it[2].isEmpty()) "" else " §7(${it[2]})" },
            sample = "§b+2 SkyBlock XP §7(Bag Upgrades) §8(69/100)")
    }

    val rules = rules(REWARDS) {
        rule("essence-found", RuleAction.HIDE,
            "^ESSENCE! .+ found ([\\d,]+) (.+) Essence!",
            compact = { "§d+${it[1]} ${it[2]} Essence" },
            sample = "§dESSENCE! §fTimo §dfound 3 Wither Essence!",
            title = "Essence found")
        rule("wither-essence-bonus", RuleAction.HIDE,
            " found a Wither Essence! Everyone gains an extra essence!",
            compact = { "§d+1 Wither Essence" },
            sample = "§dTimo found a Wither Essence! Everyone gains an extra essence!",
            title = "Wither Essence bonus")
        rule("essence-unlocked", RuleAction.HIDE,
            ".+ unlocked .+ Essence.+",
            sample = "§dTimo unlocked Wither Essence x3!",
            title = "Essence unlocked")
        // Sous-ligne de récap de fin de donjon : indentée en jeu, aplatie par clean().
        rule("essence-recap", RuleAction.HIDE,
            "^.+ Essence x[\\d,]+$",
            sample = "    Wither Essence x5",
            title = "Essence recap line")
        rule("rare-drop-notice", RuleAction.HIDE,
            "RARE DROP! (?:Hunk of Blue Ice.*|Beating Heart .+)",
            sample = "§6RARE DROP! Hunk of Blue Ice",
            title = "Rare drop notice")
        // XP : raccourci, jamais masqué par défaut — un gain qu'on ne voit plus passe pour perdu.
        rule("exp-from-playing", RuleAction.COMPACT,
            "^You earned ([\\d,]+) (Event EXP|GEXP) from playing ",
            compact = { if (it[2] == "GEXP") "§2+${it[1]} GEXP" else "§e+${it[1]} Event EXP" },
            sample = "§eYou earned 633 Event EXP from playing SkyBlock!",
            title = "Event EXP / GEXP earned")
        rule("skill-xp-bonus", RuleAction.COMPACT,
            "^BONUS! Temporarily earn (\\d+)% more skill experience!",
            compact = { "§e+${it[1]}% skill XP" },
            sample = "§eBONUS! Temporarily earn 25% more skill experience!",
            title = "Skill XP bonus")
        rule("experience-team-bonus", RuleAction.GREY,
            "Experience Team Bonus",
            sample = "§e+120 Experience Team Bonus",
            title = "Experience team bonus")
        rule("unclaimed", RuleAction.HIDE,
            "You have .+ unclaimed .+",
            sample = "§eYou have 3 unclaimed event rewards!",
            title = "Unclaimed rewards")
        rule("claim-reminder", RuleAction.HIDE,
            "^(?:>>> CLICK HERE to claim! <<<|Event rewards are deleted after 10 SkyBlock years!)$",
            sample = "   >>> CLICK HERE to claim! <<<",
            title = "Claim reminder")
        rule("kill-combo-expired", RuleAction.HIDE,
            "^Your Kill Combo has expired! You reached a (\\d+) Kill Combo!",
            compact = { "§7Combo ended §8· §f${it[1]}" },
            sample = "§cYour Kill Combo has expired! You reached a 30 Kill Combo!",
            title = "Kill combo expired")
        rule("radio-signal", RuleAction.HIDE,
            "Your radio(?: is weak\\. Find another enjoyer to boost it\\.| signal is strong!| lost signal\\. There's too many enjoyers on this channel\\.)",
            sample = "§cYour radio is weak. Find another enjoyer to boost it.",
            title = "Radio signal")
        rule("shard-charmed", RuleAction.HIDE,
            "^(?:CHARM|SALT) You charmed a (.+) and captured its Shard\\.",
            compact = { "§a+ §fShard §7(${it[1]})" },
            sample = "§dCHARM §fYou charmed a Flare and captured its Shard.",
            title = "Shard charmed")
        rule("redstone-pigmen", RuleAction.HIDE,
            "^The Redstone Pigmen are unhappy with you stealing their ores! Look out!$",
            sample = "§cThe Redstone Pigmen are unhappy with you stealing their ores! Look out!",
            title = "Redstone Pigmen warning")
    }
}
