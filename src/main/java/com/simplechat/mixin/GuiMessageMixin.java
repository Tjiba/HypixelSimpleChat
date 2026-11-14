package com.simplechat.mixin;

import com.simplechat.IHscMessage;
import com.simplechat.engine.Channel;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.class)
public class GuiMessageMixin implements IHscMessage {
    @Unique
    private Channel hsc$channel;

    @Override
    public Channel hsc$getChannel() { return hsc$channel; }

    @Override
    public void hsc$setChannel(Channel channel) { hsc$channel = channel; }
}
