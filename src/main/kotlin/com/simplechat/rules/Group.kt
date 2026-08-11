package com.simplechat.rules

import com.simplechat.engine.RuleAction

/** Catégorie d'appartenance d'un groupe : porte le gate global (« Enable ») du menu. */
enum class Category { LOBBY, SKYBLOCK, SYSTEM }

/**
 * Groupe de règles = un réglage dans le menu + sa clé dans le fichier de config.
 * [id] est écrit sur le disque : le renommer réinitialise le réglage chez les joueurs.
 * [island] null = onglet General (section [section]) ; sinon page de l'île dans l'onglet Islands.
 */
data class Group(
    val id: String,
    val title: String,
    val category: Category,
    val section: String,
    val default: RuleAction,
    val description: String = "",
    val island: String? = null,
)
