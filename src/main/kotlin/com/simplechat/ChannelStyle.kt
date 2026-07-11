package com.simplechat

/**
 * Style d'un canal. La couleur du rank n'est jamais modifiée (garde Hypixel) ; le nom prend la
 * couleur du rank par défaut. [showRank] affiche ou masque le tag de rank.
 */
data class ChannelStyle(
    val enabled: Boolean,
    val compact: Boolean,          // false = mode Vanilla (ne rien reformater)
    val showRank: Boolean,         // rank Hypixel [MVP+]
    val showGuildRank: Boolean,    // rank de guilde [Member]
    val recolorName: Boolean,      // false = nom en couleur du rank
    val nameColor: Int,            // utilisé seulement si recolorName = true
    val prefixColor: Int,
    val messageColor: Int,
    val recolorMessage: Boolean,   // false = garder les couleurs d'origine du message
)

/** Toggles indépendants du préfixe public (style SkyHanni). */
data class PublicPrefixToggles(
    val hideLevel: Boolean,
    val hideEmblem: Boolean,
)

/** Réglages du bridge guilde (portés de GuildZip). Couleur choisie par préfixe. */
data class GuildBridgeConfig(
    val botMcName: String,
    val botAlias: String,
    val guildPrefix: String,
    val officerPrefix: String,
    val guildPrefixColor: Int,
    val officerPrefixColor: Int,
    val bridgeAliasColor: Int,
    val bridgeNameColor: Int,   // couleur du pseudo Discord (relais sans rank)
    val formatAllGuild: Boolean,
    val versionTagsEnabled: Boolean,
    val v1Color: Int,
    val v2Color: Int,
    val v3Color: Int,
)
