package com.simplechat.rules.islands

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Foraging : arbres, sève, drops de bûcheronnage. General d'abord, puis une zone par sous-catégorie. */
object Foraging {

    // Un groupe par message : chacun sa ligne de menu, sans barre ni repliage.
    val TREE_GIFT = Group("foraging-tree-gift", "Tree gift", Category.SKYBLOCK, "GENERAL", RuleAction.COMPACT,
        description = "Participation and rewards of a felled tree",
        tab = "Foraging")
    val FLOOR_DROP = Group("foraging-floor-drop", "Floor drop", Category.SKYBLOCK, "GENERAL", RuleAction.COMPACT,
        description = "Logs and drops found on the ground",
        tab = "Foraging")
    val PETALFALL = Group("foraging-petalfall", "Petalfall", Category.SKYBLOCK, "GENERAL", RuleAction.COMPACT,
        tab = "Foraging")
    val WOODPECKER = Group("foraging-woodpecker", "Woodpecker", Category.SKYBLOCK, "GENERAL", RuleAction.COMPACT,
        tab = "Foraging")
    val HONEY_TREE = Group("foraging-honey-tree", "Honey tree", Category.SKYBLOCK, "HUNTING", RuleAction.COMPACT,
        description = "A mob comes out of a honey tree",
        tab = "Foraging")
    val APPEARED = Group("foraging-appeared", "Mob appeared", Category.SKYBLOCK, "HUNTING", RuleAction.COMPACT,
        description = "Any other mob showing up",
        tab = "Foraging")
    val SHARDS = Group("foraging-shards", "Caught shards", Category.SKYBLOCK, "HUNTING", RuleAction.COMPACT,
        description = "Shards dropped by a hunted mob",
        tab = "Foraging")
    val HUNTING = Group("foraging-hunting", "Escaped mobs", Category.SKYBLOCK, "HUNTING", RuleAction.COMPACT,
        description = "Mobs lost on any zone, capsule included",
        tab = "Foraging", split = false)
    val BIRDFEEDER = Group("foraging-birdfeeder", "Birdfeeder", Category.SKYBLOCK, "HUNTING", RuleAction.COMPACT,
        description = "Birds attracted by the food you deposit",
        tab = "Foraging")
    val SAFARI_ENTRY = Group("foraging-safari-entry", "Safari entry", Category.SKYBLOCK, "SAFARI", RuleAction.COMPACT,
        description = "Players entering the Critter Safari",
        tab = "Foraging", split = false)
    val SAFARI_CAPTURE = Group("foraging-safari-capture", "Capture", Category.SKYBLOCK, "SAFARI", RuleAction.COMPACT,
        description = "Capsule thrown at a critter, and what it caught",
        tab = "Foraging", split = false)
    val SAFARI_MANAGER = Group("foraging-safari-manager", "Safari staff", Category.SKYBLOCK, "SAFARI", RuleAction.HIDE,
        description = "Manager and receptionist dialog, no information",
        tab = "Foraging", split = false)
    // Le récap est fusionné hors du registre (sept messages en un) : cette règle porte le réglage
    // et l'aperçu, le travail est fait par SafariSummary avant l'évaluation.
    val SAFARI_SUMMARY = Group("foraging-safari-summary", "Safari summary", Category.SKYBLOCK, "SAFARI", RuleAction.COMPACT,
        description = "End-of-run rewards, merged into one line",
        tab = "Foraging")
    val SAFARI_MILESTONES = Group("foraging-safari-milestones", "Safari milestones", Category.SKYBLOCK, "SAFARI", RuleAction.COMPACT,
        description = "Unclaimed milestones reminder",
        tab = "Foraging")
    val SAFARI_DISABLED = Group("foraging-safari-disabled", "Feature disabled", Category.SKYBLOCK, "SAFARI", RuleAction.GREY,
        description = "What the safari forbids",
        tab = "Foraging")
    val GALATEA = Group("foraging-galatea", "Trees", Category.SKYBLOCK, "GALATEA", RuleAction.HIDE,
        description = "Trees, sap, foraging drops",
        tab = "Foraging")
    // Le Beeheemoth est un seul événement raconté en plusieurs lignes : un réglage pour le tout.
    val TORRHUS = Group("foraging-torrhus", "Beeheemoth", Category.SKYBLOCK, "TORRHUS", RuleAction.COMPACT,
        description = "Spawn, progress, down",
        tab = "Foraging", split = false)
    val HIVE = Group("foraging-hive", "Hive", Category.SKYBLOCK, "TORRHUS", RuleAction.COMPACT,
        description = "Honeyhive search, loot, angry Honeybuzz",
        tab = "Foraging", split = false)

