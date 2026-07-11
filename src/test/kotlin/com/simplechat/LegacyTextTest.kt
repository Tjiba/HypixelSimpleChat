package com.simplechat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LegacyTextTest {
    @Test fun `plain text single segment no color`() {
        assertEquals(listOf(Seg("hello", null)), LegacyText.parse("hello"))
    }

    @Test fun `color codes split into segments with rgb`() {
        assertEquals(
            listOf(Seg("[MVP", 0x5555FF), Seg("+", 0xFFAA00), Seg("]", 0x5555FF)),
            LegacyText.parse("§9[MVP§6+§9]"))
    }

    @Test fun `dark grey code maps to rgb`() {
        assertEquals(listOf(Seg("dim", 0x555555)), LegacyText.parse("§8dim"))
    }

    @Test fun `reset clears color and formatting`() {
        assertEquals(listOf(Seg("a", 0xFF5555), Seg("b", null)), LegacyText.parse("§ca§rb"))
    }

    @Test fun `bold flag carried`() {
        assertEquals(listOf(Seg("x", 0xFFFF55, bold = true)), LegacyText.parse("§e§lx"))
    }
}
