package com.simplechat.config

import com.google.gson.JsonObject
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.Registry
import com.simplechat.rules.Rule

/**
 * Les réglages du menu viennent du registre, pas d'une liste écrite à la main.
 * Un groupe qui couvre plusieurs phrases n'a pas de réglage à lui : chaque phrase porte le sien,
 * et le menu offre une barre pour toutes les mettre d'un coup.
 * Les ids sont les clés écrites dans le fichier de config.
 */
object RuleSettings {

    fun register() {
        for (group in Registry.groups) {
            val category = categoryOf(group.category)
            val phrases = perPhrase(group)
            if (phrases.isEmpty()) {
                // Groupe d'une seule règle : le groupe EST le réglage.
                category.addEnum(group.id, group.default, group.title, group.description)
                continue
            }
            for (rule in phrases) {
                category.addEnum(rule.id, rule.default, label(rule), ChatRules.clean(rule.sample))
            }
        }
    }

    /** Réglages des groupes d'une seule règle, par id de groupe. */
    fun groupActions(): Map<String, RuleAction> = buildMap {
        for (group in Registry.groups) {
            if (perPhrase(group).isNotEmpty()) continue
            put(group.id, categoryOf(group.category).entries[group.id]?.getEnum() as? RuleAction ?: group.default)
        }
    }

    /** Réglages des phrases, par id de règle. */
    fun overrides(): Map<String, RuleAction> = buildMap {
        for (group in Registry.groups) {
            val category = categoryOf(group.category)
            for (rule in perPhrase(group)) {
                put(rule.id, category.entries[rule.id]?.getEnum() as? RuleAction ?: rule.default)
            }
        }
    }

    /**
     * Config d'avant les réglages par phrase : un seul réglage par groupe. Chaque phrase encore
     * absente du fichier reprend la valeur du groupe, sinon le joueur repartirait sur les défauts.
     */
    fun migrate(root: JsonObject) {
        for (group in Registry.groups) {
            val phrases = perPhrase(group)
            if (phrases.isEmpty()) continue
            val saved = root.get(categoryOf(group.category).id) as? JsonObject ?: continue
            val legacy = saved.get(group.id)?.asString ?: continue
            val action = runCatching { RuleAction.valueOf(legacy) }.getOrNull() ?: continue
            for (rule in phrases) {
                if (saved.has(rule.id)) continue
                categoryOf(group.category).entries[rule.id]?.setEnum(action)
            }
        }
    }

    /** Phrases d'un groupe qui méritent leur propre réglage (aucune si le groupe n'en couvre qu'une
     *  ou s'il se règle d'un bloc). */
    fun perPhrase(group: Group): List<Rule> =
        Registry.byGroup[group].orEmpty().takeIf { group.split && it.size > 1 }.orEmpty()

    /** Nom de la catégorie dans le fichier de config ("Lobby", "SkyBlock", "System"). */
    fun configId(category: Category): String = categoryOf(category).id

    /** Libellé de la règle, ou à défaut son id lissé ("key-picked-up" -> "Key picked up"). */
    private fun label(rule: Rule) =
        rule.title ?: rule.id.replace('-', ' ').replaceFirstChar { it.uppercase() }

    private fun categoryOf(category: Category): HscCategory = when (category) {
        Category.LOBBY -> LobbyCleanup
        Category.SKYBLOCK -> SkyBlockCleanup
        Category.SYSTEM -> SystemCat
    }
}
