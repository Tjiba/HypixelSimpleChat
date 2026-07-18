package com.simplechat

import com.simplechat.config.HscCategory
import com.simplechat.config.HscConfig
import java.awt.Color

/** Mode d'affichage d'un canal. */
enum class ChatMode { VANILLA, COMPACT }

object Settings : HscConfig("simplechat/config") {

    var masterEnabled by boolean(true) {
        name = "Enable mod"
        description = "Master switch — off leaves chat 100% untouched"
    }
    var groupingWindowSeconds by int(3) {
        name = "Grouping window (seconds)"
        description = "Repeated messages within this delay are collapsed"
    }
    var smartCollapse by boolean(true) {
        name = "Smart collapse"
        description = "Also merge repeats that differ only by numbers (damage, coins…)"
    }
    var updateNotifications by boolean(true) {
        name = "Update notifications"
    }
    var maxMessages by int(200) {
        name = "Max chat history"
        description = "How many chat messages to keep (vanilla = 100, up to 2048)"
    }
    var showTimestamps by boolean(false) {
        name = "Show timestamps"
        description = "Prefix every message with [HH:MM]"
    }
    var timestampColor by color(Color(0x555555).rgb) { name = "Timestamp color" }

    var compactTheme by boolean(false) {
        name = "Compact color theme"
        description = "Recolor the white words of compact messages with your theme color — one consistent look"
    }
    var compactThemeColor by color(Color(0x55FFFF).rgb) { name = "Theme color" }

    var chatTabs by boolean(true) {
        name = "Enable chat tabs"
        description = "Show the All/Party/Guild tab row above the chat input"
    }
    var tabFilterMode by boolean(false) {
        name = "Only show channel messages while selected"
        description = "When you select a tab — On: show only that channel's messages · Off: send your messages to that channel"
    }

    /** Preset bundlé (config recommandée). false si la ressource manque. */
    fun applyRecommended(): Boolean {
        val txt = javaClass.getResourceAsStream("/assets/simplechat/presets/recommended.json")
            ?.bufferedReader()?.use { it.readText() } ?: return false
        applyPreset(txt)
        return true
    }

    // Tout premier lancement (aucun fichier config) : partir de la config recommandée.
    override fun firstLaunch() { applyRecommended() }

    init {
        category(GuildChat)
        category(PartyChat)
        category(PublicChat)
        category(LobbyCleanup)
        category(SkyBlockCleanup)
        category(SystemCat)
    }
}

object GuildChat : HscCategory("Guild Chat") {
    var mode by enum(ChatMode.COMPACT) {
        name = "Mode"
        description = "Vanilla = untouched · Compact = reformatted"
    }
    var showRank by boolean(true) { name = "Show Hypixel rank" }
    var showGuildRank by boolean(true) { name = "Show guild rank" }
    var recolorName by boolean(false) {
        name = "Recolor name"
        description = "Off = name in the player's rank color"
    }
    var nameColor by color(Color(0xFFFFFF).rgb) { name = "Name color" }

    // Préfixes : texte puis couleur juste dessous.
    var guildPrefix by string("G") { name = "Guild prefix" }
    var guildPrefixColor by color(Color(0x55FF55).rgb) { name = "Guild prefix color" }
    var officerPrefix by string("O") { name = "Officer prefix" }
    var officerPrefixColor by color(Color(0x55FFFF).rgb) { name = "Officer prefix color" }
    var bridgeAlias by string("Bridge") { name = "Bridge prefix" }
    var bridgeAliasColor by color(Color(0x55FF55).rgb) { name = "Bridge prefix color" }
    var bridgeNameColor by color(Color(0x55FFFF).rgb) { name = "Bridge name color" }

    var recolorMessage by boolean(false) {
        name = "Recolor message"
        description = "Off = keep Hypixel's original colors"
    }
    var messageColor by color(Color(0x55FF55).rgb) { name = "Message color" }

