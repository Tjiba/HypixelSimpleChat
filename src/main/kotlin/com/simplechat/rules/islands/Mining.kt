package com.simplechat.rules.islands

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.Section
import com.simplechat.rules.Tab
import com.simplechat.rules.rules

/** Mining : coffres à poudre, Crystal Hollows et automate du Nucleus. */
object Mining {

    // Le pavé de récompenses est fusionné hors du registre (une dizaine de messages en un) : ce
    // groupe porte le réglage et l'aperçu, le travail est fait par MiningSummary avant l'évaluation.
    val CHEST_SUMMARY = Group("mining-chest-summary", "Chest rewards", Category.SKYBLOCK, Section.GENERAL, RuleAction.COMPACT,
        description = "Powder chest loot, merged into one line",
        tab = Tab.MINING)
    val CHEST = Group("mining-chest", "Chests", Category.SKYBLOCK, Section.GENERAL, RuleAction.HIDE,
        description = "Uncovered, lock picked, already looted, item picked up",
        tab = Tab.MINING)
    val TOOL = Group("mining-tool", "Breaking Power", Category.SKYBLOCK, Section.GENERAL, RuleAction.COMPACT,
        description = "Fragilis' warning when the pickaxe is too weak",
        tab = Tab.MINING)
    // Le buff du jour se raconte en trois lignes : un seul réglage pour le tout.
    val SKY_MALL = Group("mining-sky-mall", "Sky Mall", Category.SKYBLOCK, Section.GENERAL, RuleAction.COMPACT,
        description = "The daily Sky Mall buff, merged into one line",
        tab = Tab.MINING, split = false)
    // Le buff de pioche se raconte en trois lignes : un seul réglage pour le tout.
    val PICKAXE = Group("mining-pickaxe", "Pickaxe ability", Category.SKYBLOCK, Section.GENERAL, RuleAction.COMPACT,
        description = "Mining Speed Boost and co: used, expired, available again",
        tab = Tab.MINING, split = false)
    val CRYSTAL = Group("mining-crystal", "Crystals", Category.SKYBLOCK, Section.CRYSTAL_HOLLOWS, RuleAction.COMPACT,
        description = "Crystals found, placed, waiting to be picked up, run complete",
        tab = Tab.MINING)
    val DETECTOR = Group("mining-detector", "Metal Detector", Category.SKYBLOCK, Section.CRYSTAL_HOLLOWS, RuleAction.COMPACT,
        description = "What the Metal Detector digs up",
        tab = Tab.MINING)
    val KEEPER = Group("mining-keeper", "Keepers", Category.SKYBLOCK, Section.CRYSTAL_HOLLOWS, RuleAction.HIDE,
        description = "Keeper of Diamond, Gold, Lapis and Emerald dialog",
        tab = Tab.MINING)
    // Les gardes crient tous la même chose : un seul réglage pour le tout.
    val GUARDS = Group("mining-guards", "Goblin guards", Category.SKYBLOCK, Section.CRYSTAL_HOLLOWS, RuleAction.HIDE,
        description = "Guards shouting when a crystal is stolen",
        tab = Tab.MINING, split = false)
    // Rendre les composants se raconte en plusieurs lignes : un seul réglage pour le tout.
    val AUTOMATON = Group("mining-automaton", "Automaton", Category.SKYBLOCK, Section.CRYSTAL_HOLLOWS, RuleAction.COMPACT,
        description = "Components brought to fix the giant",
        tab = Tab.MINING, split = false)

    private const val PICKAXE_ABILITIES =
        "Mining Speed Boost|Pickobulus|Maniac Miner|Vein Seeker|Sheer Force|Anomalous Desire|Gemstone Infusion"

