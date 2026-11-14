package com.simplechat.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChatChannelTest {
    @Test fun `every tab sends to its own channel`() {
        for ((tab, prefix) in listOf(0 to "/ac ", 1 to "/ac ", 2 to "/pc ", 3 to "/gc ")) {
            assertEquals(prefix + "hello", ChatChannel.route("hello", true, tab, true))
        }
    }

    @Test fun `all tab can keep the server channel when its option is disabled`() {
        assertEquals("hello", ChatChannel.route("hello", true, 0, false))
    }

    @Test fun `all tab option does not affect local party or guild`() {
        for ((tab, prefix) in listOf(1 to "/ac ", 2 to "/pc ", 3 to "/gc ")) {
            for (allTabSendsToAllChat in listOf(false, true)) {
                assertEquals(prefix + "hello", ChatChannel.route("hello", true, tab, allTabSendsToAllChat))
            }
        }
    }

    @Test fun `disabled tabs never route messages`() {
        for (tab in 0..3) {
            for (allTabSendsToAllChat in listOf(false, true)) {
                assertEquals("hello", ChatChannel.route("hello", false, tab, allTabSendsToAllChat))
            }
        }
    }

    @Test fun `commands and empty input remain untouched`() {
        for (tab in 0..3) {
            for (message in listOf("", "/ac hello", "/pc hello", "/chat party", "/msg Timo hello")) {
                assertEquals(message, ChatChannel.route(message, true, tab, false))
                assertEquals(message, ChatChannel.route(message, true, tab, true))
            }
        }
    }
}
