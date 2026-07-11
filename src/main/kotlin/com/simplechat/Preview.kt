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
        "§8[§b330§8] §6⛃ §8[§6MVP§1++§8] §6MeteoFrance§8:§r selling stuff",
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
        "RAFFLE! Notch won 5000 coins in Speed Raffle #12!",
        "You tipped 12 players in 4 games!",
    )
    private val SKYBLOCK = listOf(
        "§e[NPC] Jerry§f: What can I do for you?",
        "§c[BOSS] Maxor§r§f: I've been expecting you.",
        "Your Hyperion hit 3 enemies for 2,500,000 damage.",
        "+5 Kill Combo §6+25% §b✯ Magic Find",
        "The Zombie Soldier used Slam on you hitting you for 1,200 damage",
        "LOOT SHARE You received loot for assisting Notch!",
        "RARE REWARD! GunsBlazing239 found a Fuming Potato Book in their Obsidian Chest!",
        "You earned 211 GEXP + 633 Event EXP from playing SkyBlock!",
        "[Sacks] +64 Cobblestone",
        "Your Mage stats are doubled because you are the only player using this class!",
        "You do not have the key for this door!",
        "This ability is on cooldown for 2 more seconds.",
        "The Frozen Adventurer used Ice Spray on you!",
        "RARE DROP! Hunk of Blue Ice",
        "Inventory full? Don't forget to check out your Storage inside the SkyBlock Menu!",
        "[Bazaar] Submitting sell offer...",
        "  SLAYER QUEST STARTED!",
        "Started parkour Foraging Island!",
        "You can't use this while in combat!",
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

    /** Lignes rendues pour la catégorie affichée (via l'id du sous-config), ou vide si hors canal. */
    fun forChannel(cfg: RuleConfig, categoryId: String): List<List<Seg>> {
        val id = categoryId.lowercase()
        val samples = when {
            id.contains("guild") || id.contains("officer") -> GUILD
            id.contains("party") -> PARTY
            id.contains("public") -> PUBLIC
            id.contains("lobby") -> LOBBY
            id.contains("skyblock") -> SKYBLOCK
            id.contains("system") -> SYSTEM
            id.contains("simplechat") -> GUILD + PARTY + PUBLIC // page racine : tout
            else -> return emptyList()
        }
        return samples.map { render(cfg, it) }
    }
}
