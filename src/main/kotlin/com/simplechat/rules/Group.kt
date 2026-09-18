package com.simplechat.rules

import com.simplechat.engine.RuleAction

/** Catégorie d'appartenance d'un groupe : porte le gate global (« Enable ») du menu. */
enum class Category { LOBBY, SKYBLOCK, SYSTEM }

/**
 * En-tête sous lequel un groupe s'affiche, et [title] tel qu'il est écrit à l'écran.
 * L'ordre de déclaration EST l'ordre du menu : il ne suit plus celui de [Registry.groups],
 * qui n'existe que pour l'évaluation des règles et bouge dès qu'un pattern doit passer avant
 * un autre. Un onglet ne montre que les sections dont il a des groupes.
 */
enum class Section(val title: String) {
    NONE(""),
    GENERAL("GENERAL"),
    WORLD("WORLD"),
    COMBAT("COMBAT"),
    // Ce qu'on reçoit (items) d'un côté, ce qui bouge des coins de l'autre.
    DROPS("DROPS"),
    ECONOMY("ECONOMY"),
    CRYSTAL_HOLLOWS("CRYSTAL HOLLOWS"),
    TORRHUS("TORRHUS"),
    HUNTING("HUNTING"),
    SAFARI("SAFARI"),
    DUNGEONS("DUNGEONS"),
}

/** Contenu assez fourni pour sa propre page. L'ordre de déclaration est celui des onglets. */
enum class Tab(val title: String) {
    DUNGEONS("Dungeons"),
    FORAGING("Foraging"),
    MINING("Mining"),
}

/**
 * Groupe de règles = un réglage dans le menu + sa clé dans le fichier de config.
 * [id] est écrit sur le disque : le renommer réinitialise le réglage chez les joueurs.
 * [section] et [tab] ne le sont pas : reclasser un groupe ne coûte rien.
 * [tab] null = onglet General (section [section]) ; sinon onglet dédié à ce contenu.
 * [split] false = un seul réglage pour toutes ses phrases, même s'il en couvre plusieurs.
 */
data class Group(
    val id: String,
    val title: String,
    val category: Category,
    val section: Section,
    val default: RuleAction,
    val description: String = "",
    val tab: Tab? = null,
    val split: Boolean = true,
)
