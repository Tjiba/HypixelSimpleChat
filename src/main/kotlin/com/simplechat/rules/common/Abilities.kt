package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Cooldowns, mana, capacités prêtes, refus de spam du serveur. */
object Abilities {

    val ABILITIES = Group("abilities", "Abilities / cooldowns", Category.SKYBLOCK, "COMBAT", RuleAction.HIDE,
        description = "Cooldown, not enough mana, ability ready, class milestones, autopet, potion effects, slow down")

    val rules = rules(ABILITIES) {
        rule("cooldown-seconds", RuleAction.HIDE,
            "(?:This (?:item's ability|item|ability)|Your Ultimate) is (?:currently )?on cooldown for (?:about )?(\\d+) more seconds",
            compact = { "§7Cooldown §8· §f${it[1]}s" },
            sample = "§cThis ability is on cooldown for 2 more seconds.",
            title = "Cooldown with timer")
        rule("cooldown", RuleAction.HIDE,
            "(?:This (?:item's ability|item|ability)|Your Ultimate) is (?:temporarily disabled!|on cooldown)",
            compact = { "§7Cooldown" },
            sample = "§cThis item's ability is temporarily disabled!",
            title = "Ability on cooldown")
        rule("not-enough-mana", RuleAction.HIDE,
            "You (?:do not have enough|need at least .+) mana to (?:do this|activate this)!",
            compact = { "§bNot enough mana" },
            sample = "§cYou do not have enough mana to do this!",
            title = "Not enough mana")
        rule("ability-ready", RuleAction.HIDE,
            "^(.+) is (?:ready to use! Press DROP to activate it!|now (?:available|ready)!)",
            compact = { "§a✔ ${Fmt.rawSpan(it.raw, it[1])} §7ready" },
            sample = "§aYour Grappling Hook is ready to use! Press DROP to activate it!",
            title = "Ability ready")
        rule("item-used", RuleAction.HIDE,
            "^Used (?:Ragnarok|Throwing Axe|Healing Circle)!",
            sample = "§aUsed Ragnarok!",
            title = "Item used")
        // Atteint seulement si le compact « solo class » (avec survol) est désactivé.
        rule("solo-class-stats", RuleAction.HIDE,
            "Your .+ stats are doubled because you are the only player using this class!",
            sample = "§6Your §r§aHealer §r§6stats are doubled because you are the only player using this class!",
            title = "Solo class stats doubled")
        rule("class-milestone", RuleAction.HIDE,
            "^(Archer|Mage|Berserker|Tank|Healer) Milestone (\\S+)",
            compact = { "§6Milestone §7· ${Fmt.rawSpan(it.raw, it[1])} ${it[2]}" },
            sample = "§6Archer Milestone V: You have gained +5% damage!",
            title = "Class milestone")
        rule("creeper-veil", RuleAction.HIDE,
            "Creeper Veil (Activated|De-activated)!",
            compact = { if (it[1] == "Activated") "§7Veil on" else "§7Veil off" },
            sample = "§aCreeper Veil Activated!",
            title = "Creeper Veil on/off")
        rule("command-cooldown", RuleAction.HIDE,
            "Command Failed: This command is on cooldown! Try again in about a second!",
            sample = "§cCommand Failed: This command is on cooldown! Try again in about a second!",
            title = "Command on cooldown")
        rule("menu-disabled", RuleAction.HIDE,
            "This (?:menu is disabled here!|Terminal doesn't seem to be responsive at the moment\\.)",
            sample = "§cThis menu is disabled here!",
            title = "Menu or terminal unavailable")
        rule("doing-that-too-fast", RuleAction.HIDE,
            "(?:Whow! Slow down there!|Woah slow down, you're doing that too fast!|Please wait a (?:few seconds between refreshing!|bit before doing this!))",
            sample = "§cWoah slow down, you're doing that too fast!",
            title = "Doing that too fast")
        rule("spirit-bow", RuleAction.HIDE,
            "The Spirit Bow disintegrates as you fire off the shot!",
            sample = "§7The Spirit Bow disintegrates as you fire off the shot!",
            title = "Spirit Bow disintegrates")
        rule("giga-lightning", RuleAction.HIDE,
            "Giga Lightning.+",
            sample = "§bGiga Lightning struck a nearby enemy!",
            title = "Giga Lightning")
        rule("auto-recombobulator", RuleAction.HIDE,
            "Your Auto Recombobulator recombobulated",
            sample = "§dYour Auto Recombobulator recombobulated an item!",
            title = "Auto Recombobulator")
        rule("autopet-rules", RuleAction.HIDE,
            "(?:Only up to 2 rules may trigger at once!|Some of your autopet rules did not trigger\\.)",
            sample = "§cOnly up to 2 rules may trigger at once!",
            title = "Autopet rule limit")
        rule("potion-bag", RuleAction.HIDE,
            "You cannot put this item in the Potion Bag!",
            sample = "§cYou cannot put this item in the Potion Bag!",
            title = "Item refused by Potion Bag")
        rule("potion-effects-paused", RuleAction.HIDE,
            "(?:Your active Potion Effects have been paused|You are not allowed to use Potion Effects while in Dungeon).+restored when you leave Dungeons?!",
            sample = "§cYour active Potion Effects have been paused and will be restored when you leave Dungeons!",
            title = "Potion effects paused in dungeons")
    }
}
