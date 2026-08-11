package com.simplechat.rules

import com.simplechat.engine.RuleAction
import java.util.regex.Matcher
import java.util.regex.Pattern

/** Captures du pattern + textes d'origine, passés au compact. `it[1]` = premier groupe. */
class Match(private val m: Matcher, val clean: String, val raw: String) {
    operator fun get(i: Int): String = m.group(i) ?: ""
}

/**
 * Un message reconnu, déclaré d'un seul bloc : le pattern (sur texte nettoyé par ChatRules.clean),
 * sa version courte, son action par défaut et un exemple.
 * [compact] null = garder le message d'origine avec ses couleurs.
 * [sample] sert au test automatique de la règle et à l'aperçu du menu.
 * [title] libellé dans le menu ; null = dérivé de l'id (suffisant pour un groupe d'une seule règle).
 */
class Rule(
    val id: String,
    val group: Group,
    val default: RuleAction,
    private val pattern: Pattern,
    val compact: ((Match) -> String)?,
    val sample: String,
    val title: String?,
) {
    fun match(clean: String, raw: String): Match? {
        val m = pattern.matcher(clean)
        return if (m.find()) Match(m, clean, raw) else null
    }
}

/** Déclare les règles d'un groupe : `rules(GROUP) { rule(…); rule(…) }`. */
fun rules(group: Group, block: RuleSet.() -> Unit): List<Rule> = RuleSet(group).apply(block).list

class RuleSet(private val group: Group) {
    val list = ArrayList<Rule>()

    fun rule(
        id: String,
        default: RuleAction,
        regex: String,
        compact: ((Match) -> String)? = null,
        sample: String,
        title: String? = null,
    ) {
        list += Rule(id, group, default, Pattern.compile(regex), compact, sample, title)
    }
}
