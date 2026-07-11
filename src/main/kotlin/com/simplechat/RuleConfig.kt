package com.simplechat

/** Vue immuable de la config, passée au moteur. Construite depuis Settings au runtime, ou à la main en test. */
data class RuleConfig(
    val masterEnabled: Boolean,
    val lobbyEnabled: Boolean,
    val skyblockEnabled: Boolean,
    val systemEnabled: Boolean,
    val dedupWindowMs: Long,
    val smartCollapse: Boolean,
    val actions: Map<String, RuleAction>,
    val guildStyle: ChannelStyle,
    val partyStyle: ChannelStyle,
    val publicStyle: ChannelStyle,
    val partyPrefix: String,
    val prefix: PublicPrefixToggles,
    val bridge: GuildBridgeConfig,
    val hideGroupActions: Map<HideGroup, HideAction>,
    val customHidePatterns: List<String>,
    val showTimestamps: Boolean,
    val timestampColor: Int,
    val compactSoloClass: Boolean,
    val compactHoppity: Boolean,
) {
    fun actionOf(ruleId: String, default: RuleAction): RuleAction = actions[ruleId] ?: default

    companion object {
        private const val RGB = 0xFFFFFF

        @Volatile private var cached: RuleConfig? = null
        @Volatile private var cachedAt: Long = 0L

        /** Snapshot courant, mis en cache brièvement (hot path : appelé par message + par frame de preview).
         *  Si le build échoue (config sur disque corrompue / enum renommé), on retombe sur le dernier
         *  bon snapshot ou le défaut au lieu de crasher le chat/rendu. */
        fun current(): RuleConfig {
            val c = cached
            val now = System.currentTimeMillis()
            if (c != null && now - cachedAt < 150) return c
            val fresh = try {
                build()
            } catch (e: Throwable) {
                SimpleChatMod.LOGGER.warn("Invalid config, falling back to default: {}", e.message)
                c ?: DEFAULT
            }
            cached = fresh; cachedAt = now
            return fresh
        }

        /** Défaut « tout ON » utilisé en test et comme repli. */
        val DEFAULT = RuleConfig(
            masterEnabled = true,
            lobbyEnabled = true,
            skyblockEnabled = true,
            systemEnabled = true,
            dedupWindowMs = 3000,
            smartCollapse = true,
            actions = emptyMap(),
            guildStyle = ChannelStyle(enabled = true, compact = true, showRank = true, showGuildRank = true, recolorName = false, nameColor = 0xFFFFFF, prefixColor = 0x55FF55, messageColor = 0x55FF55, recolorMessage = false),
            partyStyle = ChannelStyle(enabled = true, compact = true, showRank = false, showGuildRank = false, recolorName = false, nameColor = 0xFFFFFF, prefixColor = 0x0000AA, messageColor = 0x55FFFF, recolorMessage = false),
            publicStyle = ChannelStyle(enabled = true, compact = true, showRank = false, showGuildRank = false, recolorName = false, nameColor = 0xFFFFFF, prefixColor = 0x555555, messageColor = 0xAAAAAA, recolorMessage = false),
            partyPrefix = "P",
            prefix = PublicPrefixToggles(hideLevel = false, hideEmblem = true),
            bridge = GuildBridgeConfig("", "Bridge", "G", "O", 0x55FF55, 0x55FFFF, 0x55FF55, 0x55FFFF, true, true, 0x55FF55, 0xFFFF55, 0xFF5555),
            hideGroupActions = HideGroup.entries.associateWith { HideAction.HIDE },
            customHidePatterns = emptyList(),
            showTimestamps = false,
            timestampColor = 0x555555,
            compactSoloClass = true,
            compactHoppity = true,
        )

        /** Projette la config Resourceful Config vers le snapshot pur. */
        private fun build(): RuleConfig = RuleConfig(
            masterEnabled = Settings.masterEnabled,
            lobbyEnabled = LobbyCleanup.enabled,
            skyblockEnabled = SkyBlockCleanup.enabled,
            systemEnabled = SystemCat.enabled,
            dedupWindowMs = Settings.groupingWindowSeconds.coerceAtLeast(0).toLong() * 1000,
            smartCollapse = Settings.smartCollapse,
            actions = mapOf(
                "lobby-join" to LobbyCleanup.lobbyJoin,
                "booster-activated" to LobbyCleanup.boosterActivated,
                "mystery-reward" to LobbyCleanup.mysteryReward,
                "claimed-currency" to LobbyCleanup.claimedCurrency,
                "pet-spawn" to LobbyCleanup.petSpawn,
                "radiating-generosity" to LobbyCleanup.radiatingGenerosity,
                "playtime-ticket" to LobbyCleanup.playtimeTicket,
                "reward-link" to LobbyCleanup.rewardLink,
                "morph-wardrobe" to LobbyCleanup.morphWardrobe,
                "raffle-win" to LobbyCleanup.raffleWin,
                "npc-dialog" to SkyBlockCleanup.npcDialog,
                "boss" to SkyBlockCleanup.boss,
                "damage-spam" to SkyBlockCleanup.damageSpam,
                "kill-combo" to SkyBlockCleanup.killCombo,
                "mob-ability" to SkyBlockCleanup.mobAbility,
                "loot-share" to SkyBlockCleanup.lootShare,
                "rare-reward" to SkyBlockCleanup.rareReward,
                "gexp" to SkyBlockCleanup.gexp,
                "sacks" to SkyBlockCleanup.sacks,
                "profile-id" to SystemCat.profileId,
                "server-routing" to SystemCat.serverRouting,
            ),
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
            hideGroupActions = mapOf(
                HideGroup.TRANSITIONS to SystemCat.transitions,
                HideGroup.NOTIFICATIONS to LobbyCleanup.notifications,
                HideGroup.DUNGEONS to SkyBlockCleanup.dungeons,
                HideGroup.ABILITIES to SkyBlockCleanup.abilities,
                HideGroup.COMBAT to SkyBlockCleanup.combat,
                HideGroup.REWARDS to SkyBlockCleanup.rewards,
                HideGroup.MISC to SkyBlockCleanup.misc,
                HideGroup.BAZAAR to SkyBlockCleanup.bazaar,
                HideGroup.SLAYER to SkyBlockCleanup.slayer,
                HideGroup.EVENTS to SkyBlockCleanup.events,
                HideGroup.WARNINGS to SkyBlockCleanup.warnings,
            ),
            customHidePatterns = SkyBlockCleanup.customPatterns.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            showTimestamps = Settings.showTimestamps,
            timestampColor = Settings.timestampColor,
            compactSoloClass = SkyBlockCleanup.soloClass,
            compactHoppity = SkyBlockCleanup.hoppity,
        )
    }
}
