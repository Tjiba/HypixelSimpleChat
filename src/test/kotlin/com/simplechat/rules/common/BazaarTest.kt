package com.simplechat.rules.common

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BazaarTest {

    private val compact = RuleConfig.DEFAULT.copy(groupActions = mapOf("bazaar" to RuleAction.COMPACT))

    @Test fun `hidden by default`() {
        assertEquals(Verdict.Hide,
            ChatRules.evaluate("§6[Bazaar] §7Submitting sell offer...", RuleConfig.DEFAULT))
        assertEquals(Verdict.Hide,
            ChatRules.evaluate("§7Putting item in escrow...", RuleConfig.DEFAULT))
    }

    @Test fun `compacted`() {
        assertEquals(Verdict.Replace("§6BZ §7· selling…"),
            ChatRules.evaluate("§6[Bazaar] §7Submitting sell offer...", compact))
        // Quantité et item ont chacun leur couleur chez Hypixel : le compact les garde telles quelles.
        assertEquals(Verdict.Replace("§6BZ §a✔ §a64x §fEnchanted Cobblestone"),
            ChatRules.evaluate("§6Buy Order Setup! §a64x §fEnchanted Cobblestone", compact))
        assertEquals(Verdict.Replace("§6AH §7· setup…"),
            ChatRules.evaluate("§7Setting up the auction...", compact))
    }

    // Lignes réelles d'achat/vente instantané : elles n'étaient couvertes par aucune règle.
    @Test fun `instant orders are compacted`() {
        assertEquals(Verdict.Replace("§6BZ §a+ §f64x Raw Cod §7· §c-20.4k"),
            ChatRules.evaluate("§6[Bazaar] §fBought §a64x §fRaw Cod §ffor §620,422 coins§f!", compact))
        assertEquals(Verdict.Replace("§6BZ §c- §f399x Ruby Veilshroom §7· §a+391.5k"),
            ChatRules.evaluate("§6[Bazaar] §fSold §a399x §fRuby Veilshroom §ffor §6391,539 coins§f!", compact))
        assertEquals(Verdict.Replace("§6BZ §a+1.4M §7· §f1x Fuming Potato Book"),
            ChatRules.evaluate(
                "§6[Bazaar] §fClaimed §61,387,133 coins §ffrom selling §a1x §fFuming Potato Book §fat §61,401,145 each§f!",
                compact))
    }

    @Test fun `an unknown Bazaar line still obeys the group`() {
        assertEquals(Verdict.Hide,
            ChatRules.evaluate("§6[Bazaar] §7Something new from Hypixel", RuleConfig.DEFAULT))
    }

    @Test fun `off leaves the message untouched`() {
        val off = RuleConfig.DEFAULT.copy(groupActions = mapOf("bazaar" to RuleAction.OFF))
        assertEquals(Verdict.Pass, ChatRules.evaluate("§6[Bazaar] §7Submitting sell offer...", off))
    }
}
