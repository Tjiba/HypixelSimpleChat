package com.simplechat.ui

import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigValueEntry
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType
import com.teamresourceful.resourcefulconfig.api.types.options.Option
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.network.chat.Component

/** Type de widget owo rendu pour une entrée RC. */
enum class WidgetKind { TOGGLE, DROPDOWN, TEXT, NUMBER, SLIDER, COLOR, UNSUPPORTED }

/** Fournit à RcWidgets les popups flottants (menu enum, color picker) que seul l'écran sait ouvrir. */
interface WidgetHost {
    fun openMenu(anchor: ButtonComponent, options: List<Pair<Component, () -> Unit>>)
    fun openColorPicker(anchor: ButtonComponent, initialRgb: Int, onPick: (Int) -> Unit)
}

object RcWidgets {
    private val FIELD = 0xFF2A2A34.toInt()
    private val FIELD_HOVER = 0xFF3A3A47.toInt()

    /** Classe une entrée RC (type + flags COLOR/RANGE) vers un WidgetKind. COLOR prime sur RANGE.
     *  Numériques limités à INTEGER : les widgets lisent/écrivent en Int (getInt/setInt), la config
     *  n'a que des Int (couleurs incluses). Les autres numériques sont UNSUPPORTED (pas de troncature). */
    fun kindOf(type: EntryType, hasColor: Boolean, hasRange: Boolean): WidgetKind = when (type) {
        EntryType.BOOLEAN -> WidgetKind.TOGGLE
        EntryType.ENUM -> WidgetKind.DROPDOWN
        EntryType.STRING -> WidgetKind.TEXT
        EntryType.INTEGER ->
            if (hasColor) WidgetKind.COLOR else if (hasRange) WidgetKind.SLIDER else WidgetKind.NUMBER
        else -> WidgetKind.UNSUPPORTED
    }

    /** Transforme une entrée RC en une ligne owo : label + widget lié. Toute édition applique setX puis onChange(). */
    fun rowFor(entry: ResourcefulConfigValueEntry, onChange: () -> Unit, host: WidgetHost): FlowLayout {
        val data = entry.options()
        val title = Component.literal(data.title().toLocalizedString())
        val hasColor = data.hasOption(Option.COLOR)
        val hasRange = data.hasOption(Option.RANGE)

        val row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
        row.verticalAlignment(VerticalAlignment.CENTER)
        row.gap(6)
        // Label prend l'espace restant (expand, pas fill=100%) → widgets alignés à droite en colonne.
        row.child(UIComponents.label(title).horizontalSizing(Sizing.expand()))

        val widget: UIComponent = when (kindOf(entry.type(), hasColor, hasRange)) {
            WidgetKind.TOGGLE ->
                UIComponents.checkbox(Component.empty()).checked(entry.getBoolean())
                    .onChanged { v -> entry.setBoolean(v); onChange() }
            WidgetKind.TEXT ->
                UIComponents.textBox(Sizing.fixed(110), entry.getString()).apply {
                    onChanged().subscribe { v -> entry.setString(v); onChange() }
                }
            WidgetKind.NUMBER ->
                UIComponents.textBox(Sizing.fixed(110), entry.getInt().toString()).apply {
                    onChanged().subscribe { v -> v.toIntOrNull()?.let { entry.setInt(it); onChange() } }
                }
            WidgetKind.COLOR -> colorCell(entry, onChange, host)
            WidgetKind.SLIDER -> {
                val range: ConfigOption.Range = data.getOption(Option.RANGE)
                UIComponents.discreteSlider(Sizing.fixed(110), range.min, range.max).apply {
                    setFromDiscreteValue(entry.getInt().toDouble())
                    onChanged().subscribe { _ -> entry.setInt(discreteValue().toInt()); onChange() }
                }
            }
            WidgetKind.DROPDOWN -> enumButton(entry, onChange, host)
            WidgetKind.UNSUPPORTED -> UIComponents.label(Component.literal("§8(unsupported)"))
        }
        row.child(widget)
        return row
    }

    // Couleur : swatch cliquable (ouvre le color picker HSV en popup) + champ hex éditable, synchronisés.
    private fun colorCell(entry: ResourcefulConfigValueEntry, onChange: () -> Unit, host: WidgetHost): UIComponent {
        fun cur() = entry.getInt() and 0xFFFFFF
        val swatch = UIComponents.button(Component.empty()) {}
            .renderer(ButtonComponent.Renderer.flat(opaque(cur()), opaque(cur()), opaque(cur())))
        swatch.horizontalSizing(Sizing.fixed(16))
        swatch.verticalSizing(Sizing.fixed(16))
        val field = UIComponents.textBox(Sizing.fixed(62), hex(entry.getInt()))
        var updating = false
        fun apply(rgb: Int, fromField: Boolean) {
            if (updating) return
            updating = true
            entry.setInt(rgb)
            swatch.renderer(ButtonComponent.Renderer.flat(opaque(rgb), opaque(rgb), opaque(rgb)))
            if (!fromField) field.text(hex(rgb))
            updating = false
            onChange()
        }
        swatch.onPress { host.openColorPicker(swatch, cur()) { apply(it, false) } }
        field.onChanged().subscribe { v -> parseHex(v)?.let { apply(it, true) } }
        val cell = UIContainers.horizontalFlow(Sizing.content(), Sizing.content())
        cell.gap(4)
        cell.verticalAlignment(VerticalAlignment.CENTER)
        cell.child(swatch)
        cell.child(field)
        return cell
    }

    private fun opaque(rgb: Int): Int = 0xFF000000.toInt() or (rgb and 0xFFFFFF)

    private fun hex(rgb: Int): String = String.format("#%06X", rgb and 0xFFFFFF)

    private fun parseHex(s: String): Int? =
        s.trim().removePrefix("#").takeIf { it.isNotEmpty() }?.toIntOrNull(16)?.and(0xFFFFFF)

    // Choix d'enum : bouton plat qui ouvre un menu déroulant flottant listant les valeurs.
    private fun enumButton(entry: ResourcefulConfigValueEntry, onChange: () -> Unit, host: WidgetHost): UIComponent {
        val constants = (entry.objectType().enumConstants ?: emptyArray()).map { it as Enum<*> }
        val btn = UIComponents.button(Component.literal(entry.getEnum()?.name ?: "?")) {}
            .renderer(ButtonComponent.Renderer.flat(FIELD, FIELD_HOVER, FIELD))
        btn.horizontalSizing(Sizing.fixed(110))
        btn.onPress {
            if (constants.isEmpty()) return@onPress
            val options = constants.map { c ->
                Component.literal(c.name) as Component to {
                    entry.setEnum(c)
                    btn.message = Component.literal(c.name)
                    onChange()
                }
            }
            host.openMenu(btn, options)
        }
        return btn
    }
}
