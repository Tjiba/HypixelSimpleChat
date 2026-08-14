package com.simplechat.config

import com.simplechat.LocalPlayer
import com.simplechat.SimpleChatMod
import com.simplechat.engine.ChannelStyle
import com.simplechat.engine.GuildBridgeConfig
import com.simplechat.engine.PublicPrefixToggles
import com.simplechat.engine.RuleAction
import com.simplechat.engine.SelfPlayer
import com.simplechat.rules.Registry

/** Vue immuable de la config, passée au moteur. Construite depuis Settings au runtime, ou à la main en test. */
data class RuleConfig(
    val masterEnabled: Boolean,
    val lobbyEnabled: Boolean,
    val skyblockEnabled: Boolean,
    val systemEnabled: Boolean,
    val groupRepeats: Boolean,
    val smartCollapse: Boolean,
    val actions: Map<String, RuleAction>,
    /** Réglage par groupe du nouveau moteur (rules/), clé = Group.id. */
    val groupActions: Map<String, RuleAction> = emptyMap(),
    val guildStyle: ChannelStyle,
    val partyStyle: ChannelStyle,
    val publicStyle: ChannelStyle,
    val partyPrefix: String,
    val prefix: PublicPrefixToggles,
    val bridge: GuildBridgeConfig,
    val customHidePatterns: List<String>,
    /** null = compte inconnu (hors partie, ou en test). */
    val self: SelfPlayer? = null,
    val showTimestamps: Boolean,
    val timestampColor: Int,
    val compactSoloClass: Boolean,
    val compactHoppity: Boolean,
    val compactTheme: Boolean = false,
    val compactThemeColor: Int = 0x55FFFF,
) {
    companion object {
        private const val RGB = 0xFFFFFF

        @Volatile private var cached: RuleConfig? = null
        @Volatile private var cachedAt: Long = 0L
        @Volatile private var cachedRevision = -1

        /** Snapshot courant, mis en cache brièvement (hot path : appelé par message + par frame de preview).
         *  Toute écriture dans la config incrémente ConfigEntry.revision et invalide le cache aussitôt,
         *  sinon deux réglages changés à moins de 150 ms d'écart afficheraient l'ancien état.
         *  Si le build échoue (config sur disque corrompue / enum renommé), on retombe sur le dernier
         *  bon snapshot ou le défaut au lieu de crasher le chat/rendu. */
        fun current(): RuleConfig {
            val c = cached
            val now = System.currentTimeMillis()
            val revision = ConfigEntry.revision
            if (c != null && revision == cachedRevision && now - cachedAt < 150) return c
            val fresh = try {
                build()
            } catch (e: Throwable) {
                SimpleChatMod.LOGGER.warn("Invalid config, falling back to default: {}", e.message)
                c ?: DEFAULT
            }
            cached = fresh; cachedAt = now; cachedRevision = revision
            return fresh
        }

        /** Défaut « tout ON » utilisé en test et comme repli. */
        val DEFAULT = RuleConfig(
            masterEnabled = true,
            lobbyEnabled = true,
            skyblockEnabled = true,
            systemEnabled = true,
            groupRepeats = true,
            smartCollapse = true,
            actions = emptyMap(),
            guildStyle = ChannelStyle(enabled = true, compact = true, showRank = true, showGuildRank = true, recolorName = false, nameColor = 0xFFFFFF, prefixColor = 0x55FF55, messageColor = 0x55FF55, recolorMessage = false),
            partyStyle = ChannelStyle(enabled = true, compact = true, showRank = false, showGuildRank = false, recolorName = false, nameColor = 0xFFFFFF, prefixColor = 0x0000AA, messageColor = 0x55FFFF, recolorMessage = false),
            publicStyle = ChannelStyle(enabled = true, compact = true, showRank = false, showGuildRank = false, recolorName = false, nameColor = 0xFFFFFF, prefixColor = 0x555555, messageColor = 0xAAAAAA, recolorMessage = false),
            partyPrefix = "P",
            prefix = PublicPrefixToggles(hideLevel = false, hideEmblem = true),
            bridge = GuildBridgeConfig("", "Bridge", "G", "O", 0x55FF55, 0x55FFFF, 0x55FF55, 0x55FFFF, true, true, 0x55FF55, 0xFFFF55, 0xFF5555),
            customHidePatterns = emptyList(),
            showTimestamps = false,
            timestampColor = 0x555555,
            compactSoloClass = true,
            compactHoppity = true,
        )

        /** Le compte connecté, ou null hors partie : l'aperçu retombe alors sur ses exemples. */
        private fun selfPlayer(): SelfPlayer? {
            val name = LocalPlayer.name()
            if (name.isEmpty()) return null
            return SelfPlayer(name, LocalPlayer.display(),
                (Settings.selfColor and RGB).takeIf { Settings.highlightSelf })
        }

        /** Projette la config Resourceful Config vers le snapshot pur. */
        private fun build(): RuleConfig = RuleConfig(
            masterEnabled = Settings.masterEnabled,
            lobbyEnabled = LobbyCleanup.enabled,
            skyblockEnabled = SkyBlockCleanup.enabled,
            systemEnabled = SystemCat.enabled,
            groupRepeats = Settings.groupRepeats,
            smartCollapse = Settings.smartCollapse,
            actions = RuleSettings.overrides(),
            groupActions = RuleSettings.groupActions(),
            guildStyle = ChannelStyle(true, GuildChat.mode == ChatMode.COMPACT, GuildChat.showRank, GuildChat.showGuildRank, GuildChat.recolorName, GuildChat.nameColor and RGB, GuildChat.guildPrefixColor and RGB, GuildChat.messageColor and RGB, GuildChat.recolorMessage),
            partyStyle = ChannelStyle(true, PartyChat.mode == ChatMode.COMPACT, PartyChat.showRank, false, PartyChat.recolorName, PartyChat.nameColor and RGB, PartyChat.prefixColor and RGB, PartyChat.messageColor and RGB, PartyChat.recolorMessage),
            publicStyle = ChannelStyle(true, PublicChat.mode == ChatMode.COMPACT, PublicChat.showRank, false, PublicChat.recolorName, PublicChat.nameColor and RGB, 0, PublicChat.messageColor and RGB, PublicChat.recolorMessage),
            partyPrefix = PartyChat.partyPrefix,
            prefix = PublicPrefixToggles(PublicChat.hideLevel, PublicChat.hideEmblem),
            bridge = GuildBridgeConfig(
                GuildChat.botMcName, GuildChat.bridgeAlias, GuildChat.guildPrefix, GuildChat.officerPrefix,
                GuildChat.guildPrefixColor and RGB, GuildChat.officerPrefixColor and RGB, GuildChat.bridgeAliasColor and RGB, GuildChat.bridgeNameColor and RGB,
                GuildChat.formatAllGuild, GuildChat.versionTags,
                GuildChat.v1Color and RGB, GuildChat.v2Color and RGB, GuildChat.v3Color and RGB,
            ),
            customHidePatterns = SkyBlockCleanup.customPatterns.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            self = selfPlayer(),
            showTimestamps = Settings.showTimestamps,
            timestampColor = Settings.timestampColor and RGB,
            compactSoloClass = SkyBlockCleanup.soloClass,
            compactHoppity = SkyBlockCleanup.hoppity,
            compactTheme = Settings.compactTheme,
            compactThemeColor = Settings.compactThemeColor and 0xFFFFFF,
        )
    }
}
