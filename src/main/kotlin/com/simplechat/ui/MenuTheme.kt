package com.simplechat.ui

/** Palette et métriques du menu custom (verre translucide moderne). Couleurs ARGB. */
internal object MenuTheme {
    const val DIM = 0x66000000              // léger voile en plus du blur
    const val GLASS = 0xD8161620.toInt()    // fenêtre en verre (sur fond flou)
    const val GLASS_BORDER = 0x40FFFFFF     // liseré clair subtil
    const val CARD = 0x14FFFFFF             // fond de panneau (blanc très léger)

    const val ACCENT = 0xFF4A5BD0.toInt()   // blurple (actif)
    const val ACCENT_SOFT = 0x804A5BD0.toInt()

    const val NAV_HOVER = 0x22FFFFFF
    const val FIELD = 0x22FFFFFF
    const val FIELD_HOVER = 0x38FFFFFF
    const val SUBTAB_IDLE = 0x18FFFFFF
    const val TOGGLE_OFF = 0x66000000     // case décochée : sombre (pas blanc)
    const val SCROLL_THUMB = 0x66FFFFFF

    const val TEXT = 0xFFECECF0.toInt()
    const val TEXT_DIM = 0xFFB9B9C4.toInt()
    const val TEXT_FAINT = 0xFF8A8A96.toInt()
    const val TEXT_TITLE = 0xFFFFFFFF.toInt()

    // Les 16 codes couleur de Minecraft n'ont ni vert forêt ni terre cuite : teintes en dur.
    private const val FORAGING = 0xFF2E7D32.toInt()   // vert foncé
    private val TAB_ACCENT = mapOf("Foraging" to FORAGING)
    private val SECTION_TINT = mapOf(
        "Foraging/GENERAL" to FORAGING,
        "Foraging/GALATEA" to 0xFF4C9A57.toInt(),   // vert forêt
        "Foraging/TORRHUS" to 0xFFC1663F.toInt(),   // canyon
    )

    /** Couleur de l'onglet actif : blurple par défaut, teinte propre pour certains contenus. */
    fun tabAccent(tab: String?): Int = TAB_ACCENT[tab] ?: ACCENT

    /** Couleur d'un en-tête de section : gris par défaut, teinte propre pour certaines zones. */
    fun sectionTint(tab: String?, title: String): Int = SECTION_TINT["$tab/$title"] ?: TEXT_FAINT

    const val PAD = 12
    const val GAP = 8
    const val TITLE_H = 24
    const val FOOTER_H = 22
    const val SIDEBAR_W = 112
    const val NAV_H = 22
    const val ROW_H = 20
    const val WIDGET_W = 96
    const val WIDGET_H = 15
}
