package com.simplechat

/** Parse une chaîne legacy Minecraft (`§`) en segments. Table des 16 couleurs vanilla. */
object LegacyText {

    private val COLORS = mapOf(
        '0' to 0x000000, '1' to 0x0000AA, '2' to 0x00AA00, '3' to 0x00AAAA,
        '4' to 0xAA0000, '5' to 0xAA00AA, '6' to 0xFFAA00, '7' to 0xAAAAAA,
        '8' to 0x555555, '9' to 0x5555FF, 'a' to 0x55FF55, 'b' to 0x55FFFF,
        'c' to 0xFF5555, 'd' to 0xFF55FF, 'e' to 0xFFFF55, 'f' to 0xFFFFFF,
    )

    /** Code `§` de la couleur vanilla exacte, ou null si le RGB n'est pas dans la palette. */
    fun codeFor(rgb: Int): Char? = COLORS.entries.firstOrNull { it.value == (rgb and 0xFFFFFF) }?.key

    fun parse(input: String): List<Seg> {
        val out = ArrayList<Seg>()
        val buf = StringBuilder()
        var color: Int? = null
        var bold = false; var italic = false; var under = false; var strike = false; var obf = false

        fun flush() {
            if (buf.isNotEmpty()) {
                out.add(Seg(buf.toString(), color, bold, italic, under, strike, obf))
                buf.setLength(0)
            }
        }

        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if ((ch == '§' || ch == '&') && i + 1 < input.length) {
                val code = input[i + 1].lowercaseChar()
                flush()
                when (code) {
                    in COLORS -> { color = COLORS[code]; bold = false; italic = false; under = false; strike = false; obf = false }
                    'l' -> bold = true
                    'o' -> italic = true
                    'n' -> under = true
                    'm' -> strike = true
                    'k' -> obf = true
                    'r' -> { color = null; bold = false; italic = false; under = false; strike = false; obf = false }
                }
                i += 2
            } else {
                buf.append(ch); i++
            }
        }
        flush()
        return if (out.isEmpty()) listOf(Seg("", null)) else out
    }
}
