package com.simplechat

import java.util.regex.Pattern

/**
 * Règles "compact + survol" (style SkyHanni) : raccourcissent un message, l'original complet
 * reste visible au survol. Pur. S'exécutent dans la branche SYSTEM, avant le hide-useless.
 */
object CompactRules {

    private val SOLO_CLASS = Pattern.compile(
        "^Your (Healer|Mage|Berserk|Archer|Tank) stats are doubled because you are the only player using this class!")

    fun compact(clean: String, cfg: RuleConfig): Verdict? {
        if (cfg.compactSoloClass) {
            val m = SOLO_CLASS.matcher(clean)
            if (m.find()) {
                val cls = m.group(1)
                return Verdict.Compact("§6$cls stats doubled §7(solo)", "§7$clean")
            }
        }
        return null
    }
}
