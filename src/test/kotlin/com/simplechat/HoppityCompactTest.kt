package com.simplechat

import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HoppityCompactTest {
    @BeforeEach fun reset() { HoppityCompact.reset() }

    @Test fun `player quotes never trigger Hoppity`() {
        for (prefix in listOf("Party > Timo: ", "Guild > Timo: ", "Officer > Timo: ", "From Timo: ", "Timo: ")) {
            for (text in listOf("NEW RABBIT!", "DUPLICATE RABBIT! +10 Chocolate", "You found a Rabbit (COMMON)",
                "HOPPITY'S HUNT You found Brutus (UNCOMMON)!")) {
                assertNull(HoppityCompact.process(prefix + text, true))
            }
        }
    }

    @Test fun `real notifications still merge across player messages`() {
        assertEquals(Verdict.Hide, HoppityCompact.process("HOPPITY'S HUNT You found Brutus (UNCOMMON)!", true))
        assertNull(HoppityCompact.process("Party > Timo: NEW RABBIT!", true))
        assertEquals(Verdict.Replace("§dDuplicate rabbit §7- §fBrutus §7(uncommon)"),
            HoppityCompact.process("DUPLICATE RABBIT! +10,612,335 Chocolate", true))
    }

    @Test fun `disabled processing and session reset discard pending rabbits`() {
        HoppityCompact.process("You found a Rabbit (COMMON)!", true)
        assertNull(HoppityCompact.process("NEW RABBIT!", false))
        assertEquals(Verdict.Replace("§dNew rabbit"), HoppityCompact.process("NEW RABBIT!", true))
        HoppityCompact.process("You found a Rabbit (COMMON)!", true)
        HoppityCompact.reset()
        assertEquals(Verdict.Replace("§dNew rabbit"), HoppityCompact.process("NEW RABBIT!", true))
    }
}
