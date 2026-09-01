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
        assertEquals(Verdict.Replace("§7Slay §f20,000 XP §7of Zombies"),
            ChatRules.evaluate("  §5§l» §7Slay §c20,000 Combat XP §7worth of Zombies.", compact))
        // L'objectif ci-dessus a nommé le mob : le "complete", qui vient bien après, le reprend.
        assertEquals(Verdict.Replace("§5Slayer §2Revenant §a✔ §fcomplete"),
            ChatRules.evaluate("  §5§lSLAYER QUEST COMPLETE!", compact))
    }

    @Test fun `start waits for its objective and takes its brand`() {
        assertEquals(Verdict.Hide, Slayer.process("SLAYER QUEST STARTED!", compact))
        assertEquals(Verdict.Replace("§5Slayer §7· §4Tarantula §7started\n§7Slay §f10,000 XP §7of Spiders"),
            Slayer.process("» Slay 10,000 Combat XP worth of Spiders.", compact))
        // La marque vient de la quête qui commence, pas de la précédente.
        assertEquals(Verdict.Hide, Slayer.process("SLAYER QUEST STARTED!", compact))
        // Une ligne vide glissée entre les deux ne casse pas l'attente.
        assertEquals(null, Slayer.process("", compact))
        assertEquals(Verdict.Replace("§5Slayer §7· §5Voidgloom §7started\n§7Slay §f20,000 XP §7of Endermen"),
            Slayer.process("» Slay 20,000 Combat XP worth of Endermen.", compact))
    }

    @Test fun `the level line comes back glued under the complete line`() {
        Slayer.process("» Slay 10,000 Combat XP worth of Spiders.", compact)
        assertEquals(null, Slayer.process("SLAYER QUEST COMPLETE!", compact))
        Slayer.displayed("Slayer Tarantula ✔ complete")
        assertEquals(Verdict.Replace(
            "§5Slayer §4Tarantula §a✔ §fcomplete\n§5Slayer §7· §fLVL 7 §8- §7Next LVL in §d256,961 XP"),
            Slayer.process("Spider Slayer LVL 7 - Next LVL in 256,961 XP!", compact))
        // La ligne déjà affichée est rendue une fois au mixin, qui la retire du chat.
        assertEquals("Slayer Tarantula ✔ complete", Slayer.stale())
        assertEquals(null, Slayer.stale())
    }

    @Test fun `a level line without its complete stays on its own`() {
        assertEquals(null, Slayer.process("Spider Slayer LVL 7 - Next LVL in 256,961 XP!", compact))
    }

    @Test fun `a lone objective keeps its own line`() {
        assertEquals(null, Slayer.process("Something else entirely", compact))
        assertEquals(null, Slayer.process("» Slay 10,000 Combat XP worth of Spiders.", compact))
    }

    @Test fun `no merge when the group is not compacted`() {
        assertEquals(null, Slayer.process("SLAYER QUEST STARTED!", RuleConfig.DEFAULT))
    }

    @Test fun `level line carries the slayer brand`() {
        assertEquals(Verdict.Replace("§5Slayer §7· §4Tarantula §fLVL 7 §8- §7Next LVL in §d256,961 XP"),
            ChatRules.evaluate("   §7Spider Slayer LVL 7 - §fNext LVL in §d256,961 XP!", compact))
        assertEquals(Verdict.Replace("§5Slayer §7· §eSven §fLVL 4 §8- §7Next LVL in §d1,000 XP"),
            ChatRules.evaluate("   §7Wolf Slayer LVL 4 - §fNext LVL in §d1,000 XP!", compact))
    }
}
