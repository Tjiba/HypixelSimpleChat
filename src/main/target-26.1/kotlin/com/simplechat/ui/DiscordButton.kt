package com.simplechat.ui

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.UIComponents
import net.minecraft.client.gui.screens.ConfirmLinkScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object DiscordButton {
    private const val URL = "https://discord.gg/6y35KkQ7h6"

    private const val BLURPLE = 0xFF5865F2.toInt()
    private const val BLURPLE_HOVER = 0xFF6D79FF.toInt()

    /** Bouton « Discord » (plat blurple) ouvrant l'invite via l'écran de confirmation vanilla. */
    fun create(screen: Screen): ButtonComponent =
        UIComponents.button(Component.literal("§fDiscord")) {
            ConfirmLinkScreen.confirmLinkNow(screen, URL)
        }.renderer(ButtonComponent.Renderer.flat(BLURPLE, BLURPLE_HOVER, BLURPLE))
}
