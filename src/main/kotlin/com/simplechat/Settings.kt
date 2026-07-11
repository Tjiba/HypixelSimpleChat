package com.simplechat

import com.teamresourceful.resourcefulconfig.api.types.info.ResourcefulConfigLink
import com.teamresourceful.resourcefulconfig.api.types.options.TranslatableValue
import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import com.teamresourceful.resourcefulconfigkt.api.ConfigKt
import java.awt.Color

/** Mode d'affichage d'un canal. */
enum class ChatMode { VANILLA, COMPACT }

object Settings : ConfigKt("simplechat/config") {
    override val name: TranslatableValue
        get() = Literal("Hypixel Simple Chat")

    // Lien natif RC (bouton dans l'en-tête du menu) — plus besoin d'overlay dessiné à la main.
    override val links: Array<ResourcefulConfigLink>
        get() = arrayOf(
            ResourcefulConfigLink.create("https://discord.gg/6y35KkQ7h6", "link", Literal("Join our Discord")),
        )

    var masterEnabled by boolean(true) {
        name = Translated("Enable mod")
        description = Translated("Master switch — off leaves chat 100% untouched")
    }
    var groupingWindowSeconds by int(3) {
        name = Translated("Grouping window (seconds)")
        description = Translated("Repeated messages within this delay are collapsed")
    }
    var smartCollapse by boolean(true) {
        name = Translated("Smart collapse")
        description = Translated("Also merge repeats that differ only by numbers (damage, coins…)")
    }
    var updateNotifications by boolean(true) {
        name = Translated("Update notifications")
    }
    var showTimestamps by boolean(false) {
        name = Translated("Show timestamps")
        description = Translated("Prefix every message with [HH:MM]")
    }
    var timestampColor by color(Color(0x555555).rgb) { name = Translated("Timestamp color") }

    init {
        category(GuildChat)
        category(PartyChat)
        category(PublicChat)
        category(LobbyCleanup)
        category(SkyBlockCleanup)
        category(SystemCat)
    }
}

object GuildChat : CategoryKt("Guild Chat") {
    var mode by enum(ChatMode.COMPACT) {
        name = Translated("Mode")
        description = Translated("Vanilla = untouched · Compact = reformatted")
    }
    var showRank by boolean(true) { name = Translated("Show Hypixel rank") }
    var showGuildRank by boolean(true) { name = Translated("Show guild rank") }
    var recolorName by boolean(false) {
        name = Translated("Recolor name")
        description = Translated("Off = name in the player's rank color")
    }
    var nameColor by color(Color(0xFFFFFF).rgb) { name = Translated("Name color") }

    // Préfixes : texte puis couleur juste dessous.
    var guildPrefix by string("G") { name = Translated("Guild prefix") }
    var guildPrefixColor by color(Color(0x55FF55).rgb) { name = Translated("Guild prefix color") }
    var officerPrefix by string("O") { name = Translated("Officer prefix") }
    var officerPrefixColor by color(Color(0x55FFFF).rgb) { name = Translated("Officer prefix color") }
    var bridgeAlias by string("Bridge") { name = Translated("Bridge prefix") }
    var bridgeAliasColor by color(Color(0x55FF55).rgb) { name = Translated("Bridge prefix color") }
    var bridgeNameColor by color(Color(0x55FFFF).rgb) { name = Translated("Bridge name color") }

    var recolorMessage by boolean(false) {
        name = Translated("Recolor message")
        description = Translated("Off = keep Hypixel's original colors")
    }
    var messageColor by color(Color(0x55FF55).rgb) { name = Translated("Message color") }

