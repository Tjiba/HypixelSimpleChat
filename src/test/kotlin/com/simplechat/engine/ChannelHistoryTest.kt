package com.simplechat.engine

import com.simplechat.config.RuleConfig
import com.simplechat.mixin.GuiMessageMixin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChannelHistoryTest {
    @Test fun `channel tabs are independent of renamed empty or conflicting prefixes`() {
        val raw = "Party > Timo: gg"
        val original = ChatRules.classify(raw)
        val message = GuiMessageMixin().apply { `hsc$setChannel`(original) }
        for (prefix in listOf("Team", "Party", "", "G")) {
            val cfg = RuleConfig.DEFAULT.copy(partyPrefix = prefix)
            val formatted = ChannelFormat.format(raw, original, cfg).orEmpty().joinToString("") { it.text }
            assertTrue(formatted.startsWith("$prefix > "))
            val channel = message.`hsc$getChannel`()
            assertTrue(ChatChannel.visible(channel, 2), formatted)
            assertFalse(ChatChannel.visible(channel, 1), formatted)
            assertFalse(ChatChannel.visible(channel, 3), formatted)
        }
    }

    @Test fun `equal rendered messages can retain different source channels`() {
        val cfg = RuleConfig.DEFAULT.copy(partyPrefix = "G", partyStyle = RuleConfig.DEFAULT.partyStyle.copy(showRank = false))
        val partyText = ChannelFormat.format("Party > Timo: gg", Channel.PARTY, cfg).orEmpty().joinToString("") { it.text }
        val guildText = ChannelFormat.format("Guild > Timo: gg", Channel.GUILD, cfg).orEmpty().joinToString("") { it.text }
        assertEquals(partyText, guildText)
        val party = GuiMessageMixin().apply { `hsc$setChannel`(Channel.PARTY) }
        val guild = GuiMessageMixin().apply { `hsc$setChannel`(Channel.GUILD) }
        assertTrue(ChatChannel.visible(party.`hsc$getChannel`(), 2))
        assertFalse(ChatChannel.visible(guild.`hsc$getChannel`(), 2))
        assertTrue(ChatChannel.visible(guild.`hsc$getChannel`(), 3))
        assertFalse(ChatChannel.visible(party.`hsc$getChannel`(), 3))
    }

    @Test fun `local excludes private group channels but keeps public system and whispers`() {
        for (channel in Channel.entries) {
            assertTrue(ChatChannel.visible(channel, 0))
            assertEquals(channel in listOf(Channel.PUBLIC, Channel.SYSTEM, Channel.WHISPER), ChatChannel.visible(channel, 1))
            assertEquals(channel == Channel.PARTY, ChatChannel.visible(channel, 2))
            assertEquals(channel in listOf(Channel.GUILD, Channel.OFFICER), ChatChannel.visible(channel, 3))
        }
    }
}
