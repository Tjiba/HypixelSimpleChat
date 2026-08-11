package com.simplechat.rules.system

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Changements de serveur, écrans d'accueil, pubs de lobby, boîtes mystère. */
object Transitions {

    val TRANSITIONS = Group("transitions", "Server / transitions", Category.SYSTEM, "", RuleAction.HIDE,
        description = "Warping, sending to server, queuing, welcome/profile lines, watchdog, mystery boxes, lobby ads")

    val rules = rules(TRANSITIONS) {
        rule("warp-island", RuleAction.HIDE,
            "^Warping you to your SkyBlock island\\.\\.\\.$",
            compact = { "§8→ §7Island" },
            sample = "§7Warping you to your SkyBlock island...",
            title = "Warping to your island")
        // « Warping... », « Sending to server … » et « Profile ID: … » appartiennent au
        // réglage Server routing / Profile ID, qui décide avant ce groupe.
        rule("queuing", RuleAction.HIDE,
            "^Queuing\\.\\.\\. (.+)$",
            compact = { "§8⌛ §7${it[1]}" },
            sample = "Queuing... Bedwars 4v4",
            title = "Queuing for a game")
        rule("welcome-skyblock", RuleAction.HIDE,
            "^Welcome to Hypixel SkyBlock!$",
            compact = { "§aWelcome back" },
            sample = "§eWelcome to Hypixel SkyBlock!",
            title = "Welcome to SkyBlock")
        rule("latest-update", RuleAction.HIDE,
            "^Latest update: SkyBlock (.+)$",
            compact = { "§7Update §8· §f${it[1]}" },
            sample = "Latest update: SkyBlock 0.20.6",
            title = "Latest update line")
        rule("playing-on-profile", RuleAction.HIDE,
            "^You are playing on profile: (.+?)(?: \\(.+\\))?$",
            compact = { "§7Profile §8· §a${it[1]}" },
            sample = "§aYou are playing on profile: §ePeach§b (Co-op)",
            title = "Profile you are playing on")
        rule("player-init-error", RuleAction.HIDE,
            "Error initializing players: undefined Hidden",
            sample = "Error initializing players: undefined Hidden",
            title = "Player init error")
        // Le bloc anti-phishing arrive indenté : clean() écrase l'indentation, on ancre sur le texte.
        rule("sketchy-links", RuleAction.HIDE,
            "^(?:Clicking sketchy links can result in your account|being stolen!|Link looks suspicious\\? - Don't click it!)$",
            sample = "  Clicking sketchy links can result in your account",
            title = "Sketchy links warning")
        rule("watchdog-announcement", RuleAction.HIDE,
            "(?:Blacklisted modifications are a bannable offense!|Staff have banned an additional .+|\\[WATCHDOG ANNOUNCEMENT])",
            sample = "§4[WATCHDOG ANNOUNCEMENT]",
            title = "Watchdog announcement")
        rule("watchdog-bans", RuleAction.HIDE,
            "^Watchdog has banned ([\\d,.]+) players in the last 7 days\\.$",
            compact = { "§cWatchdog §7· §f${it[1]} §7bans" },
            sample = "Watchdog has banned 7,209 players in the last 7 days.",
            title = "Watchdog ban count")
        rule("warp-out", RuleAction.HIDE,
            "You have 60 seconds to warp out! CLICK to warp now!",
            sample = "§cYou have 60 seconds to warp out! CLICK to warp now!",
            title = "60 seconds to warp out")
        rule("lobby-line", RuleAction.HIDE,
            ".+ the lobby!.*",
            sample = "§aTimo§e left the lobby!",
            title = "Lobby join/leave lines")
        rule("hoppity-hunt-begun", RuleAction.HIDE,
            "Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!",
            sample = "§dHoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!",
            title = "Hoppity's Hunt has begun")
        // Item pris dans le raw pour garder sa couleur de rareté. « dans une boîte » avant « une boîte ».
        rule("mystery-box-item", RuleAction.HIDE,
            ".+ found an? (.+) in an? (?:Holiday )?Mystery Box!",
            compact = { "§7Mystery Box §8· §r" + (Fmt.rawItem(it.raw, "in a") ?: "§f${it[1]}") },
            sample = "§bMeteo §efound a §5Epic Rune §ein a §6Mystery Box!",
            title = "Mystery Box item")
        rule("mystery-box-tier", RuleAction.HIDE,
            ".+ found an? (.+) Mystery Box!$",
            compact = { "§7Mystery Box §8· §r" + (Fmt.rawItem(it.raw, "Mystery Box") ?: "§f${it[1]}") },
            sample = "§bMeteo §efound a §6Rare §6Mystery Box!",
            title = "Mystery Box found")
        rule("mystery-dust", RuleAction.HIDE,
            "You earned ([\\d,]+) Mystery Dust!",
            compact = { "§7+${it[1]} Mystery Dust" },
            sample = "§b✦ §r§7You earned §r§b120 §r§7Mystery Dust!",
            title = "Mystery Dust earned")
        rule("pet-consumables", RuleAction.HIDE,
            "You earned ([\\d,]+) Pet Consumables items!",
            compact = { "§7+${it[1]} Pet Consumables" },
            sample = "You earned 3 Pet Consumables items!",
            title = "Pet Consumables earned")
        rule("smp-ad", RuleAction.HIDE,
            "You can now create your own Hypixel SMP server!",
            sample = "§aYou can now create your own Hypixel SMP server!",
            title = "Hypixel SMP ad")
        rule("snow-particles", RuleAction.HIDE,
            ".*enable Snow Particles.*",
            sample = "§eClick here to enable Snow Particles!",
            title = "Snow particles prompt")
        rule("prototype-lobby", RuleAction.HIDE,
            "Welcome to the Prototype Lobby",
            sample = "§eWelcome to the Prototype Lobby",
            title = "Prototype Lobby welcome")
        rule("tournament-ad", RuleAction.HIDE,
            "HYPIXEL is hosting a .+ tournament!",
            sample = "§6HYPIXEL is hosting a Bedwars tournament!",
            title = "Tournament ad")
    }
}
