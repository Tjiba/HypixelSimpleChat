package com.simplechat.engine

import com.simplechat.config.RuleConfig
import java.util.regex.Pattern

/** Textes saisis par le joueur dans « Custom hidden messages ». Recherche de sous-chaîne. Pur. */
object CustomHide {

    private val COLOR_ONLY = Pattern.compile("[§&][0-9a-fk-orA-FK-OR]")

    /** Retire les codes couleur/format en gardant l'espacement d'origine. */
    fun stripColors(raw: String): String = COLOR_ONLY.matcher(raw).replaceAll("")

    fun matches(raw: String, cfg: RuleConfig): Boolean {
        val text = stripColors(raw)
        return cfg.customHidePatterns.any { it.isNotBlank() && text.contains(it) }
    }
}
