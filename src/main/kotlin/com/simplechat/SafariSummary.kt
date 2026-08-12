package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import com.simplechat.rules.Fmt
import java.util.regex.Pattern

/**
 * Fusionne le récap de fin de safari — en-tête, lignes vides et trois gains — en une seule ligne,
 * posée à la place du séparateur de fin. État côté client (appelé par le mixin avant ChatRules) :
 * les sept lignes arrivent en sept messages, une règle du registre n'en verrait jamais qu'une.
 */
object SafariSummary {

    private val HEADER = Pattern.compile("^SAFARI REWARD SUMMARY$")
    private val SEPARATOR = Pattern.compile("^[▬-]{8,}$")
    private val GAIN = Pattern.compile("^\\+([\\d,]+) (Shards?|Safari Essence|Hunting Exp)$")

    /** Id du réglage dans le menu ; le récap n'a pas de règle propre, il vit hors du registre. */
    const val SETTING = "foraging-safari-summary"

    private var active = false
    private val gains = LinkedHashMap<String, String>()

    /** Verdict du récap, ou null si la ligne n'en fait pas partie. `clean` = texte décoloré. */
    fun process(clean: String, cfg: RuleConfig): Verdict? {
        val action = cfg.groupActions[SETTING] ?: RuleAction.COMPACT
        if (action == RuleAction.OFF) return null

        if (HEADER.matcher(clean).matches()) {
            active = true; gains.clear()
            return Verdict.Hide
        }
        if (!active) return null
        if (action == RuleAction.HIDE) return if (SEPARATOR.matcher(clean).matches()) end(Verdict.Hide) else Verdict.Hide

        if (clean.isEmpty()) return Verdict.Hide
        val g = GAIN.matcher(clean)
        if (g.matches()) { gains[g.group(2)] = g.group(1); return Verdict.Hide }
        // Séparateur de fin : c'est lui qui porte le récap fusionné.
        if (SEPARATOR.matcher(clean).matches()) {
            return end(if (gains.isEmpty()) Verdict.Hide else Verdict.Replace(line(action)))
        }
        // Ligne inattendue : on rend la main plutôt que d'avaler la suite du chat.
        active = false
        return null
    }

    private fun end(verdict: Verdict): Verdict {
        active = false; gains.clear()
        return verdict
    }

    private fun line(action: RuleAction): String {
        val body = gains.entries.joinToString(" §8· ") { (what, amount) ->
            when (what) {
                "Safari Essence" -> "§f+$amount Essence"
                "Hunting Exp" -> "§b+${Fmt.shortNum(amount)} Hunting Exp"
                else -> "§9+$amount Shards"
            }
        }
        return if (action == RuleAction.GREY) "§8SAFARI · " + body.replace(Regex("§."), "")
        else "§2§lSAFARI §r$body"
    }
}
