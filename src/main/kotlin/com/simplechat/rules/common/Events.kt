package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Parkours, pads de téléportation, fire sales, chasse de Hoppity, sacrifices. */
object Events {

    val EVENTS = Group("events", "Events", Category.SKYBLOCK, "WORLD & EVENTS", RuleAction.HIDE,
        description = "Parkour, teleport pads, fire sales, Hoppity eggs, sacrifice, snow cannon")

    val rules = rules(EVENTS) {
        rule("parkour-started", RuleAction.HIDE,
            "^Started parkour ",
            compact = { "§bParkour §7· start" },
            sample = "§aStarted parkour Crystal Nucleus!",
            title = "Parkour started")
        rule("parkour-finished", RuleAction.HIDE,
            "^Finished parkour .+ in (.+)!$",
            compact = { "§bParkour §a✔ §f${it[1]}" },
            sample = "§bFinished parkour Foraging Island in 1:23!",
            title = "Parkour finished")
        rule("parkour-cancelled", RuleAction.HIDE,
            "Cancelled parkour! You cannot fly\\.",
            sample = "§cCancelled parkour! You cannot fly.",
            title = "Parkour cancelled")
        rule("teleport-pad-warp", RuleAction.HIDE,
            "^Warped from the (.+) to the (.+)!$",
            compact = { "§8→ §7${it[2]}" },
            sample = "§aWarped from the Spawn to the Castle!",
            title = "Teleport pad warp")
        rule("teleport-pad-unset", RuleAction.HIDE,
            "This Teleport Pad does not have a destination set!",
            sample = "§cThis Teleport Pad does not have a destination set!",
            title = "Teleport pad without destination")
        rule("fire-sale", RuleAction.HIDE,
            ".*FIRE SALE.*",
            sample = "§d§lFIRE SALE §7Get them while they last!",
            title = "Fire sale")
        rule("fire-sale-soon", RuleAction.HIDE,
            "Fire Sales for .+ are starting soon!",
            sample = "§dFire Sales for Gems are starting soon!",
            title = "Fire sale starting soon")
        rule("hoppity-appeared", RuleAction.HIDE,
            "HOPPITY'S HUNT .+ has appeared!",
            sample = "§dHOPPITY'S HUNT §fA Chocolate Rabbit has appeared!",
            title = "Hoppity rabbit appeared")
        // Avec ou sans « near … » (Hitman Egg, etc.).
        rule("hoppity-egg-found", RuleAction.HIDE,
            "HOPPITY'S HUNT You found an? (.+?) Egg",
            compact = { "§6+ §f${it[1]} Egg" },
            sample = "§dHOPPITY'S HUNT §fYou found a Chocolate Breakfast Egg!",
            title = "Hoppity egg found")
        rule("sacrifice", RuleAction.HIDE,
            "^SACRIFICE! .+ turned .+ into ([\\d,]+) Dragon Essence!",
            compact = { "§5Sacrifice §7· §f+${it[1]} Dragon Essence" },
            sample = "§5SACRIFICE! §fTimo turned a Superior Dragon into 240 Dragon Essence!",
            title = "Dragon sacrifice")
        rule("rabbit-barn", RuleAction.HIDE,
            "Your Rabbit Barn capacity has been increased",
            sample = "§7Your §r§aRabbit Barn §r§7capacity has been increased to 5!",
            title = "Rabbit Barn upgraded")
        rule("limited-time-offer", RuleAction.HIDE,
            "♨ .+ for a limited time",
            sample = "§6♨ §eDouble chocolate for a limited time!",
            title = "Limited time offer")
        rule("snow-cannon", RuleAction.HIDE,
            ".+ mounted a Snow Cannon!",
            sample = "§bTimo mounted a Snow Cannon!",
            title = "Snow Cannon mounted")
    }
}
