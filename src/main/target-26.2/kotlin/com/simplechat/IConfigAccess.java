package com.simplechat;

import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;

/** Exposé par ConfigScreenMixin pour lire le config + la hauteur du bandeau depuis ScreenMixin. */
public interface IConfigAccess {
    ResourcefulConfig hsc$config();

    /** Y du haut de la liste d'options = bas du bandeau de titre (pour caler la preview dedans). */
    int hsc$contentTop();
}
