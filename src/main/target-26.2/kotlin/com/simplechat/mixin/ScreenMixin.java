package com.simplechat.mixin;

import com.simplechat.IConfigAccess;
import com.simplechat.Preview;
import com.simplechat.RuleConfig;
import com.simplechat.Seg;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Dessine l'aperçu live du chat par-dessus l'écran RC de notre config (bandeau titre, à droite). */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void hsc$preview(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!(((Object) this) instanceof IConfigAccess access)) return;
        ResourcefulConfig config = access.hsc$config();
        if (config == null) return;

        Font font = Minecraft.getInstance().font;
        List<List<Seg>> lines = Preview.INSTANCE.forChannel(RuleConfig.Companion.current(), config.id());
        if (lines.isEmpty()) return;

        List<Component> comps = new ArrayList<>();
        for (List<Seg> segs : lines) comps.add(com.simplechat.SegRender.toComponent(segs));

        int contentW = 0;
        for (Component c : comps) contentW = Math.max(contentW, font.width(c));

        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int lineH = font.lineHeight + 1;
        int left = Math.max(4, width - 51 - contentW);
        // Bloc centré verticalement dans le bandeau (du haut jusqu'au début de la liste d'options).
        int bannerBottom = access.hsc$contentTop();
        int blockH = comps.size() * lineH;
        int top = Math.max(2, (bannerBottom - blockH) / 2);
        int y = top;
        for (Component c : comps) {
            g.text(font, c, left, y, 0xFFFFFFFF, true);
            y += lineH;
        }
    }
}
