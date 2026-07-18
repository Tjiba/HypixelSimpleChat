package com.simplechat

import java.util.regex.Pattern

/** Groupes de messages inutiles, toggleables indépendamment. */
enum class HideGroup {
    TRANSITIONS, DUNGEONS, ABILITIES, COMBAT, REWARDS, MISC,
    // Blocs style SkyHanni (réimplémentés depuis les patterns, match sur texte décoloré).
    BAZAAR, SLAYER, EVENTS, WARNINGS, NOTIFICATIONS,
}

/**
 * Masque les messages "inutiles" (patterns repris de NoammAddons `uselessMessages.json`),
 * groupés et toggleables. Full-match sur le texte décoloré (espaces conservés). Pur.
 */
object HideUseless {

    private val COLOR_ONLY = Pattern.compile("[§&][0-9a-fk-orA-FK-OR]")

    /** Retire uniquement les codes couleur/format, garde l'espacement (les patterns en dépendent). */
    fun stripColors(raw: String): String = COLOR_ONLY.matcher(raw).replaceAll("")

    private fun p(vararg regex: String): List<Pattern> = regex.map { Pattern.compile(it) }

    private val PATTERNS: Map<HideGroup, List<Pattern>> = mapOf(
        HideGroup.TRANSITIONS to p(
            "^(?:Warping(?: you to your SkyBlock island\\.\\.\\.|\\.\\.\\.)|Sending to server .+|Queuing\\.\\.\\. .+)$",
            "^Welcome to Hypixel SkyBlock!$",
            "^Latest update: SkyBlock .+$",
            "^(?:You are playing on profile|Profile ID): .+$",
            "Error initializing players: undefined Hidden",
            "^ {2}(?:Clicking sketchy links can result in your account|being stolen!|Link looks suspicious\\? - Don't click it!)$",
            "(?:Blacklisted modifications are a bannable offense!|Staff have banned an additional .+|\\[WATCHDOG ANNOUNCEMENT])",
            "^Watchdog has banned .+ players in the last 7 days\\.$",
            "You have 60 seconds to warp out! CLICK to warp now!",
            ".+ the lobby!.*",
            "Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!",
            // Lobby / mystery box / pubs
            ".+ found an? .+ Mystery Box!",
            ".+ found an? .+ in an? (?:Holiday )?Mystery Box!",
            "You earned \\d+ Mystery Dust!",
            "You earned \\d+ Pet Consumables items!",
            "You can now create your own Hypixel SMP server!",
            ".*enable Snow Particles.*",
            "Welcome to the Prototype Lobby",
            "HYPIXEL is hosting a .+ tournament!",
        ),
        HideGroup.DUNGEONS to p(
            "(?:A .+ Key was picked up!\\?|RIGHT CLICK on .+ to open it\\. This key can only be used to open 1 door!|.+ opened a .+ door!|You hear the sound of something opening\\.\\.\\.|You do not have the key for this door!)",
            "(?:This lever has already been used\\.|Someone has already activated this lever!)",
            "(?:This chest has already been searched!|You have already opened this dungeon chest!|That chest is locked!)",
            "^(?:A mystical force .+|You cannot (?:use abilities|do that) in this room!)$",
            "^(?:You don't have enough charges to break this block right now!|There are blocks in the way!)$",
            "This creature is immune to this kind of magic!",
            "PUZZLE SOLVED!.+",
            "\\[(?:STATUE|BOSS|NPC|SKULL|BOMB|Sacks|CROWD|Healer)] .+",
            "You cannot (?:move the silverfish in that direction!|hit the silverfish while it's moving!)",
            "(?:It isn't your turn!|Don't move diagonally! Bad!|Oops! You stepped on the wrong block!)",
            ".+ Mort: .+",
            ".+ the Fairy: .+",
            "DUNGEON BUFF! .+",
            "(?:A Blessing of .+ was picked up!|.+ has obtained Blessing of .+!)",
            "(?: {5}(?:Also )?(?:grants|granted) you .+|.*Granted you.+)",
            ".+ has obtained (?:(?!Wither Key!|Blood Key!).+ Key!|Superboom TNT(?: x[0-9])?!|Revive Stone!|Premium Flesh!|Beating Heart!)",
        ),
        HideGroup.ABILITIES to p(
            "(?:This (?:item's ability|item|ability)|Your Ultimate) is (?:temporarily disabled!|on cooldown.+|currently on cooldown for .+ more seconds\\.)",
            "You (?:do not have enough|need at least .+) mana to (?:do this|activate this)!",
            "(?:.+ is ready to use! Press DROP to activate it!|.+ is now (?:available|ready)!|Used (?:Ragnarok|Throwing Axe|Healing Circle)!)",
            "Your .+ stats are doubled because you are the only player using this class!",
            "(?:Archer|Mage|Berserker|Tank|Healer) Milestone.+",
            "Creeper Veil (?:Activated|De-activated)!",
            "Command Failed: This command is on cooldown! Try again in about a second!",
            "This (?:menu is disabled here!|Terminal doesn't seem to be responsive at the moment\\.)",
            "(?:Whow! Slow down there!|Woah slow down, you're doing that too fast!|Please wait a (?:few seconds between refreshing!|bit before doing this!))",
            "The Spirit Bow disintegrates as you fire off the shot!",
            "Giga Lightning.+",
            "Your Auto Recombobulator recombobulated.*",
            "(?:Only up to 2 rules may trigger at once!|Some of your autopet rules did not trigger\\.)",
            "You cannot put this item in the Potion Bag!",
            "(?:Your active Potion Effects have been paused|You are not allowed to use Potion Effects while in Dungeon).+restored when you leave Dungeons?!.*",
        ),
        HideGroup.COMBAT to p(
            "(?:Goldor's (?:TNT Trap|Greatsword)|Necron's Nuclear Frenzy|The .+ Trap) hit you for [\\d,.]+ (?:true )?damage\\.?|A (?:Crypt Wither Skull|Spirit Sheep) exploded, hitting you for .+ damage\\.",
            "The (?:Frozen|Lost) Adventurer used (?:Ice Spray|Dragon's Breath) on you!",
            "The Mage's Magma burnt you for .+ true damage\\.",
            ".+ (?:struck|hit|exploded) .+ (?:for |you for ).+",
            "(?:.+ healed you for|You were healed for|Your fairy healed|Your Spirit Pet healed|Your tether with .+ healed you for) .+ health.*",
            "BUFF! You (?:were splashed by .+ with Healing VIII!|have gained Healing V!)",
            "You gained .+ HP worth of absorption for 3s from .+!",
            ".+ granted you .+ strength for 20 seconds!",
            "Your bone plating reduced the damage you took by .+!",
            ".+ formed a tether with you!",
            ".+ used .+ on you!",
            "Mute silenced you!",
            ".+ (?:picked up your .+ Orb!|You picked up a .+ Orb from .+ healing you for .+)",
            "A shiver runs down your spine\\.\\.\\.",
        ),
        HideGroup.REWARDS to p(
            "(?:ESSENCE! .+ found .+ Essence!|.+ unlocked .+ Essence.+| {4}.+ Essence x.+|.+ found a Wither Essence! Everyone gains an extra essence!)",
            "RARE DROP! (?:Hunk of Blue Ice.*|Beating Heart .+)",
            "^You earned .+ (?:Event EXP|GEXP) from playing .+!$",
            "BONUS! Temporarily earn [0-9]+% more skill experience!",
            " Experience Team Bonus",
            "You have .+ unclaimed .+",
            " +(?:You have [0-9]+ unclaimed event rewards!|>>> CLICK HERE to claim! <<<|Event rewards are deleted after 10 SkyBlock years!)",
            "(?:\\+[0-9]+ Kill Combo|Your Kill Combo has expired! You reached a [0-9]+ Kill Combo!).*",
            "Your radio(?: is weak\\. Find another enjoyer to boost it\\.| signal is strong!| lost signal\\. There's too many enjoyers on this channel\\.)",
            "(?:CHARM|SALT) You charmed a .+ and captured its Shard\\.",
            "^The Redstone Pigmen are unhappy with you stealing their ores! Look out!$",
            "You earned [\\d,]+ (?:GEXP|Event EXP) from playing SkyBlock!",
        ),
        HideGroup.MISC to p(
            "(?:Inventory full\\? Don't forget to check out your Storage inside the SkyBlock Menu!|You don't have any inventory space!)",
            "One or more Legacy Items in your inventory.*",
            "The Frog is exhausted.*",
        ),
        HideGroup.BAZAAR to p(
            "Putting item in escrow\\.\\.\\.",
            "Setting up the auction\\.\\.\\.",
            "\\[Bazaar] Submitting sell offer\\.\\.\\.",
            "\\[Bazaar] Submitting buy order\\.\\.\\.",
            "(?:Buy Order|Sell Offer) Setup! .+",
        ),
        HideGroup.SLAYER to p(
            " *SLAYER QUEST STARTED!",
            " *SLAYER QUEST COMPLETE!",
            " *» Slay .+ Combat XP worth of .+",
            "✆ RING\\.\\.\\..*",
        ),
        HideGroup.EVENTS to p(
            "Started parkour .+!",
            "Finished parkour .+ in .+!",
            "Cancelled parkour! You cannot fly\\.",
            "Warped from the .+ to the .+!",
            "This Teleport Pad does not have a destination set!",
            ".*FIRE SALE.*",
            "Fire Sales for .+ are starting soon!",
            "HOPPITY'S HUNT .+ has appeared!",
            "HOPPITY'S HUNT You found an? .+ Egg", // avec ou sans « near … » (Hitman Egg, etc.)
            "SACRIFICE! .+ turned .+ into .+ Dragon Essence!",
            "RARE REWARD! .+ found a .+ in their .+ Chest!",
            // Chocolate Factory / Fire Sale détaillé / Winter
            "Your Rabbit Barn capacity has been increased.*",
            "♨ .+ for a limited time.*",
            ".+ mounted a Snow Cannon!",
        ),
        HideGroup.WARNINGS to p(
            "You are sending commands too fast! Please slow down\\.",
            "You can't use this while in combat!",
            "Whoa! Slow down there!",
            "You cannot damage a tree while it is regenerating!",
            "The toughness of this tree is way too high!",
            "Monsters around here can only take damage from Axes!",
        ),
        HideGroup.NOTIFICATIONS to p(
            "You tipped \\d+ players? in \\d+ .+!",
            "Your previous Plasmaflux Power Orb was removed!",
            "You have just received .+ coins as interest.*",
            "You caught yourself an invisibug!.*",
            "Mochibear ate too much and passed out! You caught it!",
            "Achievement Unlocked: .+",
            "Thanks for the donation! I've added a Kernel to your purse\\.",
            "You haven't claimed your .+ Rewards yet!",
        ),
    )

    /** Premier groupe (action != OFF) dont un pattern matche, ou null. */
    fun matchedGroup(raw: String, cfg: RuleConfig): HideGroup? {
        val text = stripColors(raw)
        for ((group, action) in cfg.hideGroupActions) {
            if (action == HideAction.OFF) continue
            val pats = PATTERNS[group] ?: continue
            if (pats.any { it.matcher(text).find() }) return group
        }
        return null
    }

    /** true si un pattern custom (sous-chaîne) matche. */
    fun matchesCustom(raw: String, cfg: RuleConfig): Boolean {
        val text = stripColors(raw)
        return cfg.customHidePatterns.any { it.isNotBlank() && text.contains(it) }
    }
}
