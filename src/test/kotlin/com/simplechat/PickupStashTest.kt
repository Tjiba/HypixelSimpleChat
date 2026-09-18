package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import com.simplechat.rules.common.Economy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Les trois lignes du Pickup Stash deviennent une seule ligne au bouton cliquable. */
class PickupStashTest {

    private val cfg = RuleConfig.DEFAULT

    @BeforeEach fun clear() = PickupStash.reset()

    @Test fun `materials stash becomes one line`() {
        assertEquals(Verdict.Hide, PickupStash.process("You have 6,309 materials stashed away!", cfg))
        assertEquals(Verdict.Hide, PickupStash.process("(This totals 1 type of material stashed!)", cfg))
        assertEquals(
            Verdict.Replace("§bStash §7· §b6,309 §fmaterials §7· §b1 §ftype §6[PICK UP]"),
            PickupStash.process(">>> CLICK HERE to pick them up! <<<", cfg),
        )
    }

    @Test fun `item stash keeps plural types`() {
        assertEquals(Verdict.Hide, PickupStash.process("You have 18 items stashed away!", cfg))
        assertEquals(Verdict.Hide, PickupStash.process("(This totals 16 types of items stashed!)", cfg))
        assertEquals(
            Verdict.Replace("§bStash §7· §b18 §fitems §7· §b16 §ftypes §6[PICK UP]"),
            PickupStash.process(">>> CLICK HERE to pick them up! <<<", cfg),
        )
    }

    @Test fun `unexpected message stops the summary`() {
        assertEquals(Verdict.Hide, PickupStash.process("You have 18 items stashed away!", cfg))
        assertNull(PickupStash.process("Party > Timo: gg", cfg))
        assertNull(PickupStash.process("(This totals 16 types of items stashed!)", cfg))
    }

    @Test fun `HIDE drops every stash line`() {
        val hide = cfg.copy(groupActions = mapOf(Economy.PICKUP_STASH.id to RuleAction.HIDE))
        assertEquals(Verdict.Hide, PickupStash.process("You have 18 items stashed away!", hide))
        assertEquals(Verdict.Hide, PickupStash.process("(This totals 16 types of items stashed!)", hide))
        assertEquals(Verdict.Hide, PickupStash.process(">>> CLICK HERE to pick them up! <<<", hide))
    }

    @Test fun `OFF leaves every stash line untouched`() {
        val off = cfg.copy(groupActions = mapOf(Economy.PICKUP_STASH.id to RuleAction.OFF))
        assertEquals(Verdict.Pass, PickupStash.process("You have 18 items stashed away!", off))
        assertEquals(Verdict.Pass, PickupStash.process("(This totals 16 types of items stashed!)", off))
        assertEquals(Verdict.Pass, PickupStash.process(">>> CLICK HERE to pick them up! <<<", off))
    }

    @Test fun `GREY keeps all three original lines`() {
        val grey = cfg.copy(groupActions = mapOf(PickupStash.SETTING to RuleAction.GREY))
        for (text in listOf("You have 18 items stashed away!",
            "(This totals 16 types of items stashed!)", ">>> CLICK HERE to pick them up! <<<")) {
            assertEquals(Verdict.Replace("§8$text"), PickupStash.process(text, grey))
        }
        assertNull(PickupStash.process("Party > Timo: gg", grey))
    }

    @Test fun `COMPACT_GREY merges the block in grey even with a custom theme`() {
        val grey = cfg.copy(groupActions = mapOf(PickupStash.SETTING to RuleAction.COMPACT_GREY),
            compactTheme = true, compactThemeColor = 0xFF00FF)
        assertEquals(Verdict.Hide, PickupStash.process("You have 18 items stashed away!", grey))
        assertEquals(Verdict.Hide, PickupStash.process("(This totals 16 types of items stashed!)", grey))
        assertEquals(Verdict.Replace("§8Stash · 18 items · 16 types [PICK UP]"),
            PickupStash.process(">>> CLICK HERE to pick them up! <<<", grey))
    }

    @Test fun `COMPACT follows the custom theme`() {
        val themed = cfg.copy(compactTheme = true, compactThemeColor = 0xFF00FF)
        PickupStash.process("You have 18 items stashed away!", themed)
        PickupStash.process("(This totals 16 types of items stashed!)", themed)
        assertEquals(Verdict.Replace("§bStash §7· §b18 §#FF00FFitems §7· §b16 §#FF00FFtypes §6[PICK UP]"),
            PickupStash.process(">>> CLICK HERE to pick them up! <<<", themed))
    }

