package com.simplechat

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

object Screens {
    fun set(mc: Minecraft, screen: Screen?) {
        val cls = Minecraft::class.java
        val m = runCatching { cls.getMethod("setScreenAndShow", Screen::class.java) }.getOrNull()
            ?: cls.getMethod("setScreen", Screen::class.java)
        m.invoke(mc, screen)
    }
}
