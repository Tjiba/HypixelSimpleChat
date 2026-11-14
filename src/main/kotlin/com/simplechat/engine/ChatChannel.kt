package com.simplechat.engine

object ChatChannel {
    fun visible(channel: Channel, tab: Int): Boolean = when (tab) {
        1 -> channel != Channel.PARTY && channel != Channel.GUILD && channel != Channel.OFFICER
        2 -> channel == Channel.PARTY
        3 -> channel == Channel.GUILD || channel == Channel.OFFICER
        else -> true
    }

    fun route(message: String, tabs: Boolean, tab: Int, allTabSendsToAllChat: Boolean): String {
        if (!tabs || message.isEmpty() || message.startsWith("/")) return message
        if (tab == 0) return if (allTabSendsToAllChat) "/ac $message" else message
        val prefix = when (tab) { 1 -> "/ac "; 2 -> "/pc "; 3 -> "/gc "; else -> "" }
        return prefix + message
    }
}
