package com.simplechat

import com.simplechat.config.ConfigEntry
import com.simplechat.config.RuleConfig
import com.simplechat.config.Settings
import com.simplechat.engine.PersonalAction
import com.simplechat.engine.PersonalRule
import com.simplechat.engine.Collapse
import com.simplechat.engine.ChatRules
import com.simplechat.engine.Verdict
import com.simplechat.engine.RuleAction
import com.simplechat.mixin.ChatHudHarness
import com.simplechat.mixin.ChatHudMixin
import com.simplechat.mixin.ChatScreenMixin
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ChatHudProcessingTest {
    companion object {
        @JvmStatic @BeforeAll fun init(@TempDir dir: Path) {
            val field = FabricLoaderImpl::class.java.getDeclaredField("configDir").apply { isAccessible = true }
            val previous = field.get(FabricLoaderImpl.INSTANCE)
            field.set(FabricLoaderImpl.INSTANCE, dir)
            try { Settings.masterEnabled } finally { field.set(FabricLoaderImpl.INSTANCE, previous) }
        }
    }

    private val cfg = RuleConfig.DEFAULT.copy(groupRepeats = false)
    private val hud = ChatHudHarness()

    private fun config(value: RuleConfig) {
        for ((name, fieldValue) in mapOf("cached" to value, "cachedAt" to Long.MAX_VALUE,
            "cachedRevision" to ConfigEntry.revision)) {
            RuleConfig::class.java.getDeclaredField(name).apply { isAccessible = true }.set(null, fieldValue)
        }
    }

    @AfterEach fun reset() {
        RuleConfig::class.java.getDeclaredField("cached").apply { isAccessible = true }.set(null, null)
        SimpleChatMod.resetChatState()
    }

    private fun button(text: String) = Component.literal(text).withStyle {
        it.withClickEvent(ClickEvent.RunCommand("/pickup"))
    }

    @Test fun `disabled mod preserves every component and repeated message`() {
        config(cfg.copy(masterEnabled = false, showTimestamps = true, groupRepeats = true))
        val messages = listOf(
            button("✆ RING... RING... [PICK UP]"),
            Component.literal("SAFARI REWARD SUMMARY"),
            Component.literal("HOPPITY'S HUNT You found Brutus (UNCOMMON)!"),
            Component.literal("DUPLICATE RABBIT! +10 Chocolate"),
            Component.literal("Please be mindful of Discord links in chat as they may pose a security risk."),
            Component.literal("Party > Timo: gg"),
        )
        for (message in messages + messages) hud.receive(message)
        assertEquals(messages.size * 2, hud.messages.size)
        for ((message, shown) in (messages + messages).zip(hud.messages.reversed())) {
            assertSame(message, shown.content())
        }
    }

    @Test fun `system action buttons survive cleanup`() {
        config(cfg)
        val message = button("✆ RING... RING... [PICK UP]")
        hud.receive(message)
        assertSame(message, hud.messages.single().content())
    }

    @Test fun `disabled mod does not route outgoing chat or extend history`() {
        val enabled = Settings.masterEnabled
        val tabs = Settings.chatTabs
        val tabField = ChatScreenMixin::class.java.getDeclaredField("hsc\$tab").apply { isAccessible = true }
        val selected = tabField.getInt(null)
        try {
            Settings.masterEnabled = false
            Settings.chatTabs = true
            tabField.setInt(null, 2)
            val route = ChatScreenMixin::class.java.getDeclaredMethod("hsc\$routeSend", String::class.java)
                .apply { isAccessible = true }
            assertEquals("hello", route.invoke(ChatScreenMixin(), "hello"))
            val history = ChatHudMixin::class.java.getDeclaredMethod("hsc\$maxHistory", Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
            assertEquals(100, history.invoke(hud, 100))
        } finally {
            Settings.masterEnabled = enabled
            Settings.chatTabs = tabs
            tabField.setInt(null, selected)
        }
    }

    @Test fun `disabling the mod discards pending summaries before reactivation`() {
        SafariSummary.process("SAFARI REWARD SUMMARY", cfg)
        HoppityCompact.process("You found a Rabbit (COMMON)!", true)
        Collapse.remember("old message", "old message (x3)", 3)
        config(cfg.copy(masterEnabled = false))
        hud.receive(Component.literal("Party > Timo: gg"))
        config(cfg)
        assertNull(SafariSummary.process("+14 Shards", cfg))
        assertNull(Collapse.seen("old message"))
        assertEquals(Verdict.Replace("§dNew rabbit"), HoppityCompact.process("NEW RABBIT!", true))
    }

    @Test fun `muted players remain hidden even with a clickable name`() {
        config(cfg.copy(mutedPlayers = listOf("Timo")))
        hud.receive(button("Party > Timo: gg"))
        assertEquals(0, hud.messages.size)
    }

    @Test fun `personal hides remain stronger than system buttons`() {
        config(cfg.copy(personalRules = listOf(PersonalRule(text = "RING", action = PersonalAction.HIDE))))
        hud.receive(button("✆ RING... RING... [PICK UP]"))
        assertEquals(0, hud.messages.size)
    }

    @Test fun `legacy custom hides remain stronger than system buttons`() {
        config(cfg.copy(customHidePatterns = listOf("RING")))
        hud.receive(button("✆ RING... RING... [PICK UP]"))
        assertEquals(0, hud.messages.size)
    }

    @Test fun `Hoppity quotes remain player messages through the whole pipeline`() {
        config(cfg)
        hud.receive(Component.literal("Party > Timo: NEW RABBIT!"))
        assertEquals("P > Timo : NEW RABBIT!", hud.messages.single().content().string)
    }

    @Test fun `clickable rabbit discoveries only appear in their compact summaries`() {
        config(cfg)
        for ((name, rarity) in listOf("Natalie" to "COMMON", "Brutus" to "UNCOMMON", "Uncle Buck" to "RARE")) {
            hud.receive(Component.literal("§D§LHOPPITY'S HUNT §7You found $name §7($rarity)!").withStyle {
                it.withClickEvent(ClickEvent.RunCommand("/hoppity"))
            })
            hud.receive(Component.literal("DUPLICATE RABBIT! +10,612,335 Chocolate").withStyle {
                it.withClickEvent(ClickEvent.RunCommand("/hoppity"))
            })
        }
        assertEquals(listOf("Duplicate rabbit - Natalie (common)", "Duplicate rabbit - Brutus (uncommon)",
            "Duplicate rabbit - Uncle Buck (rare)"), hud.messages.reversed().map { it.content().string })
        for (message in hud.messages) {
            assertEquals(ClickEvent.RunCommand("/hoppity"), message.content().style.clickEvent)
        }
    }

    @Test fun `hidden safari removes both separators even with clickable rewards`() {
        config(cfg.copy(groupActions = mapOf(SafariSummary.SETTING to RuleAction.HIDE)))
        val bar = "§a§l" + "▬".repeat(64)
        hud.receive(Component.literal(bar))
        for (line in listOf("SAFARI REWARD SUMMARY", "", "+14 Shards", "+80 Safari Essence",
            "+24,437 Hunting Exp", bar)) {
            hud.receive(button(line))
        }
        assertEquals(0, hud.messages.size)
    }

    @Test fun `compact safari removes its opening separator with timestamps`() {
        config(cfg.copy(showTimestamps = true))
        val bar = "§a§l" + "▬".repeat(64)
        for (line in listOf(bar, "SAFARI REWARD SUMMARY", "", "+14 Shards", "+80 Safari Essence",
            "+24,437 Hunting Exp", bar)) {
            hud.receive(Component.literal(line))
        }
        assertEquals(1, hud.messages.size)
        assertEquals("SAFARI +14 Shards · +80 Essence · +24.4k Hunting Exp", hud.messages.single().content().string.substring(8))
    }

    @Test fun `safari does not remove a separator separated from its header by another message`() {
        config(cfg)
        val bar = Component.literal("§a§l" + "▬".repeat(64))
        hud.receive(bar)
        hud.receive(Component.literal("Party > Timo: gg"))
        for (line in listOf("SAFARI REWARD SUMMARY", "+14 Shards", "▬".repeat(64))) {
            hud.receive(Component.literal(line))
        }
        assertEquals(3, hud.messages.size)
        assertSame(bar, hud.messages.last().content())
    }

    @Test fun `disabled safari leaves its opening separator and header untouched`() {
        for (disabled in listOf(cfg.copy(masterEnabled = false), cfg.copy(skyblockEnabled = false),
            cfg.copy(groupActions = mapOf(SafariSummary.SETTING to RuleAction.OFF)))) {
            config(disabled)
            val bar = Component.literal("§a§l" + "▬".repeat(64))
            hud.receive(bar)
            hud.receive(Component.literal("SAFARI REWARD SUMMARY"))
            assertSame(bar, hud.messages[1].content())
        }
        assertEquals(6, hud.messages.size)
    }

    @Test fun `a hidden intervening message also prevents removing an unrelated separator`() {
        config(cfg.copy(personalRules = listOf(PersonalRule(text = "unrelated", action = PersonalAction.HIDE))))
        val bar = Component.literal("§a§l" + "▬".repeat(64))
        hud.receive(bar)
        hud.receive(Component.literal("unrelated message"))
        for (line in listOf("SAFARI REWARD SUMMARY", "+14 Shards", "▬".repeat(64))) {
            hud.receive(Component.literal(line))
        }
        assertEquals(2, hud.messages.size)
        assertSame(bar, hud.messages.last().content())
    }

    @Test fun `skyblock xp is visible immediately even inside a pending reward block`() {
        for (action in listOf(RuleAction.COMPACT, RuleAction.HIDE)) {
            SimpleChatMod.resetChatState()
            hud.messages.clear()
            config(cfg.copy(groupActions = mapOf(MiningSummary.SETTING to action, "skyblockXp" to RuleAction.COMPACT)))
            hud.receive(Component.literal("§d§l" + "▬".repeat(64)))
            hud.receive(Component.literal("  §6§lCHEST LOCKPICKED"))
            hud.receive(Component.literal("    §b+2 SkyBlock XP §7(Bag Upgrades) §8(69/100)"))
            assertTrue(hud.messages.any { "+2 SB XP (Bag Upgrades)" in it.content().string }, action.name)
            hud.receive(Component.literal("Party > Timo: gg"))
            assertTrue(hud.messages.any { "+2 SB XP (Bag Upgrades)" in it.content().string }, action.name)
        }
    }

    @Test fun `skyblock xp OFF preserves the original gain inside a pending reward block`() {
        config(cfg.copy(groupActions = mapOf("skyblockXp" to RuleAction.OFF)))
        hud.receive(Component.literal("§e§l" + "▬".repeat(64)))
        hud.receive(Component.literal("  §6§lCHEST LOCKPICKED"))
        val xp = Component.literal("    §b+2 SkyBlock XP §7(Bag Upgrades) §8(69/100)")
        hud.receive(xp)
        assertEquals(1, hud.messages.size)
        assertSame(xp, hud.messages.single().content())
    }

    @Test fun `reward blocks keep their original lines and xp gains in history`() {
        config(cfg.copy(groupRepeats = true, smartCollapse = true))
        val lines = listOf("  §b§lATTRIBUTE LEVEL UP", "  §6Rabbit Youth §31", "  §a§lREWARDS",
            "    §7§8+§b1 SkyBlock XP", "  §6§lCOLLECTION LEVEL UP §eHard Stone §8VI➜§eVII",
            "  §a§lREWARDS", "    §7§5Silex §7Recipe", "    §7§8+§b4 SkyBlock XP")
        for (line in lines) hud.receive(Component.literal(line))
        assertEquals(listOf("ATTRIBUTE LEVEL UP", "Rabbit Youth 1", "REWARDS", "+1 SB XP",
            "COLLECTION LEVEL UP Hard Stone VI➜VII", "REWARDS", "Silex Recipe", "+4 SB XP"),
            hud.messages.reversed().map { ChatRules.clean(it.content().string) })
    }

    @Test fun `unindented skyblock xp gains stay separate even when repeated`() {
        config(cfg.copy(groupRepeats = true, smartCollapse = true))
        for (amount in listOf(1, 4, 4)) hud.receive(Component.literal("§b+$amount SkyBlock XP"))
        assertEquals(listOf("+1 SB XP", "+4 SB XP", "+4 SB XP"),
            hud.messages.reversed().map { it.content().string })
    }

    @Test fun `collection separators do not start a chest summary`() {
        config(cfg.copy(groupRepeats = true))
        val bar = "§e§l" + "▬".repeat(64)
        val lines = listOf(bar, "  §6§lCOLLECTION LEVEL UP §eHard Stone §8VI➜§eVII", "",
            "  §a§lREWARDS", "    §7§5Silex §7Recipe", "    §7§8+§b4 SkyBlock XP", bar,
            "    unrelated indented message")
        for (line in lines) hud.receive(Component.literal(line))
        assertEquals(lines.map { ChatRules.clean(it).replace("+4 SkyBlock XP", "+4 SB XP") },
            hud.messages.reversed().map { ChatRules.clean(it.content().string) })
    }

    @Test fun `chest summaries still remove their own separators with timestamps`() {
        for (action in listOf(RuleAction.COMPACT, RuleAction.HIDE, RuleAction.OFF)) {
            SimpleChatMod.resetChatState()
            hud.messages.clear()
            config(cfg.copy(showTimestamps = true, groupActions = mapOf(MiningSummary.SETTING to action)))
            val bar = "§e§l" + "▬".repeat(64)
            val lines = listOf(bar, "  §6§lCHEST LOCKPICKED", "  §a§lREWARDS",
                "    §dGemstone Powder §8x537", bar)
            for (line in lines) hud.receive(Component.literal(line))
            val shown = hud.messages.reversed().map { ChatRules.clean(it.content().string.substring(8)) }
            val expected = when (action) {
                RuleAction.COMPACT -> listOf("CHEST Gemstone Powder x537")
                RuleAction.HIDE -> emptyList()
                else -> lines.map(ChatRules::clean)
            }
            assertEquals(expected, shown, action.name)
        }
    }

    @Test fun `ordinary system spam still groups across unrelated messages`() {
        config(cfg.copy(groupRepeats = true))
        val spam = Component.literal("COMPACT! You found an Enchanted Hard Stone!")
        hud.receive(spam)
        hud.receive(Component.literal("unrelated message"))
        hud.receive(spam)
        assertEquals(listOf("unrelated message", "COMPACT! You found an Enchanted Hard Stone! (x2)"),
            hud.messages.reversed().map { it.content().string })
    }

    @Test fun `safari gains remain visible when another message interrupts the summary`() {
        config(cfg.copy(groupRepeats = true))
        hud.receive(Component.literal("SAFARI REWARD SUMMARY"))
        hud.receive(Component.literal("+14 Shards"))
        assertEquals("SAFARI +14 Shards", hud.messages.single().content().string)
        hud.receive(Component.literal("Party > Timo: gg"))
        hud.receive(Component.literal("------------"))
        assertEquals("SAFARI +14 Shards", hud.messages.last().content().string)
    }

    @Test fun `safari updates one line and keeps separate runs with repeat grouping and timestamps`() {
        config(cfg.copy(groupRepeats = true, showTimestamps = true))
        repeat(2) {
            hud.receive(Component.literal("SAFARI REWARD SUMMARY"))
            hud.receive(button("+14 Shards"))
            assertEquals(it + 1, hud.messages.size)
            hud.receive(button("+80 Safari Essence"))
            assertEquals(it + 1, hud.messages.size)
            hud.receive(Component.literal("------------"))
            assertEquals(it + 1, hud.messages.size)
        }
        assertEquals(listOf("SAFARI +14 Shards · +80 Essence", "SAFARI +14 Shards · +80 Essence"),
            hud.messages.map { it.content().string.substring(8) })
    }

    @Test fun `chest gains are shown before the closing bar and survive an interruption`() {
        config(cfg.copy(groupRepeats = true, showTimestamps = true))
        hud.receive(Component.literal("§e§l" + "▬".repeat(64)))
        hud.receive(Component.literal("  §6§lCHEST LOCKPICKED"))
        hud.receive(Component.literal("    §dGemstone Powder §8x537"))
        assertEquals("CHEST Gemstone Powder x537", hud.messages.single().content().string.substring(8))
        hud.receive(Component.literal("    §b+2 SkyBlock XP"))
        hud.receive(Component.literal("    §9Goblin Egg"))
        assertEquals(2, hud.messages.size)
        assertEquals("CHEST Gemstone Powder x537 · Goblin Egg", hud.messages.first().content().string.substring(8))
        hud.receive(Component.literal("Party > Timo: gg"))
        assertEquals(3, hud.messages.size)
        assertTrue(hud.messages.any { "CHEST Gemstone Powder x537 · Goblin Egg" in it.content().string })
    }

    @Test fun `resetting a partial summary does not remove its displayed rewards`() {
        config(cfg)
        hud.receive(Component.literal("SAFARI REWARD SUMMARY"))
        hud.receive(Component.literal("+14 Shards"))
        assertEquals(1, hud.messages.size)
        SimpleChatMod.resetChatState()
        hud.receive(Component.literal("SAFARI REWARD SUMMARY"))
        hud.receive(Component.literal("+80 Safari Essence"))
        assertEquals(listOf("SAFARI +14 Shards", "SAFARI +80 Essence"),
            hud.messages.reversed().map { it.content().string })
    }
}
