package com.simplechat;

/** Pont vers le ChatComponent actif : il s'y enregistre (cross-version, Gui.getChat() absent en 26.2). */
public final class HscChatAccess {
    public static IHscChat current;

    // Feedback « Copied! » : moment + position du dernier copier (dessiné par le ChatScreenMixin).
    public static long copyTime;
    public static int copyX;
    public static int copyY;

    private HscChatAccess() {}
}
