package com.simplechat.rules

import com.simplechat.engine.LegacyText

/** Petits formatteurs partagés par les compacts. Purs. */
object Fmt {

    /** "2,637,430.3" -> "2.6M". Abrège les gros nombres pour les lignes compactes. */
    fun shortNum(raw: String): String {
        val n = raw.replace(",", "").substringBefore(".").toLongOrNull() ?: return raw
        val l = java.util.Locale.ROOT
        return when {
            n >= 1_000_000_000 -> String.format(l, "%.1fB", n / 1_000_000_000.0)
            n >= 1_000_000 -> String.format(l, "%.1fM", n / 1_000_000.0)
            n >= 1_000 -> String.format(l, "%.1fk", n / 1_000.0)
            else -> n.toString()
        }
    }

    /** "three" -> "3h". Hypixel écrit la durée des boosters en toutes lettres. */
    fun wordToHours(word: String): String = when (word.lowercase()) {
        "one" -> "1h"; "two" -> "2h"; "three" -> "3h"; "four" -> "4h"
        "five" -> "5h"; "six" -> "6h"; else -> word.toIntOrNull()?.let { "${it}h" } ?: word
    }

    /** Retire les articles de tête des sources ("The Mage's Magma", "Your fairy"). */
    fun src(s: String) = s.removePrefix("The ").removePrefix("Your ").trim()

    /** Retire l'étiquette "[NPC] " de tête : le dialogue garde son nom et ses couleurs. */
    fun stripNpc(raw: String) = raw.replaceFirst(Regex("\\[NPC]\\s*"), "")

    /** Le décalage d'origine de la ligne, que clean() aplatit : à remettre devant un compact pour
     *  qu'il garde sa place dans le pavé d'Hypixel plutôt que de se coller au bord du chat. */
    fun indent(raw: String) = raw.replace(Regex(LegacyText.CODE), "").takeWhile { it == ' ' }

    /**
     * Couleur qui teinte [marker] dans le message brut, [fallback] à défaut. Le gras et compagnie
     * sont tolérés entre les deux, pas une autre couleur : c'est la plus proche qui gagne.
     * Sert aux compacts qui recopient un mot du message — il garde alors la couleur d'Hypixel.
     */
    fun rawColor(raw: String, marker: String, fallback: String = "§f"): String =
        Regex("(${LegacyText.COLOR})(?:${LegacyText.FORMAT}|[^§])*${Regex.escape(marker)}")
            .find(raw)?.groupValues?.get(1) ?: fallback

    /**
     * Le passage du brut qui porte [text], sa couleur de tête et ses codes internes conservés.
     * Un nom de joueur y garde son rang entier ("§b[MVP§c+§b] Timo"), que [rawColor] aplatirait.
     */
    fun rawSpan(raw: String, text: String, fallback: String = "§f"): String {
        val escaped = text.map { Regex.escape(it.toString()) }.joinToString("(?:${LegacyText.CODE})*")
        val span = Regex(escaped).find(raw) ?: return fallback + text
        val lead = Regex("(${LegacyText.COLOR})(?:${LegacyText.FORMAT}|[^§])*$")
            .find(raw.substring(0, span.range.first))?.groupValues?.get(1) ?: fallback
        return lead + span.value
    }

    /** Segment coloré du raw entre "found a/an " et [endMarker], codes couleur conservés. */
    fun rawItem(raw: String, endMarker: String): String? {
        val m = Regex("found an? ").find(raw) ?: return null
        val span = raw.substring(m.range.last + 1).substringBefore(endMarker)
        return span.replace(Regex("(?:${LegacyText.CODE}|\\s)+$"), "").trim().ifEmpty { null }
    }
}
