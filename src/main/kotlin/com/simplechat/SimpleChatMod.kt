package com.simplechat

import com.simplechat.config.Settings
import com.simplechat.ui.ConfigScreens
import com.simplechat.ui.Screens
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

object SimpleChatMod : ClientModInitializer {
    @JvmField val LOGGER = LoggerFactory.getLogger("simplechat")

    private var pendingConfig = false

    override fun onInitializeClient() {
        LOGGER.info(Msg.MOD_LOADED.get())
        Settings.load()
        Updater.init()

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            val root = dispatcher.register(
                ClientCommands.literal("hsc")
                    .then(ClientCommands.literal("update").executes {
                        Updater.checkManually(it.source.client); 1
                    })
                    .then(ClientCommands.literal("debug").executes { ctx ->
                        Debug.enabled = !Debug.enabled
                        val state = if (Debug.enabled) "§aon" else "§coff"
                        ctx.source.client.player?.sendSystemMessage(
                            Component.literal("§6[§bSimple§fChat§6] §7Debug log $state§7 — see logs/latest.log"))
                        1
                    })
                    .executes { pendingConfig = true; 1 }
            )
            // Brigadier distingue la casse : /HSC n'est pas /hsc. Tout-minuscule et tout-majuscule
            // couvrent les frappes réelles sans noyer la complétion. Un alias refusé ne doit pas
            // emporter /hsc avec lui.
            runCatching {
                for (alias in listOf("HSC", "gz", "GZ", "hypixelsimplechat")) {
                    dispatcher.register(
                        ClientCommands.literal(alias).executes { pendingConfig = true; 1 }.redirect(root)
                    )
                }
            }.onFailure { LOGGER.warn("Command aliases unavailable: {}", it.message) }
        }

        // Quitter le jeu avec le menu ouvert : removed() ne passe pas, la config resterait en RAM.
        ClientLifecycleEvents.CLIENT_STOPPING.register { Settings.save() }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!pendingConfig) return@register
            pendingConfig = false
            val screen = openConfig(null)
            if (screen != null) Screens.set(client, screen)
            else client.player?.sendSystemMessage(Component.literal("Failed to open the settings screen."))
        }
    }

    private fun openConfig(parent: Screen?): Screen? = runCatching {
        ConfigScreens.open(parent)
    }.onFailure { LOGGER.error("Failed to open config screen", it) }.getOrNull()
}
