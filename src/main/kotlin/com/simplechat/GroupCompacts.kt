package com.simplechat

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Compacts par message pour les groupes hide-useless (action COMPACT).
 * Premier pattern qui matche gagne ; aucun match → null (le brut est gardé). Pur.
 * L'ordre compte : variantes spécifiques avant les génériques.
 */
object GroupCompacts {

    private fun c(regex: String, fmt: (Matcher) -> String): Pair<Pattern, (Matcher) -> String> =
        Pattern.compile(regex) to fmt

    /** Retire les articles de tête des sources ("The Mage's Magma", "Your fairy"). */
    private fun src(s: String) = s.removePrefix("The ").removePrefix("Your ").trim()

    private val COMPACTS: List<Pair<Pattern, (Matcher) -> String>> = listOf(
        // --- Transitions (System) ---
        c("^Warping you to your SkyBlock island\\.\\.\\.") { "§8→ §7Island" },
        c("^Queuing\\.\\.\\. (.+)$") { m -> "§8⌛ §7${m.group(1)}" },
        c("^Welcome to Hypixel SkyBlock!$") { "§aWelcome back" },
        c("^Latest update: SkyBlock (.+)$") { m -> "§7Update §8· §f${m.group(1)}" },
        c("^You are playing on profile: (.+?)(?: \\(.+\\))?$") { m -> "§7Profile §8· §a${m.group(1)}" },
        c("^Watchdog has banned ([\\d,.]+) players in the last 7 days\\.$") { m -> "§cWatchdog §7· §f${m.group(1)} §7bans" },
        c("^You earned ([\\d,]+) Mystery Dust!$") { m -> "§7+${m.group(1)} Mystery Dust" },
        c("^You earned ([\\d,]+) Pet Consumables items!$") { m -> "§7+${m.group(1)} Pet Consumables" },

        // --- Notifications (Lobby) ---
        c("^You tipped (\\d+) players? in (\\d+) ") { m -> "§aTipped §f${m.group(1)} §7(${m.group(2)} games)" },
        c("^You have just received ([\\d,]+) coins as interest") { m -> "§6+${ChatRules.shortNum(m.group(1))} coins §7(interest)" },
        c("^You caught yourself an invisibug!") { "§a+ §fInvisibug" },
        c("^Mochibear ate too much") { "§a+ §fMochibear" },
        c("^Achievement Unlocked: (.+)$") { m -> "§eAchievement §7· §f${m.group(1)}" },

        // --- Dungeons ---
        c("^A (.+) Key was picked up!") { m -> "§a+ §f${m.group(1)} Key" },
        c("^(.+) opened a (?:.+ )?door!$") { m -> "§7Door §8· §f${m.group(1)}" },
        c("^PUZZLE SOLVED! (.+)$") { m -> "§aPuzzle ✔ §7${m.group(1)}" },
        c("^DUNGEON BUFF! (.+)$") { m -> "§dBuff §7· §f${m.group(1)}" },
        c("^A Blessing of (.+) was picked up!") { m -> "§d+ §fBlessing of ${m.group(1)}" },
        c("^(.+) has obtained Blessing of (.+)!") { m -> "§d+ §fBlessing of ${m.group(2)}" },
        c("^(.+) has obtained (Superboom TNT(?: x\\d+)?|Revive Stone|Premium Flesh|Beating Heart)!") { m -> "§a+ §f${m.group(2)} §7(${m.group(1)})" },

        // --- Abilities ---
        c("on cooldown for (?:about )?(\\d+) more seconds") { m -> "§7Cooldown §8· §f${m.group(1)}s" },
        c("^(?:This (?:item's ability|item|ability)|Your Ultimate) is (?:temporarily disabled|on cooldown)") { "§7Cooldown" },
        c("mana to (?:do this|activate this)!") { "§bNot enough mana" },
        c("^(.+) is (?:ready to use! Press DROP|now (?:available|ready)!)") { m -> "§a✔ §f${m.group(1)} §7ready" },
        c("^(Archer|Mage|Berserker|Tank|Healer) Milestone (\\S+)") { m -> "§6Milestone §7· §f${m.group(1)} ${m.group(2)}" },
        c("^Creeper Veil (Activated|De-activated)!") { m -> if (m.group(1) == "Activated") "§7Veil on" else "§7Veil off" },

        // --- Combat / heal ---
        c("^(.+?) (?:hit|burnt) you for ([\\d,.]+) (?:true )?damage") { m -> "§c-${ChatRules.shortNum(m.group(2))} §7(${src(m.group(1))})" },
        c("^A (.+) exploded, hitting you for ([\\d,.]+) damage") { m -> "§c-${ChatRules.shortNum(m.group(2))} §7(${m.group(1)})" },
        c("^You were healed for ([\\d,.]+) health") { m -> "§a+${ChatRules.shortNum(m.group(1))}❤" },
        c("^(.+?) healed you for ([\\d,.]+) health") { m -> "§a+${ChatRules.shortNum(m.group(2))}❤ §7(${src(m.group(1))})" },
        c("^You gained ([\\d,.]+) HP worth of absorption") { m -> "§e+${ChatRules.shortNum(m.group(1))} absorption" },
        c("^(.+) granted you ([\\d,.]+) strength for 20 seconds!") { m -> "§4+${m.group(2)}❁ §7(20s)" },
        c("^(.+) formed a tether with you!$") { m -> "§a⚯ §f${m.group(1)}" },
        c("^(.+) picked up your (.+) Orb!") { m -> "§d⬤ §7${m.group(2)} (${m.group(1)})" },
        c("^You picked up a (.+) Orb from (.+?) healing you") { m -> "§d⬤ §7${m.group(1)} (${m.group(2)})" },
        c("^(.+) used (.+) on you!$") { m -> "§c${m.group(2)} §7(${src(m.group(1))})" },

        // --- Rewards / drops ---
        c("^ESSENCE! .+ found ([\\d,]+) (.+) Essence!") { m -> "§d+${m.group(1)} ${m.group(2)} Essence" },
        c(" found a Wither Essence! Everyone gains an extra essence!") { "§d+1 Wither Essence" },
        c("^You earned ([\\d,]+) (Event EXP|GEXP) from playing ") { m ->
            if (m.group(2) == "GEXP") "§2+${m.group(1)} GEXP" else "§e+${m.group(1)} Event EXP"
        },
        c("^BONUS! Temporarily earn (\\d+)% more skill experience!") { m -> "§e+${m.group(1)}% skill XP" },
        c("^Your Kill Combo has expired! You reached a (\\d+) Kill Combo!") { m -> "§7Combo ended §8· §f${m.group(1)}" },
        c("^(?:CHARM|SALT) You charmed a (.+) and captured its Shard\\.") { m -> "§a+ §fShard §7(${m.group(1)})" },

        // --- Bazaar / AH ---
        c("^\\[Bazaar] Submitting sell offer") { "§6Bazaar §7· selling…" },
        c("^\\[Bazaar] Submitting buy order") { "§6Bazaar §7· buying…" },
        c("^(?:Buy Order|Sell Offer) Setup! (.+)$") { m -> "§6Bazaar §a✔ §f${m.group(1)}" },
        c("^Putting item in escrow") { "§7Escrow…" },
        c("^Setting up the auction") { "§6AH §7· setup…" },

        // --- Slayer ---
        c("^SLAYER QUEST STARTED!") { "§5Slayer §7· started" },
        c("^SLAYER QUEST COMPLETE!") { "§5Slayer §a✔ §fcomplete" },
        c("^» Slay ([\\d,]+) Combat XP worth of (.+?)\\.?$") { m -> "§7Slay §f${m.group(1)} XP §7of ${m.group(2)}" },

        // --- Events ---
        c("^Started parkour ") { "§bParkour §7· start" },
        c("^Finished parkour .+ in (.+)!$") { m -> "§bParkour §a✔ §f${m.group(1)}" },
        c("^Warped from the (.+) to the (.+)!$") { m -> "§8→ §7${m.group(2)}" },
        c("^HOPPITY'S HUNT You found an? (.+?) Egg") { m -> "§6+ §f${m.group(1)} Egg" },
        c("^SACRIFICE! .+ turned .+ into ([\\d,]+) Dragon Essence!") { m -> "§5Sacrifice §7· §f+${m.group(1)} Dragon Essence" },
    )

