package com.simplechat.rules.islands

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Catacombes : clés, portes, leviers, coffres, énigmes, bénédictions. */
object Dungeons {

    val DUNGEONS = Group("dungeons", "Dungeons", Category.SKYBLOCK, "DUNGEONS", RuleAction.HIDE,
        description = "Keys, doors, levers, chests, puzzles, blessings, boss/NPC lines",
        island = "Dungeons")

    val rules = rules(DUNGEONS) {
        rule("key-picked-up", RuleAction.HIDE,
            "^A (.+) Key was picked up!?",
            compact = { "§a+ §f${it[1]} Key" },
            sample = "§8A §5Wither Key §8was picked up!",
            title = "Key picked up")
        rule("key-right-click", RuleAction.HIDE,
            "RIGHT CLICK on .+ to open it\\. This key can only be used to open 1 door!",
            sample = "§eRIGHT CLICK on the door to open it. This key can only be used to open 1 door!",
            title = "Key usage hint")
        rule("door-opened", RuleAction.HIDE,
            "^(.+) opened a (?:.+ )?door!$",
            compact = { "§7Door §8· §f${it[1]}" },
            sample = "§bTimo §7opened a §5WITHER §7door!",
            title = "Door opened")
        rule("door-sound", RuleAction.HIDE,
            "You hear the sound of something opening\\.\\.\\.",
            sample = "§7You hear the sound of something opening...",
            title = "Door opening sound")
        rule("door-no-key", RuleAction.HIDE,
            "You do not have the key for this door!",
            sample = "§cYou do not have the key for this door!",
            title = "Missing door key")
        rule("lever-used", RuleAction.HIDE,
            "(?:This lever has already been used\\.|Someone has already activated this lever!)",
            sample = "§cThis lever has already been used.",
            title = "Lever already used")
        rule("chest-already-opened", RuleAction.HIDE,
            "(?:This chest has already been searched!|You have already opened this dungeon chest!|That chest is locked!)",
            sample = "§cThat chest is locked!",
            title = "Chest already opened")
        rule("room-restriction", RuleAction.HIDE,
            "^(?:A mystical force .+|You cannot (?:use abilities|do that) in this room!)$",
            sample = "§cA mystical force in this room prevents you from using that ability!",
            title = "Ability blocked in room")
        rule("block-break-refused", RuleAction.HIDE,
            "^(?:You don't have enough charges to break this block right now!|There are blocks in the way!)$",
            sample = "§cThere are blocks in the way!",
            title = "Block break refused")
        rule("magic-immune", RuleAction.HIDE,
            "This creature is immune to this kind of magic!",
            sample = "§cThis creature is immune to this kind of magic!",
            title = "Creature immune to magic")
        rule("puzzle-solved", RuleAction.HIDE,
            "^PUZZLE SOLVED! (.+)$",
            compact = { "§aPuzzle ✔ §7${it[1]}" },
            sample = "§ePUZZLE SOLVED! Timo solved Water Board!",
            title = "Puzzle solved")
        rule("bracket-line", RuleAction.HIDE,
            "\\[(?:STATUE|BOSS|NPC|SKULL|BOMB|Sacks|CROWD|Healer)] .+",
            sample = "§e[CROWD] §fWow, nice job!",
            title = "Tagged lines ([BOSS], [CROWD])")
        rule("silverfish", RuleAction.HIDE,
            "You cannot (?:move the silverfish in that direction!|hit the silverfish while it's moving!)",
            sample = "§cYou cannot move the silverfish in that direction!",
            title = "Silverfish puzzle")
        rule("terminal-mistake", RuleAction.HIDE,
            "(?:It isn't your turn!|Don't move diagonally! Bad!|Oops! You stepped on the wrong block!)",
            sample = "§cOops! You stepped on the wrong block!",
            title = "Terminal mistake")
        // Les lignes préfixées « [NPC] » appartiennent au réglage NPC dialog, qui décide avant.
        rule("mort-dialog", RuleAction.HIDE,
            ".+ Mort: .+",
            sample = "§6Master Mort: Good luck in there!",
            title = "Mort dialog")
        rule("fairy-dialog", RuleAction.HIDE,
            ".+ the Fairy: .+",
            sample = "§dFairy the Fairy: Good luck!",
            title = "Fairy dialog")
        rule("dungeon-buff", RuleAction.HIDE,
            "^DUNGEON BUFF! (.+)$",
            compact = { "§dBuff §7· §f${it[1]}" },
            sample = "§dDUNGEON BUFF! Blessing of Life granted +5 HP!",
            title = "Dungeon buff")
        rule("blessing-picked-up", RuleAction.HIDE,
            "^A Blessing of (.+) was picked up!",
            compact = { "§d+ §fBlessing of ${it[1]}" },
            sample = "§dA Blessing of Power V was picked up!",
            title = "Blessing picked up")
        rule("blessing-obtained", RuleAction.HIDE,
            "^(.+) has obtained Blessing of (.+)!",
            compact = { "§d+ §fBlessing of ${it[2]}" },
            sample = "§dTimo has obtained Blessing of Wisdom!",
            title = "Blessing obtained by someone")
        // Sous-lignes de récap : indentées en jeu, aplaties par clean().
        rule("blessing-grants", RuleAction.HIDE,
            "^(?:Also )?(?:grants|granted) you .+$|Granted you.+",
            sample = "     Also grants you +10 Strength.",
            title = "Blessing detail line")
        rule("dungeon-item-obtained", RuleAction.HIDE,
            "^(.+) has obtained (?:(?!Wither Key!|Blood Key!)(.+) Key!|(Superboom TNT(?: x[0-9])?|Revive Stone|Premium Flesh|Beating Heart)!)",
            compact = { "§a+ §f${it[3].ifEmpty { "${it[2]} Key" }} §7(${it[1]})" },
            sample = "§aTimo has obtained Superboom TNT x2!",
            title = "Item obtained by someone")
    }
}