    @Test fun `disabled mod or SkyBlock leaves the block untouched for every action`() {
        for (action in RuleAction.entries) {
            val selected = cfg.copy(groupActions = mapOf(PickupStash.SETTING to action))
            for (disabled in listOf(selected.copy(masterEnabled = false), selected.copy(skyblockEnabled = false))) {
                for (text in listOf("You have 18 items stashed away!",
                    "(This totals 16 types of items stashed!)", ">>> CLICK HERE to pick them up! <<<")) {
                    assertEquals(Verdict.Pass, PickupStash.process(text, disabled))
                }
            }
        }
    }

    @Test fun `changing to a non compact mode clears the pending block`() {
        for (changed in listOf(cfg.copy(masterEnabled = false), cfg.copy(skyblockEnabled = false),
            cfg.copy(groupActions = mapOf(PickupStash.SETTING to RuleAction.OFF)),
            cfg.copy(groupActions = mapOf(PickupStash.SETTING to RuleAction.GREY)),
            cfg.copy(groupActions = mapOf(PickupStash.SETTING to RuleAction.HIDE)))) {
            PickupStash.process("You have 18 items stashed away!", cfg)
            PickupStash.process("(This totals 16 types of items stashed!)", changed)
            assertNull(PickupStash.process(">>> CLICK HERE to pick them up! <<<", cfg))
        }
    }

    @Test fun `incomplete compact block does not swallow the pickup button`() {
        PickupStash.process("You have 18 items stashed away!", cfg)
        assertEquals(Verdict.Pass, PickupStash.process(">>> CLICK HERE to pick them up! <<<", cfg))
    }

    @Test fun `HIDE also handles stash lines without a pending block`() {
        val hide = cfg.copy(groupActions = mapOf(PickupStash.SETTING to RuleAction.HIDE))
        assertEquals(Verdict.Hide, PickupStash.process(">>> CLICK HERE to pick them up! <<<", hide))
        assertEquals(Verdict.Hide, PickupStash.process("(This totals 16 types of items stashed!)", hide))
        assertNull(PickupStash.process("Party > Timo: gg", hide))
    }

    @Test fun `preview does not change the pending block`() {
        PickupStash.process("You have 18 items stashed away!", cfg)
        for (action in RuleAction.entries)
            PickupStash.preview(cfg.copy(groupActions = mapOf(PickupStash.SETTING to action)))
        PickupStash.process("(This totals 16 types of items stashed!)", cfg)
        assertEquals(Verdict.Replace("§bStash §7· §b18 §fitems §7· §b16 §ftypes §6[PICK UP]"),
            PickupStash.process(">>> CLICK HERE to pick them up! <<<", cfg))
    }

    @Test fun `pickup messages follow all five modes and preserve item colors`() {
        val samples = listOf(
            "§eFrom stash: §dWild Rose" to "§bStash §7· §dWild Rose",
            "§eYou picked up §a192 §eitems from your material stash!" to "§bStash §7· §a+192 §fitems",
            "§eYou still have §a6,117 materials §etotalling §b1 §etypes of materials in there!" to
                "§bStash §7· §a6,117 §fmaterials left §7· §b1 §ftype",
            "You picked up 1 item from your item stash!" to "§bStash §7· §a+1 §fitem",
            "You still have 2 items totalling 2 types of items in there!" to
                "§bStash §7· §a2 §fitems left §7· §b2 §ftypes",
        )
        for (action in RuleAction.entries) {
            val selected = cfg.copy(groupActions = mapOf(PickupStash.SETTING to action))
            for ((raw, compact) in samples) {
                val clean = ChatRules.clean(raw)
                val expected = when (action) {
                    RuleAction.OFF -> Verdict.Pass
                    RuleAction.GREY -> Verdict.Replace("§8$clean")
                    RuleAction.COMPACT -> Verdict.Replace(compact)
                    RuleAction.COMPACT_GREY -> Verdict.Replace("§8" + ChatRules.clean(compact))
                    RuleAction.HIDE -> Verdict.Hide
                }
                assertEquals(expected, PickupStash.process(clean, selected, raw))
                assertEquals(Verdict.Pass, PickupStash.process(clean, selected.copy(masterEnabled = false), raw))
                assertEquals(Verdict.Pass, PickupStash.process(clean, selected.copy(skyblockEnabled = false), raw))
            }
        }
        assertNull(PickupStash.process("From Player: hi", cfg))
    }

