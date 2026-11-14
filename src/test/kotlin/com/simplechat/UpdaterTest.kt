package com.simplechat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdaterTest {

    @Test fun `stable release is newer than its prereleases`() {
        for (version in listOf("1.10-alpha.1", "1.10-beta.1", "1.10-rc.1")) {
            assertTrue((Updater.compareVersions(version, "1.10") ?: error(version)) < 0)
            assertTrue((Updater.compareVersions("1.10", version) ?: error(version)) > 0)
        }
    }

    @Test fun `prereleases sort identifiers and numeric counters correctly`() {
        val versions = listOf("1.10-alpha.1", "1.10-alpha.2", "1.10-beta.2", "1.10-beta.10", "1.10-rc.1", "1.10")
        for ((older, newer) in versions.zipWithNext()) {
            assertTrue((Updater.compareVersions(older, newer) ?: error(older)) < 0, "$older < $newer")
        }
    }

    @Test fun `release numbers and build metadata keep their existing meaning`() {
        assertEquals(0, Updater.compareVersions("1.10", "1.10.0"))
        assertEquals(0, Updater.compareVersions("1.10+mc26.2", "1.10+mc26.3"))
        assertTrue((Updater.compareVersions("1.9.9", "1.10") ?: error("version")) < 0)
        assertTrue((Updater.compareVersions("1.11-alpha.1", "1.10") ?: error("version")) > 0)
    }

    @Test fun `invalid versions cannot report up to date`() {
        assertNull(Updater.compareVersions("unknown", "1.10"))
        assertNull(Updater.compareVersions("1.10", "not-a-version"))
    }
}
