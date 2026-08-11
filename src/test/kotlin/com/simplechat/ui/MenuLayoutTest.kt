package com.simplechat.ui

import com.simplechat.rules.Category
import com.simplechat.rules.Registry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Le menu est déduit du registre : tout réglage doit avoir une place, et une seule. */
class MenuLayoutTest {

    private val skyblock = MenuLayout.views["SkyBlock"]!!
    private val placed = skyblock.values.flatMap { it.values.flatten() }

    @Test fun `every skyblock group has exactly one slot`() {
        val ids = Registry.groups.filter { it.category == Category.SKYBLOCK }.map { it.id }
        val missing = ids.filter { it !in placed }
        assertTrue(missing.isEmpty(), "réglages sans place dans le menu : $missing")
        val duplicated = placed.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(duplicated.isEmpty(), "réglages placés deux fois : $duplicated")
    }

    @Test fun `general tab comes first and islands get their own tab`() {
        assertEquals("General", skyblock.keys.first())
        assertTrue("Dungeons" in skyblock.keys, "onglet d'île manquant : ${skyblock.keys}")
    }

    @Test fun `an island tab holds only its own settings, without a section header`() {
        val dungeons = skyblock["Dungeons"]!!
        assertEquals(listOf(""), dungeons.keys.toList())
        assertTrue("soloClass" in dungeons.values.first(), "soloClass devrait vivre avec Dungeons")
        assertTrue("dungeons" in dungeons.values.first())
    }

    @Test fun `non-rule settings keep their spot`() {
        val general = skyblock["General"]!!
        assertEquals(listOf("enabled", "customPatterns"), general["GENERAL"]!!.take(2))
        assertTrue("hoppity" in general["WORLD & EVENTS"]!!)
    }

    @Test fun `each phrase sits right under its group`() {
        val dungeons = MenuLayout.views["SkyBlock"]!!["Dungeons"]!!.values.first()
        val phrases = Registry.byGroup.entries.first { it.key.id == "dungeons" }.value.map { it.id }
        assertEquals(listOf("soloClass", "dungeons") + phrases, dungeons)
    }

    // Un groupe d'une seule règle EST cette règle : une ligne, pas deux.
    @Test fun `a single-rule group gets a single row`() {
        val boss = Registry.byGroup.entries.first { it.key.id == "boss" }.value
        assertEquals(1, boss.size)
        assertEquals(1, placed.count { it == "boss" })
    }

    // La barre du haut de page écrit dans tous ces réglages : un id en trop ou en double serait
    // une écriture parasite.
    @Test fun `the category bar covers every settable rule, once`() {
        val ids = MenuLayout.categoryBulk["SkyBlock"]!!
        assertEquals(ids.size, ids.distinct().size, "id en double dans la barre de catégorie")
        val expected = Registry.groups.filter { it.category == Category.SKYBLOCK }
            .sumOf { Registry.byGroup[it]!!.size.takeIf { n -> n > 1 } ?: 1 }
        assertEquals(expected, ids.size)
    }

    // Lobby et System passaient en liste plate : leurs phrases s'affichaient sans barre de
    // groupe ni repliage, exactement le mur de réglages que le repliage doit éviter.
    @Test fun `every rule category has a view`() {
        for (category in listOf("Lobby", "System")) {
            val view = MenuLayout.views[category]
            assertTrue(view != null, "catégorie sans vue : $category")
            assertEquals(1, view!!.size, "$category ne doit avoir qu'un onglet")
            val ids = view.values.first().values.flatten()
            for (groupId in MenuLayout.bulk.keys) {
                val phrases = MenuLayout.bulk.getValue(groupId)
                if (phrases.none { it in ids }) continue
                assertTrue(groupId in ids, "$groupId affiche ses phrases sans sa barre de groupe")
            }
        }
    }

    @Test fun `no empty section`() {
        for ((tab, sections) in skyblock) {
            for ((title, ids) in sections) {
                assertTrue(ids.isNotEmpty(), "section vide : $tab / $title")
            }
        }
    }
}
