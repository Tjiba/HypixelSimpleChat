package com.simplechat.ui

import com.simplechat.BazaarSummary
import com.simplechat.config.RuleConfig
import com.simplechat.engine.SelfPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** L'aperçu des canaux joueur parle du joueur : il doit s'y reconnaître, rang compris. */
class PreviewTest {

    private fun textOf(cfg: RuleConfig, category: String?) =
        Preview.forSettings(cfg, category, emptyList())
            .joinToString("\n") { line -> line.segs.joinToString("") { it.text } }

    private val self = RuleConfig.DEFAULT.copy(self = SelfPlayer("non00w", "§6[MVP§c++§6] non00w"))

    @Test fun `guild and public previews use the player's own name and rank`() {
        for (category in listOf("Guild Chat", "Public Chat", null)) {
            val text = textOf(self, category)
            assert(text.contains("non00w")) { "$category : pseudo absent — $text" }
            assert(!text.contains("MeteoFrance")) { "$category : exemple resté — $text" }
        }
        assert(textOf(self, "Guild Chat").contains("[MVP++]"))
    }

    // Sa couleur ne se lit que comparée à celle des autres : une ligne à son nom, une au nom de
    // quelqu'un d'autre.
    @Test fun `player previews name the player next to someone else`() {
        for (category in listOf("Guild Chat", "Public Chat")) {
            val lines = textOf(self, category).lines()
            assert(lines.any { it.contains("non00w") && !it.contains("Player") }) { "$category : sa ligne manque — $lines" }
            assert(lines.any { it.contains("Player") && !it.contains("non00w") }) { "$category : ligne d'un autre manque — $lines" }
        }
    }

    // Le pseudo repris de la liste des joueurs reste reconnu comme le sien : sa couleur s'applique.
    @Test fun `own color shows in the preview, rank untouched`() {
        val cfg = self.copy(self = self.self!!.copy(highlightColor = 0xFF00FF))
        val line = Preview.forSettings(cfg, "Guild Chat", emptyList())
            .first { l -> l.segs.any { it.text == "non00w" } }.segs
        assertEquals(0xFF00FF, line.first { it.text == "non00w" }.color)
        assertEquals(0xFFAA00, line.first { it.text.contains("MVP") }.color) // §6 du rang gardé
    }

    // Le rang n'est pas toujours donné par le serveur ; le pseudo, lui, l'est toujours.
    @Test fun `an unknown rank still shows the player's name`() {
        val noRank = RuleConfig.DEFAULT.copy(self = SelfPlayer("non00w"))
        for (category in listOf("Guild Chat", "Public Chat")) {
            val text = textOf(noRank, category)
            assert(text.contains("non00w")) { "$category : pseudo absent — $text" }
            assert(!text.contains("MeteoFrance")) { "$category : $text" }
        }
        // Le rang de l'exemple reste porté par la ligne (Public Chat le masque par défaut).
        assert(textOf(noRank, "Guild Chat").contains("MVP"))
    }

    // Sur SkyBlock la liste des joueurs sert de tableau de bord : son entrée porte son niveau et son
    // emblème, pas son rang. Recopiée telle quelle, elle donnerait une ligne à deux niveaux.
    private val onSkyblock = RuleConfig.DEFAULT.copy(self = SelfPlayer("non00w", "§7[§b372§7] §bnon00w §d✳"))

    @Test fun `a skyblock tab entry is not taken for a rank`() {
        for (category in listOf("Guild Chat", "Public Chat")) {
            val text = textOf(onSkyblock, category)
            assert(text.contains("non00w")) { "$category : pseudo absent — $text" }
            assert(!text.contains("✳")) { "$category : entrée de tab recopiée — $text" }
        }
        // Un message de guilde ne porte pas de niveau : celui du tab n'a rien à y faire.
        assert(!textOf(onSkyblock, "Guild Chat").contains("372"))
    }

    // Son niveau vient de son entrée de tab ; la ligne de l'autre joueur garde celui de l'exemple,
    // sinon les deux lignes se ressemblent trop pour qu'on voie laquelle parle de lui.
    @Test fun `the public preview carries the player's own level`() {
        val lines = textOf(onSkyblock, "Public Chat").lines()
        assert(lines.any { it.contains("[372]") && it.contains("non00w") }) { "son niveau manque — $lines" }
        assert(lines.any { it.contains("[330]") && it.contains("Player") }) { "exemple perdu — $lines" }
    }

    // Le lot Bazaar n'a pas de règle : son aperçu est accroché au réglage de couleur, survol compris.
    @Test fun `the bazaar batch previews its line and its tooltip`() {
        val line = Preview.forSettings(RuleConfig.DEFAULT, "SkyBlock", listOf(BazaarSummary.SETTING)).first()
        val text = line.segs.joinToString("") { it.text }
        assert(text.contains("3,100 items") && text.contains("3 sales")) { "ligne du lot absente — $text" }
        assert(line.hover?.contains("Whale Bait") == true) { "survol du lot absent — ${line.hover}" }
    }

    // Hors partie : personne à nommer, les exemples restent.
    @Test fun `preview keeps its samples when there is no player`() {
        assert(textOf(RuleConfig.DEFAULT, "Guild Chat").contains("Player"))
        assert(textOf(RuleConfig.DEFAULT, "Public Chat").contains("MeteoFrance"))
    }
}
