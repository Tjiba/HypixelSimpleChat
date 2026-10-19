package com.simplechat.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChatChannelTest {
    @Test fun `all local party and guild route only outside filter mode`() {
        for ((tab, prefix) in listOf(0 to "/ac ", 1 to "/ac ", 2 to "/pc ", 3 to "/gc ")) {
            assertEquals(prefix + "hello", ChatChannel.route("hello", true, false, tab))
            assertEquals("hello", ChatChannel.route("hello", true, true, tab))
            assertEquals("hello", ChatChannel.route("hello", false, false, tab))
        }
    }

    @Test fun `commands and empty input remain untouched`() {
        for (tab in 0..3) {
            for (message in listOf("", "/ac hello", "/pc hello", "/chat party", "/msg Timo hello")) {
                assertEquals(message, ChatChannel.route(message, true, false, tab))
            }
        }
    }
}
