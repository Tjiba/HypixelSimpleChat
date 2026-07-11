package com.simplechat

import com.simplechat.ui.HscConfigScreen
import net.minecraft.client.gui.screens.Screen

/** 26.1: owo-based custom screen. RC stays the storage backend. */
object ConfigScreens {
    fun open(parent: Screen?): Screen? {
        val root = SimpleChatMod.config ?: return null
        return HscConfigScreen(parent, root)
    }
}
