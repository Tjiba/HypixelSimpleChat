package com.simplechat

import com.simplechat.menu.MenuScreen
import net.minecraft.client.gui.screens.Screen

/** Menu custom 100% maison, commun aux deux cibles. Stockage : config/HscConfig (JSON Gson). */
object ConfigScreens {
    fun open(parent: Screen?): Screen? = MenuScreen(parent)
}
