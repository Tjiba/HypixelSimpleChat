package com.simplechat.engine

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.StringDecomposer

/** Convertit une liste de Seg (couleurs/format RGB) en Component Minecraft. Partagé preview + mixins. */
object SegRender {
    @JvmStatic
    fun highlight(message: Component, rule: PersonalRule): Component {
        if (rule.text.isBlank()) return message
        val plain = StringDecomposer.getPlainText(message)
        val highlighted = BooleanArray(plain.length)
        var match = plain.indexOf(rule.text, ignoreCase = !rule.caseSensitive)
        if (match < 0) return message
        while (match >= 0) {
            highlighted.fill(true, match, match + rule.text.length)
            match = plain.indexOf(rule.text, match + 1, ignoreCase = !rule.caseSensitive)
        }
        val out = Component.empty()
        val buf = StringBuilder()
        var currentStyle = Style.EMPTY
        var offset = 0

        fun flush() {
            if (buf.isNotEmpty()) {
                out.append(Component.literal(buf.toString()).withStyle(currentStyle))
                buf.setLength(0)
            }
        }

        // Hypixel mélange styles et codes § : le rendu vanilla résout les deux sans perdre les survols.
        StringDecomposer.iterateFormatted(message, Style.EMPTY) { _, style, codePoint ->
            var st = style
            if (highlighted[offset]) {
                st = st.withColor(rule.color)
                if (rule.bold) st = st.withBold(true)
                if (rule.italic) st = st.withItalic(true)
                if (rule.underlined) st = st.withUnderlined(true)
            }
            if (st != currentStyle) {
                flush()
                currentStyle = st
            }
            buf.appendCodePoint(codePoint)
            offset += Character.charCount(codePoint)
            true
        }
        flush()
        return out
    }

    @JvmStatic
    fun toComponent(segs: List<Seg>): Component {
        val out: MutableComponent = Component.empty()
        for (s in segs) {
            val part = Component.literal(s.text).withStyle { style ->
                var st = style
                s.color?.let { st = st.withColor(it) }
                if (s.bold) st = st.withBold(true)
                if (s.italic) st = st.withItalic(true)
                if (s.underlined) st = st.withUnderlined(true)
                if (s.strikethrough) st = st.withStrikethrough(true)
                if (s.obfuscated) st = st.withObfuscated(true)
                st
            }
            out.append(part)
        }
        return out
    }
}
