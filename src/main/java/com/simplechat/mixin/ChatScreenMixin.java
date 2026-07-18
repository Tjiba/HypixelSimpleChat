package com.simplechat.mixin;

import com.simplechat.HscChatAccess;
import com.simplechat.IHscChat;
import com.simplechat.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Interactions chat : clic-droit = copie (+ « Copied! »), Ctrl+F = recherche live,
 * onglets All/Party/Guild au-dessus de la saisie (filtre OU routage d'envoi selon le réglage).
 * Tout est confort : require=0 partout — si un patch MC casse un injector, la feature se
 * désactive au lieu de crasher le jeu.
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    private static final long HSC_FEEDBACK_MS = 1000L;
    private static final String[] HSC_TABS = {"All", "Party", "Guild"};

    private static boolean hsc$searchMode;
    private static String hsc$applied;
    private static int hsc$matches = -1;
    private static int hsc$tab = 0; // 0=All,1=Party,2=Guild

    @Shadow
    protected EditBox input;

    /** Rectangles {x1,y1,x2,y2} des onglets — source unique pour le rendu ET le clic. */
    private int[][] hsc$tabRects() {
        Font font = Minecraft.getInstance().font;
        int[][] rects = new int[HSC_TABS.length][];
        int tx = input.getX();
        int ty = input.getY() - 14;
        for (int i = 0; i < HSC_TABS.length; i++) {
            int w = font.width(HSC_TABS[i]) + 8;
            rects[i] = new int[]{tx, ty, tx + w, ty + 12};
            tx += w + 3;
        }
        return rects;
    }

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void hsc$reset(String s, boolean b, CallbackInfo ci) {
        hsc$searchMode = false;
        hsc$applied = null;
        IHscChat c = HscChatAccess.current;
        if (c != null) {
            c.hsc$applySearch("");
            c.hsc$setChannelFilter(Settings.INSTANCE.getChatTabs() && Settings.INSTANCE.getTabFilterMode() ? hsc$tab : 0);
        }
    }

    // Routage d'envoi : en mode « send » (filtre off), préfixe le message selon l'onglet actif.
    @ModifyVariable(method = "handleChatInput", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private String hsc$routeSend(String message) {
        if (!Settings.INSTANCE.getChatTabs() || Settings.INSTANCE.getTabFilterMode()) return message;
        if (message == null || message.isEmpty() || message.startsWith("/")) return message;
        return switch (hsc$tab) {
            case 1 -> "/pc " + message;
            case 2 -> "/gc " + message;
            default -> message; // All : pas de routage
        };
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void hsc$onClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        // Clic gauche sur un onglet (si activés, cachés pendant une commande).
        if (event.button() == 0 && !hsc$searchMode && Settings.INSTANCE.getChatTabs() && !input.getValue().startsWith("/")) {
            int[][] rects = hsc$tabRects();
            for (int i = 0; i < rects.length; i++) {
                int[] r = rects[i];
                if (event.x() >= r[0] && event.x() <= r[2] && event.y() >= r[1] && event.y() <= r[3]) {
                    hsc$selectTab(i);
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
        // Clic droit = copie.
        if (event.button() == 1) {
            IHscChat chat = HscChatAccess.current;
            if (chat == null) return;
            GuiMessage msg = chat.hsc$messageAt(event.x(), event.y());
            if (msg == null) return;
            Minecraft.getInstance().keyboardHandler.setClipboard(msg.content().getString());
            HscChatAccess.copyTime = System.currentTimeMillis();
            HscChatAccess.copyX = (int) event.x();
            HscChatAccess.copyY = (int) event.y();
            cir.setReturnValue(true);
        }
    }

    private void hsc$selectTab(int i) {
        hsc$tab = i;
        IHscChat c = HscChatAccess.current;
        if (c != null) c.hsc$setChannelFilter(Settings.INSTANCE.getTabFilterMode() ? i : 0);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void hsc$searchKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        int key = event.key();
        boolean ctrl = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        if (key == GLFW.GLFW_KEY_F && ctrl) {
            hsc$searchMode = !hsc$searchMode;
            input.setValue("");
            hsc$applied = null;
            if (!hsc$searchMode) hsc$clearSearch();
            cir.setReturnValue(true);
            return;
        }
        if (hsc$searchMode && (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) { // sortir
            hsc$searchMode = false;
            input.setValue("");
            hsc$applied = null;
            hsc$clearSearch();
            cir.setReturnValue(true);
        }
    }

    private static void hsc$clearSearch() {
        IHscChat c = HscChatAccess.current;
        if (c != null) c.hsc$applySearch(""); // garde le filtre d'onglet, efface juste la recherche
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void hsc$overlay(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Font font = Minecraft.getInstance().font;

        // Feedback « Copied! ».
        long dt = System.currentTimeMillis() - HscChatAccess.copyTime;
        if (dt >= 0 && dt <= HSC_FEEDBACK_MS) {
            int alpha = dt > 600 ? (int) (255L * (HSC_FEEDBACK_MS - dt) / 400L) : 255;
            alpha = Math.max(0, Math.min(255, alpha));
            Component label = Component.literal("Copied!");
            int w = font.width(label);
            int x = Math.min(HscChatAccess.copyX + 8, Minecraft.getInstance().getWindow().getGuiScaledWidth() - w - 6);
            int y = Math.max(2, HscChatAccess.copyY - 14);
            gfx.fill(x - 3, y - 2, x + w + 3, y + font.lineHeight + 2, ((int) (alpha * 0.7) << 24));
            gfx.text(font, label, x, y, (alpha << 24) | 0x55FF55);
        }

        int ty = input.getY() - 14;
        // Rappel discret du raccourci recherche, aligné à droite au-dessus de la saisie.
        if (!hsc$searchMode) {
            String hint = "Ctrl+F to search";
            int hw = font.width(hint);
            int hx = input.getX() + input.getWidth() - hw - 2;
            gfx.fill(hx - 3, ty, hx + hw + 3, ty + 12, 0x60000000);
            gfx.text(font, Component.literal(hint), hx, ty + 2, 0x80B0B0B8);
        }
        if (hsc$searchMode) {
            // Barre de recherche (remplace les onglets).
            String q = input.getValue();
            if (!q.equals(hsc$applied)) {
                hsc$applied = q;
                IHscChat c = HscChatAccess.current;
                hsc$matches = c != null ? c.hsc$applySearch(q) : -1;
            }
            String label = q.isEmpty() ? "Search · type to filter"
                    : "Search · " + hsc$matches + (hsc$matches == 1 ? " match" : " matches");
            int w = font.width(label);
            gfx.fill(input.getX() - 2, ty - 2, input.getX() + w + 4, ty + font.lineHeight + 2, 0xC0000000);
            gfx.text(font, Component.literal("§b" + label), input.getX(), ty, 0xFFFFFFFF);
        } else if (Settings.INSTANCE.getChatTabs() && !input.getValue().startsWith("/")) {
            // Onglets (si activés, cachés pendant une commande). Fond léger.
            int[][] rects = hsc$tabRects();
            for (int i = 0; i < rects.length; i++) {
                int[] r = rects[i];
                boolean active = i == hsc$tab;
                gfx.fill(r[0], r[1], r[2], r[3], active ? 0xE84A5BD0 : 0xD8202028);
                gfx.text(font, Component.literal(HSC_TABS[i]), r[0] + 4, r[1] + 2, active ? 0xFFFFFFFF : 0xFFB0B0B8);
            }
        }
    }
}
