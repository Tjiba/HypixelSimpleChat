package com.simplechat.ui

import com.simplechat.config.RuleSettings
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.Registry

/**
 * Plan du menu, construit depuis le registre de règles : onglet haut → section → ids de réglages.
 * Une catégorie absente d'ici s'affiche en liste plate. Pur, aucun import Minecraft.
 */
object MenuLayout {

    /** Ordre d'affichage des sections de l'onglet General ; une section inconnue passe en fin. */
    private val SECTION_ORDER = listOf("GENERAL", "WORLD & EVENTS", "COMBAT", "ECONOMY")

    /** Réglages qui ne viennent pas d'une règle : placés à la main, en tête de leur section. */
    private val EXTRAS = mapOf(
        "GENERAL" to listOf("enabled", "customPatterns"),
        "WORLD & EVENTS" to listOf("hoppity"),
        "Dungeons" to listOf("soloClass"),
    )

    val views: Map<String?, LinkedHashMap<String, LinkedHashMap<String, List<String>>>> = build()

    /** Groupes couvrant plusieurs phrases : id du groupe -> ids de ses phrases. */
    val bulk: Map<String, List<String>> = Registry.groups.mapNotNull { group ->
        RuleSettings.perPhrase(group).takeIf { it.isNotEmpty() }?.let { group.id to it.map { rule -> rule.id } }
    }.toMap()

    /** Phrase -> groupe auquel elle appartient (pour la replier avec lui). */
    val phraseOwner: Map<String, String> =
        bulk.flatMap { (groupId, ids) -> ids.map { it to groupId } }.toMap()

    /** Tous les réglages de règles d'une catégorie, pour la barre du haut de page. */
    val categoryBulk: Map<String, List<String>> = Registry.groups
        .groupBy { RuleSettings.configId(it.category) }
        .mapValues { (_, groups) ->
            groups.flatMap { group ->
                RuleSettings.perPhrase(group).map { it.id }.ifEmpty { listOf(group.id) }
            }
        }

    /** Titre et description de chaque groupe, pour la barre « tout mettre à ». */
    val titles: Map<String, Pair<String, String>> =
        Registry.groups.associate { it.id to (it.title to it.description) }

    private fun build(): Map<String?, LinkedHashMap<String, LinkedHashMap<String, List<String>>>> {
        val skyblock = Registry.groups.filter { it.category == Category.SKYBLOCK }

        val bySection = skyblock.filter { it.island == null }.groupBy { it.section }
        val general = LinkedHashMap<String, List<String>>()
        for (section in SECTION_ORDER + bySection.keys.filter { it !in SECTION_ORDER }) {
            val ids = EXTRAS[section].orEmpty() + bySection[section].orEmpty().flatMap { withPhrases(it) }
            if (ids.isNotEmpty()) general[section] = ids
        }

        // Une île = un onglet, sans en-tête de section : la page ne parle que d'elle.
        val tabs = linkedMapOf("General" to general)
        for ((island, groups) in skyblock.filter { it.island != null }.groupBy { it.island!! }) {
            tabs[island] = linkedMapOf("" to EXTRAS[island].orEmpty() + groups.flatMap { withPhrases(it) })
        }

        return mapOf("SkyBlock" to tabs)
    }

    /** Le réglage du groupe, puis un réglage par phrase qu'il couvre. */
    private fun withPhrases(group: Group): List<String> =
        listOf(group.id) + RuleSettings.perPhrase(group).map { it.id }
}
