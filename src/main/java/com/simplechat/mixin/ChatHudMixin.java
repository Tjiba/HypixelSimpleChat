package com.simplechat.mixin;

import com.simplechat.ChatRules;
import com.simplechat.HscChatAccess;
import com.simplechat.IHscChat;
import com.simplechat.LegacyText;
import com.simplechat.RuleConfig;
import com.simplechat.Seg;
import com.simplechat.Verdict;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatHudMixin implements IHscChat {

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

    @Accessor("trimmedMessages")
    abstract List<GuiMessage.Line> hsc$trimmedLines();

    @Accessor("chatScrollbarPos")
    abstract int hsc$scrollPos();

    @Invoker("getScale")
    abstract double hsc$getScale();

    @Invoker("getLineHeight")
    abstract int hsc$getLineHeight();

    @Invoker("getLinesPerPage")
    abstract int hsc$getLinesPerPage();

    @Invoker("isChatFocused")
    abstract boolean hsc$isFocused();

    @Invoker("refreshTrimmedMessages")
    abstract void hsc$refreshTrimmed();

    // Le ChatComponent actif s'enregistre (le ChatScreenMixin le récupère sans Gui.getChat(), absent en 26.2).
    // require=0 : feature confort — si MC change la signature, on dégrade au lieu de crasher.
    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void hsc$register(Minecraft mc, CallbackInfo ci) {
        HscChatAccess.current = this;
    }

    @Shadow
    public abstract void setVisibleMessageFilter(java.util.function.Predicate<GuiMessage> filter);

    private int hsc$tabCode = 0;   // 0=All, 1=Party, 2=Guild(+Officer)
    private String hsc$query = "";

    @Override
    public void hsc$setChannelFilter(int tabCode) {
        hsc$tabCode = tabCode;
        hsc$reapplyFilter();
    }

    @Override
    public int hsc$applySearch(String query) {
        hsc$query = query == null ? "" : query.trim();
        return hsc$reapplyFilter();
    }

    /** Reconstruit le prédicat combiné (onglet + recherche) et l'applique. Renvoie le nb de correspondances. */
    private int hsc$reapplyFilter() {
        final int tab = hsc$tabCode;
        final String q = hsc$query.toLowerCase(java.util.Locale.ROOT);
        final String[] tokens = q.isEmpty() ? new String[0] : q.split("\\s+");
        java.util.function.Predicate<GuiMessage> pred = m -> {
            String content = m.content().getString();
            if (tab != 0) {
                com.simplechat.Channel ch = ChatRules.INSTANCE.classify(hsc$stripLead(content));
                boolean ok = switch (tab) {
                    case 1 -> ch == com.simplechat.Channel.PARTY;
                    case 2 -> ch == com.simplechat.Channel.GUILD || ch == com.simplechat.Channel.OFFICER;
                    default -> true;
                };
                if (!ok) return false;
            }
            if (tokens.length > 0) {
                String s = content.toLowerCase(java.util.Locale.ROOT);
                for (String t : tokens) if (!s.contains(t)) return false;
            }
            return true;
        };
        setVisibleMessageFilter(pred);
        hsc$refreshTrimmed();
        if (tab == 0 && tokens.length == 0) return -1;
        int count = 0;
        try { for (GuiMessage m : hsc$allMessages()) if (pred.test(m)) count++; } catch (Throwable ignored) {}
        return count;
    }

    /** Retire un éventuel timestamp de tête "[HH:MM] " pour que classify voie le préfixe de canal. */
    private static String hsc$stripLead(String s) {
        if (s.length() >= 8 && s.charAt(0) == '[' && s.charAt(3) == ':' && s.charAt(6) == ']' && s.charAt(7) == ' ')
            return s.substring(8);
        return s;
    }

    /** Message affiché sous le curseur : géométrie du chat recalculée (pas de helper vanilla). */
    @Override
    public GuiMessage hsc$messageAt(double mouseX, double mouseY) {
        try {
            if (!hsc$isFocused()) return null;
            List<GuiMessage.Line> lines = hsc$trimmedLines();
            if (lines == null || lines.isEmpty()) return null;
            double scale = hsc$getScale();
            int guiH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            double x = mouseX / scale - 4.0;
            double y = (guiH - mouseY - 40.0) / scale / (double) hsc$getLineHeight();
            if (x < 0.0 || y < 0.0) return null;
            int visible = Math.min(hsc$getLinesPerPage(), lines.size());
            if (y >= visible) return null;
            int idx = (int) Math.floor(y) + hsc$scrollPos();
            if (idx < 0 || idx >= lines.size()) return null;
            return lines.get(idx).parent();
        } catch (Throwable t) {
            return null;
        }
    }

    // Historique étendu : remplace la limite vanilla de 100 (allMessages + lignes visibles) par le réglage.
    // require=0 : si un patch MC déplace la constante, on retombe sur les 100 vanilla au lieu de crasher.
    @ModifyConstant(method = {"addMessageToQueue", "addMessageToDisplayQueue"}, constant = @Constant(intValue = 100), require = 0)
    private int hsc$maxHistory(int original) {
        return Math.max(100, Math.min(2048, com.simplechat.Settings.INSTANCE.getMaxMessages()));
    }

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
        // Avertissement Discord greffé par Hypixel : toujours retiré, sans toggle.
        String stripped = ChatRules.INSTANCE.stripDiscordWarning(legacy);
        boolean warned = !stripped.equals(legacy);
        legacy = stripped;
        String clean = ChatRules.INSTANCE.clean(legacy);
        if (warned && clean.isEmpty()) { ci.cancel(); return; }
        Verdict v = com.simplechat.HoppityCompact.INSTANCE.process(clean, cfg.getCompactHoppity());
        if (v == null) v = ChatRules.INSTANCE.evaluate(legacy, cfg);

        if (v instanceof Verdict.Hide) { ci.cancel(); return; }

        // #2 : préserver les items/entités linkés -> ne pas reformater un message joueur qui en contient.
        if (v instanceof Verdict.Segments && hsc$hasItemLink(original)) {
            v = com.simplechat.Verdict.Pass.INSTANCE;
        }

        // Bouton cliquable (accepter un appel Abiphone, rejoindre une party…) : ne pas masquer/
        // reformater un message SYSTEM — le Replace reconstruit le texte et perdrait le ClickEvent.
        // Les messages joueurs (Segments) restent formatés : Hypixel met un clic /msg sur tous les
        // pseudos, le garde-fou bloquerait sinon tout le reformat de canal.
        if ((v instanceof Verdict.Hide || v instanceof Verdict.Replace || v instanceof Verdict.Compact)
                && hsc$hasActionClick(original)) {
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
            base = warned ? build(legacy) : original; // Pass (rebuild si l'avertissement a été retiré)
        }
        boolean untouched = v instanceof Verdict.Pass && !warned;
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

    /** true si un style porte un clic-commande (bouton d'action : run/suggest command).
     *  Les OpenUrl ne comptent pas — les liens web restent compactables. */
    private static boolean hsc$hasActionClick(Component c) {
        return c.visit((style, text) -> {
            net.minecraft.network.chat.ClickEvent e = style.getClickEvent();
            if (e instanceof net.minecraft.network.chat.ClickEvent.RunCommand
                    || e instanceof net.minecraft.network.chat.ClickEvent.SuggestCommand) {
                return java.util.Optional.of(Boolean.TRUE);
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY).isPresent();
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
