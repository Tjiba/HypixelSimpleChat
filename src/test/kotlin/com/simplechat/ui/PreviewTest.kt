package com.simplechat.ui

import com.simplechat.config.RuleConfig
import com.simplechat.engine.SelfPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** L'aperçu des canaux joueur parle du joueur : il doit s'y reconnaître, rang compris. */
class PreviewTest {

    private fun textOf(cfg: RuleConfig, category: String?) =
        Preview.forSettings(cfg, category, emptyList())
            .joinToString("\n") { line -> line.joinToString("") { it.text } }

    private val self = RuleConfig.DEFAULT.copy(self = SelfPlayer("non00w", "§6[MVP§c++§6] non00w"))

    @Test fun `guild and public previews use the player's own name and rank`() {
        for (category in listOf("Guild Chat", "Public Chat", null)) {
            val text = textOf(self, category)
            assert(text.contains("non00w")) { "$category : pseudo absent — $text" }
            assert(!text.contains("Player")) { "$category : exemple resté — $text" }
            assert(!text.contains("MeteoFrance")) { "$category : exemple resté — $text" }
        }
        assert(textOf(self, "Guild Chat").contains("[MVP++]"))
    }

    // Le pseudo repris de la liste des joueurs reste reconnu comme le sien : sa couleur s'applique.
    @Test fun `own color shows in the preview, rank untouched`() {
        val cfg = self.copy(self = self.self!!.copy(highlightColor = 0xFF00FF))
        val segs = Preview.forSettings(cfg, "Guild Chat", emptyList()).flatten()
        assertEquals(0xFF00FF, segs.first { it.text == "non00w" }.color)
        assertEquals(0xFFAA00, segs.first { it.text.contains("MVP") }.color) // §6 du rang gardé
    }

    // Le rang n'est pas toujours donné par le serveur ; le pseudo, lui, l'est toujours.
    @Test fun `an unknown rank still shows the player's name`() {
        val noRank = RuleConfig.DEFAULT.copy(self = SelfPlayer("non00w"))
        for (category in listOf("Guild Chat", "Public Chat")) {
            val text = textOf(noRank, category)
            assert(text.contains("non00w")) { "$category : pseudo absent — $text" }
            assert(!text.contains("Player") && !text.contains("MeteoFrance")) { "$category : $text" }
        }
        // Le rang de l'exemple reste porté par la ligne (Public Chat le masque par défaut).
        assert(textOf(noRank, "Guild Chat").contains("MVP"))
    }

    // Hors partie : personne à nommer, les exemples restent.
    @Test fun `preview keeps its samples when there is no player`() {
        assert(textOf(RuleConfig.DEFAULT, "Guild Chat").contains("Player"))
        assert(textOf(RuleConfig.DEFAULT, "Public Chat").contains("MeteoFrance"))
    }
}
