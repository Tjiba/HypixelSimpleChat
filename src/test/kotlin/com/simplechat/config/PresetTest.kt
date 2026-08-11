package com.simplechat.config

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.simplechat.rules.Registry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Le preset bundlé doit parler des réglages qui existent vraiment : une clé orpheline
 * (groupe renommé) serait ignorée en silence et le joueur repartirait sur les défauts.
 */
class PresetTest {

    // Réglages des catégories Lobby/SkyBlock/System qui ne viennent pas d'une règle.
    private val extras = setOf("enabled", "customPatterns", "soloClass", "hoppity")

    @Test fun `every rule key of the bundled preset exists in the registry`() {
        val txt = javaClass.getResourceAsStream("/assets/simplechat/presets/recommended.json")!!
            .bufferedReader().use { it.readText() }
        val root = JsonParser.parseString(txt).asJsonObject
        val known = Registry.groups.map { it.id }.toSet() + extras

        val unknown = mutableListOf<String>()
        for (category in listOf("Lobby", "SkyBlock", "System")) {
            val obj = root.get(category) as? JsonObject
            assertTrue(obj != null, "catégorie absente du preset : $category")
            for (key in obj!!.keySet()) if (key !in known) unknown += "$category.$key"
        }
        assertTrue(unknown.isEmpty(), "clés orphelines dans recommended.json : $unknown")
    }

    @Test fun `every registry group is covered by the preset`() {
        val txt = javaClass.getResourceAsStream("/assets/simplechat/presets/recommended.json")!!
            .bufferedReader().use { it.readText() }
        val root = JsonParser.parseString(txt).asJsonObject
        val inPreset = listOf("Lobby", "SkyBlock", "System")
            .mapNotNull { root.get(it) as? JsonObject }
            .flatMap { it.keySet() }
            .toSet()

        val missing = Registry.groups.map { it.id }.filter { it !in inPreset }
        assertTrue(missing.isEmpty(), "groupes absents du preset recommandé : $missing")
    }
}
