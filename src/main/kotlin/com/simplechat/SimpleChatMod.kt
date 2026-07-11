package com.simplechat

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

object SimpleChatMod : ClientModInitializer {
    @JvmField val LOGGER = LoggerFactory.getLogger("simplechat")
    @JvmField val configurator = Configurator("simplechat")

    /** Config RC enregistré (renvoyé par register) — source unique, évite de deviner l'id de config. */
    @JvmField var config: ResourcefulConfig? = null
    private var pendingConfig = false

    override fun onInitializeClient() {
        LOGGER.info(Msg.MOD_LOADED.get())
        config = Settings.register(configurator)
        Updater.init()

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("hsc")
                    .then(ClientCommands.literal("update").executes {
                        Updater.checkManually(it.source.client); 1
                    })
                    .executes { pendingConfig = true; 1 }
            )
        }

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
