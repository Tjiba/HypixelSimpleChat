package com.simplechat

/** Échantillons de chat rendus avec la config courante, pour l'aperçu live par catégorie. Pur. */
object Preview {

    private val GUILD = listOf(
        "Guild > BotName: G > DiscordUser: hey from discord",
        "Guild > [MVP++] BotName: [V1] DiscordUser: gg wp",
        "Guild > §b[MVP§c+§b] Player §7[Member]§f: hey all",
        "Officer > §6[MVP§1++§6] Officer §e[Staff]§f: on it",
    )
    private val PARTY = listOf(
        "Party > §b[MVP§c+§b] Friend§8: §ron my way",
    )
    private val PUBLIC = listOf(
        "§8[§d330§8] §6⛃ §8[§6MVP§1++§8] §6MeteoFrance§8:§r selling stuff",
    )

    // Échantillons dans l'ordre des options de chaque catégorie (une ligne par règle togglable).
    private val LOBBY = listOf(
        "§b[MVP§c+§b] Notch §ejoined the lobby!",
        "Activated your booster. You now have two hours of 2x coins.",
        "You have claimed a [Rare] reward card!",
        "You have successfully claimed 500 coins and 2 Mystery Dust!",
        "Spawned your Bat!",
        "You are still radiating with generosity!",
        "PLAYTIME! You gained a Playtime Chest!",
        "Click the link to visit our website and claim your reward: hypixel.net/link",
        "You are now morphed into a Cow!",
        "You tipped 12 players in 4 games!",
    )
    // Ordre calé sur le menu SkyBlock : General, World & Events, Combat, Economy, puis Islands.
    private val SKYBLOCK = listOf(
        // General (petSummon, abiphoneRing)
        "§aYou summoned your §6Baby Yeti§a!",
        "§a✆ RING... RING...",
        // World & Events (npcDialog, events, rewards, misc)
        "§e[NPC] Jerry§f: What can I do for you?",
        "Started parkour Foraging Island!",
        "RARE DROP! Hunk of Blue Ice",
        "Inventory full? Don't forget to check out your Storage inside the SkyBlock Menu!",
        // Combat (boss, damage, kill combo, mob ability, combat, abilities, warnings, slayer)
        "§c[BOSS] Maxor§r§f: I've been expecting you.",
        "Your Hyperion hit 3 enemies for 2,500,000 damage.",
        "+5 Kill Combo §6+25% §b✯ Magic Find",
        "The Zombie Soldier used Slam on you hitting you for 1,200 damage",
        "The Frozen Adventurer used Ice Spray on you!",
        "This ability is on cooldown for 2 more seconds.",
        "You can't use this while in combat!",
        "  SLAYER QUEST STARTED!",
        // Economy (bazaar, sacks, loot share, gexp, rare reward)
        "[Bazaar] Submitting sell offer...",
        "[Sacks] +64 Cobblestone",
        "LOOT SHARE You received loot for assisting Notch!",
        "You earned 211 GEXP + 633 Event EXP from playing SkyBlock!",
        "RARE REWARD! GunsBlazing239 found a Fuming Potato Book in their Obsidian Chest!",
        // Islands (dungeons, solo class)
        "You do not have the key for this door!",
        "Your Mage stats are doubled because you are the only player using this class!",
    )
    // Onglet SkyBlock « Islands » : messages spécifiques par île (aperçu du contenu à venir).
    private val SKYBLOCK_ISLANDS = listOf(
        "§6[Crimson Isle] §cKuudra hit you for 12,450 damage!",
        "§6Your reputation with the Barbarians increased! §a(+12)",
        "§bYou received §a+1,240 Mithril Powder§b!",
        "§2Commission Complete: §fMithril Miner!",
        "§5[Rift] §fYou gained §d+15 Motes§f!",
        "§aGalatea §7» §2Foraging XP §a+250",
    )
    private val SYSTEM = listOf(
        "Sending to server mini42S...",
        "Profile ID: 12ab34-cd56ef",
        "Warping you to your SkyBlock island...",
    )

    private fun render(cfg: RuleConfig, raw: String): List<Seg> =
        when (val v = ChatRules.evaluate(raw, cfg)) {
            is Verdict.Segments -> v.segs
            is Verdict.Compact -> LegacyText.parse(v.shortLegacy)
            is Verdict.Replace -> LegacyText.parse(v.legacy)
            Verdict.Hide -> listOf(Seg("(hidden)", 0x555555, italic = true))
            Verdict.Pass -> LegacyText.parse(raw)
        }

    private val TS_FMT = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

    /** Lignes rendues pour la catégorie affichée (via l'id du sous-config), ou vide si hors canal. */
    fun forChannel(cfg: RuleConfig, categoryId: String): List<List<Seg>> {
        val id = categoryId.lowercase()
        val samples = when {
            id.contains("guild") || id.contains("officer") -> GUILD
            id.contains("party") -> PARTY
            id.contains("public") -> PUBLIC
            id.contains("lobby") -> LOBBY
            id.contains("skyblock_islands") -> SKYBLOCK_ISLANDS
            id.contains("skyblock") -> SKYBLOCK
            id.contains("system") -> SYSTEM
            id.contains("simplechat") -> PUBLIC + PARTY + GUILD // page racine : tout (ordre sidebar)
            else -> return emptyList()
        }
        val lines = samples.map { render(cfg, it) }
        if (!cfg.showTimestamps) return lines
        val ts = Seg("[${java.time.LocalTime.now().format(TS_FMT)}] ", cfg.timestampColor)
        return lines.map { listOf(ts) + it }
    }
}