    // Bridge avancé (porté de GuildZip)
    var botMcName by string("") {
        name = "Bridge bot name"
        description = "Only this account's messages are formatted (empty = auto-detect)"
    }
    var formatAllGuild by boolean(true) {
        name = "Format all guild messages"
        description = "Format every guild message, not just Discord bridge relays"
    }
    var versionTags by boolean(true) {
        name = "Show V1/V2/V3 tags"
        description = "Show the guild version label instead of the bridge alias when detected"
    }
    var v1Color by color(Color(0x55FF55).rgb) { name = "V1 color" }
    var v2Color by color(Color(0xFFFF55).rgb) { name = "V2 color" }
    var v3Color by color(Color(0xFF5555).rgb) { name = "V3 color" }
}

object PartyChat : HscCategory("Party Chat") {
    var mode by enum(ChatMode.COMPACT) {
        name = "Mode"
        description = "Vanilla = untouched · Compact = reformatted"
    }
    var showRank by boolean(false) { name = "Show rank" }
    var recolorName by boolean(false) {
        name = "Recolor name"
        description = "Off = name in the player's rank color"
    }
    var nameColor by color(Color(0xFFFFFF).rgb) { name = "Name color" }

    var partyPrefix by string("P") { name = "Party prefix" }
    var prefixColor by color(Color(0x0000AA).rgb) { name = "Party prefix color" }

    var recolorMessage by boolean(false) {
        name = "Recolor message"
        description = "Off = keep Hypixel's original colors"
    }
    var messageColor by color(Color(0x55FFFF).rgb) { name = "Message color" }
}

object PublicChat : HscCategory("Public Chat") {
    var mode by enum(ChatMode.COMPACT) {
        name = "Mode"
        description = "Vanilla = untouched · Compact = reformatted"
    }
    var showRank by boolean(false) { name = "Show rank" }
    var recolorName by boolean(false) {
        name = "Recolor name"
        description = "Off = name in the player's rank color"
    }
    var nameColor by color(Color(0xFFFFFF).rgb) { name = "Name color" }

    var hideLevel by boolean(false) { name = "Hide level [221]" }
    var hideEmblem by boolean(true) { name = "Hide emblem" }

    var recolorMessage by boolean(false) {
        name = "Recolor message"
        description = "Off = keep Hypixel's original colors"
    }
    var messageColor by color(Color(0xAAAAAA).rgb) { name = "Message color" }
}

object LobbyCleanup : HscCategory("Lobby") {
    var enabled by boolean(true) { name = "Enable" }

    var lobbyJoin by enum(RuleAction.HIDE) {
        name = "Lobby join/leave"
        description = "OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove"
    }
    var boosterActivated by enum(RuleAction.COMPACT) { name = "Booster activated" }
    var mysteryReward by enum(RuleAction.COMPACT) { name = "Daily/mystery reward" }
    var claimedCurrency by enum(RuleAction.COMPACT) { name = "Claimed rewards" }
    var petSpawn by enum(RuleAction.COMPACT) { name = "Pet spawn/despawn" }
    var radiatingGenerosity by enum(RuleAction.COMPACT) { name = "Radiating generosity" }
    var playtimeTicket by enum(RuleAction.COMPACT) { name = "Playtime ticket" }
    var rewardLink by enum(RuleAction.COMPACT) { name = "Reward website link" }
    var morphWardrobe by enum(RuleAction.COMPACT) { name = "Morph / wardrobe" }

    // Groupe de spam (contexte lobby/global). Dropdown OFF/GREY/COMPACT/COMPACT_GREY/HIDE.
    var notifications by enum(HideAction.HIDE) {
        name = "Notifications"
        description = "Tipped players, plasmaflux, bank interest, hunting catches, achievements, unclaimed rewards reminder"
    }
}

object SystemCat : HscCategory("System") {
    var enabled by boolean(true) { name = "Enable" }

    var serverRouting by enum(RuleAction.COMPACT) {
        name = "Server routing / warping"
        description = "OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove"
    }
    var profileId by enum(RuleAction.COMPACT) { name = "Profile ID line" }

