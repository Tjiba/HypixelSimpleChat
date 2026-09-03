package com.simplechat.rules.islands

import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.Section
import com.simplechat.rules.Tab
import com.simplechat.rules.rules

/** Catacombes : clés, portes, leviers, coffres, énigmes, bénédictions. */
object Dungeons {

    val DUNGEONS = Group("dungeons", "Dungeons", Category.SKYBLOCK, Section.DUNGEONS, RuleAction.HIDE,
        description = "Keys, doors, levers, chests, puzzles, blessings, boss/NPC lines",
        tab = Tab.DUNGEONS)

    /** Déclarée avant Npc dans le registre : ces PNJ arrivent tantôt nus, tantôt préfixés
     *  « [NPC] », et le réglage générique des dialogues les avalerait. */
    val npc = rules(DUNGEONS) {
        rule("mort-dialog", RuleAction.HIDE,
            "^(?:\\[(?:BOSS|NPC)] )?(?:Master )?Mort: .+",
            compact = { untag(it.raw) },
            sample = "§6Master Mort: Good luck in there!",
            title = "Mort dialog")
        // Le Watcher et les boss d'étage n'existent que dans les Catacombes : leur réplique tient à
        // ce réglage-ci, tagée « [BOSS] » ou nue. Sans ça, c'est « Boss messages » (Combat) qui
        // les avalait, et le joueur avait beau tout mettre en HIDE ici, rien ne changeait.
        rule("watcher-dialog", RuleAction.HIDE,
            "^(?:\\[(?:BOSS|NPC)] )?The Watcher: .+",
            compact = { untag(it.raw) },
            sample = "§cThe Watcher§f: Go, fight!",
            title = "The Watcher dialog")
        rule("boss-dialog", RuleAction.HIDE,
            "^(?:\\[(?:BOSS|NPC)] )?(?:${ChatRules.FLOOR_BOSSES}): .+",
            compact = { untag(it.raw) },
            sample = "§cBonzo§f: Sike",
            title = "Boss dialog")
        rule("fairy-dialog", RuleAction.HIDE,
            ".+ the Fairy: .+",
            compact = { untag(it.raw) },
            sample = "§dFairy the Fairy: Good luck!",
            title = "Fairy dialog")
    }

    /** La réplique sans son étiquette de tête : le compact garde la phrase et ses couleurs. */
    private fun untag(raw: String) = raw.replaceFirst(Regex("\\[(?:BOSS|NPC)]\\s*"), "")

    val rules = rules(DUNGEONS) {
        rule("key-picked-up", RuleAction.HIDE,
            "^A (.+) Key was picked up!?",
            compact = { "§a+ ${Fmt.rawSpan(it.raw, it[1])} Key" },
            sample = "§8A §5Wither Key §8was picked up!",
            title = "Key picked up")
        rule("key-right-click", RuleAction.HIDE,
            "RIGHT CLICK on .+ to open it\\. This key can only be used to open 1 door!",
            sample = "§eRIGHT CLICK on the door to open it. This key can only be used to open 1 door!",
            title = "Key usage hint")
        rule("door-opened", RuleAction.HIDE,
            "^(.+) opened a (?:.+ )?door!$",
            compact = { "§7Door §8· ${Fmt.rawSpan(it.raw, it[1])}" },
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
        rule("blood-door-opened", RuleAction.HIDE,
            "^The (.+) DOOR has been opened!",
            compact = { "§7Door §8· ${Fmt.rawSpan(it.raw, it[1])}" },
            sample = "§cThe §c§lBLOOD DOOR§r§c has been opened!",
            title = "Blood door opened")
        // Les clés de boss ont leur propre réglage : c'est la seule ligne qui dit qu'un joueur
        // a ramassé celle qui ouvre la salle suivante.
        rule("boss-key-obtained", RuleAction.HIDE,
            "^(.+) has obtained (Wither|Blood) Key!",
            compact = { "§a+ ${Fmt.rawSpan(it.raw, "${it[2]} Key")} §7(${it[1]})" },
            sample = "§aTimo has obtained §5Wither Key§a!",
            title = "Boss key obtained")
        rule("dungeon-buff", RuleAction.HIDE,
            "^DUNGEON BUFF! (.+)$",
            compact = { "§dBuff §7· ${Fmt.rawSpan(it.raw, it[1])}" },
            sample = "§dDUNGEON BUFF! Blessing of Life granted +5 HP!",
            title = "Dungeon buff")
        rule("blessing-picked-up", RuleAction.HIDE,
            "^A Blessing of (.+) was picked up!",
            compact = { "§d+ ${Fmt.rawSpan(it.raw, "Blessing of ${it[1]}")}" },
            sample = "§dA Blessing of Power V was picked up!",
            title = "Blessing picked up")
        rule("blessing-obtained", RuleAction.HIDE,
            "^(.+) has obtained Blessing of (.+)!",
            compact = { "§d+ ${Fmt.rawSpan(it.raw, "Blessing of ${it[2]}")}" },
            sample = "§dTimo has obtained Blessing of Wisdom!",
            title = "Blessing obtained by someone")
        // Sous-lignes de récap : indentées en jeu, aplaties par clean().
        rule("blessing-grants", RuleAction.HIDE,
            "^(?:Also )?(?:grants|granted) you .+$|Granted you.+",
            sample = "     Also grants you +10 Strength.",
            title = "Blessing detail line")
        rule("dungeon-item-obtained", RuleAction.HIDE,
            "^(.+) has obtained (?:(?!Wither Key!|Blood Key!)(.+) Key!|(Superboom TNT(?: x[0-9])?|Revive Stone|Premium Flesh|Beating Heart)!)",
            compact = { "§a+ ${Fmt.rawColor(it.raw, it[3].ifEmpty { it[2] })}${it[3].ifEmpty { "${it[2]} Key" }} §7(${it[1]})" },
            sample = "§aTimo has obtained Superboom TNT x2!",
            title = "Item obtained by someone")
    }
}
