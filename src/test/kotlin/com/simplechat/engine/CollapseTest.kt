package com.simplechat.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Collapse est un singleton : chaque test prend ses propres clés. */
class CollapseTest {

    @Test fun `a counted line comes back with its count`() {
        Collapse.remember("a-line", "You found a gift", 1)
        assertEquals(Collapse.Seen(1, "You found a gift"), Collapse.seen("a-line"))
    }

    // Le cœur du repli : d'autres lignes passent, le compteur du message reste.
    @Test fun `another message in between does not lose the count`() {
        Collapse.remember("b-line", "You found a gift (x2)", 2)
        Collapse.remember("b-other", "Player: gg", 1)
        assertEquals(Collapse.Seen(2, "You found a gift (x2)"), Collapse.seen("b-line"))
    }

    @Test fun `recounting overwrites the line to look for`() {
        Collapse.remember("c-line", "You found a gift", 1)
        Collapse.remember("c-line", "You found a gift (x2)", 2)
        assertEquals(Collapse.Seen(2, "You found a gift (x2)"), Collapse.seen("c-line"))
    }

    @Test fun `unknown key has nothing to collapse`() {
        assertNull(Collapse.seen("d-never-seen"))
    }

    // Les séparateurs vides d'Hypixel : les compter afficherait un "(x13)" tout seul sur une ligne.
    @Test fun `blank lines are never counted`() {
        Collapse.remember("", "", 1)
        Collapse.remember("   ", "   ", 1)
        assertNull(Collapse.seen(""))
        assertNull(Collapse.seen("   "))
    }
}
