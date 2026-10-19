package com.simplechat.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UnreadChannelsTest {
    @BeforeEach fun reset() = UnreadChannels.reset()

    @Test fun `local keeps party and guild unread until their tabs are opened`() {
        UnreadChannels.select(true, 1)
        Channel.entries.forEach { UnreadChannels.received(it) }
        assertEquals(0, UnreadChannels.count(1))
        assertEquals(1, UnreadChannels.count(2))
        assertEquals(2, UnreadChannels.count(3))
        UnreadChannels.select(true, 1)
        assertEquals(1, UnreadChannels.count(2))
        assertEquals(2, UnreadChannels.count(3))
        UnreadChannels.select(true, 2)
        assertEquals(0, UnreadChannels.count(2))
        assertEquals(2, UnreadChannels.count(3))
        UnreadChannels.select(true, 3)
        assertEquals(0, UnreadChannels.count(3))
    }

    @Test fun `only off channel messages count and officer belongs to guild`() {
        UnreadChannels.select(true, 2)
        listOf(Channel.PARTY, Channel.PUBLIC, Channel.SYSTEM, Channel.GUILD, Channel.OFFICER).forEach { UnreadChannels.received(it) }
        assertEquals(0, UnreadChannels.count(2))
        assertEquals(2, UnreadChannels.count(3))
        UnreadChannels.select(true, 2)
        assertEquals(2, UnreadChannels.count(3))
        UnreadChannels.select(true, 3)
        assertEquals(0, UnreadChannels.count(3))
        UnreadChannels.received(Channel.PARTY)
        assertEquals(1, UnreadChannels.count(2))
    }

    @Test fun `all tab and disabled filtering clear counts and do not accumulate`() {
        UnreadChannels.select(true, 2)
        UnreadChannels.received(Channel.GUILD)
        UnreadChannels.select(true, 0)
        UnreadChannels.received(Channel.GUILD)
        assertEquals(0, UnreadChannels.count(3))
        UnreadChannels.select(true, 3)
        UnreadChannels.received(Channel.PARTY)
        UnreadChannels.select(false, 3)
        UnreadChannels.received(Channel.PARTY)
        assertEquals(0, UnreadChannels.count(2))
    }

    @Test fun `disconnect clears the session and counters are bounded`() {
        UnreadChannels.select(true, 2)
        repeat(2000) { UnreadChannels.received(Channel.GUILD) }
        assertEquals(999, UnreadChannels.count(3))
        UnreadChannels.reset()
        assertEquals(0, UnreadChannels.count(3))
    }
}
