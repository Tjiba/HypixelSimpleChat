package com.simplechat.rules

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.LegacyText
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Seg
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Versions courtes des groupes de spam (action COMPACT) + thème couleur. */
class CompactsTest {

    private fun cfg(group: String) =
        RuleConfig.DEFAULT.copy(groupActions = mapOf(group to RuleAction.COMPACT))

    @Test fun `transitions compacted`() {
        val c = cfg("transitions")
        assertEquals(Verdict.Replace("§8⌛ §7Bedwars 4v4"),
            ChatRules.evaluate("Queuing... Bedwars 4v4", c))
        assertEquals(Verdict.Replace("§aWelcome back"),
            ChatRules.evaluate("§eWelcome to Hypixel SkyBlock!", c))
        assertEquals(Verdict.Replace("§7Profile §8· §aMango"),
            ChatRules.evaluate("You are playing on profile: Mango (Co-op)", c))
    }

    @Test fun `notifications compacted`() {
        val c = cfg("notifications")
        assertEquals(Verdict.Replace("§aTipped §f12 §7(4 games)"),
            ChatRules.evaluate("§aYou tipped 12 players in 4 games!", c))
        assertEquals(Verdict.Replace("§eAchievement §7· §eBedwars Banker"),
            ChatRules.evaluate("§eAchievement Unlocked: Bedwars Banker", c))
    }

    @Test fun `nothing left to tip is hidden`() {
        val c = RuleConfig.DEFAULT.copy(actions = mapOf("tipped-nobody" to RuleAction.HIDE))
        assertEquals(Verdict.Hide, ChatRules.evaluate(
            "§cYou already tipped everyone that has boosters active, so there isn't anybody to be tipped right now!", c))
    }

    @Test fun `dungeons compacted`() {
        val c = cfg("dungeons")
        assertEquals(Verdict.Replace("§a+ §5Wither Key"),
            ChatRules.evaluate("§8A §5Wither Key §8was picked up!", c))
        assertEquals(Verdict.Replace("§d+ §dBlessing of Power V"),
            ChatRules.evaluate("§dA Blessing of Power V was picked up!", c))
        // Un nom de joueur garde son rang entier, avec le § interne du "+".
        assertEquals(Verdict.Replace("§7Door §8· §b[MVP§c+§b] Timo"),
            ChatRules.evaluate("§b[MVP§c+§b] Timo §7opened a §5WITHER §7door!", c))
    }

    @Test fun `foraging compacted`() {
        val c = RuleConfig.DEFAULT
        assertEquals(Verdict.Replace("§a§lPETALFALL!"),
            ChatRules.evaluate("§a§lPETALFALL! §r§fYou felled the entire §aTree§f!", c))
        // Le vert d'Hypixel n'est pas dans la palette vanilla : le compact reprend le sien.
        assertEquals(Verdict.Replace("§#3BE63B§lPETALFALL!"),
            ChatRules.evaluate("§#3BE63B§lPETALFALL! §r§fYou felled the entire §#3BE63BTree§f!", c))
        // L'item garde sa rareté ; sans quantité, pas de "x" qui traîne.
        assertEquals(Verdict.Replace("§6§lFLOOR DROP! §r§fFig Log §7x512"),
            ChatRules.evaluate("§6§lFLOOR DROP! §fYou found Fig Log §7x512 §fon the ground!", c))
        assertEquals(Verdict.Replace("§6§lFLOOR DROP! §r§5Ancient Bark"),
            ChatRules.evaluate("§6§lFLOOR DROP! §fYou found §5Ancient Bark §fon the ground!", c))
        assertEquals(Verdict.Replace("§9Mangrove Tree Gift §8- §a100% §8| §e5 rewards"),
            ChatRules.evaluate("§9Mangrove Tree Gift. §7You helped cut §a100.0% §7and gained §e5 rewards§a!", c))
        // Les couleurs suivent le message brut, pas une table écrite à la main.
        assertEquals(Verdict.Replace("§bHelix Tree Gift §8- §a86.8% §8| §e4 rewards"),
            ChatRules.evaluate("§bHelix Tree Gift. §7You helped cut §a86.8% §7and gained §e4 rewards§a!", c))
        // Sous 10% de participation, Hypixel passe le pourcentage en rouge : le compact le garde.
        assertEquals(Verdict.Replace("§dFig Tree Gift §8- §c9.8% §8| §e0 rewards"),
            ChatRules.evaluate("§dFig Tree Gift. §7You helped cut §c9.8% §7and gained §e0 rewards§a!", c))
    }

    @Test fun `beeheemoth compacted`() {
        val c = cfg("foraging-torrhus")
        assertEquals(Verdict.Replace("§d§lBEEHEEMOTH! §aCritter Safari Entrance"),
            ChatRules.evaluate("§dBEEHEEMOTH! §fA §dBeeheemoth §fhas spawned at §aCritter Safari Entrance§f!", c))
        assertEquals(Verdict.Replace("§d§lBEEHEEMOTH! §7slowing"),
            ChatRules.evaluate("§dBEEHEEMOTH! §fThe §dBeeheemoth §fis starting to slow down!", c))
        assertEquals(Verdict.Replace("§d§lBEEHEEMOTH! §aexhausted"),
            ChatRules.evaluate("§dBEEHEEMOTH! §fThe §dBeeheemoth §fis exhausted - not long now!", c))
        // Couleur hors palette vanilla : elle survit au compact au lieu d'être approximée.
        assertEquals(Verdict.Replace("§#3BE63B§lBEEHEEMOTH! §aMega Tree"),
            ChatRules.evaluate("§#3BE63BBEEHEEMOTH! §fA §dBeeheemoth §fhas spawned at §aMega Tree§f!", c))
    }