    val rules =
        rules(TREE_GIFT) {
            // Couleurs reprises du brut : l'arbre (Mangrove §9, Helix §b, Fig §d) et surtout le
            // pourcentage, rouge sous les 10% de participation, vert au-dessus.
            rule("tree-gift", RuleAction.COMPACT,
                "^(.+?) Tree Gift\\. You helped cut ([\\d.]+)% and gained ([\\d,]+) rewards!",
                compact = {
                    val tree = Fmt.rawColor(it.raw, "Tree Gift", "§9")
                    val pct = Fmt.rawColor(it.raw, "${it[2]}%", "§a")
                    val reward = Fmt.rawColor(it.raw, "${it[3]} rewards", "§e")
                    "$tree${it[1]} Tree Gift §8- $pct${it[2].removeSuffix(".0")}% §8| $reward${it[3]} rewards"
                },
                sample = "§9Mangrove Tree Gift. §7You helped cut §a100.0% §7and gained §e5 rewards§a!")
        } +
        rules(FLOOR_DROP) {
            // L'item garde sa couleur de rareté ; la quantité manque sur les drops uniques.
            rule("floor-drop", RuleAction.COMPACT,
                "^FLOOR DROP! You found (.+?)(?: x([\\d,]+))? on the ground!",
                compact = {
                    "${Fmt.rawColor(it.raw, "FLOOR DROP!", "§6")}§lFLOOR DROP! §r${Fmt.rawSpan(it.raw, it[1])}" +
                        if (it[2].isEmpty()) "" else " §7x${it[2]}"
                },
                sample = "§6§lFLOOR DROP! §fYou found Fig Log §7x512 §fon the ground!")
        } +
        rules(PETALFALL) {
            // Couleur reprise du brut : Hypixel colore ces cris hors palette vanilla.
            rule("petalfall", RuleAction.COMPACT,
                "^PETALFALL! You felled the entire Tree!",
                compact = { "${Fmt.rawColor(it.raw, "PETALFALL!", "§a")}§lPETALFALL!" },
                sample = "§#3BE63B§lPETALFALL! §r§fYou felled the entire §#3BE63BTree§f!")
        } +
        rules(WOODPECKER) {
            rule("woodpecker", RuleAction.COMPACT,
                "^WOODPECKER! You felled the entire Tree!",
                compact = { "${Fmt.rawColor(it.raw, "WOODPECKER!", "§a")}§lWOODPECKER!" },
                sample = "§#3BE63B§lWOODPECKER! §r§fYou felled the entire §#3BE63BTree§f!")
        } +
        rules(TORRHUS) {
            // Déclarée avant Combat dans le registre : le générique "… DOWN!" des boss l'avalerait.
            rule("beeheemoth-down", RuleAction.OFF,
                "^BEEHEEMOTH DOWN!",
                sample = "§6§lBEEHEEMOTH DOWN!",
                title = "Beeheemoth down")
            rule("beeheemoth-spawn", RuleAction.COMPACT,
                "^BEEHEEMOTH! A Beeheemoth has spawned at (.+)!$",
                compact = {
                    "${Fmt.rawColor(it.raw, "BEEHEEMOTH!", "§d")}§lBEEHEEMOTH! " +
                        "${Fmt.rawSpan(it.raw, it[1], "§a")}"
                },
                sample = "§dBEEHEEMOTH! §fA §dBeeheemoth §fhas spawned at §aMega Tree§f!",
                title = "Beeheemoth spawn")
            // Les trois stades disent où en est le mob : le compact garde le mot, pas la phrase.
            rule("beeheemoth-progress", RuleAction.COMPACT,
                "^BEEHEEMOTH! The Beeheemoth is (starting to slow down|getting more fatigued|exhausted)",
                compact = {
                    val head = "${Fmt.rawColor(it.raw, "BEEHEEMOTH!", "§d")}§lBEEHEEMOTH! "
                    head + when (it[1]) {
                        "starting to slow down" -> "§7slowing"
                        "getting more fatigued" -> "§efatigued"
                        else -> "§aexhausted"
                    }
                },
                sample = "§dBEEHEEMOTH! §fThe §dBeeheemoth §fis getting more fatigued!",
                title = "Beeheemoth progress")
        } +
        rules(HIVE) {
            // La ligne d'attente ne dit rien : ce qui compte arrive juste après. Compact vide pour
            // qu'elle disparaisse aussi quand le réglage du groupe est sur COMPACT.
            rule("hive-search", RuleAction.HIDE,
                "^You stick your hand into the honeyhive and feel around",
                compact = { "" },
                sample = "§7§oYou stick your hand into the honeyhive and feel around...",
                title = "Hive search")
            rule("hive-loot", RuleAction.COMPACT,
                "^HIVE! You found (.+?) x([\\d,]+) and (.+?)!*$",
                compact = {
                    "${Fmt.rawColor(it.raw, "HIVE!", "§6")}§lHIVE §r${Fmt.rawSpan(it.raw, it[1])} " +
                        "§7x${it[2]} §8+ ${Fmt.rawSpan(it.raw, it[3])}"
                },
                sample = "§6§lHIVE! §fYou found Honeycomb §7x21 §fand §aEnchanted Honeycomb§f!",
                title = "Hive loot")
            rule("hive-honeybuzz", RuleAction.COMPACT,
                "^A (.+?) appeared, angry at you for stealing its honey!",
                compact = { "${Fmt.rawSpan(it.raw, it[1])} appeared" },
                sample = "§7§oA Honeybuzz appeared, angry at you for stealing its honey!",
                title = "Angry Honeybuzz")
        } +
        rules(SAFARI_ENTRY) {
            // Hypixel encadre l'entrée de deux lignes de tirets, aplaties par clean().
            rule("safari-entered", RuleAction.COMPACT,
                "^-*\\s*(.+?) entered Critter Safari!",
                compact = { "§2Safari §8· ${Fmt.rawSpan(it.raw, it[1])} §aentered" },
                sample = "§7-----------------------------\n§b[MVP§c++§b] EnvoieTonSMIC §aentered Critter Safari!",
                title = "Player entered")
            rule("safari-joined", RuleAction.COMPACT,
                "^(?:. )?(.+?) joined the Critter Safari\\.",
                compact = { "§2Safari §8· ${Fmt.rawSpan(it.raw, it[1])} §ajoined" },
                sample = "§6ᛜ §bMeteoFrance §ejoined the Critter Safari.",
                title = "Player joined")
        } +
        rules(SAFARI_CAPTURE) {
            // Le jet ne dit rien de plus que la capture ou l'échappée qui suit, et se répète à
            // chaque capsule : compact vide pour qu'il disparaisse aussi sur COMPACT.
            rule("capsule-thrown", RuleAction.HIDE,
                "^You threw a .+? at the .+?!",
                compact = { "" },
                sample = "§7You threw a §cCritter Capsule §7at the §9Nozzlenose§7!",
                title = "Capsule thrown")
            // Le shard porte le nom du critter : la quantité suffit, et seulement au-delà d'un.
            rule("critter-caught", RuleAction.COMPACT,
                "^CAPTURE! You caught an? (.+?) and gained (?:an?|([\\d,]+)x) ",
                compact = {
                    "${Fmt.rawColor(it.raw, "CAPTURE!", "§a")}§lCAPTURE! §r${Fmt.rawSpan(it.raw, it[1])}" +
                        if (it[2].isEmpty()) "" else " §7x${it[2]}"
                },
                sample = "§a§lCAPTURE! §7You caught a §9Mantis Shrimp§7 and gained 2x §9Mantis Shrimp Shard§7!",
                title = "Critter caught")
        } +
        rules(SAFARI_MANAGER) {
            // Hypixel étiquette ces lignes "[NPC] " : sans l'étiquette dans le pattern, le dialogue
            // PNJ générique les prendrait et le réglage Safari staff ne servirait à rien.
            rule("safari-manager", RuleAction.HIDE,
                "^(?:\\[NPC] )?Safari Manager: ",
                compact = { Fmt.stripNpc(it.raw) },
                sample = "§e[NPC] §aSafari Manager§f: §rLooks good to me. Have fun out there!",
                title = "Safari Manager")
            // Le choix cliquable qui suit ("Select an option") n'est pas touché : lui porte l'action.
            rule("safari-receptionist", RuleAction.HIDE,
                "^(?:\\[NPC] )?Safari Receptionist: ",
                compact = { Fmt.stripNpc(it.raw) },
                sample = "§e[NPC] §aSafari Receptionist§f: §rWelcome to the Critter Safari!",
                title = "Safari Receptionist")
        } +
        rules(SAFARI_SUMMARY) {
            // Jamais atteinte en jeu : SafariSummary tranche avant. Elle existe pour le réglage
            // et pour montrer dans l'aperçu ce que la fusion produit.
            rule("safari-summary", RuleAction.COMPACT,
                "^SAFARI REWARD SUMMARY$",
                compact = { "§2§lSAFARI §r§9+14 Shards §8· §f+80 Essence §8· §b+24.4k Hunting Exp" },
                sample = "  §2§lSAFARI REWARD SUMMARY")
        } +
        rules(SAFARI_MILESTONES) {
            rule("safari-milestones", RuleAction.COMPACT,
                "^MILESTONES! You have unclaimed (.+?) Milestones!",
                compact = { "§2Milestones §7· ${Fmt.rawSpan(it.raw, it[1])} §7unclaimed" },
                sample = "§2§lMILESTONES! §aYou have unclaimed §2Safari§a Milestones! §6§lCLICK HERE §eto view them!")
        } +
        rules(SAFARI_DISABLED) {
            rule("safari-disabled", RuleAction.GREY,
                "^This feature is disabled in the Critter Safari!",
                sample = "§cThis feature is disabled in the Critter Safari!")
        } +
        rules(HONEY_TREE) {
            rule("honey-tree", RuleAction.COMPACT,
                "^HONEY TREE! (.+?) has appeared!",
                compact = {
                    "${Fmt.rawColor(it.raw, "HONEY TREE!", "§6")}§lHONEY TREE! §r${Fmt.rawSpan(it.raw, it[1])}"
                },
                sample = "§6§lHONEY TREE! §cWoodlouse §6has appeared!")
        } +
        // Après HONEY_TREE : la forme précise gagne. Ancrée en tête pour ne pas ramasser les
        // annonces à préfixe (« HOPPITY'S HUNT … has appeared! »), qui ont leur propre réglage.
        rules(APPEARED) {
            // Deux formes chez Hypixel : « A X has appeared! » et « - A wild X appeared! »
            // (chasse), cette dernière encadrée de tirets comme l'entrée du safari.
            rule("mob-appeared", RuleAction.COMPACT,
                "^-*\\s*An? (?:wild )?(.+?) (?:has )?appeared!",
                compact = { "${Fmt.rawSpan(it.raw, it[1])} appeared" },
                sample = "§eA §cWoodlouse §ehas appeared!")
        } +
        rules(SHARDS) {
            // Le passage entier est recopié : quantité, nom et « Shards » gardent leurs couleurs.
            rule("caught-shards", RuleAction.COMPACT,
                "^You caught (x[\\d,]+ .+? Shards?)!",
                compact = { Fmt.rawSpan(it.raw, it[1]) },
                sample = "§aYou caught §7x2 §aParched §aShards§a!")
        } +
        rules(BIRDFEEDER) {
            rule("bird-attracted", RuleAction.COMPACT,
                "^A (.+?) was attracted to the Birdfeeder!",
                compact = { "${Fmt.rawSpan(it.raw, it[1])} §7attracted" },
                sample = "§7A §aBluebird §7was attracted to the Birdfeeder!",
                title = "Bird attracted")
            rule("bird-food-full", RuleAction.GREY,
                "^You cannot deposit any more bird food",
                sample = "§cYou cannot deposit any more bird food. Wait for a bird to appear!",
                title = "Birdfeeder full")
            // Tim redonne le mode d'emploi du Birdfeeder à chaque passage devant lui.
            rule("birdwatcher-tim", RuleAction.HIDE,
                "^(?:\\[NPC] )?Birdwatcher Tim: ",
                compact = { Fmt.stripNpc(it.raw) },
                sample = "§e[NPC] §aBirdwatcher Tim§f: §rIf you place some food in it and wait a while, a Bird will show up!",
                title = "Birdwatcher Tim")
        } +
        rules(HUNTING) {
            // Tous les mobs de chasse ratés passent par là, chacun avec sa couleur.
            rule("mob-escaped", RuleAction.COMPACT,
                "^You (?:looked away|didn't reel|reeled too early)! (.+?) escaped!",
                compact = { "${Fmt.rawSpan(it.raw, "${it[1]} escaped")}" },
                sample = "§cYou looked away! §fGroundhog §cescaped!",
                title = "Escaped a catch")
            // Safari : la capsule ratée dit la même chose, autrement.
            rule("capsule-escaped", RuleAction.COMPACT,
                "^The (.+?) escaped your Critter Capsule!",
                compact = { Fmt.rawSpan(it.raw, "${it[1]} escaped") },
                sample = "§7The §9Nozzlenose §7escaped your Critter Capsule!",
                title = "Escaped the capsule")
        }

}
