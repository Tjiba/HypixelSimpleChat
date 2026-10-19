package com.simplechat.engine

object ChatChannel {
    fun route(message: String, tabs: Boolean, filtering: Boolean, tab: Int): String {
        if (!tabs || filtering || message.isEmpty() || message.startsWith("/")) return message
        val prefix = when (tab) { 0, 1 -> "/ac "; 2 -> "/pc "; 3 -> "/gc "; else -> "" }
        return prefix + message
    }
}