    @Test fun `hive compacted`() {
        val c = cfg("foraging-hive")
        // La ligne d'attente disparaît même quand le réglage du groupe est sur COMPACT.
        assertEquals(Verdict.Hide,
            ChatRules.evaluate("§7§oYou stick your hand into the honeyhive and feel around...", c))
        assertEquals(Verdict.Replace("§6§lHIVE §r§fHoneycomb §7x21 §8+ §aEnchanted Honeycomb"),
            ChatRules.evaluate("§6§lHIVE! §fYou found Honeycomb §7x21 §fand §aEnchanted Honeycomb§f!", c))
        assertEquals(Verdict.Replace("§7Honeybuzz appeared"),
            ChatRules.evaluate("§7§oA Honeybuzz appeared, angry at you for stealing its honey!", c))
    }

    // Un sac se remplit et se vide : les deux sens sont reconnus, la couleur suit le signe.
    @Test fun `sacks compacted both ways`() {
        val c = cfg("sacks")
        assertEquals(Verdict.Replace("§a+64 Cobblestone"),
            ChatRules.evaluate("§6[Sacks] §a+64 Cobblestone", c))
        assertEquals(Verdict.Replace("§c-12 items. (Last 5s.)"),
            ChatRules.evaluate("§6[Sacks] §c-12§e items.§8 (Last 5s.)", c))
    }

    @Test fun `hunted mobs compacted`() {
        assertEquals(Verdict.Replace("§6§lHONEY TREE! §r§cWoodlouse"),
            ChatRules.evaluate("§6§lHONEY TREE! §cWoodlouse §6has appeared!", RuleConfig.DEFAULT))
        // Quantité, nom et « Shards » gardent chacun leur couleur.
        assertEquals(Verdict.Replace("§fx2 Honeybuzz §aShards"),
            ChatRules.evaluate("§aYou caught §fx2 Honeybuzz §aShards!", RuleConfig.DEFAULT))
    }

    // Les deux tournures d'apparition, dont celle de la chasse encadrée de tirets.
    @Test fun `appeared mobs compacted in both wordings`() {
        assertEquals(Verdict.Replace("§cWoodlouse appeared"),
            ChatRules.evaluate("§eA §cWoodlouse §ehas appeared!", RuleConfig.DEFAULT))
        assertEquals(Verdict.Replace("§6Groundhog appeared"),
            ChatRules.evaluate("§f- A wild §6Groundhog §fappeared!", RuleConfig.DEFAULT))
    }

    // Un seul réglage pour tous les mobs de chasse : le nom et sa couleur viennent du brut.
    @Test fun `escaped mobs compacted`() {
        val c = cfg("foraging-hunting")
        assertEquals(Verdict.Replace("§dBeeheemoth escaped"),
            ChatRules.evaluate("§cYou looked away! §dBeeheemoth escaped§c!", c))
        assertEquals(Verdict.Replace("§aWoodlouse escaped"),
            ChatRules.evaluate("§cYou looked away! §aWoodlouse escaped§c!", c))
        assertEquals(Verdict.Replace("§fHoneyhog escaped"),
            ChatRules.evaluate("§cYou didn't reel! §fHoneyhog escaped§c!", c))
        assertEquals(Verdict.Replace("§fHoneyhog escaped"),
            ChatRules.evaluate("§cYou reeled too early! §fHoneyhog escaped§c!", c))
    }

    @Test fun `abilities compacted`() {
        val c = cfg("abilities")
        assertEquals(Verdict.Replace("§7Cooldown §8· §f2s"),
            ChatRules.evaluate("§cThis ability is on cooldown for 2 more seconds.", c))
        assertEquals(Verdict.Replace("§bNot enough mana"),
            ChatRules.evaluate("§cYou do not have enough mana to do this!", c))
    }

    @Test fun `combat and heal compacted`() {
        val c = cfg("combat")
        assertEquals(Verdict.Replace("§c-1.2k §7(Goldor's TNT Trap)"),
            ChatRules.evaluate("§cGoldor's TNT Trap hit you for 1,200 true damage", c))
        assertEquals(Verdict.Replace("§a+500❤ §7(MeteoFrance)"),
            ChatRules.evaluate("§aMeteoFrance healed you for 500 health!", c))
    }

    @Test fun `rewards compacted`() {
        val c = cfg("rewards")
        assertEquals(Verdict.Replace("§e+633 Event EXP"),
            ChatRules.evaluate("§eYou earned 633 Event EXP from playing SkyBlock!", c))
        assertEquals(Verdict.Replace("§7Combo ended §8· §f30"),
            ChatRules.evaluate("§cYour Kill Combo has expired! You reached a 30 Kill Combo!", c))
    }

    @Test fun `events compacted`() {
        val c = cfg("events")
        assertEquals(Verdict.Replace("§bParkour §a✔ §f1:23"),
            ChatRules.evaluate("§bFinished parkour Foraging Island in 1:23!", c))
        assertEquals(Verdict.Replace("§8→ §7Castle"),
            ChatRules.evaluate("§aWarped from the Spawn to the Castle!", c))
    }

    @Test fun `rare reward keeps item rarity color`() {
        assertEquals(Verdict.Replace("§6§lRARE REWARD §r§fTioLDK §7· §r§9Fuming Potato Book §fin Bedrock Chest"),
            ChatRules.evaluate("§6§lRARE REWARD! §fTioLDK §efound a §9Fuming Potato Book §ein their §aBedrock Chest§e!", RuleConfig.DEFAULT))
    }

    @Test fun `mystery box keeps item rarity color`() {
        assertEquals(Verdict.Replace("§7Mystery Box §8· §r§5Epic Rune"),
            ChatRules.evaluate("§bMeteo §efound a §5Epic Rune §ein a §6Mystery Box!", cfg("transitions")))
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
