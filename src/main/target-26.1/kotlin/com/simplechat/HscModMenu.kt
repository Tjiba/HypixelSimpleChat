package com.simplechat

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen

/** Intégration Mod Menu (26.1) : le bouton config ouvre l'écran owo, pas celui de RC. */
class HscModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory<Screen> { parent -> ConfigScreens.open(parent) ?: parent }
}
