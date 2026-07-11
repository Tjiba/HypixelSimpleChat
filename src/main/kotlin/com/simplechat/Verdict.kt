package com.simplechat

/** Résultat d'évaluation d'un message par ChatRules. Aucun type Minecraft. */
sealed interface Verdict {
    /** Laisser le message strictement inchangé. */
    data object Pass : Verdict
    /** Annuler l'affichage (ci.cancel). */
    data object Hide : Verdict
    /** Remplacer par une chaîne legacy (§-codée). */
    data class Replace(val legacy: String) : Verdict
    /** Remplacer par des segments RGB déjà construits (couleurs par canal). */
    data class Segments(val segs: List<Seg>) : Verdict
    /** Ligne courte affichée, message complet visible au survol (compact style SkyHanni). */
    data class Compact(val shortLegacy: String, val hoverLegacy: String) : Verdict
}

/** Action configurable par règle. Le collapse des répétitions est global (pas une action). */
enum class RuleAction { OFF, GREY, COMPACT, COMPACT_GREY, HIDE }

/** Action des groupes de spam bulk : pas de COMPACT_GREY (identique à GREY sans reformat). */
enum class HideAction { OFF, GREY, COMPACT, HIDE }

/** Segment de texte coloré/formaté produit par LegacyText, consommé par le mixin. */
data class Seg(
    @JvmField val text: String,
    @JvmField val color: Int?,
    @JvmField val bold: Boolean = false,
    @JvmField val italic: Boolean = false,
    @JvmField val underlined: Boolean = false,
    @JvmField val strikethrough: Boolean = false,
    @JvmField val obfuscated: Boolean = false,
)