    // Groupe de spam (transitions serveur). Dropdown OFF/GREY/COMPACT/COMPACT_GREY/HIDE.
    var transitions by enum(HideAction.HIDE) {
        name = "Server / transitions"
        description = "Warping, sending to server, queuing, welcome/profile lines, watchdog, mystery boxes, lobby ads"
    }
}

object SkyBlockCleanup : HscCategory("SkyBlock") {
    var enabled by boolean(true) { name = "Enable" }

    var petSummon by enum(RuleAction.COMPACT) {
        name = "Pet summon/despawn"
        description = "'You summoned your …' — compact keeps the pet's rarity color"
    }
    var abiphoneRing by enum(RuleAction.HIDE) {
        name = "Abiphone ring"
        description = "'✆ RING…' lines — the clickable pickup line always stays"
    }

    var npcDialog by enum(RuleAction.GREY) {
        name = "NPC dialog"
        description = "OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove"
    }
    var boss by enum(RuleAction.HIDE) {
        name = "Boss messages"
        description = "[BOSS] / [STATUE] dialog, '… has spawned!', 'ARACHNE DOWN!' shouts"
    }
    var damageSpam by enum(RuleAction.GREY) { name = "Damage numbers" }
    var killCombo by enum(RuleAction.GREY) { name = "Kill combo" }
    var mobAbility by enum(RuleAction.GREY) { name = "Mob abilities" }
    var lootShare by enum(RuleAction.COMPACT) { name = "Loot share" }
    var rareReward by enum(RuleAction.COMPACT) { name = "Rare reward (chest)" }
    var gexp by enum(RuleAction.COMPACT) { name = "Guild EXP earned" }
    var sacks by enum(RuleAction.GREY) { name = "Sacks notifications" }

    var soloClass by boolean(true) {
        name = "Compact solo class stats"
        description = "Shorten 'stats are doubled…' — full text on hover"
    }
    var hoppity by boolean(true) {
        name = "Compact Hoppity rabbits"
        description = "Merge 'You found X' + 'New/Duplicate Rabbit!' into one short line"
    }

    // Groupes de spam (contexte SkyBlock). Dropdown : OFF/GREY/COMPACT/COMPACT_GREY/HIDE.
    var dungeons by enum(HideAction.HIDE) {
        name = "Dungeons"
        description = "Keys, doors, levers, chests, puzzles, blessings, boss/NPC lines"
    }
    var abilities by enum(HideAction.HIDE) {
        name = "Abilities / cooldowns"
        description = "Cooldown, not enough mana, ability ready, class milestones, autopet, potion effects, slow down"
    }
    var combat by enum(HideAction.HIDE) {
        name = "Combat / heal"
        description = "Damage taken, healing, buffs, tethers, orbs"
    }
    var rewards by enum(HideAction.HIDE) {
        name = "Rewards / drops"
        description = "Essence finds, Event EXP bonuses, unclaimed rewards, radio signal, shard charms, expired combo"
    }
    var misc by enum(HideAction.HIDE) {
        name = "Misc"
        description = "Inventory full, Legacy Items notice, exhausted Frog"
    }
    var bazaar by enum(HideAction.HIDE) {
        name = "Bazaar / Auction House"
        description = "Escrow, submitting offers, order setup spam"
    }
    var slayer by enum(HideAction.HIDE) {
        name = "Slayer"
        description = "Quest started/complete, slay lines"
    }
    var events by enum(HideAction.HIDE) {
        name = "Events"
        description = "Parkour, teleport pads, fire sales, Hoppity eggs, sacrifice, snow cannon"
    }
    var warnings by enum(HideAction.HIDE) {
        name = "Warnings"
        description = "Too fast, can't use in combat, tree regenerating, wrong tool"
    }
    var customPatterns by string("") {
        name = "Custom hidden messages"
        description = "Comma-separated text — any message containing one of these is hidden"
    }
}
