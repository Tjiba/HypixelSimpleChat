package com.simplechat.rules.common

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.Registry
import com.simplechat.rules.Rule
import com.simplechat.rules.rules

/** Quêtes slayer. La sonnerie Abiphone (« ✆ RING ») n'est pas là : elle est actionnable. */
object Slayer {

    val SLAYER = Group(
        id = "slayer",
        title = "Slayer",
        category = Category.SKYBLOCK,
        section = "COMBAT",
        default = RuleAction.HIDE,
        description = "Quest started/complete, slay lines",
    )

    val rules = rules(SLAYER) {
        rule("slayer-quest-started", RuleAction.HIDE,
            "^SLAYER QUEST STARTED!",
            compact = { "§5Slayer §7· started" },
            sample = "  §5§lSLAYER QUEST STARTED!",
            title = "Quest started")
        rule("slayer-quest-complete", RuleAction.HIDE,
            "^SLAYER QUEST COMPLETE!",
            compact = { "§5Slayer ${mark()}§a✔ §fcomplete" },
            sample = "  §5§lSLAYER QUEST COMPLETE!",
            title = "Quest complete")
        rule("slayer-objective", RuleAction.HIDE,
            "^» Slay ([\\d,]+) Combat XP worth of (.+?)\\.?$",
            compact = { last = brand(it[2]); "§7Slay §f${it[1]} XP §7of ${it[2]}" },
            sample = "  §5§l» §7Slay §c20,000 Combat XP §7worth of Zombies.",
            title = "Slay objective")
        rule("slayer-level", RuleAction.HIDE,
            "^(\\w+) Slayer LVL (\\d+) [-–—] Next LVL in ([\\d,]+) XP",
            compact = { levelLine(brand(it[1]), it[2], it[3]) },
            sample = "   §7Spider Slayer LVL 7 - §fNext LVL in §d256,961 XP!",
            title = "Level progress")
    }

    /** La marque du boss et sa couleur, déduites du mob de la quête : Hypixel dit "Zombies" dans
     *  l'objectif et "Zombie Slayer" à la montée de niveau, jamais "Revenant". La clé est le
     *  préfixe commun au singulier et au pluriel. */
    private val BRANDS = mapOf(
        "Zombie" to "§2Revenant",
        "Spider" to "§4Tarantula",
        "Wol" to "§eSven",
        "Enderm" to "§5Voidgloom",
        "Blaze" to "§4Inferno",
        "Vampire" to "§4Riftstalker",
    )

    private fun brand(mob: String) =
        BRANDS.entries.firstOrNull { mob.startsWith(it.key) }?.value ?: "§7$mob"

    // Marque de la quête en cours, posée par l'objectif : le "complete" arrive bien après lui,
    // il la reprend telle quelle.
    private var last: String? = null

    private fun mark() = last?.let { "$it " } ?: ""

    /** "Slayer · LVL 7 - Next LVL in 256,961 XP". [boss] saute quand la ligne du dessus le dit déjà. */
    private fun levelLine(boss: String?, lvl: String, xp: String) =
        "§5Slayer §7· ${boss?.plus(" ") ?: ""}§fLVL $lvl §8- §7Next LVL in §d$xp XP"

    private val STARTED = rules.first { it.id == "slayer-quest-started" }
    private val OBJECTIVE = rules.first { it.id == "slayer-objective" }
    private val COMPLETE = rules.first { it.id == "slayer-quest-complete" }
    private val LEVEL = rules.first { it.id == "slayer-level" }
    private var pending = false

    /**
     * Le "started" attend son objectif : seule cette ligne-là nomme le mob, donc le slayer. Il
     * ressort avec elle, au-dessus d'elle, nommé par la quête qui commence et jamais par celle
     * d'avant — les deux lignes tiennent dans un message, séparées d'un saut de ligne.
     * Appelé par le mixin avant le registre ; si l'un des deux réglages n'est pas COMPACT, les
     * règles reprennent la main ligne par ligne.
     */
    fun process(clean: String, cfg: RuleConfig): Verdict? {
        // Hypixel aère ses pavés : une ligne vide au milieu d'un bloc n'interrompt aucune attente.
        if (clean.isEmpty()) return null
        start(clean, cfg)?.let { return it }
        return level(clean, cfg)
    }

    private fun start(clean: String, cfg: RuleConfig): Verdict? {
        if (!merged(STARTED, cfg) || !merged(OBJECTIVE, cfg)) return drop()
        if (STARTED.match(clean, clean) != null) { pending = true; return Verdict.Hide }
        val m = OBJECTIVE.match(clean, clean) ?: return drop()
        last = brand(m[2])
        // Objectif sans "started" devant (relog, rappel d'Hypixel) : sa règle l'affiche normalement.
        if (!pending) return null
        pending = false
        val objective = OBJECTIVE.compact?.invoke(m) ?: clean
        return Verdict.Replace(ChatRules.theme("§5Slayer §7· ${mark()}§7started\n$objective", cfg))
    }

    /**
     * La ligne de niveau se recolle sous le "complete", qui la précède de peu — le compteur RNG
     * peut s'être glissé entre les deux. Le "complete" s'affiche normalement puis quitte le chat,
     * comme la ligne d'un lot Bazaar : rien n'est retenu, donc rien n'est perdu si la ligne de
     * niveau ne vient pas (slayer au max, déconnexion).
     */
    private fun level(clean: String, cfg: RuleConfig): Verdict? {
        COMPLETE.match(clean, clean)?.let { m ->
            capture = merged(COMPLETE, cfg)
            done = COMPLETE.compact?.invoke(m) ?: ""
            shown = null
            return null
        }
        val m = LEVEL.match(clean, clean) ?: return null
        // Aucun "complete" affiché juste avant : la règle de la ligne de niveau décide seule.
        stale = shown ?: return null
        shown = null
        return Verdict.Replace(ChatRules.theme(done + "\n" + levelLine(null, m[2], m[3]), cfg))
    }

    /** Ligne du "complete" à retirer du chat avant d'afficher la paire, une seule fois. */
    fun stale(): String? {
        val s = stale
        stale = null
        return s
    }

    /** Ce que le mixin vient d'afficher pour le "complete" : c'est elle que la paire remplace. */
    fun displayed(rendered: String) {
        if (!capture) return
        capture = false
        shown = rendered
    }

    /** Toute autre ligne lâche le "started" en attente : mieux vaut le perdre que le recoller
     *  à l'objectif d'une autre quête. */
    private fun drop(): Verdict? {
        pending = false
        return null
    }

    // Le "complete" affiché : sa ligne rendue, pour la retirer, et son compact, pour la reposer.
    private var capture = false
    private var shown: String? = null
    private var stale: String? = null
    private var done = ""

    private fun merged(rule: Rule, cfg: RuleConfig) =
        cfg.masterEnabled && cfg.skyblockEnabled && Registry.actionOf(rule, cfg) == RuleAction.COMPACT
}
