package com.simplechat.mixin;

import com.simplechat.ChatRules;
import com.simplechat.LegacyText;
import com.simplechat.RuleConfig;
import com.simplechat.Seg;
import com.simplechat.Verdict;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatHudMixin {

    private static final ThreadLocal<Boolean> HSC_REENTRANT = ThreadLocal.withInitial(() -> false);

    // Collapse global des répétitions : mémorise la dernière ligne affichée pour l'éditer avec (xN).
    private static String HSC_LAST_KEY = null;
    private static int HSC_LAST_COUNT = 0;
    private static long HSC_LAST_TIME = 0L;
    private static String HSC_LAST_RENDERED = null;

    @Invoker("addMessage")
    abstract void hsc$invokeAddMessage(Component message, MessageSignature signature,
                                       GuiMessageSource source, GuiMessageTag tag);

    @Accessor("allMessages")
    abstract List<GuiMessage> hsc$allMessages();

    @Invoker("refreshTrimmedMessages")
    abstract void hsc$refreshTrimmed();

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hsc$onAddMessage(Component original, MessageSignature signature,
                                  GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (HSC_REENTRANT.get() || original == null) return;

        RuleConfig cfg = RuleConfig.Companion.current();
        String legacy = hsc$toLegacy(original);
        String clean = ChatRules.INSTANCE.clean(legacy);
        Verdict v = com.simplechat.HoppityCompact.INSTANCE.process(clean, cfg.getCompactHoppity());
        if (v == null) v = ChatRules.INSTANCE.evaluate(legacy, cfg);

        if (v instanceof Verdict.Hide) { ci.cancel(); return; }

        // #2 : préserver les items/entités linkés -> ne pas reformater un message joueur qui en contient.
        if (v instanceof Verdict.Segments && hsc$hasItemLink(original)) {
            v = com.simplechat.Verdict.Pass.INSTANCE;
        }

        // #3 : collapse intelligent (normalise les nombres) pour les messages système reformatés.
        boolean smart = cfg.getSmartCollapse() && (v instanceof Verdict.Replace);
        String key = smart ? ChatRules.INSTANCE.collapseKey(clean) : clean;
        Component base;
        if (v instanceof Verdict.Segments sv) {
            base = buildSegs(sv.getSegs());
        } else if (v instanceof Verdict.Compact cv) {
            Component hover = build(cv.getHoverLegacy());
            base = Component.empty().append(build(cv.getShortLegacy()))
                    .withStyle(s -> s.withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(hover)));
        } else if (v instanceof Verdict.Replace rv) {
            base = build(rv.getLegacy());
        } else {
            base = original; // Pass
        }
        boolean untouched = v instanceof Verdict.Pass;
        hsc$display(base, key, untouched, cfg, signature, source, tag, ci);
    }

    /** Affiche [base] ; si identique à la dernière ligne dans la fenêtre, l'édite avec un compteur (xN). */
    private void hsc$display(Component base, String key, boolean untouched, RuleConfig cfg,
                             MessageSignature sig, GuiMessageSource src, GuiMessageTag tag, CallbackInfo ci) {
        long now = System.currentTimeMillis();
        long window = cfg.getDedupWindowMs();
        List<GuiMessage> all = null;
        try { all = hsc$allMessages(); } catch (Throwable ignored) {}

        boolean canCollapse = window > 0 && all != null && !all.isEmpty()
                && key.equals(HSC_LAST_KEY) && (now - HSC_LAST_TIME) < window
                && HSC_LAST_RENDERED != null
                && all.get(0).content().getString().equals(HSC_LAST_RENDERED);

        if (canCollapse) {
            boolean edited = false;
            try { all.remove(0); hsc$refreshTrimmed(); edited = true; } catch (Throwable ignored) {}
            if (edited) {
                HSC_LAST_COUNT++;
                Component disp = withTimestamp(hsc$withCount(base, HSC_LAST_COUNT), cfg);
                HSC_LAST_TIME = now;
                HSC_LAST_RENDERED = disp.getString();
                reAdd(disp, sig, src, tag, ci);
                return;
            }
            // Édition impossible -> repli en ajout normal ci-dessous (sans compteur).
        }

        HSC_LAST_KEY = key;
        HSC_LAST_COUNT = 1;
        HSC_LAST_TIME = now;

        // Message intact + pas de timestamp : laisser MC l'ajouter tel quel. Ne PAS annuler/ré-ajouter,
        // sinon les autres mods injectant sur addMessage traitent chaque ligne en double.
        if (untouched && !cfg.getShowTimestamps()) {
            HSC_LAST_RENDERED = base.getString();
            return;
        }

        Component disp = withTimestamp(base, cfg);
        HSC_LAST_RENDERED = disp.getString();
        reAdd(disp, sig, src, tag, ci);
    }

    private static Component hsc$withCount(Component base, int count) {
        return Component.empty().append(base)
                .append(Component.literal(" (x" + count + ")").withStyle(s -> s.withColor(0x555555)));
    }

    /** true si un style porte un item/entité linké (ShowItem/ShowEntity). On ignore les ShowText
     *  (hover de rang sur les pseudos) qui n'ont pas de valeur à préserver quand on reformate. */
    private static boolean hsc$hasItemLink(Component c) {
        return c.visit((style, text) -> {
            net.minecraft.network.chat.HoverEvent h = style.getHoverEvent();
            if (h instanceof net.minecraft.network.chat.HoverEvent.ShowItem
                    || h instanceof net.minecraft.network.chat.HoverEvent.ShowEntity) {
                return java.util.Optional.of(Boolean.TRUE);
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY).isPresent();
    }

    /** Convertit le Component (dont les couleurs peuvent être dans le Style, pas en §) en chaîne legacy §. */
    private static String hsc$toLegacy(Component c) {
        StringBuilder sb = new StringBuilder();
        c.visit((style, text) -> {
            hsc$appendStyle(sb, style);
            sb.append(text);
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return sb.toString();
    }

    private static void hsc$appendStyle(StringBuilder sb, net.minecraft.network.chat.Style style) {
        sb.append("§r");
        net.minecraft.network.chat.TextColor col = style.getColor();
        if (col != null) {
            Character code = LegacyText.INSTANCE.codeFor(col.getValue() & 0xFFFFFF);
            if (code != null) sb.append('§').append(code.charValue());
        }
        if (style.isBold()) sb.append("§l");
        if (style.isItalic()) sb.append("§o");
        if (style.isUnderlined()) sb.append("§n");
        if (style.isStrikethrough()) sb.append("§m");
        if (style.isObfuscated()) sb.append("§k");
    }

    private static final java.time.format.DateTimeFormatter TS_FMT =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    private static Component withTimestamp(Component base, RuleConfig cfg) {
        if (!cfg.getShowTimestamps()) return base;
        int col = cfg.getTimestampColor();
        String t = java.time.LocalTime.now().format(TS_FMT);
        return Component.empty()
                .append(Component.literal("[" + t + "] ").withStyle(s -> s.withColor(col)))
                .append(base);
    }

    private void reAdd(Component newComp, MessageSignature signature,
                       GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        HSC_REENTRANT.set(true);
        try {
            hsc$invokeAddMessage(newComp, signature, source, tag);
        } finally {
            HSC_REENTRANT.set(false);
        }
        ci.cancel();
    }

    private static Component build(String legacy) {
        return buildSegs(LegacyText.INSTANCE.parse(legacy));
    }

    private static Component buildSegs(List<Seg> segs) {
        return com.simplechat.SegRender.toComponent(segs);
    }
}
