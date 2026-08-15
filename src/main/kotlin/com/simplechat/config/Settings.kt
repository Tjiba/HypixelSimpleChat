package com.simplechat.config

import com.google.gson.JsonObject
import java.awt.Color

/** Mode d'affichage d'un canal. */
enum class ChatMode { VANILLA, COMPACT }

object Settings : HscConfig("simplechat/config") {

    var masterEnabled by boolean(true) {
        name = "Enable mod"
        description = "Master switch. Off leaves chat 100% untouched"
    }
    var groupRepeats by boolean(true) {
        name = "Group repeats"
        description = "Merge an identical message into its (xN) line, even with other messages in between"
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

    var highlightSelf by boolean(false) {
        name = "Highlight my name"
        description = "Color your own name in chat, to spot your messages at a glance"
    }
    var selfColor by color(Color(0xFFAA00).rgb) { name = "My name color" }

    var compactTheme by boolean(false) {
        name = "Compact color theme"
        description = "Recolor the white words of compact messages with your theme color"
    }
    var compactThemeColor by color(Color(0x55FFFF).rgb) { name = "Theme color" }

    var chatTabs by boolean(true) {
        name = "Enable chat tabs"
        description = "Show the All/Party/Guild tab row above the chat input"
    }
    var tabFilterMode by boolean(false) {
        name = "Only show channel messages while selected"
        description = "When you select a tab. On: show only that channel's messages · Off: send your messages to that channel"
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

    // Config d'avant les réglages par phrase : chaque phrase reprend la valeur de son groupe.
    override fun migrate(root: JsonObject) {
        RuleSettings.migrate(root)
        // Config d'avant le simple on/off : une fenêtre à 0 seconde valait « ne regroupe pas ».
        val seconds = runCatching { root.get("groupingWindowSeconds")?.asInt }.getOrNull() ?: return
        groupRepeats = seconds > 0
    }

    init {
        category(GuildChat)
        category(PartyChat)
        category(PublicChat)
        category(LobbyCleanup)
        category(SkyBlockCleanup)
        category(SystemCat)
        RuleSettings.register()
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

// Les réglages de règles sont ajoutés par RuleSettings depuis le registre : seules les
// options qui ne sont pas des règles restent écrites ici.
object LobbyCleanup : HscCategory("Lobby") {
    var enabled by boolean(true) { name = "Enable" }
}

object SystemCat : HscCategory("System") {
    var enabled by boolean(true) { name = "Enable" }
}

object SkyBlockCleanup : HscCategory("SkyBlock") {
    var enabled by boolean(true) { name = "Enable" }

    var soloClass by boolean(true) {
        name = "Compact solo class stats"
        description = "Shorten 'stats are doubled…', full text on hover"
    }
    var hoppity by boolean(true) {
        name = "Compact Hoppity rabbits"
        description = "Merge 'You found X' + 'New/Duplicate Rabbit!' into one short line"
    }

    var bazaarItemsColor by color(Color(0xFFAA00).rgb) {
        name = "Bazaar batch: items"
        description = "Color of the item count when several orders are merged into one line"
    }
    var bazaarSalesColor by color(Color(0x00AAAA).rgb) {
        name = "Bazaar batch: orders"
        description = "Color of the sale/buy count on that same line"
    }

    var customPatterns by string("") {
        name = "Custom hidden messages"
        description = "Comma-separated text. Any message containing one of these is hidden"
    }
}
