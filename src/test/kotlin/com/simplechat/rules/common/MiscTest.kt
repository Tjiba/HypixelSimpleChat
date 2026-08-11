package com.simplechat.rules.common

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MiscTest {

    private val full = "§cInventory full? Don't forget to check out your Storage inside the SkyBlock Menu!"

    @Test fun `hidden by default`() {
        assertEquals(Verdict.Hide, ChatRules.evaluate(full, RuleConfig.DEFAULT))
        assertEquals(Verdict.Hide, ChatRules.evaluate("§eThe Frog is exhausted and needs to rest.", RuleConfig.DEFAULT))
    }

    // Aucun compact déclaré pour ce groupe : COMPACT garde le message d'origine.
    @Test fun `compact without a short version keeps the raw message`() {
        val compact = RuleConfig.DEFAULT.copy(groupActions = mapOf("misc" to RuleAction.COMPACT))
        assertEquals(Verdict.Replace(full), ChatRules.evaluate(full, compact))
    }
}