    // Bridge avancé (porté de GuildZip)
    var botMcName by string("") {
        name = Translated("Bridge bot name")
        description = Translated("Only this account's messages are formatted (empty = auto-detect)")
    }
    var formatAllGuild by boolean(true) {
        name = Translated("Format all guild messages")
        description = Translated("Format every guild message, not just Discord bridge relays")
    }
    var versionTags by boolean(true) {
        name = Translated("Show V1/V2/V3 tags")
        description = Translated("Show the guild version label instead of the bridge alias when detected")
    }
    var v1Color by color(Color(0x55FF55).rgb) { name = Translated("V1 color") }
    var v2Color by color(Color(0xFFFF55).rgb) { name = Translated("V2 color") }
    var v3Color by color(Color(0xFF5555).rgb) { name = Translated("V3 color") }
}

object PartyChat : CategoryKt("Party Chat") {
    var mode by enum(ChatMode.COMPACT) {
        name = Translated("Mode")
        description = Translated("Vanilla = untouched · Compact = reformatted")
    }
    var showRank by boolean(false) { name = Translated("Show rank") }
    var recolorName by boolean(false) {
        name = Translated("Recolor name")
        description = Translated("Off = name in the player's rank color")
    }
    var nameColor by color(Color(0xFFFFFF).rgb) { name = Translated("Name color") }

    var partyPrefix by string("P") { name = Translated("Party prefix") }
    var prefixColor by color(Color(0x0000AA).rgb) { name = Translated("Party prefix color") }

    var recolorMessage by boolean(false) {
        name = Translated("Recolor message")
        description = Translated("Off = keep Hypixel's original colors")
    }
    var messageColor by color(Color(0x55FFFF).rgb) { name = Translated("Message color") }
}

object PublicChat : CategoryKt("Public Chat") {
    var mode by enum(ChatMode.COMPACT) {
        name = Translated("Mode")
        description = Translated("Vanilla = untouched · Compact = reformatted")
    }
    var showRank by boolean(false) { name = Translated("Show rank") }
    var recolorName by boolean(false) {
        name = Translated("Recolor name")
        description = Translated("Off = name in the player's rank color")
    }
    var nameColor by color(Color(0xFFFFFF).rgb) { name = Translated("Name color") }

    var hideLevel by boolean(false) { name = Translated("Hide level [221]") }
    var hideEmblem by boolean(true) { name = Translated("Hide emblem") }

    var recolorMessage by boolean(false) {
        name = Translated("Recolor message")
        description = Translated("Off = keep Hypixel's original colors")
    }
    var messageColor by color(Color(0xAAAAAA).rgb) { name = Translated("Message color") }
}

object LobbyCleanup : CategoryKt("Lobby") {
    var enabled by boolean(true) { name = Translated("Enable") }

    var lobbyJoin by enum(RuleAction.HIDE) {
        name = Translated("Lobby join/leave")
        description = Translated("OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove")
    }
    var boosterActivated by enum(RuleAction.COMPACT) { name = Translated("Booster activated") }
    var mysteryReward by enum(RuleAction.COMPACT) { name = Translated("Daily/mystery reward") }
    var claimedCurrency by enum(RuleAction.COMPACT) { name = Translated("Claimed rewards") }
    var petSpawn by enum(RuleAction.OFF) { name = Translated("Pet spawn/despawn") }
    var radiatingGenerosity by enum(RuleAction.GREY) { name = Translated("Radiating generosity") }
    var playtimeTicket by enum(RuleAction.GREY) { name = Translated("Playtime ticket") }
    var rewardLink by enum(RuleAction.GREY) { name = Translated("Reward website link") }
    var morphWardrobe by enum(RuleAction.GREY) { name = Translated("Morph / wardrobe") }
    var raffleWin by enum(RuleAction.COMPACT) { name = Translated("Raffle winners") }

    // Groupe de spam (contexte lobby/global). Dropdown OFF/GREY/COMPACT/COMPACT_GREY/HIDE.
    var notifications by enum(HideAction.HIDE) {
        name = Translated("Notifications")
        description = Translated("Tipped players, plasmaflux, interest, hunting catches")
    }
}

object SystemCat : CategoryKt("System") {
    var enabled by boolean(true) { name = Translated("Enable") }

