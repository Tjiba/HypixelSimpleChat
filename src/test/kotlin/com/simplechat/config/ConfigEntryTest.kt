package com.simplechat.config

import com.simplechat.engine.RuleAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Le compteur de révision est ce qui invalide l'instantané de RuleConfig : sans lui,
 * deux réglages changés coup sur coup laissent l'aperçu sur l'état précédent.
 */
class ConfigEntryTest {

    private fun entry() = ConfigEntry("test", EntryKind.ENUM, "Test", "", RuleAction.HIDE,
        RuleAction.entries.toList())

    @Test fun `writing a value bumps the revision`() {
        val e = entry()
        val before = ConfigEntry.revision
        e.setEnum(RuleAction.COMPACT)
        assertNotEquals(before, ConfigEntry.revision)
    }

    @Test fun `reading a value leaves the revision alone`() {
        val e = entry()
        e.setEnum(RuleAction.GREY)
        val after = ConfigEntry.revision
        repeat(3) { e.getEnum() }
        assertEquals(after, ConfigEntry.revision)
    }
}
