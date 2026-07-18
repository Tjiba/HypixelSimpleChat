package com.simplechat;

import net.minecraft.client.multiplayer.chat.GuiMessage;

/** Implémenté par ChatComponent (via mixin) : expose le message sous le curseur. */
public interface IHscChat {
    /** Message affiché sous (mouseX, mouseY) en coords écran, ou null. */
    GuiMessage hsc$messageAt(double mouseX, double mouseY);

    /** Filtre d'onglet : 0=All, 1=Party, 2=Guild(+Officer). Combiné à la recherche courante. */
    void hsc$setChannelFilter(int tabCode);

    /** Applique une recherche (tokens AND, insensible casse), combinée au filtre d'onglet.
     *  Renvoie le nombre de correspondances, ou -1 si aucun filtre actif. */
    int hsc$applySearch(String query);
}
