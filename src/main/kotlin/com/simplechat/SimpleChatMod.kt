package com.simplechat

import com.mojang.brigadier.arguments.StringArgumentType
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
                    .then(ClientCommands.literal("treegift")
                        .then(ClientCommands.literal("reset").executes { ctx ->
                            TreeGiftTotals.reset()
                            ctx.source.client.player?.sendSystemMessage(
                                Component.literal("§6[§bSimple§fChat§6] §7Tree gift totals cleared"))
                            1
                        })
                        .executes { ctx ->
                            val player = ctx.source.client.player
                            for (line in TreeGiftTotals.report()) player?.sendSystemMessage(Component.literal(line))
                            1
                        })
                    .then(ClientCommands.literal("mute")
                        .then(ClientCommands.argument("name", StringArgumentType.word()).executes { ctx ->
                            muteFeedback(ctx.source.client, StringArgumentType.getString(ctx, "name"), true)
                            1
                        })
                        .executes { ctx ->
                            val muted = Settings.mutedList()
                            ctx.source.client.player?.sendSystemMessage(Component.literal(
                                if (muted.isEmpty()) "§6[§bSimple§fChat§6] §7Nobody is muted"
                                else "§6[§bSimple§fChat§6] §7Muted: §f" + muted.joinToString("§7, §f")))
                            1
                        })
                    .then(ClientCommands.literal("unmute")
                        .then(ClientCommands.argument("name", StringArgumentType.word()).executes { ctx ->
                            muteFeedback(ctx.source.client, StringArgumentType.getString(ctx, "name"), false)
                            1
                        }))
                    .then(ClientCommands.literal("debug").executes { ctx ->
                        Debug.enabled = !Debug.enabled
                        val state = if (Debug.enabled) "§aon" else "§coff"
                        ctx.source.client.player?.sendSystemMessage(
                            Component.literal("§6[§bSimple§fChat§6] §7Debug log $state§7, see logs/latest.log"))
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

    private fun muteFeedback(client: Minecraft, name: String, muted: Boolean) {
        val changed = Settings.setMuted(name, muted)
        if (changed) Settings.save()
        val state = if (muted) "§cmuted" else "§aunmuted"
        val verb = if (changed) "is now" else "was already"
        client.player?.sendSystemMessage(Component.literal("§6[§bSimple§fChat§6] §f$name §7$verb $state"))
    }

    private fun openConfig(parent: Screen?): Screen? = runCatching {
        ConfigScreens.open(parent)
    }.onFailure { LOGGER.error("Failed to open config screen", it) }.getOrNull()
}
