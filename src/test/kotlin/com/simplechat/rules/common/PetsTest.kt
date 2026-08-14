package com.simplechat.rules.common

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PetsTest {

    private val autopet = "§cAutopet §eequipped your §7[Lvl 100] §dFlying Fish§e! §a§lVIEW RULE"

    // Le familier garde sa couleur de rareté. La règle d'autopet, elle, n'est écrite que dans le
    // survol d'Hypixel : le mixin le reporte sur la ligne courte, hors de portée d'un test moteur.
    @Test fun `autopet keeps the level and the pet's color`() {
        assertEquals(Verdict.Replace("§cAutopet §7[100] §dFlying Fish"),
            ChatRules.evaluate(autopet, RuleConfig.DEFAULT))
    }

    @Test fun `autopet can be hidden`() {
        val cfg = RuleConfig.DEFAULT.copy(groupActions = mapOf("autopet" to RuleAction.HIDE))
        assertEquals(Verdict.Hide, ChatRules.evaluate(autopet, cfg))
    }

    // Sans le lien cliquable, la ligne se compacte pareil.
    @Test fun `autopet without the VIEW RULE tail`() {
        assertEquals(Verdict.Replace("§cAutopet §7[1] §7Rock"),
            ChatRules.evaluate("§cAutopet §eequipped your §7[Lvl 1] §7Rock§e!", RuleConfig.DEFAULT))
    }
}
