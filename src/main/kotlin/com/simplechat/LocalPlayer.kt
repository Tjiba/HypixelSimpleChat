package com.simplechat

import net.minecraft.client.Minecraft

/** Le compte connecté, vu depuis le client. Vide tant qu'aucune session/partie n'est ouverte. */
object LocalPlayer {

    fun name(): String = runCatching { Minecraft.getInstance().user.name }.getOrDefault("")

    /**
     * Pseudo tel qu'Hypixel l'affiche ("§6[MVP§c++§6] non00w"), rang et couleurs compris.
     * La liste des joueurs le porte quand le serveur l'a envoyée, sinon le nom affiché en jeu
     * (au moins sa couleur d'équipe). Vide si aucune des deux ne parle de lui : sur SkyBlock la
     * liste sert aussi de tableau de bord, et recopier une de ses cases n'aurait aucun sens.
     */
    fun display(): String = runCatching {
        val name = name()
        if (name.isEmpty()) return ""
        val mc = Minecraft.getInstance()
        val connection = mc.connection
        // Par UUID de session, sinon par pseudo : sur SkyBlock la liste des joueurs sert aussi de
        // tableau de bord et l'entrée du joueur n'y porte pas toujours son UUID.
        val info = connection?.getPlayerInfo(mc.user.profileId)
            ?: connection?.getPlayerInfoIgnoreCase(name)
        val shown = info?.tabListDisplayName ?: mc.player?.displayName ?: return ""
        ComponentLegacy.of(shown).takeIf { it.contains(name) }.orEmpty()
    }.getOrDefault("")
}