    var serverRouting by enum(RuleAction.COMPACT) {
        name = Translated("Server routing / warping")
        description = Translated("OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove")
    }
    var profileId by enum(RuleAction.COMPACT) { name = Translated("Profile ID line") }

    // Groupe de spam (transitions serveur). Dropdown OFF/GREY/COMPACT/COMPACT_GREY/HIDE.
    var transitions by enum(HideAction.HIDE) {
        name = Translated("Server / transitions")
        description = Translated("Warping, sending to server, profile switch, watchdog, welcome, queuing")
    }
}

object SkyBlockCleanup : CategoryKt("SkyBlock") {
    var enabled by boolean(true) { name = Translated("Enable") }

    var npcDialog by enum(RuleAction.GREY) {
        name = Translated("NPC dialog")
        description = Translated("OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove")
    }
    var boss by enum(RuleAction.HIDE) {
        name = Translated("Boss messages")
        description = Translated("Boss/summon lines (ARACHNE, dungeon bosses, etc.)")
    }
    var damageSpam by enum(RuleAction.GREY) { name = Translated("Damage numbers") }
    var killCombo by enum(RuleAction.GREY) { name = Translated("Kill combo") }
    var mobAbility by enum(RuleAction.GREY) { name = Translated("Mob abilities") }
    var lootShare by enum(RuleAction.COMPACT) { name = Translated("Loot share") }
    var rareReward by enum(RuleAction.COMPACT) { name = Translated("Rare reward (chest)") }
    var gexp by enum(RuleAction.COMPACT) { name = Translated("Guild EXP earned") }
    var sacks by enum(RuleAction.GREY) { name = Translated("Sacks notifications") }

    var soloClass by boolean(true) {
        name = Translated("Compact solo class stats")
        description = Translated("Shorten 'stats are doubled…' — full text on hover")
    }
    var hoppity by boolean(true) {
        name = Translated("Compact Hoppity rabbits")
        description = Translated("Merge 'You found X' + 'New/Duplicate Rabbit!' into one short line")
    }

    // Groupes de spam (contexte SkyBlock). Dropdown : OFF/GREY/COMPACT/COMPACT_GREY/HIDE.
    var dungeons by enum(HideAction.HIDE) {
        name = Translated("Dungeons")
        description = Translated("Keys, doors, levers, chests, puzzles, blessings, boss/NPC lines")
    }
    var abilities by enum(HideAction.HIDE) {
        name = Translated("Abilities / cooldowns")
        description = Translated("Cooldown, not enough mana, ability ready, autopet, slow down")
    }
    var combat by enum(HideAction.HIDE) {
        name = Translated("Combat / heal")
        description = Translated("Damage taken, healing, buffs, tethers, orbs")
    }
    var rewards by enum(HideAction.HIDE) {
        name = Translated("Rewards / drops")
        description = Translated("Essence, event EXP, kill combo, radio, unclaimed rewards")
    }
    var misc by enum(HideAction.HIDE) {
        name = Translated("Misc")
        description = Translated("Inventory full and other odds and ends")
    }
    var bazaar by enum(HideAction.HIDE) {
        name = Translated("Bazaar / Auction House")
        description = Translated("Escrow, submitting offers, order setup spam")
    }
    var slayer by enum(HideAction.HIDE) {
        name = Translated("Slayer")
        description = Translated("Quest started/complete, boss ring, slay lines")
    }
    var events by enum(HideAction.HIDE) {
        name = Translated("Events")
        description = Translated("Parkour, teleport pads, fire sales, Hoppity, sacrifice, rare rewards")
    }
    var warnings by enum(HideAction.HIDE) {
        name = Translated("Warnings")
        description = Translated("Too fast, can't use in combat, tree regenerating, wrong tool")
    }
    var customPatterns by string("") {
        name = Translated("Custom hidden messages")
        description = Translated("Comma-separated text — any message containing one of these is hidden")
    }
}
