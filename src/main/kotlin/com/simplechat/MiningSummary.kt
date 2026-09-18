package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import com.simplechat.rules.Fmt
import com.simplechat.rules.islands.Mining
import java.util.regex.Pattern

/**
 * Fusionne le pavé de récompenses d'un coffre des Crystal Hollows — deux barres de ▬, le titre du
 * coffre, l'en-tête REWARDS et une ligne par gain — en une seule ligne, posée à la place de la
 * barre de fin. État côté client (appelé par le mixin avant ChatRules) : les lignes arrivent en
 * messages séparés, une règle du registre n'en verrait jamais qu'une.
 */
object MiningSummary {

    // Sur le brut : c'est sa couleur qui distingue le pavé des coffres des autres pavés à ▬
    // d'Hypixel (bestiaire, cristaux du Nucleus), que clean() aplatit tous pareil.
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
    private var title: String? = null
    private var seen = 0
    private val gains = ArrayList<Pair<String, String>>()

    /** Verdict d'une ligne du pavé, ou null si elle n'en fait pas partie. `clean` = texte décoloré. */
    fun process(clean: String, raw: String, cfg: RuleConfig): Verdict? {
        if (CRYSTAL_WRAPPER.matcher(raw).matches()) {
            val crystal = cfg.groupActions[Mining.CRYSTAL.id] ?: Mining.CRYSTAL.default
            return if (crystal == RuleAction.OFF) null else Verdict.Hide
        }
        val action = cfg.groupActions[SETTING] ?: RuleAction.COMPACT
        if (action == RuleAction.OFF) return null

        if (WRAPPER.matcher(raw).matches()) {
            if (!active) {
                active = true; title = null; seen = 0; gains.clear()
                return Verdict.Hide
            }
            // Barre de fin : c'est elle qui porte le récap fusionné.
            return end(
                if (action == RuleAction.HIDE || gains.isEmpty()) Verdict.Hide
                else Verdict.Replace(line(action))
            )
        }
        if (!active) return null
        if (++seen > MAX_LINES) { active = false; return null }
        if (action == RuleAction.HIDE) return Verdict.Hide

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
                return Verdict.Hide
            }
        }
        // Ligne inattendue : on rend la main plutôt que d'avaler la suite du chat.
        active = false
        return null
    }

    private fun end(verdict: Verdict): Verdict {
        active = false; title = null; seen = 0; gains.clear()
        return verdict
    }

    private fun line(action: RuleAction): String {
        val body = gains.joinToString(" §8· ") { (name, amount) ->
            if (amount.isEmpty()) name else "$name §7x${Fmt.shortNum(amount)}"
        }
        val head = title ?: "§6§lCHEST"
        return if (action == RuleAction.GREY) "§8" + "$head $body".replace(Regex("§."), "")
        else "$head §r$body"
    }
}
