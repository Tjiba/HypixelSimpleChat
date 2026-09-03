package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Le pavé d'un coffre arrive en une dizaine de messages : seul un état côté client les fusionne. */
class MiningSummaryTest {

    private val cfg = RuleConfig.DEFAULT
    private val bar = "§e§l" + "▬".repeat(64)
    private val otherBar = "§3§l" + "▬".repeat(64)

    private fun feed(raw: String, config: RuleConfig = cfg) =
        MiningSummary.process(ChatRules.clean(raw), raw, config)

    @Test fun `a whole chest becomes one line`() {
        assertEquals(Verdict.Hide, feed(bar))
        assertEquals(Verdict.Hide, feed("  §r§6§lCHEST LOCKPICKED"))
        assertEquals(Verdict.Hide, feed("§r"))
        assertEquals(Verdict.Hide, feed("  §r§a§lREWARDS"))
        assertEquals(Verdict.Hide, feed("    §r§dGemstone Powder §r§8x537"))
        assertEquals(Verdict.Hide, feed("    §r§a⸕ Flawed Amber Gemstone §r§8x2"))
        assertEquals(Verdict.Hide, feed("    §r§9Goblin Egg"))
        assertEquals(
            Verdict.Replace(
                "§6§lCHEST §r§dGemstone Powder §7x537 §8· §a⸕ Flawed Amber Gemstone §7x2 §8· §9Goblin Egg"
            ),
            feed(bar),
        )
    }

    // Le titre du coffre à butin est écrit autrement, et dans sa propre couleur.
    @Test fun `a loot chest keeps its own name and color`() {
        feed(bar)
        assertEquals(Verdict.Hide, feed("  §r§5§lLOOT CHEST COLLECTED"))
        assertEquals(Verdict.Hide, feed("    §r§5Treasurite"))
        assertEquals(Verdict.Replace("§5§lLOOT CHEST §r§5Treasurite"), feed(bar))
    }

    // Les autres pavés à ▬ d'Hypixel ont d'autres couleurs : ils ne sont pas à nous.
    @Test fun `bars of another color are left alone`() {
        assertNull(feed(otherBar))
        assertNull(feed("  §r§6§lCHEST LOCKPICKED"))
    }

    // Une ligne inattendue rend la main : sans ça le pavé avalerait la suite du chat.
    @Test fun `an unexpected line ends the block`() {
        assertEquals(Verdict.Hide, feed(bar))
        assertNull(feed("§7Party > Timo: gg"))
        assertNull(feed("    §r§dGemstone Powder §r§8x537"))
    }

    @Test fun `HIDE drops the whole block`() {
        val hide = cfg.copy(groupActions = mapOf(MiningSummary.SETTING to RuleAction.HIDE))
        assertEquals(Verdict.Hide, feed(bar, hide))
        assertEquals(Verdict.Hide, feed("  §r§6§lCHEST LOCKPICKED", hide))
        assertEquals(Verdict.Hide, feed("    §r§dGemstone Powder §r§8x537", hide))
        assertEquals(Verdict.Hide, feed(bar, hide))
    }

    @Test fun `OFF leaves every line untouched`() {
        val off = cfg.copy(groupActions = mapOf(MiningSummary.SETTING to RuleAction.OFF))
        assertNull(feed(bar, off))
        assertNull(feed("    §r§dGemstone Powder §r§8x537", off))
    }
}
