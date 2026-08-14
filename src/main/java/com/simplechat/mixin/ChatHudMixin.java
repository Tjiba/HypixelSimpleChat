package com.simplechat.mixin;

import com.simplechat.HscChatAccess;
import com.simplechat.IHscChat;
import com.simplechat.config.RuleConfig;
import com.simplechat.engine.ChatRules;
import com.simplechat.engine.Collapse;
import com.simplechat.engine.LegacyText;
import com.simplechat.engine.Seg;
import com.simplechat.engine.Verdict;
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

    // Collapse global des répétitions : Collapse retient les lignes récentes, le mixin retrouve
    // celle qui revient dans l'historique et la ré-affiche avec (xN).

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
                com.simplechat.engine.Channel ch = ChatRules.INSTANCE.classify(hsc$stripLead(content));
                boolean ok = switch (tab) {
                    case 1 -> ch == com.simplechat.engine.Channel.PARTY;
                    case 2 -> ch == com.simplechat.engine.Channel.GUILD || ch == com.simplechat.engine.Channel.OFFICER;
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
        return Math.max(100, Math.min(2048, com.simplechat.config.Settings.INSTANCE.getMaxMessages()));
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
        String legacy = com.simplechat.ComponentLegacy.of(original);
        // Avertissement Discord greffé par Hypixel : toujours retiré, sans toggle.
        String stripped = ChatRules.INSTANCE.stripDiscordWarning(legacy);
        boolean warned = !stripped.equals(legacy);
        legacy = stripped;
        String clean = ChatRules.INSTANCE.clean(legacy);
        if (warned && clean.isEmpty()) { ci.cancel(); return; }
        Verdict v = com.simplechat.SafariSummary.INSTANCE.process(clean, cfg);
        if (v == null) v = com.simplechat.HoppityCompact.INSTANCE.process(clean, cfg.getCompactHoppity());
        if (v == null) v = ChatRules.INSTANCE.evaluate(legacy, cfg);

        // Compté avant le masquage : un gift caché doit quand même entrer dans le total.
        String giftTotals = hsc$treeGiftTotals(clean, legacy, original);

        if (v instanceof Verdict.Hide) { ci.cancel(); return; }

        // #2 : préserver les items/entités linkés -> ne pas reformater un message joueur qui en contient.
        if (v instanceof Verdict.Segments && hsc$hasItemLink(original)) {
            v = com.simplechat.engine.Verdict.Pass.INSTANCE;
        }

        // Bouton cliquable (accepter un appel Abiphone, rejoindre une party…) : ne pas masquer/
        // reformater un message SYSTEM — le Replace reconstruit le texte et perdrait le ClickEvent.
        // Les messages joueurs (Segments) restent formatés : Hypixel met un clic /msg sur tous les
        // pseudos, le garde-fou bloquerait sinon tout le reformat de canal.
        if (v instanceof Verdict.Hide && hsc$hasActionClick(original)) {
            com.simplechat.Debug.logGuard("clickable button", legacy);
            v = com.simplechat.engine.Verdict.Pass.INSTANCE;
        }

        // Lien web (changelog, page de récompense) : le reformat reconstruit le texte et perdrait
        // l'URL. Masquer reste permis — c'est un choix explicite du joueur, pas une perte muette.
        if ((v instanceof Verdict.Replace || v instanceof Verdict.Compact) && hsc$hasUrlClick(original)) {
            com.simplechat.Debug.logGuard("web link", legacy);
            v = com.simplechat.engine.Verdict.Pass.INSTANCE;
        }

        com.simplechat.Debug.log(legacy, clean, v);

        // #3 : collapse intelligent (normalise les nombres) pour les messages système reformatés.
        boolean smart = cfg.getSmartCollapse() && (v instanceof Verdict.Replace);
        String key = smart ? ChatRules.INSTANCE.collapseKey(clean) : clean;
        boolean system = ChatRules.INSTANCE.classify(clean) == com.simplechat.engine.Channel.SYSTEM;
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
        // Un message reformaté garde ses boutons et son survol : le texte est reconstruit, les
        // ClickEvent et HoverEvent non. Le détail (XP gagnée, contenu d'un gift) reste à portée.
        if (v instanceof Verdict.Replace || v instanceof Verdict.Compact) {
            if (hsc$hasActionClick(original)) base = hsc$withClickables(base, original);
            base = hsc$withHover(base, original, giftTotals);
        }
        boolean untouched = v instanceof Verdict.Pass && !warned;
        hsc$display(base, key, untouched, system, cfg, signature, source, tag, ci);
    }

    /** Affiche [base] ; si la même ligne est déjà dans la fenêtre, la reprend en bas avec (xN). */
    private void hsc$display(Component base, String key, boolean untouched, boolean system, RuleConfig cfg,
                             MessageSignature sig, GuiMessageSource src, GuiMessageTag tag, CallbackInfo ci) {
        Collapse.Seen seen = cfg.getGroupRepeats() ? Collapse.INSTANCE.seen(key) : null;

        // Le spam système est rattrapé même si d'autres lignes sont passées entre-temps. Le chat
        // joueur ne se replie que sur la ligne juste au-dessus : sans fenêtre de temps, un "gg"
        // dit vingt minutes plus tôt n'a rien à faire en bas du chat avec un (x2).
        if (seen != null && hsc$removeLine(seen.getRendered(), system ? Integer.MAX_VALUE : 1)) {
            int count = seen.getCount() + 1;
            com.simplechat.Debug.logCollapsed(key, count);
            Component disp = withTimestamp(hsc$withCount(base, count), cfg);
            Collapse.INSTANCE.remember(key, disp.getString(), count);
            reAdd(disp, sig, src, tag, ci);
            return;
        }
        // Ligne introuvable (sortie de l'historique, chat vidé) -> ajout normal, compteur reparti à 1.

        // Message intact + pas de timestamp : laisser MC l'ajouter tel quel. Ne PAS annuler/ré-ajouter,
        // sinon les autres mods injectant sur addMessage traitent chaque ligne en double.
        if (untouched && !cfg.getShowTimestamps()) {
            Collapse.INSTANCE.remember(key, base.getString(), 1);
            return;
        }

        Component disp = withTimestamp(base, cfg);
        Collapse.INSTANCE.remember(key, disp.getString(), 1);
        com.simplechat.Debug.logRendered(disp.getString());
        reAdd(disp, sig, src, tag, ci);
    }

    /** Retire la ligne affichée [rendered], cherchée parmi les [depth] plus récentes. false si
     *  elle n'y est plus : trimmée par la limite d'historique, ou chat vidé depuis. */
    private boolean hsc$removeLine(String rendered, int depth) {
        try {
            List<GuiMessage> all = hsc$allMessages();
            if (all == null) return false;
            int n = Math.min(all.size(), depth);
            for (int i = 0; i < n; i++) {
                if (!all.get(i).content().getString().equals(rendered)) continue;
                all.remove(i);
                hsc$refreshTrimmed();
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static Component hsc$withCount(Component base, int count) {
        return Component.empty().append(base)
                .append(Component.literal(" (x" + count + ")").withStyle(s -> s.withColor(0x555555)));
    }

    /** true si un style porte un lien web (OpenUrl). Reformater le message le détruirait. */
    private static boolean hsc$hasUrlClick(Component c) {
        return c.visit((style, text) -> {
            if (style.getClickEvent() instanceof net.minecraft.network.chat.ClickEvent.OpenUrl) {
                return java.util.Optional.of(Boolean.TRUE);
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY).isPresent();
    }

    /** true si un style porte un clic-commande (bouton d'action : run/suggest command).
     *  Les OpenUrl sont traités à part : ils interdisent le reformat, pas le masquage. */
    /**
     * Compact + les boutons du message d'origine. Seul un vrai bouton — un fragment entre crochets,
     * "[PICK UP]" — est recollé ; recopier un fragment de phrase cliquable dupliquerait le texte
     * qu'on vient justement de raccourcir. Tout le reste passe par le ClickEvent porté par la ligne.
     */
    private static Component hsc$withClickables(Component compact, Component original) {
        net.minecraft.network.chat.MutableComponent out = Component.empty().append(compact);
        String shortText = compact.getString();
        original.visit((style, text) -> {
            net.minecraft.network.chat.ClickEvent e = style.getClickEvent();
            if (!(e instanceof net.minecraft.network.chat.ClickEvent.RunCommand)
                    && !(e instanceof net.minecraft.network.chat.ClickEvent.SuggestCommand)) {
                return java.util.Optional.empty();
            }
            // Un bouton est court et entre crochets. Tout ce qui ressemble à une phrase est
            // exclu : le recoller réafficherait le texte qu'on vient de raccourcir.
            String run = text.trim();
            boolean button = run.length() <= 24 && run.startsWith("[") && run.endsWith("]")
                    && !shortText.contains(run);
            if (button) out.append(Component.literal(" ").append(Component.literal(run).setStyle(style)));
            else out.withStyle(s -> s.withClickEvent(e).withHoverEvent(style.getHoverEvent()));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return out;
    }

    /** Reporte le survol du message d'origine sur la ligne compacte, si elle n'en a pas déjà un.
     *  [totals] — totaux de session d'un Tree Gift — prend la place du détail d'Hypixel : le gain
     *  d'un seul gift est déjà résumé par la ligne, le survol ne sert qu'au cumul. */
    private static Component hsc$withHover(Component compact, Component original, String totals) {
        if (compact.getStyle().getHoverEvent() != null) return compact;
        net.minecraft.network.chat.HoverEvent hover = hsc$hoverOf(original);
        if (hover == null) return compact;
        if (totals != null) hover = new net.minecraft.network.chat.HoverEvent.ShowText(build(totals));
        final net.minecraft.network.chat.HoverEvent h = hover;
        return Component.empty().append(compact).withStyle(s -> s.withHoverEvent(h));
    }

    /** Premier survol porté par un style du message, ou null. */
    private static net.minecraft.network.chat.HoverEvent hsc$hoverOf(Component c) {
        return c.<net.minecraft.network.chat.HoverEvent>visit(
                (style, text) -> java.util.Optional.ofNullable(style.getHoverEvent()),
                net.minecraft.network.chat.Style.EMPTY).orElse(null);
    }

    /** Totaux de session du Tree Gift porté par [clean], ou null. Les quantités gagnées ne sont
     *  écrites que dans le survol d'Hypixel : c'est la seule source à cumuler. */
    private static String hsc$treeGiftTotals(String clean, String legacy, Component original) {
        String tree = com.simplechat.TreeGiftTotals.INSTANCE.tree(clean);
        if (tree == null) return null;
        if (!(hsc$hoverOf(original) instanceof net.minecraft.network.chat.HoverEvent.ShowText text)) return null;
        return com.simplechat.TreeGiftTotals.INSTANCE.record(
                tree, legacy, com.simplechat.ComponentLegacy.of(text.value()));
    }

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
        return com.simplechat.engine.SegRender.toComponent(segs);
    }
}
