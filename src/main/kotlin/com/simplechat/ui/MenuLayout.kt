package com.simplechat.ui

import com.simplechat.config.RuleSettings
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.Registry
import com.simplechat.rules.Section
import com.simplechat.rules.Tab

/**
 * Plan du menu, construit depuis le registre de règles : onglet haut → section → ids de réglages.
 * Une catégorie absente d'ici s'affiche en liste plate. Pur, aucun import Minecraft.
 */
object MenuLayout {

    // Réglages qui ne viennent pas d'une règle : placés à la main, en tête de leur bloc. Une map
    // par endroit où on peut en poser — sections de l'onglet General, onglets, catégories sans
    // section — pour qu'une clé désigne toujours la même chose.
    private val GENERAL_EXTRAS: Map<Section, List<String>> = mapOf(
        Section.GENERAL to listOf("enabled", "customPatterns"),
        Section.WORLD to listOf("hoppity"),
        Section.ECONOMY to listOf("bazaarItemsColor", "bazaarSalesColor"),
    )
    private val TAB_EXTRAS: Map<Tab, List<String>> = mapOf(
        Tab.DUNGEONS to listOf("soloClass"),
    )
    private val CATEGORY_EXTRAS: Map<Category, List<String>> = mapOf(
        Category.LOBBY to listOf("enabled"),
        Category.SYSTEM to listOf("enabled"),
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

        val bySection = skyblock.filter { it.tab == null }.groupBy { it.section }
        val general = LinkedHashMap<String, List<String>>()
        for (section in Section.entries) {
            val ids = GENERAL_EXTRAS[section].orEmpty() + bySection[section].orEmpty().flatMap { withPhrases(it) }
            if (ids.isNotEmpty()) general[section.title] = ids
        }

        // Un contenu = un onglet. Une seule section dedans : pas d'en-tête, la page ne parle que
        // d'elle. Plusieurs : en-têtes, dans l'ordre de déclaration de Section.
        val tabs = linkedMapOf("General" to general)
        for (tab in Tab.entries) {
            val groups = skyblock.filter { it.tab == tab }
            if (groups.isEmpty()) continue
            val extras = TAB_EXTRAS[tab].orEmpty()
            val bySection = groups.groupBy { it.section }
            val page = LinkedHashMap<String, List<String>>()
            val sections = Section.entries.filter { it in bySection }
            if (sections.size == 1) page[""] = extras + groups.flatMap { withPhrases(it) }
            else sections.forEachIndexed { i, section ->
                page[section.title] = (if (i == 0) extras else emptyList()) +
                    bySection.getValue(section).flatMap { withPhrases(it) }
            }
            tabs[tab.title] = page
        }

        val views = linkedMapOf<String?, LinkedHashMap<String, LinkedHashMap<String, List<String>>>>(
            "SkyBlock" to tabs,
        )

        // Lobby et System n'ont pas de sections : un seul onglet, sans en-tête. Ils passent
        // quand même par ici pour avoir les barres de groupe et le repliage.
        for (category in listOf(Category.LOBBY, Category.SYSTEM)) {
            val groups = Registry.groups.filter { it.category == category }
            if (groups.isEmpty()) continue
            val ids = CATEGORY_EXTRAS[category].orEmpty() + groups.flatMap { withPhrases(it) }
            views[RuleSettings.configId(category)] = linkedMapOf("General" to linkedMapOf("" to ids))
        }

        return views
    }

    /** Le réglage du groupe, puis un réglage par phrase qu'il couvre. */
    private fun withPhrases(group: Group): List<String> =
        listOf(group.id) + RuleSettings.perPhrase(group).map { it.id }
}
