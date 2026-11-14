package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import com.simplechat.rules.Fmt
import java.util.regex.Pattern

/**
 * Fusionne le récap de fin de safari — en-tête, lignes vides et trois gains — en une seule ligne,
 * complétée à chaque gain. État côté client (appelé par le mixin avant ChatRules) :
 * les sept lignes arrivent en sept messages, une règle du registre n'en verrait jamais qu'une.
 */
object SafariSummary {

    private val HEADER = Pattern.compile("^SAFARI REWARD SUMMARY$")
    private val SEPARATOR = Pattern.compile("^[▬-]{8,}$")
    private val GAIN = Pattern.compile("^\\+([\\d,]+) (Shards?|Safari Essence|Hunting Exp)$")

    /** Id du réglage dans le menu ; le récap n'a pas de règle propre, il vit hors du registre. */
    const val SETTING = "foraging-safari-summary"

    private const val MAX_LINES = 8

    private var active = false
    private var seen = 0
    private val gains = LinkedHashMap<String, String>()
    private var shown: String? = null
    private var stale: String? = null
    private var pending = false

    fun trimsOpening(clean: String, cfg: RuleConfig): Boolean =
        cfg.masterEnabled && cfg.skyblockEnabled && cfg.groupActions[SETTING] != RuleAction.OFF
            && HEADER.matcher(clean).matches()

    fun isSeparator(clean: String): Boolean = SEPARATOR.matcher(clean).matches()

    /** Verdict du récap, ou null si la ligne n'en fait pas partie. `clean` = texte décoloré. */
    fun process(clean: String, cfg: RuleConfig): Verdict? {
        pending = false
        val action = cfg.groupActions[SETTING] ?: RuleAction.COMPACT
        if (!cfg.masterEnabled || !cfg.skyblockEnabled || action == RuleAction.OFF) {
            reset()
            return null
        }

        if (HEADER.matcher(clean).matches()) {
            reset(); active = true
            return Verdict.Hide
        }
        if (!active) return null
        if (++seen > MAX_LINES) { reset(); return null }

        if (clean.isEmpty()) return Verdict.Hide
        val g = GAIN.matcher(clean)
        if (g.matches()) {
            gains[g.group(2)] = g.group(1)
            if (action == RuleAction.HIDE) return Verdict.Hide
            stale = shown; pending = true
            return Verdict.Replace(line(action))
        }
        // Les gains sont déjà affichés ; la fin ferme le récap sans le déplacer.
        if (isSeparator(clean)) {
            reset()
            return Verdict.Hide
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
        active = false; seen = 0; gains.clear()
        shown = null; stale = null; pending = false
    }

    private fun line(action: RuleAction): String {
        val body = gains.entries.joinToString(" §8· ") { (what, amount) ->
            when (what) {
                "Safari Essence" -> "§f+$amount Essence"
                "Hunting Exp" -> "§b+${Fmt.shortNum(amount)} Hunting Exp"
                else -> "§9+$amount Shards"
            }
        }
        return if (action == RuleAction.GREY || action == RuleAction.COMPACT_GREY) "§8SAFARI · " + ChatRules.clean(body)
        else "§2§lSAFARI §r$body"
    }
}
