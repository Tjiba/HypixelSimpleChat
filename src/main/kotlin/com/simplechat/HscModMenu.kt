package com.simplechat

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen

/** Intégration Mod Menu (entrypoint chargé seulement si ModMenu est installé). */
class HscModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
        ConfigScreenFactory<Screen> { parent -> ConfigScreens.open(parent) ?: parent }
}