    // Mystery Box : item extrait du raw pour garder sa couleur de rareté.
    private val BOX_IN = Pattern.compile(" found an? (.+) in an? (?:Holiday )?Mystery Box!")
    private val BOX_TIER = Pattern.compile(" found an? (.+) Mystery Box!$")

    /** Segment coloré du raw entre "found a/an " et [endMarker], codes couleur conservés. */
    fun rawItem(raw: String, endMarker: String): String? {
        val m = Regex("found an? ").find(raw) ?: return null
        val span = raw.substring(m.range.last + 1).substringBefore(endMarker)
        return span.replace(Regex("(?:§.|\\s)+$"), "").trim().ifEmpty { null }
    }

    /** Compact du message ou null si aucun beautifier ne matche. */
    fun beautify(clean: String, raw: String): String? {
        var m = BOX_IN.matcher(clean)
        if (m.find()) return "§7Mystery Box §8· §r" + (rawItem(raw, "in a") ?: "§f${m.group(1)}")
        m = BOX_TIER.matcher(clean)
        if (m.find()) return "§7Mystery Box §8· §r" + (rawItem(raw, "Mystery Box") ?: "§f${m.group(1)}")
        for ((pattern, fmt) in COMPACTS) {
            val matcher = pattern.matcher(clean)
            if (matcher.find()) return fmt(matcher)
        }
        return null
    }
}
