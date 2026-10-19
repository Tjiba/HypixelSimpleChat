package com.simplechat.engine

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/** Convertit une liste de Seg (couleurs/format RGB) en Component Minecraft. Partagé preview + mixins. */
object SegRender {
    @JvmStatic
    fun highlight(message: Component, rule: PersonalRule): Component {
        if (rule.text.isBlank()) return message
        val plain = CustomHide.stripColors(message.string)
        val highlighted = BooleanArray(plain.length)
        var match = plain.indexOf(rule.text, ignoreCase = !rule.caseSensitive)
        if (match < 0) return message
        while (match >= 0) {
            highlighted.fill(true, match, match + rule.text.length)
            match = plain.indexOf(rule.text, match + 1, ignoreCase = !rule.caseSensitive)
        }
        val out = Component.empty()
        var offset = 0
        message.visit({ style, text ->
            val clean = CustomHide.stripColors(text)
            var start = 0
            while (start < clean.length) {
                val selected = highlighted[offset + start]
                var end = start + 1
                while (end < clean.length && highlighted[offset + end] == selected) end++
                var st = style
                if (selected) {
                    st = st.withColor(rule.color)
                    if (rule.bold) st = st.withBold(true)
                    if (rule.italic) st = st.withItalic(true)
                    if (rule.underlined) st = st.withUnderlined(true)
                }
                out.append(Component.literal(clean.substring(start, end)).withStyle(st))
                start = end
            }
            offset += clean.length
            java.util.Optional.empty<Unit>()
        }, net.minecraft.network.chat.Style.EMPTY)
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
