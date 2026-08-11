package com.simplechat.rules.common

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SlayerTest {

    private val compact = RuleConfig.DEFAULT.copy(groupActions = mapOf("slayer" to RuleAction.COMPACT))

    @Test fun `hidden by default`() {
        assertEquals(Verdict.Hide, ChatRules.evaluate("§5§lSLAYER QUEST COMPLETE!", RuleConfig.DEFAULT))
    }

    @Test fun `compacted`() {
        assertEquals(Verdict.Replace("§5Slayer §7· started"),
            ChatRules.evaluate("  §5§lSLAYER QUEST STARTED!", compact))
        assertEquals(Verdict.Replace("§7Slay §f20,000 XP §7of Zombies"),
            ChatRules.evaluate("  §5§l» §7Slay §c20,000 Combat XP §7worth of Zombies.", compact))
    }
}
