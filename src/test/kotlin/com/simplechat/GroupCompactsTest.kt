package com.simplechat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Compacts par message des groupes hide-useless (action COMPACT) + thème couleur. */
class GroupCompactsTest {

    private fun cfg(group: HideGroup) =
        RuleConfig.DEFAULT.copy(hideGroupActions = mapOf(group to HideAction.COMPACT))

    @Test fun `transitions compacted`() {
        val c = cfg(HideGroup.TRANSITIONS)
        assertEquals(Verdict.Replace("§8⌛ §7Bedwars 4v4"),
            ChatRules.evaluate("Queuing... Bedwars 4v4", c))
        assertEquals(Verdict.Replace("§aWelcome back"),
            ChatRules.evaluate("§eWelcome to Hypixel SkyBlock!", c))
        assertEquals(Verdict.Replace("§7Profile §8· §aMango"),
            ChatRules.evaluate("You are playing on profile: Mango (Co-op)", c))
    }

    @Test fun `notifications compacted`() {
        val c = cfg(HideGroup.NOTIFICATIONS)
        assertEquals(Verdict.Replace("§aTipped §f12 §7(4 games)"),
            ChatRules.evaluate("§aYou tipped 12 players in 4 games!", c))
        assertEquals(Verdict.Replace("§eAchievement §7· §fBedwars Banker"),
            ChatRules.evaluate("§eAchievement Unlocked: Bedwars Banker", c))
    }

    @Test fun `dungeons compacted`() {
        val c = cfg(HideGroup.DUNGEONS)
        assertEquals(Verdict.Replace("§a+ §fWither Key"),
            ChatRules.evaluate("§8A §5Wither Key §8was picked up!", c))
        assertEquals(Verdict.Replace("§d+ §fBlessing of Power V"),
            ChatRules.evaluate("§dA Blessing of Power V was picked up!", c))
    }

    @Test fun `abilities compacted`() {
        val c = cfg(HideGroup.ABILITIES)
        assertEquals(Verdict.Replace("§7Cooldown §8· §f2s"),
            ChatRules.evaluate("§cThis ability is on cooldown for 2 more seconds.", c))
        assertEquals(Verdict.Replace("§bNot enough mana"),
            ChatRules.evaluate("§cYou do not have enough mana to do this!", c))
    }

    @Test fun `combat and heal compacted`() {
        val c = cfg(HideGroup.COMBAT)
        assertEquals(Verdict.Replace("§c-1.2k §7(Goldor's TNT Trap)"),
            ChatRules.evaluate("§cGoldor's TNT Trap hit you for 1,200 true damage", c))
        assertEquals(Verdict.Replace("§a+500❤ §7(MeteoFrance)"),
            ChatRules.evaluate("§aMeteoFrance healed you for 500 health!", c))
    }

    @Test fun `rewards compacted`() {
        val c = cfg(HideGroup.REWARDS)
        assertEquals(Verdict.Replace("§e+633 Event EXP"),
            ChatRules.evaluate("§eYou earned 633 Event EXP from playing SkyBlock!", c))
        assertEquals(Verdict.Replace("§7Combo ended §8· §f30"),
            ChatRules.evaluate("§cYour Kill Combo has expired! You reached a 30 Kill Combo!", c))
    }

    @Test fun `bazaar compacted`() {
        val c = cfg(HideGroup.BAZAAR)
        assertEquals(Verdict.Replace("§6Bazaar §7· selling…"),
            ChatRules.evaluate("§6[Bazaar] §7Submitting sell offer...", c))
    }

    @Test fun `slayer compacted`() {
        val c = cfg(HideGroup.SLAYER)
        assertEquals(Verdict.Replace("§5Slayer §7· started"),
            ChatRules.evaluate("  §5§lSLAYER QUEST STARTED!", c))
    }

    @Test fun `events compacted`() {
        val c = cfg(HideGroup.EVENTS)
        assertEquals(Verdict.Replace("§bParkour §a✔ §f1:23"),
            ChatRules.evaluate("§bFinished parkour Foraging Island in 1:23!", c))
        assertEquals(Verdict.Replace("§8→ §7Castle"),
            ChatRules.evaluate("§aWarped from the Spawn to the Castle!", c))
    }

    @Test fun `unknown group message keeps raw on compact`() {
        val c = cfg(HideGroup.MISC)
        assertEquals(Verdict.Replace("§cInventory full? Don't forget to check out your Storage inside the SkyBlock Menu!"),
            ChatRules.evaluate("§cInventory full? Don't forget to check out your Storage inside the SkyBlock Menu!", c))
    }

    @Test fun `rare reward keeps item rarity color`() {
        assertEquals(Verdict.Replace("§6§lRARE REWARD §r§fTioLDK §7· §r§9Fuming Potato Book §fin Bedrock Chest"),
            ChatRules.evaluate("§6§lRARE REWARD! §fTioLDK §efound a §9Fuming Potato Book §ein their §aBedrock Chest§e!", RuleConfig.DEFAULT))
    }

    @Test fun `mystery box keeps item rarity color`() {
        val c = cfg(HideGroup.TRANSITIONS)
        assertEquals(Verdict.Replace("§7Mystery Box §8· §r§5Epic Rune"),
            ChatRules.evaluate("§bMeteo §efound a §5Epic Rune §ein a §6Mystery Box!", c))
    }

    @Test fun `theme recolors white words of compacts`() {
        val themed = RuleConfig.DEFAULT.copy(
            actions = mapOf("pet-spawn" to RuleAction.COMPACT),
            compactTheme = true, compactThemeColor = 0xFF00FF,
        )
        assertEquals(Verdict.Replace("§a+ §#FF00FFBat"),
            ChatRules.evaluate("Spawned your Bat!", themed))
    }

    @Test fun `legacy parser reads custom hex color`() {
        assertEquals(listOf(Seg("+ ", 0x55FF55), Seg("Bat", 0xFF00FF)),
            LegacyText.parse("§a+ §#FF00FFBat"))
    }
}