    val rules =
        rules(CHEST_SUMMARY) {
            // Jamais atteinte en jeu : MiningSummary tranche avant. Elle existe pour le réglage
            // et pour montrer dans l'aperçu ce que la fusion produit.
            rule("chest-summary", RuleAction.COMPACT,
                "^(?:CHEST LOCKPICKED|LOOT CHEST COLLECTED)$",
                compact = {
                    "§6§lCHEST §r§dGemstone Powder §7x537 §8· " +
                        "§a⸕ Flawed Amber Gemstone §7x2 §8· §9Goblin Egg"
                },
                sample = "  §r§6§lCHEST LOCKPICKED")
        } +
        rules(CHEST) {
            // Le coffre s'annonce puis se déballe : l'annonce ne dit rien que le pavé ne redise.
            // Compact vide pour qu'elle disparaisse aussi quand le groupe est sur COMPACT.
            rule("chest-uncovered", RuleAction.HIDE,
                "^You uncovered a treasure chest!",
                compact = { "" },
                sample = "§aYou uncovered a treasure chest!",
                title = "Chest uncovered")
            rule("chest-lockpicked", RuleAction.HIDE,
                "^You have successfully picked the lock on this chest!",
                compact = { "" },
                sample = "§6You have successfully picked the lock on this chest!",
                title = "Lock picked")
            // Celle-ci dit quelque chose d'utile — ne pas y retourner — mais se répète : grisée.
            rule("chest-looted", RuleAction.GREY,
                "^This chest has already been looted",
                sample = "§cThis chest has already been looted.",
                title = "Already looted")
            // L'objet ramassé au sol après le coffre : le pavé l'a déjà annoncé.
            rule("chest-received", RuleAction.HIDE,
                "^You received (\\d+) (Wishing Compass|Ascension Rope|Oil Barrel)\\.",
                compact = { "§a+${it[1]} ${Fmt.rawSpan(it.raw, it[2])}" },
                sample = "§aYou received §r§f1 §r§9Ascension Rope§r§a.",
                title = "Item picked up")
        } +
        rules(TOOL) {
            // Le renvoi vers Fragilis tient la moitié de la ligne et ne sert qu'une fois.
            rule("breaking-power", RuleAction.COMPACT,
                "^You need a tool with a Breaking Power of (\\d+) to mine (.+?)!",
                compact = { "§c⛏ Breaking Power ${it[1]} §8· ${Fmt.rawSpan(it.raw, it[2], "§7")}" },
                sample = "§cYou need a tool with a §r§aBreaking Power §r§cof §r§66§r§c to mine " +
                    "Ruby Gemstone Block§r§c! Speak to §r§dFragilis §r§cby the entrance to the " +
                    "Crystal Hollows to learn more!")
        } +
        rules(SKY_MALL) {
            // Seule la ligne du buff porte une information : les deux autres se replient dessus.
            rule("sky-mall-new-day", RuleAction.COMPACT,
                "^New day! Your Sky Mall buff changed!",
                compact = { "" },
                sample = "§bNew day! §r§eYour §r§2Sky Mall §r§ebuff changed!",
                title = "New day")
            rule("sky-mall-buff", RuleAction.COMPACT,
                "^New buff: (?:Gain )?(.+ (?:Mining Speed|Mining Fortune|more Powder while mining|" +
                    "Pickaxe Ability cooldowns|chance to find Golden and Diamond Goblins|Titanium drops))\\.$",
                compact = { "§2☀ Sky Mall §8· ${Fmt.rawSpan(it.raw, it[1], "§a")}" },
                sample = "§eNew buff: §r§aGain §r§6+20% Mining Speed§r§a.",
                title = "Buff of the day")
            rule("sky-mall-toggle", RuleAction.COMPACT,
                "^You can disable this messaging by toggling Sky Mall in your /hotm!",
                compact = { "" },
                sample = "§8§oYou can disable this messaging by toggling Sky Mall in your /hotm!",
                title = "Toggle hint")
        } +
        rules(PICKAXE) {
            rule("pickaxe-used", RuleAction.COMPACT,
                "^You used your (.+?) Pickaxe Ability!",
                compact = { "§6⛏ §r${Fmt.rawSpan(it.raw, it[1])} §7used" },
                sample = "§aYou used your §r§6Mining Speed Boost §r§aPickaxe Ability!",
                title = "Ability used")
            // "Your X has expired" et "X is now available" sont génériques : la liste des
            // capacités de pioche les garde ici, hors du réglage Abilities.
            rule("pickaxe-expired", RuleAction.COMPACT,
                "^Your ($PICKAXE_ABILITIES) has expired!",
                compact = { "§6⛏ §7${it[1]} expired" },
                sample = "§cYour Mining Speed Boost has expired!",
                title = "Ability expired")
            rule("pickaxe-available", RuleAction.COMPACT,
                "^($PICKAXE_ABILITIES) is now available!",
                compact = { "§6⛏ §r${Fmt.rawSpan(it.raw, it[1])} §aready" },
                sample = "§a§r§6Mining Speed Boost §r§ais now available!",
                title = "Ability available")
        } +
        rules(CRYSTAL) {
            rule("crystal-found", RuleAction.COMPACT,
                "^✦ CRYSTAL FOUND \\((\\d+)/(\\d+)\\)",
                compact = { "§5§l✦ CRYSTAL §r§7${it[1]}§8/§7${it[2]}" },
                sample = "§f                       §r§5§l✦ CRYSTAL FOUND §r§7(1§r§7/5§r§7)",
                title = "Crystal found")
            // La ligne d'après ne porte que le nom du cristal : repliée avec le compteur.
            rule("crystal-name", RuleAction.HIDE,
                "^(Amber|Amethyst|Jade|Sapphire|Topaz) Crystal$",
                compact = { "" },
                sample = "§f                                §r§6Amber Crystal",
                title = "Crystal name")
            rule("crystal-placed", RuleAction.COMPACT,
                "^✦ You placed the (.+?)!",
                compact = { "§d✦ §r${Fmt.rawSpan(it.raw, it[1])} §7placed" },
                sample = "§5§l✦ §r§dYou placed the §r§bSapphire Crystal§r§d!",
                title = "Crystal placed")
            rule("crystal-pickup", RuleAction.GREY,
                "^PICK IT UP!$",
                sample = "§6§lPICK IT UP!",
                title = "Pick it up")
            // Suit chaque cristal placé : la ligne du dessus dit déjà tout.
            rule("crystal-keep-exploring", RuleAction.HIDE,
                "^Keep exploring the Crystal Hollows to find the rest!",
                compact = { "" },
                sample = "  §r§dKeep exploring the §r§5Crystal Hollows §r§dto find the rest!",
                title = "Keep exploring")
            rule("nucleus-bundle", RuleAction.COMPACT,
                "^You've earned a Crystal Loot Bundle!",
                compact = { "§5§l✦ §r§dNucleus run complete" },
                sample = "  §r§7You've earned a §r§5Crystal Loot Bundle§r§7!",
                title = "Run complete")
            rule("nucleus-vault", RuleAction.HIDE,
                "^Pick it up near the Nucleus Vault!",
                compact = { "" },
                sample = "  §r§7Pick it up near the §r§5Nucleus Vault§r§7!",
                title = "Vault hint")
        } +
        rules(DETECTOR) {
            // Le passage entier est recopié : la trouvaille garde sa couleur de rareté.
            rule("metal-detector", RuleAction.COMPACT,
                "^You found (.+?) with your Metal Detector!",
                compact = { "§c⌖ §r${Fmt.rawSpan(it.raw, it[1])}" },
                sample = "§aYou found §r§a☘ Flawed Jade Gemstone §r§8x2 §r§awith your §r§cMetal Detector§r§a!")
        } +
        rules(KEEPER) {
            // Étiquetées "[NPC] " : déclarées avant Npc, sinon le dialogue générique les prendrait
            // et le réglage Keepers ne servirait à rien.
            rule("keeper-dialog", RuleAction.HIDE,
                "^(?:\\[NPC] )?Keeper of \\w+: ",
                compact = { Fmt.stripNpc(it.raw) },
                sample = "§e[NPC] §6Keeper of Diamond§f: §rExcellent! You have returned the " +
                    "§cScavenged Diamond Axe §rto its rightful place!")
        } +
        rules(GUARDS) {
            rule("guard-shout", RuleAction.HIDE,
                "^\\[GUARD] ",
                sample = "§c[GUARD] Ooblak§r§f: §r§eTHEY'RE STEALING THE CRYSTAL! GET THEM!",
                title = "Guard shout")
            rule("guard-escaped", RuleAction.HIDE,
                "^Whew! That was a close one, better get out of here",
                sample = "§8§oWhew! That was a close one, better get out of here...",
                title = "Escaped")
            rule("guard-stench", RuleAction.HIDE,
                "^The Goblin King's foul stench has dissipated!",
                sample = "§cThe Goblin King's §r§afoul stench §r§chas dissipated!",
                title = "Stench dissipated")
        } +
        rules(AUTOMATON) {
            // Sans ancre : l'automate parle tantôt nu, tantôt étiqueté "[NPC] ".
            rule("automaton-component", RuleAction.COMPACT,
                "Thanks for bringing me the (.+?)! Bring me (.+?) more components? to fix the giant!",
                compact = { "§9⚙ §r${Fmt.rawSpan(it.raw, it[1])} §8· §7${it[2]} more" },
                sample = "§rThanks for bringing me the §9Electron Transmitter§r! Bring me one more component to fix the giant!",
                title = "Component brought")
            rule("automaton-done", RuleAction.COMPACT,
                "You've brought me all of the components",
                compact = { "§a⚙ §7All components brought" },
                sample = "§rYou've brought me all of the components!",
                title = "All components")
            rule("automaton-wrong", RuleAction.HIDE,
                "That's not one of the components I need!",
                sample = "§rThat's not one of the components I need! Bring me one of the missing components:",
                title = "Wrong component")
            // La liste des composants manquants : un nom nu par ligne, sous le préambule.
            rule("automaton-missing", RuleAction.HIDE,
                "^(?:FTX 3070|Electron Transmitter|Superlite Motor|Synthetic Heart|Control Switch|Robotron Reflector)$",
                sample = "  §r§9FTX 3070",
                title = "Missing component")
            rule("automaton-fixed", RuleAction.HIDE,
                "^Wait a minute\\. This will work just fine\\.",
                sample = "§rWait a minute. This will work just fine.",
                title = "Giant fixed")
        }
}
