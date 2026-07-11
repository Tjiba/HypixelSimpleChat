package com.simplechat

import com.google.gson.JsonParser
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object Updater {
    @JvmField
    val CURRENT_VERSION: String = FabricLoader.getInstance()
        .getModContainer("simplechat")
        .map { it.metadata.version.friendlyString }.orElse("unknown")

    private val MC_VERSION: String? = FabricLoader.getInstance()
        .getModContainer("minecraft")
        .map { it.metadata.version.friendlyString }.orElse(null)

    private val API_URL = "https://api.modrinth.com/v2/project/hypixel-simple-chat/version" +
        (MC_VERSION?.let { "?loaders=%5B%22fabric%22%5D&game_versions=%5B%22$it%22%5D" } ?: "")

    private const val MODRINTH_PAGE = "https://modrinth.com/mod/hypixel-simple-chat"
    private var latest: String? = null
    private var notified = false

    fun init() {
        ClientPlayConnectionEvents.JOIN.register { _, _, client ->
            if (notified || !Settings.updateNotifications || !isHypixel(client)) return@register
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute {
                check().thenRun {
                    val v = latest ?: return@thenRun
                    if (notified || compareVersions(CURRENT_VERSION, v) >= 0) return@thenRun
                    notified = true
                    send(client, updateMessage())
                }
            }
        }
    }

    fun checkManually(client: Minecraft) {
        send(client, Component.literal(Msg.UPDATE_CHECKING.get()))
        latest = null
        check().thenRun {
            val v = latest
            if (v == null) {
                send(client, Component.literal(Msg.UPDATE_CHECK_FAILED.get())); return@thenRun
            }
            val cmp = compareVersions(CURRENT_VERSION, v)
            val msg = when {
                cmp < 0 -> updateMessage()
                cmp > 0 -> Component.literal(Msg.UPDATE_DEV_VERSION.format(CURRENT_VERSION, v))
                else -> Component.literal(Msg.UPDATE_UP_TO_DATE.format(CURRENT_VERSION))
            }
            send(client, msg)
        }
    }

    fun compareVersions(v1: String, v2: String): Int = try {
        val p1 = v1.split("."); val p2 = v2.split(".")
        var result = 0
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val n1 = if (i < p1.size) p1[i].toInt() else 0
            val n2 = if (i < p2.size) p2[i].toInt() else 0
            if (n1 < n2) { result = -1; break }
            if (n1 > n2) { result = 1; break }
        }
        result
    } catch (e: NumberFormatException) {
        SimpleChatMod.LOGGER.warn("Version comparison error: ${e.message}")
        0
    }

    private fun check(): CompletableFuture<Void> = CompletableFuture.runAsync {
        runCatching { fetchLatest()?.let { latest = it } }
            .onFailure { SimpleChatMod.LOGGER.error("Error checking version", it) }
    }

    private fun fetchLatest(): String? {
        val conn = URI(API_URL).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("User-Agent", "HypixelSimpleChat-Mod")
        if (conn.responseCode != 200) { conn.disconnect(); return null }
        val json = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        return JsonParser.parseString(json).asJsonArray.firstOrNull()
            ?.asJsonObject?.takeIf { it.has("version_number") }
            ?.get("version_number")?.asString
    }

    private fun isHypixel(client: Minecraft?): Boolean =
        client?.currentServer?.ip?.lowercase()?.contains("hypixel.net") == true

    private fun send(client: Minecraft?, msg: Component) {
        client?.execute { client.player?.sendSystemMessage(msg) }
    }

    private fun updateMessage(): Component =
        Component.literal("").append(prefix())
            .append(Component.literal(Msg.UPDATE_AVAILABLE.get()))
            .withStyle { it
                .withClickEvent(ClickEvent.OpenUrl(URI.create(MODRINTH_PAGE)))
                .withHoverEvent(HoverEvent.ShowText(Component.literal(MODRINTH_PAGE)))
            }

    private fun prefix(): MutableComponent = Component.literal("§6[§bSimple§fChat§6]")
}
