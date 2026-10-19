package com.simplechat.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.screens.Screen

object Screens {
    private val textInputFocus = runCatching {
        Minecraft::class.java.getMethod("onTextInputFocusChange", GuiEventListener::class.java, Boolean::class.javaPrimitiveType)
    }.getOrNull()

    fun textInputFocus(screen: Screen, focused: Boolean) {
        textInputFocus?.invoke(Minecraft.getInstance(), screen, focused)
    }

    fun set(mc: Minecraft, screen: Screen?) {
        val cls = Minecraft::class.java
        val m = runCatching { cls.getMethod("setScreenAndShow", Screen::class.java) }.getOrNull()
            ?: cls.getMethod("setScreen", Screen::class.java)
        m.invoke(mc, screen)
    }
}
