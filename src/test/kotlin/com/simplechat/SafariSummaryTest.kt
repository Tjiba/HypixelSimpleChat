package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Le récap arrive en sept messages : seul un état côté client peut les fusionner. */
class SafariSummaryTest {

    private val cfg = RuleConfig.DEFAULT

    @BeforeEach fun reset() { SafariSummary.reset() }

    @Test fun `HIDE stops at an unrelated message`() {
        val hide = cfg.copy(groupActions = mapOf(SafariSummary.SETTING to RuleAction.HIDE))
        assertEquals(Verdict.Hide, SafariSummary.process("SAFARI REWARD SUMMARY", hide))
        assertEquals(Verdict.Hide, SafariSummary.process("+14 Shards", hide))
        repeat(100) { assertNull(SafariSummary.process("Party > Timo: gg", hide)) }
        assertNull(SafariSummary.process("+80 Safari Essence", hide))
    }

    @Test fun `disabled settings discard an incomplete summary`() {
        for (disabled in listOf(cfg.copy(masterEnabled = false), cfg.copy(skyblockEnabled = false),
            cfg.copy(groupActions = mapOf(SafariSummary.SETTING to RuleAction.OFF)))) {
            SafariSummary.process("SAFARI REWARD SUMMARY", cfg)
            assertNull(SafariSummary.process("+14 Shards", disabled))
            assertNull(SafariSummary.process("SAFARI REWARD SUMMARY", disabled))
            assertNull(SafariSummary.process("+80 Safari Essence", cfg))
        }
    }

    @Test fun `an unfinished summary has a bounded number of lines`() {
        SafariSummary.process("SAFARI REWARD SUMMARY", cfg)
        repeat(8) { assertEquals(Verdict.Hide, SafariSummary.process("", cfg)) }
        assertNull(SafariSummary.process("", cfg))
        assertNull(SafariSummary.process("+14 Shards", cfg))
    }

    @Test fun `session reset discards an incomplete summary`() {
        SafariSummary.process("SAFARI REWARD SUMMARY", cfg)
        SafariSummary.reset()
        assertNull(SafariSummary.process("+14 Shards", cfg))
    }

    @Test fun `HIDE still removes a complete summary`() {
        val hide = cfg.copy(groupActions = mapOf(SafariSummary.SETTING to RuleAction.HIDE))
        for (line in listOf("SAFARI REWARD SUMMARY", "", "+14 Shards", "+80 Safari Essence", "------------")) {
            assertEquals(Verdict.Hide, SafariSummary.process(line, hide))
        }
        assertNull(SafariSummary.process("Party > Timo: gg", hide))
    }

    @Test fun `seven lines become one`() {
        assertEquals(Verdict.Hide, SafariSummary.process("SAFARI REWARD SUMMARY", cfg))
        assertEquals(Verdict.Hide, SafariSummary.process("", cfg))
        assertEquals(Verdict.Replace("§2§lSAFARI §r§9+14 Shards"), SafariSummary.process("+14 Shards", cfg))
        assertEquals(Verdict.Replace("§2§lSAFARI §r§9+14 Shards §8· §f+80 Essence"),
            SafariSummary.process("+80 Safari Essence", cfg))
        assertEquals(Verdict.Replace("§2§lSAFARI §r§9+14 Shards §8· §f+80 Essence §8· §b+24.4k Hunting Exp"),
            SafariSummary.process("+24,437 Hunting Exp", cfg))
        assertEquals(Verdict.Hide, SafariSummary.process("▬▬▬▬▬▬▬▬▬▬▬▬", cfg))
    }

    @Test fun `grey modes remove all summary colors`() {
        for (action in listOf(RuleAction.GREY, RuleAction.COMPACT_GREY)) {
            SafariSummary.reset()
            val grey = cfg.copy(groupActions = mapOf(SafariSummary.SETTING to action))
            SafariSummary.process("SAFARI REWARD SUMMARY", grey)
            assertEquals(Verdict.Replace("§8SAFARI · +14 Shards"), SafariSummary.process("+14 Shards", grey))
            assertEquals(Verdict.Replace("§8SAFARI · +14 Shards · +80 Essence"), SafariSummary.process("+80 Safari Essence", grey))
        }
    }

    // Le séparateur d'ouverture, et tous ceux des autres pavés d'Hypixel, ne sont pas à nous.
    @Test fun `separators outside a summary are left alone`() {
        assertNull(SafariSummary.process("▬▬▬▬▬▬▬▬▬▬▬▬", cfg))
        assertNull(SafariSummary.process("BESTIARY MILESTONE", cfg))
    }

    // Une ligne inattendue rend la main : sans ça le bloc avalerait la suite du chat.
    @Test fun `an unexpected line ends the block`() {
        assertEquals(Verdict.Hide, SafariSummary.process("SAFARI REWARD SUMMARY", cfg))
        assertNull(SafariSummary.process("Party > Timo: gg", cfg))
        assertNull(SafariSummary.process("+14 Shards", cfg))
    }

    @Test fun `OFF leaves every line untouched`() {
        val off = cfg.copy(groupActions = mapOf(SafariSummary.SETTING to RuleAction.OFF))
        assertNull(SafariSummary.process("SAFARI REWARD SUMMARY", off))
        assertNull(SafariSummary.process("+14 Shards", off))
    }
}
