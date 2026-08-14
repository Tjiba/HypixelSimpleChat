package com.simplechat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Les quantités d'un Tree Gift ne vivent que dans le survol : seul un état côté client les cumule. */
class TreeGiftTotalsTest {

    private val raw = "§dFig Tree Gift. §7You helped cut §a100.0% §7and gained §e5 rewards§a!"
    private val hover = "§aForest Essence §7x16\n§bForaging Experience §7x700\n§7Tender Wood §7x0-8"

    @BeforeEach fun clear() = TreeGiftTotals.reset()

    @Test fun `the tree names the total`() {
        assertEquals("Fig", TreeGiftTotals.tree("Fig Tree Gift. You helped cut 100.0% and gained 5 rewards!"))
        assertNull(TreeGiftTotals.tree("FLOOR DROP! You found Fig Log x512 on the ground!"))
    }

    // Couleurs d'Hypixel gardées : l'arbre prend celle du message, chaque gain celle du survol.
    @Test fun `two gifts of the same tree add up`() {
        TreeGiftTotals.record("Fig", raw, hover)
        assertEquals(
            "§dFig §8· §72 gifts this session" +
                "\n§aForest Essence §7x32\n§bForaging Experience §7x1,400\n§7Tender Wood §7x0-16",
            TreeGiftTotals.record("Fig", raw, hover))
    }

    // Un petit arbre ne donne pas les mêmes quantités qu'un grand : un total par essence.
    @Test fun `each tree keeps its own total`() {
        TreeGiftTotals.record("Fig", raw, hover)
        TreeGiftTotals.record("Mangrove", "§9Mangrove Tree Gift. §7You helped cut", "§aForest Essence §7x40")
        assertEquals(
            listOf(
                "§dFig §8· §71 gift this session",
                " §8· §aForest Essence §7x16",
                " §8· §bForaging Experience §7x700",
                " §8· §7Tender Wood §7x0-8",
                "§9Mangrove §8· §71 gift this session",
                " §8· §aForest Essence §7x40",
            ),
            TreeGiftTotals.report())
    }

    // Le survol d'un rang, d'un item : rien à compter, et surtout pas un gift de plus.
    @Test fun `a hover without gains counts nothing`() {
        assertNull(TreeGiftTotals.record("Fig", raw, "§7Hypixel Level §8· §b421"))
        assertEquals(listOf("§7No Tree Gift counted this session."), TreeGiftTotals.report())
    }
}
