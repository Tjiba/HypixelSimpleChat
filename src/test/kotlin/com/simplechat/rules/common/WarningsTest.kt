package com.simplechat.rules.common

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WarningsTest {

    @Test fun `hidden by default`() {
        assertEquals(Verdict.Hide, ChatRules.evaluate("§cWhoa! Slow down there!", RuleConfig.DEFAULT))
        assertEquals(Verdict.Hide, ChatRules.evaluate("§cYou can't use this while in combat!", RuleConfig.DEFAULT))
        assertEquals(Verdict.Hide,
            ChatRules.evaluate("§cMonsters around here can only take damage from Axes!", RuleConfig.DEFAULT))
    }

    @Test fun `greyed`() {
        val grey = RuleConfig.DEFAULT.copy(groupActions = mapOf("warnings" to RuleAction.GREY))
        assertEquals(Verdict.Replace("§8You can't use this while in combat!"),
            ChatRules.evaluate("§cYou can't use this while in combat!", grey))
    }
}
