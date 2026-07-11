package com.simplechat.ui

import com.simplechat.Preview
import com.simplechat.RuleConfig
import com.simplechat.Screens
import com.simplechat.SegRender
import com.simplechat.Updater
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import com.teamresourceful.resourcefulconfig.api.types.elements.ResourcefulConfigEntryElement
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigValueEntry
import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.ColorPickerComponent
import io.wispforest.owo.ui.component.DropdownComponent
import io.wispforest.owo.ui.component.TextureComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.CursorStyle
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConfirmLinkScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.Component

/** Menu de config owo (dark moderne). 26.1 uniquement ; RC reste le stockage. */
class HscConfigScreen(
    private val parent: Screen?,
    private val root: ResourcefulConfig,
) : BaseOwoScreen<StackLayout>(Component.literal("Hypixel Simple Chat")), WidgetHost {

    private lateinit var body: FlowLayout
    private lateinit var previewPanel: FlowLayout
    private var currentCategory: String? = null
    private val tabs = LinkedHashMap<String?, ButtonComponent>()

    override fun createAdapter(): OwoUIAdapter<StackLayout> =
        OwoUIAdapter.create(this) { h, v -> UIContainers.stack(h, v) }

    override fun build(rootStack: StackLayout) {
        // Tout le contenu dans un flow ; la racine reste un StackLayout pour poser les popups
        // (dropdowns / color picker) en overlay sans décaler le layout.
        val content = UIContainers.verticalFlow(Sizing.fill(100), Sizing.fill(100))
        content.surface(Surface.flat(BG))
        content.padding(Insets.of(12))
        content.gap(8)

        content.child(UIComponents.label(Component.literal("§f§lHypixel §7Simple §fChat")))

        val tabRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
        tabRow.gap(4)
        addTab(tabRow, null, "General")
        for ((id, _) in root.categories()) addTab(tabRow, id, id)
        content.child(tabRow)

        body = UIContainers.verticalFlow(Sizing.fill(100), Sizing.expand())
        content.child(body)

        val footer = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
        footer.gap(6)
        footer.verticalAlignment(VerticalAlignment.CENTER)
        footer.child(githubButton())
        footer.child(UIComponents.label(Component.literal("§8v${Updater.CURRENT_VERSION}")))
        footer.child(UIComponents.label(Component.empty()).horizontalSizing(Sizing.expand())) // pousse à droite
        footer.child(UIComponents.label(Component.literal("§7Missing a message? Help us §8→")))
        footer.child(DiscordButton.create(this))
        content.child(footer)

        rootStack.child(content)
        showCategory(null)
    }

    /** Logo GitHub du footer : image (github.png 384×384) scalée à 16px, cliquable vers le repo. */
    private fun githubButton(): TextureComponent {
        val tex = Identifier.fromNamespaceAndPath("simplechat", "textures/gui/github.png")
        val img = UIComponents.texture(tex, 0, 0, 384, 384, 384, 384)
        img.horizontalSizing(Sizing.fixed(16))
        img.verticalSizing(Sizing.fixed(16))
        img.cursorStyle(CursorStyle.HAND)
        img.mouseDown().subscribe { _, _ ->
            ConfirmLinkScreen.confirmLinkNow(this, "https://github.com/Tjiba/HypixelSimpleChat")
            true
        }
        return img
    }

    private fun addTab(row: FlowLayout, id: String?, label: String) {
        val btn = UIComponents.button(Component.literal(label)) { showCategory(id) }
            .renderer(ButtonComponent.Renderer.flat(TAB_IDLE, TAB_HOVER, TAB_IDLE))
        tabs[id] = btn
        row.child(btn)
    }

    /** Colore l'onglet actif en accent, les autres en plat sombre. */
    private fun styleTabs() {
        for ((id, btn) in tabs) {
            btn.renderer(
                if (id == currentCategory) ButtonComponent.Renderer.flat(TAB_ACTIVE, TAB_ACTIVE, TAB_ACTIVE)
                else ButtonComponent.Renderer.flat(TAB_IDLE, TAB_HOVER, TAB_IDLE),
            )
        }
    }

    // Options de la catégorie à gauche (panneau scrollable), aperçu live à droite.
    private fun showCategory(id: String?) {
        currentCategory = id
        styleTabs()
        body.clearChildren()
        val cfg = if (id == null) root else root.categories()[id] ?: root

        val split = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fill())
        split.gap(8)

        val list = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
        list.gap(2)
        list.padding(Insets.of(6))
        for (element in cfg.elements()) {
            if (element.isHidden) continue
            // Les entrées valeur sont enveloppées dans un ResourcefulConfigEntryElement : déballer via entry().
            val entry = (element as? ResourcefulConfigEntryElement)?.entry() as? ResourcefulConfigValueEntry ?: continue
            list.child(RcWidgets.rowFor(entry, { onValueChanged() }, this))
        }
        val scroll = UIContainers.verticalScroll(Sizing.fill(60), Sizing.fill(), list)
        scroll.surface(Surface.flat(PANEL).and(Surface.outline(OUTLINE)))
        split.child(scroll)

        previewPanel = UIContainers.verticalFlow(Sizing.fill(38), Sizing.fill())
        previewPanel.gap(6)
        previewPanel.padding(Insets.of(8))
        previewPanel.surface(Surface.flat(INSET).and(Surface.outline(OUTLINE)))
        split.child(previewPanel)

        body.child(split)
        rebuildPreview()
    }

    /** Appelé après chaque édition d'un widget. */
    private fun onValueChanged() = rebuildPreview()

    /** Ouvre un menu déroulant flottant sous [anchor] ; se ferme au clic dehors ou à la sélection. */
    override fun openMenu(anchor: ButtonComponent, options: List<Pair<Component, () -> Unit>>) {
        val root = uiAdapter.rootComponent
        DropdownComponent.openContextMenu(
            this, root, { r, dd -> r.child(dd) },
            anchor.x().toDouble(), (anchor.y() + anchor.height()).toDouble(),
        ) { dd ->
            for ((label, action) in options) {
                dd.button(label) { menu -> action(); root.removeChild(menu) }
            }
        }
    }

    /** Ouvre un color picker HSV flottant sous [anchor] ; onPick reçoit le RGB à chaque changement. */
    override fun openColorPicker(anchor: ButtonComponent, initialRgb: Int, onPick: (Int) -> Unit) {
        val root = uiAdapter.rootComponent
        DropdownComponent.openContextMenu(
            this, root, { r, dd -> r.child(dd) },
            anchor.x().toDouble(), (anchor.y() + anchor.height()).toDouble(),
        ) { dd ->
            val picker = ColorPickerComponent()
            picker.horizontalSizing(Sizing.fixed(120))
            picker.verticalSizing(Sizing.fixed(120))
            picker.selectedColor(Color.ofArgb(0xFF000000.toInt() or (initialRgb and 0xFFFFFF)))
            picker.onChanged().subscribe { c -> onPick(c.rgb() and 0xFFFFFF) }
            dd.child(picker)
        }
    }

    /** Reconstruit l'aperçu du canal courant depuis la config live. */
    private fun rebuildPreview() {
        if (!::previewPanel.isInitialized) return
        previewPanel.clearChildren()
        previewPanel.child(UIComponents.label(Component.literal("§8§lPREVIEW")))
        val catId = currentCategory ?: "simplechat"
        val lines = Preview.forChannel(RuleConfig.current(), catId)
        for (segs in lines) {
            previewPanel.child(UIComponents.label(SegRender.toComponent(segs)))
        }
    }

    override fun removed() {
        root.save()
        super.removed()
    }

    override fun onClose() {
        Screens.set(Minecraft.getInstance(), parent)
    }

    companion object {
        private val BG = 0xF0121216.toInt()      // fond global sombre
        private val PANEL = 0xFF1E1E26.toInt()    // panneau options
        private val INSET = 0xFF141419.toInt()    // panneau preview (plus sombre)
        private val OUTLINE = 0xFF34343F.toInt()  // liseré des panneaux
        private val TAB_IDLE = 0xFF2A2A34.toInt()
        private val TAB_HOVER = 0xFF3A3A47.toInt()
        private val TAB_ACTIVE = 0xFF4A5BD0.toInt() // accent blurple
    }
}
