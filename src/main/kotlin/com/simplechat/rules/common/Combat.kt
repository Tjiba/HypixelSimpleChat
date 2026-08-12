package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Lignes de boss et chiffres de dégâts, valables sur toutes les îles. */
object Combat {

    val BOSS = Group("boss", "Boss messages", Category.SKYBLOCK, "COMBAT", RuleAction.HIDE,
        description = "[BOSS] / [STATUE] dialog, '… has spawned!', 'ARACHNE DOWN!' shouts")
    val DAMAGE = Group("damageSpam", "Damage numbers", Category.SKYBLOCK, "COMBAT", RuleAction.GREY)
    val KILL_COMBO = Group("killCombo", "Kill combo", Category.SKYBLOCK, "COMBAT", RuleAction.GREY)
    val MOB_ABILITY = Group("mobAbility", "Mob abilities", Category.SKYBLOCK, "COMBAT", RuleAction.GREY)
    val COMBAT_HEAL = Group("combat", "Combat / heal", Category.SKYBLOCK, "COMBAT", RuleAction.HIDE,
        description = "Damage taken, healing, buffs, tethers, orbs")

    val rules =
        rules(BOSS) {
            rule("boss", RuleAction.HIDE,
                "^\\[(?:BOSS|STATUE)] |^ *[A-Z][A-Z ]+ DOWN!| has spawned!$",
                compact = { it.raw.replaceFirst(Regex("\\[(?:BOSS|STATUE)]\\s*"), "") },
                sample = "§c[BOSS] Maxor§r§f: I've been expecting you.")
        } +
        rules(DAMAGE) {
            rule("damage-spam", RuleAction.GREY,
                "^Your (.+?) hit \\d+ enem(?:y|ies) for ([0-9,.]+) damage\\.",
                compact = { "§6${it[1]} §7· §c${Fmt.shortNum(it[2])}" },
                sample = "Your Implosion hit 4 enemies for 2,637,430.3 damage.")
        } +
        rules(KILL_COMBO) {
            rule("kill-combo", RuleAction.GREY,
                "^\\+(\\d+) Kill Combo (.*)$",
                compact = {
                    "§6+${it[1]} Combo §7" + it[2].replace("✯", "").replace(Regex("\\s+"), " ").trim()
                },
                sample = "+5 Kill Combo §6+3% §b✯ Magic Find")
        } +
        rules(MOB_ABILITY) {
            rule("mob-ability", RuleAction.GREY,
                "^.+ used (.+?) on you hitting you for ([0-9,.]+) damage",
                compact = { "§c${it[1]} §7· §f-${Fmt.shortNum(it[2])}" },
                sample = "The Zombie Soldier used Slam on you hitting you for 1,200 damage")
        }

    /** Le tout-venant du combat : un seul réglage, évalué après les règles précises. */
    val spam =
        rules(COMBAT_HEAL) {
            rule("combat-hit-you", RuleAction.HIDE,
                "^(.+?) (?:hit|burnt) you for ([\\d,.]+) (?:true )?damage",
                compact = { "§c-${Fmt.shortNum(it[2])} §7(${Fmt.src(it[1])})" },
                sample = "§cGoldor's TNT Trap hit you for 1,200 true damage",
                title = "Damage taken")
            rule("combat-explosion", RuleAction.HIDE,
                "^A (.+) exploded, hitting you for ([\\d,.]+) damage",
                compact = { "§c-${Fmt.shortNum(it[2])} §7(${it[1]})" },
                sample = "A Crypt Wither Skull exploded, hitting you for 3,400 damage.",
                title = "Explosion damage")
            rule("combat-healed", RuleAction.HIDE,
                "^You were healed for ([\\d,.]+) health",
                compact = { "§a+${Fmt.shortNum(it[1])}❤" },
                sample = "§aYou were healed for 500 health!",
                title = "You were healed")
            rule("combat-healed-by", RuleAction.HIDE,
                "^(.+?) healed you for ([\\d,.]+) health",
                compact = { "§a+${Fmt.shortNum(it[2])}❤ §7(${Fmt.src(it[1])})" },
                sample = "§aMeteoFrance healed you for 500 health!",
                title = "Healed by someone")
            rule("combat-absorption", RuleAction.HIDE,
                "^You gained ([\\d,.]+) HP worth of absorption",
                compact = { "§e+${Fmt.shortNum(it[1])} absorption" },
                sample = "§eYou gained 250 HP worth of absorption for 3s from Timo!",
                title = "Absorption gained")
            rule("combat-strength", RuleAction.HIDE,
                "^(.+) granted you ([\\d,.]+) strength for 20 seconds!",
                compact = { "§4+${it[2]}❁ §7(20s)" },
                sample = "§cTimo granted you 50 strength for 20 seconds!",
                title = "Strength granted")
            rule("combat-tether", RuleAction.HIDE,
                "^(.+) formed a tether with you!$",
                compact = { "§a⚯ ${Fmt.rawSpan(it.raw, it[1])}" },
                sample = "§aTimo formed a tether with you!",
                title = "Tether formed")
            rule("combat-orb-taken", RuleAction.HIDE,
                "^(.+) picked up your (.+) Orb!",
                compact = { "§d⬤ §7${it[2]} (${it[1]})" },
                sample = "§dTimo picked up your Mana Flux Orb!",
                title = "Your orb picked up")
            rule("combat-orb-picked", RuleAction.HIDE,
                "^You picked up a (.+) Orb from (.+?) healing you",
                compact = { "§d⬤ §7${it[1]} (${it[2]})" },
                sample = "§dYou picked up a Radiant Orb from Timo healing you for 120 health!",
                title = "Orb picked up")
            rule("combat-heal-buff", RuleAction.HIDE,
                "BUFF! You (?:were splashed by .+ with Healing VIII!|have gained Healing V!)",
                sample = "§aBUFF! You have gained Healing V!",
                title = "Healing buff")
            rule("combat-bone-plating", RuleAction.HIDE,
                "Your bone plating reduced the damage you took by .+!",
                sample = "§aYour bone plating reduced the damage you took by 20%!",
                title = "Bone plating")
            rule("combat-mute", RuleAction.HIDE,
                "Mute silenced you!",
                sample = "§cMute silenced you!",
                title = "Mute silenced you")
            rule("combat-shiver", RuleAction.HIDE,
                "A shiver runs down your spine\\.\\.\\.",
                sample = "§7A shiver runs down your spine...",
                title = "Shiver down your spine")
            rule("combat-used-on-you", RuleAction.HIDE,
                "^(.+) used (.+) on you!$",
                compact = { "§c${it[2]} §7(${Fmt.src(it[1])})" },
                sample = "The Frozen Adventurer used Ice Spray on you!",
                title = "Ability used on you")
            // Filet générique, en dernier : tout ce qui frappe sans forme connue.
            rule("combat-generic-hit", RuleAction.HIDE,
                ".+ (?:struck|hit|exploded) .+ (?:for |you for ).+",
                sample = "§cSomething struck Timo for 400 damage",
                title = "Other damage lines")
        }
}
