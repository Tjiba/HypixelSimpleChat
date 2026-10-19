package com.simplechat.engine

/** Compte les messages reçus hors du canal affiché, pas les lignes créées par le reformatage. */
object UnreadChannels {
    private var party = 0
    private var guild = 0
    private var enabled = false
    private var selected = 0

    fun select(active: Boolean, tab: Int) {
        enabled = active
        selected = tab
        if (!active || tab == 0) { party = 0; guild = 0 }
        if (tab == 2) party = 0
        if (tab == 3) guild = 0
    }

    fun received(channel: Channel) {
        if (!enabled || selected == 0) return
        when (channel) {
            Channel.PARTY -> if (selected != 2) party = (party + 1).coerceAtMost(999)
            Channel.GUILD, Channel.OFFICER -> if (selected != 3) guild = (guild + 1).coerceAtMost(999)
            else -> {}
        }
    }

    fun count(tab: Int): Int = when (tab) { 2 -> party; 3 -> guild; else -> 0 }

    fun reset() { party = 0; guild = 0; enabled = false; selected = 0 }
}