    @Test fun `stash padding is removed only for compact or hidden blocks`() {
        for (action in RuleAction.entries) {
            PickupStash.reset()
            val selected = cfg.copy(groupActions = mapOf(PickupStash.SETTING to action))
            val trim = action == RuleAction.COMPACT || action == RuleAction.COMPACT_GREY || action == RuleAction.HIDE
            val start = "You have 18 items stashed away!"
            assertEquals(trim, PickupStash.trimsPadding(start, selected))
            assertEquals(false, PickupStash.trimsPadding("Party > Timo: gg", selected))
            assertNull(PickupStash.process("", selected))
            PickupStash.process(start, selected)
            assertEquals(if (trim) Verdict.Hide else null, PickupStash.process("", selected))
            PickupStash.process("(This totals 16 types of items stashed!)", selected)
            PickupStash.process(">>> CLICK HERE to pick them up! <<<", selected)
            repeat(2) { assertEquals(if (trim) Verdict.Hide else null, PickupStash.process("", selected)) }
            assertNull(PickupStash.process("Party > Timo: gg", selected))
            assertNull(PickupStash.process("", selected))
        }
    }

    @Test fun `pickup padding and theme follow settings`() {
        val raw = "You picked up 192 items from your material stash!"
        val themed = cfg.copy(compactTheme = true, compactThemeColor = 0xFF00FF)
        assertEquals(Verdict.Replace("§bStash §7· §a+192 §#FF00FFitems"), PickupStash.process(raw, themed))
        assertEquals(Verdict.Hide, PickupStash.process("", themed))
        for (disabled in listOf(cfg.copy(masterEnabled = false), cfg.copy(skyblockEnabled = false))) {
            assertEquals(false, PickupStash.trimsPadding(raw, disabled))
            assertNull(PickupStash.process("", disabled))
        }
    }

    @Test fun `pickup replaces its displayed message with names and totals on separate lines`() {
        assertEquals(Verdict.Replace("§bStash §7· §dWild Rose"),
            PickupStash.process("From stash: Wild Rose", cfg, "§eFrom stash: §dWild Rose"))
        assertNull(PickupStash.stale())
        PickupStash.displayed("[14:14] Stash · Wild Rose")
        assertEquals(Verdict.Hide, PickupStash.process("", cfg))
        assertEquals(Verdict.Replace("§bStash §7· §dWild Rose\n§bStash §7· §a+192 §fitems"),
            PickupStash.process("You picked up 192 items from your material stash!", cfg))
        assertEquals("[14:14] Stash · Wild Rose", PickupStash.stale())
        assertNull(PickupStash.stale())
        PickupStash.displayed("[14:14] Stash · Wild Rose\nStash · +192 items")
        PickupStash.preview(cfg)
        assertEquals(Verdict.Replace("§bStash §7· §dWild Rose\n§bStash §7· §a+192 §fitems §7· §a6,117 §fmaterials left §7· §b1 §ftype"),
            PickupStash.process("You still have 6,117 materials totalling 1 types of materials in there!", cfg))
        assertEquals("[14:14] Stash · Wild Rose\nStash · +192 items", PickupStash.stale())
    }

    @Test fun `pickup groups repeated items and greys the whole merged message`() {
        val grey = cfg.copy(groupActions = mapOf(PickupStash.SETTING to RuleAction.COMPACT_GREY))
        repeat(22) { PickupStash.process("From stash: Wild Rose", grey) }
        PickupStash.process("From stash: Dandelion", grey)
        assertEquals(Verdict.Replace("§8Stash · Wild Rose · Dandelion\n§8Stash · +192 items"),
            PickupStash.process("You picked up 192 items from your material stash!", grey))
    }

    @Test fun `repeated item notices do not become an item quantity`() {
        repeat(22) {
            assertEquals(Verdict.Replace("§bStash §7· §fWild Rose"),
                PickupStash.process("From stash: Wild Rose", cfg))
        }
        PickupStash.process("You picked up 788 items from your material stash!", cfg)
        assertEquals(Verdict.Replace("§bStash §7· §fWild Rose\n§bStash §7· §a+788 §fitems §7· §a37,372 §fmaterials left §7· §b1 §ftype"),
            PickupStash.process("You still have 37,372 materials totalling 1 types of materials in there!", cfg))
    }

    @Test fun `completed and interrupted pickups cannot absorb the next retrieval`() {
        PickupStash.process("From stash: Wild Rose", cfg)
        PickupStash.process("You picked up 192 items from your material stash!", cfg)
        PickupStash.process("You still have 6,117 materials totalling 1 types of materials in there!", cfg)
        PickupStash.displayed("previous complete stash")
        assertEquals(Verdict.Replace("§bStash §7· §fDandelion"), PickupStash.process("From stash: Dandelion", cfg))
        assertNull(PickupStash.stale())
        PickupStash.displayed("incomplete pickup")
        assertNull(PickupStash.process("Party > Timo: gg", cfg))
        assertEquals(Verdict.Replace("§bStash §7· §a+192 §fitems"),
            PickupStash.process("You picked up 192 items from your material stash!", cfg))
        assertNull(PickupStash.stale())
    }
}
