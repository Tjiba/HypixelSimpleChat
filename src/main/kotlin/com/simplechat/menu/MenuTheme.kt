package com.simplechat.menu

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
