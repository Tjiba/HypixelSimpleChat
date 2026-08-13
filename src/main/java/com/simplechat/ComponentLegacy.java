package com.simplechat;

import com.simplechat.engine.LegacyText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;

/** Component (couleurs dans le Style) -> chaîne legacy §. Le moteur ne travaille que sur du legacy. */
public final class ComponentLegacy {

    private ComponentLegacy() {}

    public static String of(Component c) {
        StringBuilder sb = new StringBuilder();
        c.visit((style, text) -> {
            appendStyle(sb, style);
            sb.append(text);
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    private static void appendStyle(StringBuilder sb, Style style) {
        sb.append("§r");
        TextColor col = style.getColor();
        if (col != null) {
            int rgb = col.getValue() & 0xFFFFFF;
            Character code = LegacyText.INSTANCE.codeFor(rgb);
            // Hors palette vanilla : en §#RRGGBB, sinon la couleur d'Hypixel serait perdue ici.
            if (code != null) sb.append('§').append(code.charValue());
            else sb.append(String.format("§#%06X", rgb));
        }
        if (style.isBold()) sb.append("§l");
        if (style.isItalic()) sb.append("§o");
        if (style.isUnderlined()) sb.append("§n");
        if (style.isStrikethrough()) sb.append("§m");
        if (style.isObfuscated()) sb.append("§k");
    }
}
