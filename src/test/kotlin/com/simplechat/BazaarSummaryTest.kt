package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Les ordres d'un lot arrivent en autant de messages : seul un état côté client peut les additionner. */
class BazaarSummaryTest {

    private val whaleBait = "§6[Bazaar] §fSold §a92x §aWhale Bait §ffor §61,500,000 coins§f!"
    private val lilyPad = "§6[Bazaar] §fSold §a5x §aEnchanted Lily Pad §ffor §6500,000 coins§f!"
    private val rawCod = "§6[Bazaar] §fBought §a64x §fRaw Cod §ffor §620,422 coins§f!"

    private val shown = Verdict.Replace("§6BZ §c- §f92x §aWhale Bait §7· §a+1.5M")

    @BeforeEach fun clear() = BazaarSummary.reset()

    private fun order(raw: String, verdict: Verdict = shown) =
        BazaarSummary.process(ChatRules.clean(raw), raw, verdict, RuleConfig.DEFAULT)

    @Test fun `the first order keeps its own line`() {
        assertNull(order(whaleBait))
    }

    @Test fun `the second order takes the place of the first`() {
        assertNull(order(whaleBait))
        BazaarSummary.displayed("BZ - 92x Whale Bait · +1.5M")
        assertEquals(
            Verdict.Compact(
                "§6BZ §c- §#FFAA0097 items §8· §#00AAAA2 sales §7· §a+2.0M",
                "§f92x §aWhale Bait §8· §a+1.5M\n§f5x §aEnchanted Lily Pad §8· §a+500.0k"),
            order(lilyPad))
        assertEquals("BZ - 92x Whale Bait · +1.5M", BazaarSummary.stale())
        assertNull(BazaarSummary.stale())
    }

    // Deux fois le même item : une seule ligne de survol, quantité et gain additionnés.
    @Test fun `the same item is added up`() {
        assertNull(order(whaleBait))
        BazaarSummary.displayed("BZ - 92x Whale Bait · +1.5M")
        assertEquals(
            Verdict.Compact("§6BZ §c- §#FFAA00184 items §8· §#00AAAA2 sales §7· §a+3.0M",
                "§f184x §aWhale Bait §8· §a+3.0M"),
            order(whaleBait))
    }

    // Un achat au milieu des ventes ouvre son propre lot : les coins ne vont pas dans le même sens.
    @Test fun `a buy opens its own batch`() {
        assertNull(order(whaleBait))
        BazaarSummary.displayed("BZ - 92x Whale Bait · +1.5M")
        assertNull(order(rawCod))
        BazaarSummary.displayed("BZ + 64x Raw Cod · -20.4k")
        assertEquals(
            Verdict.Compact("§6BZ §a+ §#FFAA00128 items §8· §#00AAAA2 buys §7· §c-40.8k",
                "§f128x §fRaw Cod §8· §c-40.8k"),
            order(rawCod))
    }

    // Ordres masqués ou laissés tels quels : le joueur n'a pas demandé nos totaux.
    @Test fun `hidden orders are never merged`() {
        assertNull(order(whaleBait, Verdict.Hide))
        assertNull(order(lilyPad, Verdict.Hide))
        assertNull(order(whaleBait, Verdict.Pass))
    }

    // Réglage GREY : la ligne du lot part au gris comme celles qu'elle remplace.
    @Test fun `a greyed order gives a greyed batch`() {
        val grey = Verdict.Replace("§8BZ - 92x Whale Bait · +1.5M")
        assertNull(order(whaleBait, grey))
        BazaarSummary.displayed("BZ - 92x Whale Bait · +1.5M")
        assertEquals(
            Verdict.Compact(
                "§8BZ - 97 items · 2 sales · +2.0M",
                "§f92x §aWhale Bait §8· §a+1.5M\n§f5x §aEnchanted Lily Pad §8· §a+500.0k"),
            order(lilyPad, grey))
    }

    // Le survol se lit par le gain : le plus gros ordre en tête, pas l'ordre d'arrivée.
    @Test fun `the tooltip is sorted by price`() {
        assertNull(order(lilyPad))
        BazaarSummary.displayed("BZ - 5x Enchanted Lily Pad · +500.0k")
        val v = order(whaleBait) as Verdict.Compact
        assertEquals("§f92x §aWhale Bait §8· §a+1.5M\n§f5x §aEnchanted Lily Pad §8· §a+500.0k", v.hoverLegacy)
    }

    // Chaque compteur suit son réglage : ni l'item ni le total n'y touchent.
    @Test fun `each count follows its configured color`() {
        val cfg = RuleConfig.DEFAULT.copy(bazaarItemsColor = 0xFF5555, bazaarSalesColor = 0x55FF55)
        assertNull(BazaarSummary.process(ChatRules.clean(whaleBait), whaleBait, shown, cfg))
        BazaarSummary.displayed("BZ - 92x Whale Bait · +1.5M")
        val v = BazaarSummary.process(ChatRules.clean(lilyPad), lilyPad, shown, cfg) as Verdict.Compact
        assertEquals("§6BZ §c- §#FF555597 items §8· §#55FF552 sales §7· §a+2.0M", v.shortLegacy)
    }

    @Test fun `other messages are left alone`() {
        assertNull(order("§6[Bazaar] §7Claiming order...", Verdict.Hide))
        assertNull(order("§bParty §8> §7Timo§f: gg"))
    }
}
