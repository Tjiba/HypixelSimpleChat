package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import com.simplechat.rules.Fmt
import com.simplechat.rules.common.Drops
import com.simplechat.rules.islands.Mining
import java.util.regex.Pattern

/**
 * Fusionne le pavé de récompenses d'un coffre des Crystal Hollows — deux barres de ▬, le titre du
 * coffre, l'en-tête REWARDS et une ligne par gain — en une seule ligne, complétée à chaque gain.
 * État côté client (appelé par le mixin avant ChatRules) : les lignes arrivent en
 * messages séparés, une règle du registre n'en verrait jamais qu'une.
 */
object MiningSummary {

    // La couleur indique un coffre possible ; les collections partagent aussi ces barres.
    // Seul le titre qui suit confirme le coffre.
    // ComponentLegacy ouvre chaque segment par un §r : la couleur n'est jamais en tête de ligne.
    private val WRAPPER = Pattern.compile("^(?:§r)*§[ed]§l▬{16,}$")
    // Barres du "CRYSTAL FOUND" du Nucleus : les deux lignes du milieu ont leurs règles, les
    // barres n'ont que leur couleur pour se distinguer — d'où ici plutôt que dans le registre.
    private val CRYSTAL_WRAPPER = Pattern.compile("^(?:§r)*§5§l▬{16,}$")
    private val TITLE = Pattern.compile("^(?:CHEST LOCKPICKED|LOOT CHEST COLLECTED)$")
    private val HEADER = Pattern.compile("^REWARDS$")
    private val GAIN = Pattern.compile("^(.+?)(?: x([\\d,]+))?$")

    /** Au-delà, le pavé n'en est pas un : on rend la main plutôt que d'avaler le chat. */
    private const val MAX_LINES = 24

    /** Id du réglage dans le menu ; le récap n'a pas de règle propre, il vit hors du registre. */
    const val SETTING = "mining-chest-summary"

    private var active = false
    private var opening = false
    private var title: String? = null
    private var seen = 0
    private val gains = ArrayList<Pair<String, String>>()
    private var shown: String? = null
    private var stale: String? = null
    private var pending = false

    fun trimsOpening(clean: String, cfg: RuleConfig): Boolean =
        opening && cfg.masterEnabled && cfg.skyblockEnabled && cfg.groupActions[SETTING] != RuleAction.OFF
            && TITLE.matcher(clean).matches()

    /** Verdict d'une ligne du pavé, ou null si elle n'en fait pas partie. `clean` = texte décoloré. */
    fun process(clean: String, raw: String, cfg: RuleConfig): Verdict? {
        pending = false
        if (!cfg.masterEnabled || !cfg.skyblockEnabled) { reset(); return null }
        if (CRYSTAL_WRAPPER.matcher(raw).matches()) {
            val crystal = cfg.groupActions[Mining.CRYSTAL.id] ?: Mining.CRYSTAL.default
            return if (crystal == RuleAction.OFF) null else Verdict.Hide
        }
        val action = cfg.groupActions[SETTING] ?: RuleAction.COMPACT
        if (action == RuleAction.OFF) { reset(); return null }

        if (WRAPPER.matcher(raw).matches()) {
            if (!active) {
                reset(); opening = true
                return null
            }
            // Les gains sont déjà affichés ; la fin ferme le récap sans le déplacer.
            reset()
            return Verdict.Hide
        }
        if (!active) {
            val starts = opening && TITLE.matcher(clean).matches()
            opening = false
            if (!starts) return null
            active = true
        }
        if (++seen > MAX_LINES) { reset(); return null }

        // Même indentée, l'XP SkyBlock garde son réglage dédié hors du récap du coffre.
        if (Drops.isSkyblockXp(clean)) return null
        if (clean.isEmpty() || HEADER.matcher(clean).matches()) return Verdict.Hide
        if (TITLE.matcher(clean).matches()) {
            // "CHEST LOCKPICKED" -> "CHEST", "LOOT CHEST COLLECTED" -> "LOOT CHEST", couleur d'Hypixel.
            val name = clean.substringBefore(" LOCKPICKED").substringBefore(" COLLECTED")
            title = Fmt.rawColor(raw, "CHEST", "§6") + "§l" + name
            return Verdict.Hide
        }
        // Les gains sont les seules lignes indentées de quatre espaces.
        if (Fmt.indent(raw).length >= 4) {
            val m = GAIN.matcher(clean)
            if (m.matches()) {
                gains += Fmt.rawSpan(raw, m.group(1)) to (m.group(2) ?: "")
                if (action == RuleAction.HIDE) return Verdict.Hide
                stale = shown; pending = true
                return Verdict.Replace(line(action))
            }
        }
        // Ligne inattendue : on rend la main plutôt que d'avaler la suite du chat.
        reset()
        return null
    }

    fun isUpdating() = pending

    fun stale(): String? {
        val s = stale
        stale = null
        return s
    }

    fun displayed(rendered: String) {
        if (!pending) return
        pending = false
        shown = rendered
    }

    fun reset() {
        active = false; opening = false; title = null; seen = 0; gains.clear()
        shown = null; stale = null; pending = false
    }

    private fun line(action: RuleAction): String {
        val body = gains.joinToString(" §8· ") { (name, amount) ->
            if (amount.isEmpty()) name else "$name §7x${Fmt.shortNum(amount)}"
        }
        val head = title ?: "§6§lCHEST"
        return if (action == RuleAction.GREY || action == RuleAction.COMPACT_GREY) "§8" + ChatRules.clean("$head $body")
        else "$head §r$body"
    }
}
