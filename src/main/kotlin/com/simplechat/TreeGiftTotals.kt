package com.simplechat

import com.simplechat.engine.LegacyText
import com.simplechat.rules.Fmt
import java.util.regex.Pattern

/**
 * Cumule les récompenses des Tree Gifts de la session, un total par arbre — un petit Fig ne rapporte
 * pas les mêmes quantités qu'un grand Mangrove. Les quantités ne sont écrites nulle part dans le
 * message : elles vivent dans le survol d'Hypixel. Le mixin le passe ici avant de le recoller sur la
 * ligne compacte, et récupère le récap à y ajouter. État côté client, remis à zéro au lancement.
 */
object TreeGiftTotals {

    private val GIFT = Pattern.compile("^(.+?) Tree Gift\\. You helped cut")
    // "Tender Wood x0-8" : Hypixel écrit une fourchette quand la quantité n'est pas garantie.
    private val GAIN = Pattern.compile("^(.+?) x([\\d,]+)(?:-([\\d,]+))?$")
    private val CODE = Pattern.compile(LegacyText.CODE)

    /** [label] = le nom tel qu'Hypixel l'écrit, sa couleur gardée ; la clé du total reste décolorée. */
    private class Total(val label: String, var min: Long, var max: Long)

    private class Tree(val label: String) {
        var gifts = 0
        val gains = LinkedHashMap<String, Total>()
    }

    private val trees = LinkedHashMap<String, Tree>()

    /** Le nom de l'arbre si [clean] est un Tree Gift, null sinon — le mixin n'extrait le survol que pour ceux-là. */
    fun tree(clean: String): String? {
        val m = GIFT.matcher(clean)
        return if (m.find()) m.group(1) else null
    }

    /** Compte le gift et rend le récap legacy qui remplace le survol, ou null s'il ne détaille rien.
     *  [raw] sert à reprendre la couleur qu'Hypixel donne à l'arbre. */
    fun record(tree: String, raw: String, hoverLegacy: String): String? {
        val gains = parse(hoverLegacy)
        if (gains.isEmpty()) return null

        val t = trees.getOrPut(tree) { Tree(Fmt.rawSpan(raw, tree, "§2")) }
        t.gifts++
        for ((what, got) in gains) {
            val total = t.gains[what]
            if (total == null) {
                t.gains[what] = got
                continue
            }
            total.min += got.min
            total.max += got.max
        }
        return header(t) + t.gains.values.joinToString("") { "\n${line(it)}" }
    }

    /** Récap de `/hsc treegift` : un en-tête par arbre, puis ses gains. */
    fun report(): List<String> {
        if (trees.isEmpty()) return listOf("§7No Tree Gift counted this session.")
        val out = ArrayList<String>()
        for (tree in trees.values) {
            out += header(tree)
            for (total in tree.gains.values) out += " §8· ${line(total)}"
        }
        return out
    }

    fun reset() = trees.clear()

    private fun parse(hoverLegacy: String): Map<String, Total> {
        val out = LinkedHashMap<String, Total>()
        for (raw in hoverLegacy.split('\n')) {
            val g = GAIN.matcher(CODE.matcher(raw).replaceAll("").trim())
            if (!g.matches()) continue
            val min = num(g.group(2)) ?: continue
            val what = g.group(1).trim()
            // Couleur reprise du survol : rareté de l'item, teinte de l'XP, gris des fourchettes.
            out[what] = Total(Fmt.rawSpan(raw, what, "§7"), min, g.group(3)?.let { num(it) } ?: min)
        }
        return out
    }

    private fun header(tree: Tree) =
        "${tree.label} §8· §7${tree.gifts} gift${if (tree.gifts > 1) "s" else ""} this session"

    private fun line(total: Total) = "${total.label} §7x${amount(total)}"

    private fun amount(total: Total): String =
        if (total.min == total.max) group(total.min) else "${group(total.min)}-${group(total.max)}"

    private fun group(n: Long) = String.format(java.util.Locale.ROOT, "%,d", n)

    private fun num(s: String) = s.replace(",", "").toLongOrNull()
}
