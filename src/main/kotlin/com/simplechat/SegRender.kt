package com.simplechat

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/** Convertit une liste de Seg (couleurs/format RGB) en Component Minecraft. Partagé preview + mixins. */
object SegRender {
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
