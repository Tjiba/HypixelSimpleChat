package com.simplechat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChatRulesTest {

    private val cfg = RuleConfig.DEFAULT

    @Test fun `strips color codes and normalizes whitespace`() {
        assertEquals("[MVP+] Name joined the lobby!",
            ChatRules.clean("§b[MVP§6+§b] Name§f §6joined the lobby!"))
    }

    // guild/party ne sont plus HARD_PASS (Task 1 Phase A) : ils seront routés vers
    // ChannelFormat en Task 6. Le formatage guild/party revient à ce moment-là.
    @Test fun `whisper never touched`() {
        assertEquals(Verdict.Pass, ChatRules.evaluate("From [MVP+] Foo: yo", cfg))
        assertEquals(Verdict.Pass, ChatRules.evaluate("To [MVP+] Foo: yo", cfg))
    }

    @Test fun `unknown message passes through`() {
        assertEquals(Verdict.Pass, ChatRules.evaluate("something totally unrecognized 12345", cfg))
    }

    @Test fun `master disabled passes everything`() {
        val off = cfg.copy(masterEnabled = false)
        assertEquals(Verdict.Pass,
            ChatRules.evaluate("§b[MVP§6+§b] Name§f §6joined the lobby!", off))
    }

    @Test fun `lobby join hidden by default`() {
        assertEquals(Verdict.Hide,
            ChatRules.evaluate("§b[MVP§6+§b] Name§f §6joined the lobby!", cfg))
        assertEquals(Verdict.Hide,
            ChatRules.evaluate(" §b>§c>§a>§r §6[MVP§9++§6] Meteo§f §6joined the lobby! §a<§c<§b<", cfg))
    }

    @Test fun `lobby join OFF passes`() {
        val off = cfg.copy(actions = mapOf("lobby-join" to RuleAction.OFF))
        assertEquals(Verdict.Pass,
            ChatRules.evaluate("§b[MVP§6+§b] Name§f §6joined the lobby!", off))
    }

    @Test fun `lobby join COMPACT keeps rank colors without arrows`() {
        val b = cfg.copy(actions = mapOf("lobby-join" to RuleAction.COMPACT))
        assertEquals(Verdict.Replace("[MVP§6+§b] Name§f §8joined"),
            ChatRules.evaluate("§b[MVP§6+§b] Name§f §6joined the lobby!", b))
    }

    // Routage par canal (Task 6) : guild/party/public passent par ChannelFormat -> Verdict.Segments.
    @Test fun `guild routed to channel format`() {
        val v = ChatRules.evaluate("Guild > [MVP+] Foo: gg wp", RuleConfig.DEFAULT)
        assert(v is Verdict.Segments)
        assertEquals("gg wp", (v as Verdict.Segments).segs.last().text)
    }

    @Test fun `party routed`() {
        assert(ChatRules.evaluate("Party > §b[MVP§c+§b] Foo§f: go", RuleConfig.DEFAULT) is Verdict.Segments)
    }

    @Test fun `public routed`() {
        assert(ChatRules.evaluate("[221] ⛃ §b[MVP§c+§b] Milo§f: sell", RuleConfig.DEFAULT) is Verdict.Segments)
    }

    @Test fun `whisper still passes`() {
        assertEquals(Verdict.Pass, ChatRules.evaluate("From [MVP+] Foo: yo", RuleConfig.DEFAULT))
    }

    @Test fun `pet spawn and despawn compacted`() {
        val b = cfg.copy(actions = mapOf("pet-spawn" to RuleAction.COMPACT))
        assertEquals(Verdict.Replace("§a+ §fBat"),
            ChatRules.evaluate("§aSpawned your §6Bat§a!", b))
        assertEquals(Verdict.Replace("§c- §fBat"),
            ChatRules.evaluate("Despawned your Bat!", b))
    }

    @Test fun `radiating generosity compacted`() {
        val b = cfg.copy(actions = mapOf("radiating-generosity" to RuleAction.COMPACT))
        assertEquals(Verdict.Replace("§eRadiating generosity"),
            ChatRules.evaluate("§eYou are still radiating with generosity!", b))
    }

    @Test fun `playtime ticket compacted`() {
        val b = cfg.copy(actions = mapOf("playtime-ticket" to RuleAction.COMPACT))
        assertEquals(Verdict.Replace("§ePlaytime §7· §a+Chest"),
            ChatRules.evaluate("§6PLAYTIME! §eYou gained a Playtime Chest!", b))
    }

    @Test fun `reward link compacted`() {
        val b = cfg.copy(actions = mapOf("reward-link" to RuleAction.COMPACT))
        assertEquals(Verdict.Replace("§6Claim reward: §bhypixel.net/link"),
            ChatRules.evaluate("Click the link to visit our website and claim your reward: hypixel.net/link", b))
    }

    @Test fun `morph and wardrobe compacted`() {
        val b = cfg.copy(actions = mapOf("morph-wardrobe" to RuleAction.COMPACT))
        assertEquals(Verdict.Replace("§7Morphed §8→ §fCow"),
            ChatRules.evaluate("§aYou are now morphed into a §6Cow§a!", b))
        assertEquals(Verdict.Replace("§7Morphed §8→ §fSheep"),
            ChatRules.evaluate("You are now morphed as a Sheep Morph for 5 minutes.", b))
        assertEquals(Verdict.Replace("§7Cloak §8→ §fShimmer"),
            ChatRules.evaluate("You selected the Shimmer Cloak!", b))
        assertEquals(Verdict.Replace("§7Reset morph"),
            ChatRules.evaluate("Reset your Morph!", b))
        assertEquals(Verdict.Replace("§7Reset morph"),
            ChatRules.evaluate("§aMorph reset.", b))
        assertEquals(Verdict.Replace("§7Right-click §fSlimeball §7to activate"),
            ChatRules.evaluate("§7Right-Click with the §aSlimeball §7selected to activate", b))
    }

    @Test fun `abiphone ring hidden by default, compactable`() {
        assertEquals(Verdict.Hide,
            ChatRules.evaluate("§a✆ RING... RING...", cfg))
        val b = cfg.copy(actions = mapOf("abiphone-ring" to RuleAction.COMPACT))
        assertEquals(Verdict.Replace("§a✆ §7Ring…"),
            ChatRules.evaluate("§a✆ RING...", b))
    }

    @Test fun `skyblock pet summon keeps rarity color`() {
        assertEquals(Verdict.Replace("§a+ §r§6Baby Yeti"),
            ChatRules.evaluate("§aYou summoned your §6Baby Yeti§a!", cfg))
        assertEquals(Verdict.Replace("§c- §r§5Ghoul"),
            ChatRules.evaluate("§aYou despawned your §5Ghoul§a!", cfg))
    }

    @Test fun `booster reformatted`() {
        assertEquals(Verdict.Replace("§eBooster §f4.0x§e coins §7· 3h"),
            ChatRules.evaluate("Activated your booster. You now have three hours of 4.0x coins.", cfg))
    }

    @Test fun `mystery reward card reformatted`() {
        assertEquals(Verdict.Replace("§6Daily reward §7· §f[Legendary Mystery Dust]"),
            ChatRules.evaluate("You have claimed a [Legendary Mystery Dust] reward card!", cfg))
    }

    @Test fun `claimed currency compacted`() {
        assertEquals(Verdict.Replace("§aClaimed §f2,200 Hypixel Experience§a, §f3,000 Arcade Coins"),
            ChatRules.evaluate("You have successfully claimed 2,200 Hypixel Experience and 3,000 Arcade Coins!", cfg))
    }

    @Test fun `npc dialog dimmed`() {
        assertEquals(Verdict.Replace("§8[NPC] Simon: We hope you enjoy the festivities this year!"),
            ChatRules.evaluate("§e[NPC] Simon§f: We hope you enjoy the festivities this year!", cfg))
    }

    @Test fun `radiating generosity compacted by default`() {
        assertEquals(Verdict.Replace("§eRadiating generosity"),
            ChatRules.evaluate("You are still radiating with §bGenerosity!", cfg))
    }

    @Test fun `profile id compacted`() {
        assertEquals(Verdict.Replace("§8ceccda75-3780-4791-b93c-87d1e7bc397f"),
            ChatRules.evaluate("Profile ID: ceccda75-3780-4791-b93c-87d1e7bc397f", cfg))
    }

    @Test fun `profile name line hidden by hide-useless`() {
        // Pas de règle v1 -> repli hide-useless (groupe transitions, ON par défaut).
        assertEquals(Verdict.Hide,
            ChatRules.evaluate("§aYou are playing on profile: §ePeach§b (Co-op)", cfg))
    }

    @Test fun `hide-useless off leaves message`() {
        val off = cfg.copy(hideGroupActions = emptyMap())
        assertEquals(Verdict.Pass,
            ChatRules.evaluate("§aYou are playing on profile: §ePeach§b (Co-op)", off))
    }

    @Test fun `hide-useless group GREY dims instead of hiding`() {
        val grey = cfg.copy(hideGroupActions = mapOf(HideGroup.TRANSITIONS to HideAction.GREY))
        assertEquals(Verdict.Replace("§8You are playing on profile: Peach (Co-op)"),
            ChatRules.evaluate("§aYou are playing on profile: §ePeach§b (Co-op)", grey))
    }

    @Test fun `custom hide pattern`() {
        val c = cfg.copy(customHidePatterns = listOf("hunting for a specific spam"))
        assertEquals(Verdict.Hide,
            ChatRules.evaluate("something hunting for a specific spam here", c))
    }

    @Test fun `skyhanni-style groups hide`() {
        assertEquals(Verdict.Hide, ChatRules.evaluate("§7Putting item in escrow...", cfg))
        assertEquals(Verdict.Hide, ChatRules.evaluate("§aStarted parkour Crystal Nucleus!", cfg))
        assertEquals(Verdict.Hide, ChatRules.evaluate("§cWhoa! Slow down there!", cfg))
        assertEquals(Verdict.Hide, ChatRules.evaluate("§5§lSLAYER QUEST COMPLETE!", cfg))
    }

    @Test fun `solo class compacted with hover`() {
        val v = ChatRules.evaluate(
            "§6Your §r§aHealer §r§6stats are doubled because you are the only player using this class!", cfg)
        assert(v is Verdict.Compact)
        assertEquals("§6Healer stats doubled §7(solo)", (v as Verdict.Compact).shortLegacy)
    }

    @Test fun `hoppity merges found and duplicate`() {
        assertEquals(Verdict.Hide, HoppityCompact.process("HOPPITY'S HUNT You found Brutus (UNCOMMON)!", true))
        assertEquals(Verdict.Replace("§dDuplicate rabbit §7- §fBrutus §7(uncommon)"),
            HoppityCompact.process("DUPLICATE RABBIT! +10,612,335 Chocolate", true))
    }

    @Test fun `skyhanni block 2 hides`() {
        assertEquals(Verdict.Hide, ChatRules.evaluate("§b✦ §r§7You earned §r§b120 §r§7Mystery Dust!", cfg))
        assertEquals(Verdict.Hide, ChatRules.evaluate("§aYou earned §r§a1,234 GEXP from playing SkyBlock!", cfg))
        assertEquals(Verdict.Hide, ChatRules.evaluate("§7Your §r§aRabbit Barn §r§7capacity has been increased to 5!", cfg))
    }

    @Test fun `server routing compacted`() {
        assertEquals(Verdict.Replace("§8→ §7mega8E"),
            ChatRules.evaluate("Sending to server mega8E...", cfg))
    }

    @Test fun `loot share compacted`() {
        assertEquals(Verdict.Replace("§6Loot share §7· §f__Anoteros__"),
            ChatRules.evaluate("§eLOOT SHARE §fYou received loot for assisting §b__Anoteros__§f!", cfg))
    }

    @Test fun `damage compact is colored and short`() {
        val b = cfg.copy(actions = mapOf("damage-spam" to RuleAction.COMPACT))
        assertEquals(Verdict.Replace("§6Implosion §7· §c2.6M"),
            ChatRules.evaluate("Your Implosion hit 4 enemies for 2,637,430.3 damage.", b))
    }

    @Test fun `damage spam greyed by default`() {
        assertEquals(Verdict.Replace("§8Your Implosion hit 3 enemies for 2,296,984.7 damage."),
            ChatRules.evaluate("Your Implosion hit 3 enemies for 2,296,984.7 damage.", cfg))
    }

    @Test fun `kill combo greyed by default`() {
        assertEquals(Verdict.Replace("§8+5 Kill Combo +3% ✯ Magic Find"),
            ChatRules.evaluate("+5 Kill Combo +3% ✯ Magic Find", cfg))
    }

    @Test fun `pet spawn compacted by default`() {
        assertEquals(Verdict.Replace("§a+ §fTurtle companion"),
            ChatRules.evaluate("Spawned your Turtle companion!", cfg))
    }

    @Test fun `strips discord warning suffix keeping message`() {
        assertEquals("§9Foo: ecris sur discord",
            ChatRules.stripDiscordWarning(
                "§9Foo: ecris sur discord Please be mindful of Discord links in chat as they may pose a security risk"))
    }

    // Forme réelle du mixin : hsc$toLegacy greffe §r+couleur devant le segment d'avertissement.
    @Test fun `strips discord warning even when preceded by color codes`() {
        assertEquals("§9Foo: ecris sur discord",
            ChatRules.stripDiscordWarning(
                "§9Foo: ecris sur discord §r§9Please be mindful of Discord links in chat as they may pose a security risk"))
    }

    @Test fun `discord warning strip leaves unrelated message intact`() {
        val msg = "Guild > [MVP+] Foo: gg wp"
        assertEquals(msg, ChatRules.stripDiscordWarning(msg))
    }
}
