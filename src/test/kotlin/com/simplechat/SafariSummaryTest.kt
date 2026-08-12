package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Le récap arrive en sept messages : seul un état côté client peut les fusionner. */
class SafariSummaryTest {

    private val cfg = RuleConfig.DEFAULT

    @Test fun `seven lines become one`() {
        assertEquals(Verdict.Hide, SafariSummary.process("SAFARI REWARD SUMMARY", cfg))
        assertEquals(Verdict.Hide, SafariSummary.process("", cfg))
        assertEquals(Verdict.Hide, SafariSummary.process("+14 Shards", cfg))
        assertEquals(Verdict.Hide, SafariSummary.process("+80 Safari Essence", cfg))
        assertEquals(Verdict.Hide, SafariSummary.process("+24,437 Hunting Exp", cfg))
        assertEquals(Verdict.Replace("§2§lSAFARI §r§9+14 Shards §8· §f+80 Essence §8· §b+24.4k Hunting Exp"),
            SafariSummary.process("▬▬▬▬▬▬▬▬▬▬▬▬", cfg))
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
