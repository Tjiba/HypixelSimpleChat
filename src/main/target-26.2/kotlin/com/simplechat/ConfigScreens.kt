package com.simplechat

import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen
import net.minecraft.client.gui.screens.Screen

/** 26.2: the config screen is Resourceful Config's own factory. */
object ConfigScreens {
    fun open(parent: Screen?): Screen? =
        ResourcefulConfigScreen.getFactory("simplechat").apply(parent)
}
