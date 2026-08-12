package com.simplechat.ui

import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.rules.Registry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** L'aperçu doit rester aligné sur la liste de réglages : mêmes réglages, même ordre. */
class PreviewTest {

    private val ruleIds = Registry.groups.map { it.id }.toSet()

    /** Chaque exemple doit déclencher la règle de son propre réglage, réglages dans l'ordre. */
    private fun checkAligned(ids: List<String>) {
        val samples = Preview.samplesFor(ids)
        val resolved = samples.map { s -> Registry.find(ChatRules.clean(s), s)?.first?.group?.id }
        assertTrue(resolved.none { it == null }, "un exemple ne déclenche aucune règle : $samples")
        assertEquals(ids.filter { it in ruleIds }, resolved.distinct(),
            "l'aperçu ne suit pas l'ordre des réglages")
    }

    @Test fun `general tab is aligned`() {
        checkAligned(MenuLayout.views["SkyBlock"]!!["General"]!!.values.flatten())
    }

    @Test fun `a content tab is aligned`() {
        checkAligned(MenuLayout.views["SkyBlock"]!!["Dungeons"]!!.values.flatten())
        checkAligned(MenuLayout.views["SkyBlock"]!!["Foraging"]!!.values.flatten())
    }

    // Une phrase au compact vide disparaît toujours : la montrer barrée laisserait croire qu'elle
    // dépend du réglage, alors qu'elle ne s'affichera jamais.
    @Test fun `a phrase that always vanishes has no preview line`() {
        val hive = Preview.samplesFor(listOf("foraging-hive"))
        assertTrue(hive.none { it.contains("honeyhive") }, "la ligne toujours masquée s'affiche : $hive")
        assertEquals(2, hive.size)
    }

    // Un groupe non dépliable n'a qu'une ligne de réglage : l'aperçu doit quand même montrer
    // les phrases qu'elle englobe, sinon rien ne dit ce qu'on est en train de régler. Sauf
    // celles qu'on laisse passer intactes (OFF) : elles n'ont rien à montrer.
    @Test fun `a whole group previews the phrases it changes`() {
        val phrases = Registry.rules.filter { it.group.id == "foraging-torrhus" }
        assertTrue(phrases.size > 1, "Torrhus devrait couvrir plusieurs messages")
        val changed = phrases.filter { it.default != RuleAction.OFF }
        assertTrue(changed.size < phrases.size, "Torrhus devrait avoir au moins une phrase OFF")
        assertEquals(changed.map { it.sample }, Preview.samplesFor(listOf("foraging-torrhus")))
    }

    // Une ligne d'aperçu par ligne de réglage : replié = un exemple, déplié = tous.
    @Test fun `a collapsed group shows one example, an expanded one shows them all`() {
        val phrases = Registry.rules.filter { it.group.id == "dungeons" }.map { it.id }
        assertTrue(phrases.size > 1, "le réglage Dungeons devrait couvrir plusieurs messages")
        assertEquals(1, Preview.samplesFor(listOf("dungeons")).size)
        assertEquals(phrases.size, Preview.samplesFor(listOf("dungeons") + phrases).size)
    }

    // En recherche, les résultats traversent les catégories : l'aperçu doit les suivre eux,
    // pas les exemples de la page où on se trouvait.
    @Test fun `search results drive the preview`() {
        val lines = Preview.forSettings(
            com.simplechat.config.RuleConfig.DEFAULT, Preview.SEARCH, listOf("puzzle-solved", "bank-interest"))
        assertEquals(3, lines.size, "2 messages + la ligne vide qui les sépare")
    }

    @Test fun `settings without a rule are skipped, not shifted`() {
        val expected = Preview.samplesFor(listOf("bazaar")).size + Preview.samplesFor(listOf("slayer")).size
        assertEquals(expected, Preview.samplesFor(listOf("enabled", "bazaar", "customPatterns", "slayer")).size)
    }
}
