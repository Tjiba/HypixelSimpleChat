package com.simplechat.engine

/**
 * Mémoire des lignes affichées pour le repli en (xN). Pure : le mixin fournit le texte réellement
 * affiché, elle ne répond qu'à « cette ligne, on l'a déjà comptée ». Une clé par ligne distincte :
 * le repli doit retrouver un message même si d'autres sont passés depuis. Pas de fenêtre de temps —
 * une ligne sortie du chat n'est plus retrouvable, le compteur repart de lui-même.
 * Les lignes vides — Hypixel en sème comme séparateurs — ne se comptent pas : un "(x13)" seul
 * sur une ligne blanche n'apprend rien et mange l'espacement.
 */
object Collapse {

    /** Autant de clés que l'historique peut tenir de lignes (limite haute de Max chat history). */
    private const val MAX_KEYS = 2048

    /** Dernière occurrence affichée : son texte dans le chat, et son compteur. */
    data class Seen(val count: Int, val rendered: String)

    private class Entry(var count: Int, var rendered: String)

    private val recent = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>) = size > MAX_KEYS
    }

    /** La dernière ligne affichée pour [key], ou null si elle n'a jamais été comptée. */
    fun seen(key: String): Seen? {
        if (key.isBlank()) return null
        val e = recent[key] ?: return null
        return Seen(e.count, e.rendered)
    }

    /** Note ce qui vient d'être affiché pour [key] : c'est lui qu'on ira retrouver au prochain doublon. */
    fun remember(key: String, rendered: String, count: Int) {
        if (key.isBlank()) return
        val e = recent[key]
        if (e == null) {
            recent[key] = Entry(count, rendered)
            return
        }
        e.count = count
        e.rendered = rendered
    }
}
