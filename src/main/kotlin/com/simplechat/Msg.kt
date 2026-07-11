package com.simplechat

enum class Msg(val text: String) {
    MOD_LOADED("Hypixel Simple Chat loaded!"),
    UPDATE_AVAILABLE("§8: New update available, check on §2Modrinth."),
    UPDATE_CHECKING("§eChecking for updates..."),
    UPDATE_UP_TO_DATE("§aYou are using the latest version! §7(v%s)"),
    UPDATE_CHECK_FAILED("§cFailed to check for updates. Please check your internet connection."),
    UPDATE_DEV_VERSION("§aYou are using a development version! §7(current: v%s, latest stable: v%s)");

    fun get(): String = text
    fun format(vararg args: Any?): String = String.format(text, *args)
}
