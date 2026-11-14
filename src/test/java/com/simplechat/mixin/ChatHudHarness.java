package com.simplechat.mixin;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Exécute le traitement réel du mixin sans démarrer le rendu Minecraft. */
public class ChatHudHarness extends ChatHudMixin {
    public final List<GuiMessage> messages = new ArrayList<>();

    public void receive(Component message) throws ReflectiveOperationException {
        var method = ChatHudMixin.class.getDeclaredMethod("hsc$onAddMessage", Component.class,
                MessageSignature.class, GuiMessageSource.class, GuiMessageTag.class, CallbackInfo.class);
        method.setAccessible(true);
        CallbackInfo ci = new CallbackInfo("addMessage", true);
        method.invoke(this, message, null, null, null, ci);
        if (!ci.isCancelled()) hsc$invokeAddMessage(message, null, null, null);
    }

    @Override
    void hsc$invokeAddMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag) {
        messages.addFirst(new GuiMessage(0, message, signature, source, tag));
    }

    @Override
    List<GuiMessage> hsc$allMessages() { return messages; }

    @Override
    List<GuiMessage.Line> hsc$trimmedLines() { return List.of(); }

    @Override
    int hsc$scrollPos() { return 0; }

    @Override
    double hsc$getScale() { return 1; }

    @Override
    int hsc$getLineHeight() { return 9; }

    @Override
    int hsc$getLinesPerPage() { return 10; }

    @Override
    boolean hsc$isFocused() { return false; }

    @Override
    void hsc$refreshTrimmed() {}

    @Override
    void hsc$invokeAddToDisplay(GuiMessage message) {}

    @Override
    public void setVisibleMessageFilter(Predicate<GuiMessage> filter) {}

    @Override
    public void resetChatScroll() {}
}
