package com.simplechat.mixin;

import com.simplechat.IConfigAccess;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import com.teamresourceful.resourcefulconfig.client.ConfigScreen;
import com.teamresourceful.resourcefulconfig.client.components.options.OptionsListWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Expose le config + le haut de la liste d'options pour caler l'aperçu du chat dans le bandeau. */
@Mixin(ConfigScreen.class)
public abstract class ConfigScreenMixin implements IConfigAccess {

    @Shadow @Final private ResourcefulConfig config;
    @Shadow private OptionsListWidget optionsList;

    @Override
    public ResourcefulConfig hsc$config() {
        return config;
    }

    @Override
    public int hsc$contentTop() {
        return optionsList != null ? optionsList.getY() : 40;
    }
}
