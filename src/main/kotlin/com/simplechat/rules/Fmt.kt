package com.simplechat.rules

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

    /** Segment coloré du raw entre "found a/an " et [endMarker], codes couleur conservés. */
    fun rawItem(raw: String, endMarker: String): String? {
        val m = Regex("found an? ").find(raw) ?: return null
        val span = raw.substring(m.range.last + 1).substringBefore(endMarker)
        return span.replace(Regex("(?:§.|\\s)+$"), "").trim().ifEmpty { null }
    }
}
