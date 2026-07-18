package com.simplechat.menu

import com.simplechat.Preview
import com.simplechat.RuleConfig
import com.simplechat.Screens
import com.simplechat.SegRender
import com.simplechat.Settings
import com.simplechat.Updater
import com.simplechat.config.ConfigEntry
import com.simplechat.config.ConfigGroup
import com.simplechat.config.EntryKind
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ConfirmLinkScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Menu de config 100% custom. Fenêtre en verre translucide, coins arrondis,
 * fond flou. Nav en sidebar · réglages · preview. Stockage : config/HscConfig (JSON maison).
 * Rendu direct via GuiGraphicsExtractor : une impl shared, API 2D identique entre versions.
 */
class MenuScreen(private val parent: Screen?) : Screen(
    Minecraft.getInstance(),
    Minecraft.getInstance().font,
    Component.literal("Hypixel Simple Chat"),
) {
    private var currentCategory: String? = null
    private var topTab: String? = null
    private val items = ArrayList<Item>()
    private var scroll = 0
    private var openDropdown: Setting? = null
    private var dropdownConstants: List<Enum<*>> = emptyList()
    private var focused: Setting? = null
    private var editBuffer = ""
    private var previewLines: List<Component> = emptyList()
    // Color picker HSV : ouvert au clic sur le carré de couleur.
    private var picker: Setting? = null
    private var ph = 0f
    private var ps = 0f
    private var pv = 0f
    private var pickerDrag = 0 // 0 aucun, 1 carré SV, 2 barre teinte
    private val pickerSq = 90
    // Recherche globale : filtre les réglages de toutes les catégories (titre + description).
    private var searchQuery = ""
    private var searchFocused = false

    init { select(null) }

    private enum class Kind { TOGGLE, DROPDOWN, TEXT, NUMBER, COLOR }
    private sealed class Item
    private class Header(val title: String) : Item()
    private class Setting(val entry: ConfigEntry, val id: String, val title: String, val kind: Kind, val desc: String) : Item()
    private object PresetRow : Item()
    private class Rect(val key: String?, val label: String, val x1: Int, val y1: Int, val x2: Int, val y2: Int)

    private val headerH = 22

    // --- fenêtre + régions (centrée, recalculée à chaque frame) ---
    private fun winW() = (width - 20).coerceAtMost(680)
    private fun winH() = (height - 20).coerceAtMost(380)
    private fun winX() = (width - winW()) / 2
    private fun winY() = (height - winH()) / 2
    private fun regionTop() = winY() + MenuTheme.TITLE_H
    private fun regionBottom() = winY() + winH() - MenuTheme.FOOTER_H
    private fun sideX1() = winX() + MenuTheme.PAD
    private fun sideX2() = sideX1() + MenuTheme.SIDEBAR_W
    private fun mainX1() = sideX2() + MenuTheme.GAP
    private fun mainX2() = winX() + winW() - MenuTheme.PAD
    // Responsive : la preview ne s'affiche que s'il reste assez de place ; sinon les réglages prennent tout.
    private fun showPreview() = mainX2() - mainX1() >= 300
    private fun settingsX2() = if (showPreview()) mainX1() + (((mainX2() - mainX1()) - MenuTheme.GAP) * 0.58f).toInt() else mainX2()
    private fun previewX1() = settingsX2() + MenuTheme.GAP
    private fun widgetW() = minOf(MenuTheme.WIDGET_W, (settingsX2() - mainX1()) / 2)
    private val topTabH = 18
    private fun hasTopTabs() = searchQuery.isEmpty() && VIEWS.containsKey(currentCategory)
    private fun searchRect(): IntArray {
        val wr = winX() + winW()
        return intArrayOf(wr - 150, winY() + 5, wr - 26, winY() + 19)
    }
    private fun listTop() = if (hasTopTabs()) regionTop() + 4 + topTabH + 6 else regionTop() + 4
    private fun viewportH() = regionBottom() - listTop()
    private fun contentHeight() = items.sumOf { if (it is Header) headerH else MenuTheme.ROW_H } + 6
    private fun maxScroll() = maxOf(0, contentHeight() - viewportH())

    /** Parcourt les items avec leur top écran (scroll appliqué) et leur hauteur. */
    private fun eachItem(action: (item: Item, top: Int, h: Int) -> Unit) {
        var y = listTop()
        for (item in items) {
            val h = if (item is Header) headerH else MenuTheme.ROW_H
            action(item, y - scroll, h)
            y += h
        }
    }

    private fun settingTop(target: Setting): Int? {
        var y = listTop()
        for (item in items) {
            if (item === target) return y - scroll
            y += if (item is Header) headerH else MenuTheme.ROW_H
        }
        return null
    }

    private fun widgetBoxAt(top: Int): IntArray {
        val t = top + (MenuTheme.ROW_H - MenuTheme.WIDGET_H) / 2
        val right = settingsX2() - 8
        return intArrayOf(right - widgetW(), t, right, t + MenuTheme.WIDGET_H)
    }

    private fun toggleBoxAt(top: Int): IntArray {
        val s = 13
        val t = top + (MenuTheme.ROW_H - s) / 2
        val right = settingsX2() - 8
        return intArrayOf(right - s, t, right, t + s)
    }

    // --- data ---
    private fun categoryConfig(id: String?): ConfigGroup =
        if (id == null) Settings else Settings.categories[id] ?: Settings

    private fun kindOf(entry: ConfigEntry): Kind = when (entry.kind) {
        EntryKind.BOOLEAN -> Kind.TOGGLE
        EntryKind.ENUM -> Kind.DROPDOWN
        EntryKind.STRING -> Kind.TEXT
        EntryKind.INT -> Kind.NUMBER
        EntryKind.COLOR -> Kind.COLOR
    }

    private fun settingsOf(cfg: ConfigGroup): LinkedHashMap<String, Setting> {
        val byId = LinkedHashMap<String, Setting>()
        for (entry in cfg.entries.values) {
            byId[entry.id] = Setting(entry, entry.id, entry.title, kindOf(entry), entry.description)
        }
        return byId
    }

    /** Construit la liste : recherche globale, sinon catégorie avec sections → en-têtes + réglages groupés ; sinon liste plate. */
    private fun buildItems() {
        items.clear()
        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase()
            val cats = listOf<String?>(null) + Settings.categories.keys
            for (id in cats) {
                val cfg = categoryConfig(id)
                val hits = settingsOf(cfg).values.filter {
                    it.title.lowercase().contains(q) || it.desc.lowercase().contains(q)
                }
                if (hits.isEmpty()) continue
                items.add(Header((id ?: "General").uppercase()))
                items.addAll(hits)
            }
            return
        }
        val byId = settingsOf(categoryConfig(currentCategory))
        // Sections sans onglets (ex. General) : header par section (titre vide = pas de header).
        val flat = FLAT_SECTIONS[currentCategory]
        if (flat != null) {
            if (currentCategory == null) items.add(PresetRow)
            for ((title, ids) in flat) {
                val group = ids.mapNotNull { byId[it] }
                if (group.isEmpty()) continue
                if (title.isNotEmpty()) items.add(Header(title))
                items.addAll(group)
            }
            val placed = flat.values.flatten().toSet()
            items.addAll(byId.filterKeys { it !in placed }.values) // ajout futur non rangé → en fin
            return
        }
        val views = VIEWS[currentCategory]
        if (views == null) {
            items.addAll(byId.values)
            return
        }
        val sections = views[topTab] ?: views.values.first()
        for ((title, ids) in sections) {
            val group = ids.mapNotNull { byId[it] }
            if (group.isEmpty()) continue
            items.add(Header(title))
            items.addAll(group)
        }
        // Filet : id rangé dans aucune vue (ajout futur) → sous MISC, sur le 1er onglet.
        val placed = views.values.flatMap { it.values.flatten() }.toSet()
        val extra = byId.filterKeys { it !in placed }.values
        if (extra.isNotEmpty() && topTab == views.keys.first()) { items.add(Header("MISC")); items.addAll(extra) }
    }

    private fun rebuildPreview() {
        val catId = if (currentCategory == "SkyBlock" && topTab == "Islands") "skyblock_islands"
        else currentCategory ?: "simplechat"
        previewLines = Preview.forChannel(RuleConfig.current(), catId).map { SegRender.toComponent(it) }
    }

    private fun select(id: String?) {
        currentCategory = id
        topTab = VIEWS[id]?.keys?.first()
        searchQuery = ""; searchFocused = false
        scroll = 0; focused = null; openDropdown = null; picker = null
        buildItems(); rebuildPreview()
    }

    private fun selectTop(tab: String) {
        topTab = tab
        scroll = 0; focused = null; openDropdown = null; picker = null
        buildItems(); rebuildPreview()
    }

    private fun onValueChanged() = rebuildPreview()

    // --- rendu ---
    override fun extractRenderState(gfx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // Le parent rend le fond flou du jeu (une seule fois par frame — pas d'appel direct au blur).
        super.extractRenderState(gfx, mouseX, mouseY, delta)
        gfx.fill(0, 0, width, height, MenuTheme.DIM)

        val wx = winX(); val wy = winY(); val wr = wx + winW(); val wb = wy + winH()
        rrb(gfx, wx, wy, wr, wb, 8, MenuTheme.GLASS, MenuTheme.GLASS_BORDER)
        gfx.text(font, Component.literal("§f§lHypixel §7Simple §fChat"), wx + MenuTheme.PAD, wy + (MenuTheme.TITLE_H - font.lineHeight) / 2, MenuTheme.TEXT_TITLE)
        val cxHover = inRect(mouseX, mouseY, wr - 20, wy + 6, wr - 6, wy + 20)
        gfx.text(font, "✕", wr - 17, wy + (MenuTheme.TITLE_H - font.lineHeight) / 2, if (cxHover) MenuTheme.TEXT else MenuTheme.TEXT_FAINT)

        val sr = searchRect()
        rr(gfx, sr[0], sr[1], sr[2], sr[3], 4, if (searchFocused) MenuTheme.FIELD_HOVER else MenuTheme.FIELD)
        val sTxt = when {
            searchFocused -> "${searchQuery}_"
            searchQuery.isEmpty() -> "§8Search…"
            else -> searchQuery
        }
        gfx.enableScissor(sr[0] + 3, sr[1], sr[2] - 2, sr[3])
        gfx.text(font, sTxt, sr[0] + 5, sr[1] + (sr[3] - sr[1] - font.lineHeight) / 2 + 1, MenuTheme.TEXT_DIM)
        gfx.disableScissor()

        renderNav(gfx, mouseX, mouseY)
        renderSettings(gfx, mouseX, mouseY)
        renderPreview(gfx)
        renderFooter(gfx, mouseX, mouseY)
        renderDropdown(gfx, mouseX, mouseY)
        renderColorPicker(gfx)
        renderTooltip(gfx, mouseX, mouseY)
    }

    private fun renderNav(gfx: GuiGraphicsExtractor, mx: Int, my: Int) {
        for (n in navRects()) {
            val on = n.key == currentCategory
            val hover = inRect(mx, my, n.x1, n.y1, n.x2, n.y2)
            if (on) rr(gfx, n.x1, n.y1, n.x2, n.y2, 5, MenuTheme.ACCENT)
            else if (hover) rr(gfx, n.x1, n.y1, n.x2, n.y2, 5, MenuTheme.NAV_HOVER)
            gfx.text(font, n.label, n.x1 + 8, n.y1 + (MenuTheme.NAV_H - font.lineHeight) / 2, if (on) MenuTheme.TEXT_TITLE else MenuTheme.TEXT_DIM)
        }
    }

    private fun renderSettings(gfx: GuiGraphicsExtractor, mx: Int, my: Int) {
        val x1 = mainX1(); val x2 = settingsX2(); val top = regionTop(); val bot = regionBottom()
        rr(gfx, x1, top, x2, bot, 6, MenuTheme.CARD)
        for (t in topTabRects()) {
            val on = t.key == topTab
            val hover = inRect(mx, my, t.x1, t.y1, t.x2, t.y2)
            rr(gfx, t.x1, t.y1, t.x2, t.y2, 5, if (on) MenuTheme.ACCENT else if (hover) MenuTheme.FIELD_HOVER else MenuTheme.SUBTAB_IDLE)
            gfx.centeredText(font, t.label, (t.x1 + t.x2) / 2, t.y1 + (topTabH - font.lineHeight) / 2, if (on) MenuTheme.TEXT_TITLE else MenuTheme.TEXT_DIM)
        }
        gfx.enableScissor(x1, listTop(), x2, bot)
        scroll = scroll.coerceIn(0, maxScroll())
        eachItem { item, itop, h ->
            if (itop + h >= listTop() && itop <= bot) when (item) {
                is Header -> renderHeader(gfx, item, itop)
                is Setting -> renderRow(gfx, item, itop, mx, my)
                is PresetRow -> renderPresetRow(gfx, itop, mx, my)
            }
        }
        if (items.isEmpty()) {
            val msg = if (searchQuery.isNotEmpty()) "§7No matching settings" else "§7Island-specific rules coming soon"
            gfx.centeredText(font, Component.literal(msg), (x1 + x2) / 2, (listTop() + bot) / 2, MenuTheme.TEXT_FAINT)
        }
        gfx.disableScissor()
        if (maxScroll() > 0) {
            val vh = bot - listTop()
            val thumbH = maxOf(10, vh * vh / contentHeight())
            val thumbY = listTop() + (vh - thumbH) * scroll / maxScroll()
            gfx.fill(x2 - 3, listTop(), x2 - 1, bot, MenuTheme.FIELD)
            rr(gfx, x2 - 3, thumbY, x2 - 1, thumbY + thumbH, 1, MenuTheme.SCROLL_THUMB)
        }
    }

    private fun renderHeader(gfx: GuiGraphicsExtractor, header: Header, top: Int) {
        val label = "§l${header.title}"
        val ty = top + headerH - font.lineHeight - 3
        gfx.text(font, label, mainX1() + 8, ty, MenuTheme.TEXT_FAINT)
        val lineX = mainX1() + 8 + font.width(label) + 8
        gfx.fill(lineX, ty + font.lineHeight / 2, settingsX2() - 10, ty + font.lineHeight / 2 + 1, MenuTheme.GLASS_BORDER)
    }

    private fun renderRow(gfx: GuiGraphicsExtractor, row: Setting, top: Int, mx: Int, my: Int) {
        val labelY = top + (MenuTheme.ROW_H - font.lineHeight) / 2
        val widgetLeft = if (row.kind == Kind.TOGGLE) toggleBoxAt(top)[0] else widgetBoxAt(top)[0]
        // Clip du label pour qu'il ne passe jamais sous le widget (fenêtre étroite).
        gfx.enableScissor(mainX1() + 8, top, widgetLeft - 4, top + MenuTheme.ROW_H)
        gfx.text(font, row.title, mainX1() + 10, labelY, MenuTheme.TEXT)
        gfx.disableScissor()
        when (row.kind) {
            Kind.TOGGLE -> {
                val b = toggleBoxAt(top)
                val on = row.entry.getBoolean()
                rr(gfx, b[0], b[1], b[2], b[3], 4, if (on) MenuTheme.ACCENT else MenuTheme.TOGGLE_OFF)
                if (on) gfx.centeredText(font, "✔", (b[0] + b[2]) / 2, b[1] + (13 - font.lineHeight) / 2, MenuTheme.TEXT_TITLE)
            }
            Kind.DROPDOWN, Kind.TEXT, Kind.NUMBER -> {
                val b = widgetBoxAt(top)
                val hover = inRect(mx, my, b[0], b[1], b[2], b[3])
                rr(gfx, b[0], b[1], b[2], b[3], 4, if (hover) MenuTheme.FIELD_HOVER else MenuTheme.FIELD)
                val value = when (row.kind) {
                    Kind.DROPDOWN -> row.entry.getEnum().name
                    Kind.NUMBER -> if (focused === row) editBuffer else row.entry.getInt().toString()
                    else -> if (focused === row) editBuffer else row.entry.getString()
                }
                val txt = if (focused === row) "${value}_" else value
                gfx.text(font, txt, b[0] + 6, b[1] + (MenuTheme.WIDGET_H - font.lineHeight) / 2, MenuTheme.TEXT)
            }
            Kind.COLOR -> {
                val b = widgetBoxAt(top)
                val rgb = row.entry.getInt() and 0xFFFFFF
                rr(gfx, b[0], b[1], b[0] + 13, b[3], 3, opaque(rgb))
                val hx = b[0] + 18
                val hover = inRect(mx, my, hx, b[1], b[2], b[3])
                rr(gfx, hx, b[1], b[2], b[3], 4, if (hover) MenuTheme.FIELD_HOVER else MenuTheme.FIELD)
                val value = if (focused === row) editBuffer else hex(rgb)
                val txt = if (focused === row) "${value}_" else value
                gfx.text(font, txt, hx + 6, b[1] + (MenuTheme.WIDGET_H - font.lineHeight) / 2, MenuTheme.TEXT)
            }
        }
    }

    /** Boutons du preset : (label, x1, x2), alignés à droite comme les widgets. */
    private fun presetButtons(top: Int): List<Triple<String, Int, Int>> {
        var right = settingsX2() - 8
        val res = ArrayList<Triple<String, Int, Int>>()
        for (label in listOf("Default", "Recommended")) { // droite → gauche
            val w = font.width(label) + 12
            res.add(Triple(label, right - w, right))
            right -= w + 4
        }
        return res
    }

    private fun presetBtnY(top: Int) = top + (MenuTheme.ROW_H - MenuTheme.WIDGET_H) / 2

    private fun renderPresetRow(gfx: GuiGraphicsExtractor, top: Int, mx: Int, my: Int) {
        gfx.text(font, "Preset", mainX1() + 10, top + (MenuTheme.ROW_H - font.lineHeight) / 2, MenuTheme.TEXT)
        val y = presetBtnY(top)
        for ((label, x1, x2) in presetButtons(top)) {
            val hover = inRect(mx, my, x1, y, x2, y + MenuTheme.WIDGET_H)
            val accent = label == "Recommended"
            rr(gfx, x1, y, x2, y + MenuTheme.WIDGET_H, 4, if (accent) MenuTheme.ACCENT else if (hover) MenuTheme.FIELD_HOVER else MenuTheme.FIELD)
            gfx.centeredText(font, label, (x1 + x2) / 2, y + (MenuTheme.WIDGET_H - font.lineHeight) / 2, if (accent) MenuTheme.TEXT_TITLE else MenuTheme.TEXT)
        }
    }

    private fun applyPreset(recommended: Boolean) {
        if (recommended) {
            val txt = javaClass.getResourceAsStream("/assets/simplechat/presets/recommended.json")
                ?.bufferedReader()?.use { it.readText() }
            if (txt != null) Settings.applyPreset(txt) else Settings.resetToDefaults()
        } else {
            Settings.resetToDefaults()
        }
        Settings.save()
        onValueChanged()
    }

    private fun renderPreview(gfx: GuiGraphicsExtractor) {
        if (!showPreview()) return
        val x1 = previewX1(); val x2 = mainX2(); val top = regionTop(); val bot = regionBottom()
        rr(gfx, x1, top, x2, bot, 6, MenuTheme.CARD)
        gfx.enableScissor(x1, top, x2, bot)
        gfx.text(font, Component.literal("§8§lPREVIEW"), x1 + 8, top + 7, MenuTheme.TEXT_FAINT)
        var y = top + 21
        val w = x2 - x1 - 16
        for (line in previewLines) {
            if (y > bot) break
            gfx.textWithWordWrap(font, line, x1 + 8, y, w, MenuTheme.TEXT)
            y += font.wordWrapHeight(line, w) + 2
        }
        gfx.disableScissor()
    }

    private fun renderFooter(gfx: GuiGraphicsExtractor, mx: Int, my: Int) {
        val fy = winY() + winH() - MenuTheme.FOOTER_H + (MenuTheme.FOOTER_H - font.lineHeight) / 2
        val lx = winX() + MenuTheme.PAD
        val ghHover = inRect(mx, my, lx, fy - 2, lx + font.width("GitHub"), fy + 10)
        gfx.text(font, Component.literal(if (ghHover) "§fGitHub" else "§7GitHub"), lx, fy, MenuTheme.TEXT_DIM)
        gfx.text(font, Component.literal("§8v${Updater.CURRENT_VERSION}"), lx + font.width("GitHub") + 10, fy, MenuTheme.TEXT_FAINT)

        val dLabel = "Discord"
        val dW = font.width(dLabel) + 16
        val dX = mainX2() - dW
        rr(gfx, dX, fy - 3, dX + dW, fy + 11, 5, MenuTheme.ACCENT)
        gfx.centeredText(font, dLabel, dX + dW / 2, fy, MenuTheme.TEXT_TITLE)
        val help = "§7Bug? Missing something? Want to help? §8→"
        gfx.text(font, Component.literal(help), dX - 8 - font.width(Component.literal(help).string), fy, MenuTheme.TEXT_DIM)
    }

    /** Haut du popup dropdown : sous le widget, ou au-dessus si ça déborde de la fenêtre. */
    private fun dropdownTop(b: IntArray, count: Int): Int {
        val h = 15 * count + 2
        val below = b[3] + 2
        return if (below + h <= winY() + winH()) below else b[1] - 2 - h
    }

    private fun renderDropdown(gfx: GuiGraphicsExtractor, mx: Int, my: Int) {
        val row = openDropdown ?: return
        val top = settingTop(row) ?: return
        val b = widgetBoxAt(top)
        gfx.nextStratum()
        var y = dropdownTop(b, dropdownConstants.size)
        val h = 15
        rr(gfx, b[0], y, b[2], y + h * dropdownConstants.size + 2, 5, MenuTheme.GLASS)
        y += 1
        for (c in dropdownConstants) {
            val hover = inRect(mx, my, b[0], y, b[2], y + h)
            if (hover) rr(gfx, b[0] + 1, y, b[2] - 1, y + h, 4, MenuTheme.FIELD_HOVER)
            gfx.text(font, c.name, b[0] + 6, y + (h - font.lineHeight) / 2, MenuTheme.TEXT)
            y += h
        }
    }

    // --- entrées ---
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = event.x().toInt(); val my = event.y().toInt()

        val pk = picker
        if (pk != null) {
            val top = settingTop(pk)
            if (top != null) {
                val b = widgetBoxAt(top)
                val sq = pickerSq; val sqX = b[0]; val sqY = pickerSqY(b); val hueX = sqX + sq + 6; val hueW = 12; val pad = 6
                if (inRect(mx, my, sqX, sqY, sqX + sq, sqY + sq)) { pickerDrag = 1; updateSV(mx, my, sqX, sqY); return true }
                if (inRect(mx, my, hueX, sqY, hueX + hueW, sqY + sq)) { pickerDrag = 2; updateHue(my, sqY); return true }
                if (inRect(mx, my, sqX - pad, sqY - pad, hueX + hueW + pad, sqY + sq + pad + 14)) return true // clic dans le panneau
            }
            picker = null; pickerDrag = 0; return true
        }

        val dd = openDropdown
        if (dd != null) {
            val top = settingTop(dd)
            if (top != null) {
                val b = widgetBoxAt(top)
                var y = dropdownTop(b, dropdownConstants.size) + 1
                for (c in dropdownConstants) {
                    if (inRect(mx, my, b[0], y, b[2], y + 15)) { dd.entry.setEnum(c); onValueChanged(); openDropdown = null; return true }
                    y += 15
                }
            }
            openDropdown = null; return true
        }

        if (focused != null) { commitFocused(); focused = null }

        val sr = searchRect()
        if (inRect(mx, my, sr[0], sr[1], sr[2], sr[3])) { searchFocused = true; return true }
        searchFocused = false

        if (inRect(mx, my, winX() + winW() - 20, winY() + 6, winX() + winW() - 6, winY() + 20)) { onClose(); return true }

        for (n in navRects()) if (inRect(mx, my, n.x1, n.y1, n.x2, n.y2)) { select(n.key); return true }
        for (t in topTabRects()) if (inRect(mx, my, t.x1, t.y1, t.x2, t.y2)) { selectTop(t.key!!); return true }

        val fy = winY() + winH() - MenuTheme.FOOTER_H + (MenuTheme.FOOTER_H - font.lineHeight) / 2
        val lx = winX() + MenuTheme.PAD
        if (inRect(mx, my, lx, fy - 2, lx + font.width("GitHub"), fy + 10)) {
            ConfirmLinkScreen.confirmLinkNow(this, "https://github.com/Tjiba/HypixelSimpleChat"); return true
        }
        val dW = font.width("Discord") + 16
        val dX = mainX2() - dW
        if (inRect(mx, my, dX, fy - 3, dX + dW, fy + 11)) {
            ConfirmLinkScreen.confirmLinkNow(this, "https://discord.gg/AbyVvwqmUF"); return true
        }

        if (inRect(mx, my, mainX1(), listTop(), settingsX2(), regionBottom())) {
            var handled = false
            eachItem { item, itop, _ ->
                if (handled || my < itop || my > itop + MenuTheme.ROW_H) return@eachItem
                if (item is Setting && handleRowClick(item, itop, mx, my)) handled = true
                if (item is PresetRow) {
                    val y = presetBtnY(itop)
                    for ((label, x1, x2) in presetButtons(itop)) {
                        if (inRect(mx, my, x1, y, x2, y + MenuTheme.WIDGET_H)) {
                            applyPreset(label == "Recommended"); handled = true; break
                        }
                    }
                }
            }
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    private fun handleRowClick(row: Setting, top: Int, mx: Int, my: Int): Boolean = when (row.kind) {
        Kind.TOGGLE -> {
            val b = toggleBoxAt(top)
            if (!inRect(mx, my, b[0], b[1], b[2], b[3])) false
            else { row.entry.setBoolean(!row.entry.getBoolean()); onValueChanged(); true }
        }
        Kind.DROPDOWN -> {
            val b = widgetBoxAt(top)
            if (!inRect(mx, my, b[0], b[1], b[2], b[3])) false
            else {
                dropdownConstants = row.entry.enumConstants
                picker = null; openDropdown = row; true
            }
        }
        Kind.COLOR -> {
            val b = widgetBoxAt(top)
            if (inRect(mx, my, b[0], b[1], b[0] + 13, b[3])) { openPicker(row); true }   // carré → picker
            else if (inRect(mx, my, b[0] + 18, b[1], b[2], b[3])) {                       // champ hex → édition
                picker = null; focused = row; editBuffer = hex(row.entry.getInt() and 0xFFFFFF); true
            } else false
        }
        else -> { // TEXT, NUMBER
            val b = widgetBoxAt(top)
            if (!inRect(mx, my, b[0], b[1], b[2], b[3])) false
            else {
                focused = row
                editBuffer = if (row.kind == Kind.NUMBER) row.entry.getInt().toString() else row.entry.getString()
                true
            }
        }
    }

    /** Haut du carré SV du picker : sous le widget, ou au-dessus si ça déborde de la fenêtre. */
    private fun pickerSqY(b: IntArray): Int {
        val below = b[3] + 3
        return if (below + pickerSq + 6 + 14 <= winY() + winH()) below else b[1] - 3 - pickerSq - 6 - 14
    }

    private fun openPicker(row: Setting) {
        focused = null; openDropdown = null; picker = row
        val rgb = row.entry.getInt() and 0xFFFFFF
        val hsb = java.awt.Color.RGBtoHSB((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, null)
        ph = hsb[0]; ps = hsb[1]; pv = hsb[2]
    }

    private fun applyPicker() {
        val row = picker ?: return
        row.entry.setInt(java.awt.Color.HSBtoRGB(ph, ps, pv) and 0xFFFFFF)
        onValueChanged()
    }

    private fun updateSV(mx: Int, my: Int, sqX: Int, sqY: Int) {
        ps = ((mx - sqX) / pickerSq.toFloat()).coerceIn(0f, 1f)
        pv = (1f - (my - sqY) / pickerSq.toFloat()).coerceIn(0f, 1f)
        applyPicker()
    }

    private fun updateHue(my: Int, sqY: Int) {
        ph = ((my - sqY) / pickerSq.toFloat()).coerceIn(0f, 1f)
        applyPicker()
    }

    /** Popup color picker : carré Saturation/Valeur + barre de teinte, sous le carré de couleur. */
    private fun renderColorPicker(gfx: GuiGraphicsExtractor) {
        val row = picker ?: return
        val top = settingTop(row) ?: return
        val b = widgetBoxAt(top)
        val sq = pickerSq; val hueW = 12; val pad = 6
        val sqX = b[0]; val sqY = pickerSqY(b); val hueX = sqX + sq + 6
        gfx.nextStratum()
        rr(gfx, sqX - pad, sqY - pad, hueX + hueW + pad, sqY + sq + pad + 14, 6, MenuTheme.GLASS)
        // Carré SV : chaque colonne = dégradé (couleur pleine en haut → noir en bas).
        for (i in 0 until sq) {
            val topCol = java.awt.Color.HSBtoRGB(ph, i / (sq - 1f), 1f)
            gfx.fillGradient(sqX + i, sqY, sqX + i + 1, sqY + sq, topCol, 0xFF000000.toInt())
        }
        outline(gfx, sqX, sqY, sqX + sq, sqY + sq, MenuTheme.GLASS_BORDER)
        val cx = sqX + (ps * sq).toInt(); val cy = sqY + ((1f - pv) * sq).toInt()
        outline(gfx, cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFFFFF.toInt())
        // Barre de teinte.
        for (i in 0 until sq) gfx.fill(hueX, sqY + i, hueX + hueW, sqY + i + 1, java.awt.Color.HSBtoRGB(i / (sq - 1f), 1f, 1f))
        outline(gfx, hueX, sqY, hueX + hueW, sqY + sq, MenuTheme.GLASS_BORDER)
        val hy = sqY + (ph * sq).toInt()
        gfx.fill(hueX - 1, hy - 1, hueX + hueW + 1, hy + 1, 0xFFFFFFFF.toInt())
        gfx.text(font, Component.literal("§7" + hex(row.entry.getInt() and 0xFFFFFF)), sqX, sqY + sq + 3, MenuTheme.TEXT)
    }

    /** Tooltip : description de la ligne survolée (masqué si un popup est ouvert). */
    private fun renderTooltip(gfx: GuiGraphicsExtractor, mx: Int, my: Int) {
        if (openDropdown != null || picker != null) return
        if (!inRect(mx, my, mainX1(), listTop(), settingsX2(), regionBottom())) return
        var hovered: Setting? = null
        eachItem { item, top, h -> if (item is Setting && my >= top && my < top + h) hovered = item }
        val desc = hovered?.desc?.takeIf { it.isNotEmpty() } ?: return
        val text = Component.literal(desc)
        val w = minOf(font.width(desc), 200)
        val h = font.wordWrapHeight(text, w)
        var x = mx + 10
        var y = my + 12
        if (x + w + 11 > width) x = maxOf(6, width - w - 11)
        if (y + h + 10 > height) y = my - h - 14
        gfx.nextStratum()
        rrb(gfx, x - 5, y - 4, x + w + 5, y + h + 4, 4, MenuTheme.GLASS, MenuTheme.GLASS_BORDER)
        gfx.textWithWordWrap(font, text, x, y, w, MenuTheme.TEXT)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (picker != null && pickerDrag != 0) {
            val top = settingTop(picker!!)
            if (top != null) {
                val b = widgetBoxAt(top); val sqX = b[0]; val sqY = pickerSqY(b)
                if (pickerDrag == 1) updateSV(event.x().toInt(), event.y().toInt(), sqX, sqY) else updateHue(event.y().toInt(), sqY)
            }
            return true
        }
        return super.mouseDragged(event, dx, dy)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        pickerDrag = 0
        return super.mouseReleased(event)
    }

    private fun commitFocused() {
        val row = focused ?: return
        when (row.kind) {
            Kind.TEXT -> row.entry.setString(editBuffer)
            Kind.NUMBER -> editBuffer.toIntOrNull()?.let { row.entry.setInt(it) }
            Kind.COLOR -> parseHex(editBuffer)?.let { row.entry.setInt(it) }
            else -> {}
        }
        onValueChanged()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        openDropdown = null; picker = null
        if (inRect(mouseX.toInt(), mouseY.toInt(), mainX1(), listTop(), settingsX2(), regionBottom())) {
            scroll = (scroll - (scrollY * MenuTheme.ROW_H).toInt()).coerceIn(0, maxScroll())
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val key = event.key()
        if (picker != null) { if (key == GLFW.GLFW_KEY_ESCAPE) picker = null; return true }
        if (openDropdown != null) { if (key == GLFW.GLFW_KEY_ESCAPE) openDropdown = null; return true }
        if (searchFocused) {
            when (key) {
                GLFW.GLFW_KEY_ESCAPE -> { searchQuery = ""; searchFocused = false; scroll = 0; buildItems() }
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> searchFocused = false
                GLFW.GLFW_KEY_BACKSPACE -> if (searchQuery.isNotEmpty()) { searchQuery = searchQuery.dropLast(1); scroll = 0; buildItems() }
            }
            return true
        }
        if (focused != null) {
            when (key) {
                GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> { commitFocused(); focused = null }
                GLFW.GLFW_KEY_BACKSPACE -> if (editBuffer.isNotEmpty()) editBuffer = editBuffer.dropLast(1)
            }
            return true
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (searchFocused) {
            searchQuery += event.codepointAsString()
            scroll = 0; buildItems()
            return true
        }
        if (focused == null) return super.charTyped(event)
        editBuffer += event.codepointAsString()
        return true
    }

    override fun onClose() {
        Settings.save()
        Screens.set(Minecraft.getInstance(), parent)
    }

    private fun topTabRects(): List<Rect> {
        val res = ArrayList<Rect>()
        if (!hasTopTabs()) return res
        val tabs = VIEWS[currentCategory]?.keys ?: return res
        var x = mainX1() + 4
        val y = regionTop() + 4
        for (t in tabs) {
            val w = font.width(t) + 16
            res.add(Rect(t, t, x, y, x + w, y + topTabH)); x += w + 5
        }
        return res
    }

    private fun navRects(): List<Rect> {
        val available = Settings.categories.keys
        val order = ArrayList<String?>()
        for (id in NAV_ORDER) if (id == null || id in available) order.add(id)
        for (id in available) if (id !in NAV_ORDER) order.add(id) // catégorie non listée → en fin
        val res = ArrayList<Rect>()
        var y = regionTop()
        for (id in order) {
            res.add(Rect(id, id ?: "General", sideX1(), y, sideX2(), y + MenuTheme.NAV_H))
            y += MenuTheme.NAV_H + 3
        }
        return res
    }

    // --- helpers ---
    private fun inRect(mx: Int, my: Int, x1: Int, y1: Int, x2: Int, y2: Int) = mx in x1..x2 && my in y1..y2

    /** Liseré rectangulaire 1px. */
    private fun outline(gfx: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, c: Int) {
        gfx.fill(x1, y1, x2, y1 + 1, c)
        gfx.fill(x1, y2 - 1, x2, y2, c)
        gfx.fill(x1, y1, x1 + 1, y2, c)
        gfx.fill(x2 - 1, y1, x2, y2, c)
    }

    /** Rectangle à coins arrondis (dessin additif, par ligne — compatible fond flou). */
    private fun rr(gfx: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, r: Int, c: Int) {
        val h = y2 - y1
        if (h <= 0 || x2 <= x1) return
        val rad = minOf(r, h / 2, (x2 - x1) / 2)
        for (dy in 0 until h) {
            val edge = minOf(dy, h - 1 - dy)
            val inset = if (edge < rad) {
                val k = rad - 1 - edge
                rad - Math.round(Math.sqrt((rad * rad - k * k).toDouble())).toInt()
            } else 0
            gfx.fill(x1 + inset, y1 + dy, x2 - inset, y1 + dy + 1, c)
        }
    }

    private fun rrb(gfx: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, r: Int, fill: Int, border: Int) {
        rr(gfx, x1, y1, x2, y2, r, border)
        rr(gfx, x1 + 1, y1 + 1, x2 - 1, y2 - 1, maxOf(0, r - 1), fill)
    }

    private fun opaque(rgb: Int) = 0xFF000000.toInt() or (rgb and 0xFFFFFF)
    private fun hex(rgb: Int) = "#%06X".format(rgb and 0xFFFFFF)
    private fun parseHex(s: String): Int? =
        s.trim().removePrefix("#").takeIf { it.isNotEmpty() }?.toIntOrNull(16)?.and(0xFFFFFF)

    companion object {
        // Ordre des catégories dans la sidebar (null = General).
        private val NAV_ORDER = listOf<String?>(null, "Public Chat", "Party Chat", "Guild Chat", "SkyBlock", "Lobby", "System")

        // Sections d'une catégorie SANS onglets (header par section ; titre vide = pas de header).
        private val FLAT_SECTIONS: Map<String?, LinkedHashMap<String, List<String>>> = mapOf(
            null to linkedMapOf(
                "" to listOf("masterEnabled", "groupingWindowSeconds", "smartCollapse", "updateNotifications", "maxMessages", "showTimestamps", "timestampColor", "compactTheme", "compactThemeColor"),
                "CHAT TABS" to listOf("chatTabs", "tabFilterMode"),
            ),
        )

        // Vues d'une catégorie : onglet haut → sections → ids RC (= nom du champ Kotlin).
        // SkyBlock a 2 onglets (General transversal / Islands spécifique par île).
        // Autres catégories : absentes ici → liste plate.
        private val VIEWS: Map<String?, LinkedHashMap<String, LinkedHashMap<String, List<String>>>> = mapOf(
            "SkyBlock" to linkedMapOf(
                "General" to linkedMapOf(
                    "GENERAL" to listOf("enabled", "petSummon", "customPatterns"),
                    "WORLD & EVENTS" to listOf("npcDialog", "events", "hoppity", "rewards", "misc"),
                    "COMBAT" to listOf("boss", "damageSpam", "killCombo", "mobAbility", "combat", "abilities", "warnings", "slayer"),
                    "ECONOMY" to listOf("bazaar", "sacks", "lootShare", "gexp", "rareReward"),
                    "DUNGEONS" to listOf("dungeons", "soloClass"),
                ),
                // Règles spécifiques par île — à venir (Crimson, Mining, Rift, Galatea…).
                "Islands" to linkedMapOf(),
            ),
        )
    }
}
