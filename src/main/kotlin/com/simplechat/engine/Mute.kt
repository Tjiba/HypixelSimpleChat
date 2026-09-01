package com.simplechat.engine

import com.simplechat.config.RuleConfig

/** Pseudos mis en sourdine par le joueur : leurs messages sont masqués. Pur. */
object Mute {

    /** Vrai si l'auteur du message est dans la liste. Un pseudo relayé par le bridge porte souvent
     *  des décorations (✿Nom✿) : une entrée contenue dans le pseudo suffit. */
    fun matches(clean: String, channel: Channel, cfg: RuleConfig): Boolean {
        if (cfg.mutedPlayers.isEmpty()) return false
        val author = ChannelFormat.author(clean, channel) ?: return false
        return cfg.mutedPlayers.any { it.isNotBlank() && author.contains(it, ignoreCase = true) }
    }
}
