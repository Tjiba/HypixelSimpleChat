package com.simplechat

import com.simplechat.engine.LegacyText
import com.simplechat.engine.Verdict
import com.simplechat.rules.Fmt
import java.util.regex.Pattern

/**
 * Fusionne un lot d'ordres Bazaar — les ventes qui tombent coup sur coup, ou les achats — en une
 * seule ligne de total, le détail item par item passé au survol. Un lot par sens : un achat après
 * des ventes ferme le leur et ouvre le sien. État côté client, appelé par le mixin après ChatRules :
 * chaque ordre arrive en un message, une règle du registre n'en verrait jamais qu'un. Le premier
 * ordre garde sa ligne détaillée — c'est le deuxième qui la remplace par le total.
 */
object BazaarSummary {

    private val SOLD = Pattern.compile("^\\[Bazaar] Sold ([\\d,]+)x (.+) for ([\\d,.]+) coins!")
    private val BOUGHT = Pattern.compile("^\\[Bazaar] Bought ([\\d,]+)x (.+) for ([\\d,.]+) coins!")
    private val CLAIMED = Pattern.compile("^\\[Bazaar] Claimed ([\\d,.]+) coins from selling ([\\d,]+)x (.+) at ")

    /** Deux ordres séparés de plus que ça sont deux lots : le second repart sur sa propre ligne. */
    private const val WINDOW_MS = 5_000L

    /** Au-delà, le survol déborderait de l'écran : le reste est compté sur une dernière ligne. */
    private const val MAX_HOVER = 20

    private class Item(val label: String, var qty: Long, var coins: Long)

    private class Order(val name: String, val label: String, val qty: Long, val coins: Long, val sell: Boolean)

    private val batch = LinkedHashMap<String, Item>()
    private var sell = true
    private var grey = false
    private var orders = 0
    private var lastAt = 0L
    private var shown: String? = null
    private var stale: String? = null
    private var pending = false

    /** Ligne du lot, ou null si le message n'est pas un ordre affiché. [verdict] = ce que les règles
     *  en ont fait : masqué ou laissé tel quel, le joueur ne veut pas de nos totaux. */
    fun process(clean: String, raw: String, verdict: Verdict): Verdict? {
        pending = false
        val order = parse(clean, raw) ?: return null
        if (verdict !is Verdict.Replace) {
            reset()
            return null
        }

        val now = System.currentTimeMillis()
        if (now - lastAt > WINDOW_MS || order.sell != sell) reset()
        lastAt = now
        sell = order.sell
        grey = verdict.legacy.startsWith("§8")
        orders++
        val item = batch[order.name]
        if (item == null) batch[order.name] = Item(order.label, order.qty, order.coins)
        else {
            item.qty += order.qty
            item.coins += order.coins
        }

        pending = true
        // Premier ordre du lot : sa ligne détaillée dit déjà tout, on la laisse passer.
        if (orders == 1) return null
        stale = shown
        return Verdict.Compact(line(), hover())
    }

    /** Ligne du lot à retirer du chat avant d'afficher la nouvelle, une seule fois. */
    fun stale(): String? {
        val s = stale
        stale = null
        return s
    }

    /** Ce que le mixin vient d'afficher pour l'ordre en cours : c'est elle que le prochain remplace. */
    fun displayed(rendered: String) {
        if (!pending) return
        pending = false
        shown = rendered
    }

    internal fun reset() {
        batch.clear()
        orders = 0
        shown = null
        stale = null
        pending = false
    }

    private fun parse(clean: String, raw: String): Order? {
        val sold = SOLD.matcher(clean)
        if (sold.find()) return order(raw, sold.group(2), sold.group(1), sold.group(3), true)
        val bought = BOUGHT.matcher(clean)
        if (bought.find()) return order(raw, bought.group(2), bought.group(1), bought.group(3), false)
        val claimed = CLAIMED.matcher(clean)
        if (claimed.find()) return order(raw, claimed.group(3), claimed.group(2), claimed.group(1), true)
        return null
    }

    private fun order(raw: String, name: String, qty: String, coins: String, sell: Boolean): Order? {
        val n = num(qty) ?: return null
        val c = num(coins) ?: return null
        return Order(name, Fmt.rawColor(raw, name) + name, n, c, sell)
    }

    private fun line(): String {
        val items = batch.values.sumOf { it.qty }
        val head = if (sell) "§6BZ §c-" else "§6BZ §a+"
        val kind = if (sell) "sale" else "buy"
        val full = "$head §f${group(items)} item${plural(items)} §8· §f$orders $kind${plural(orders.toLong())} " +
            "§7· ${sign()}${Fmt.shortNum(total().toString())}"
        return if (grey) "§8" + full.replace(Regex(LegacyText.CODE), "") else full
    }

    private fun hover(): String {
        val lines = batch.values.take(MAX_HOVER).map {
            "§f${group(it.qty)}x ${it.label} §8· ${sign()}${Fmt.shortNum(it.coins.toString())}"
        }
        val rest = batch.size - lines.size
        return (if (rest > 0) lines + "§8… and $rest more" else lines).joinToString("\n")
    }

    private fun total() = batch.values.sumOf { it.coins }

    private fun sign() = if (sell) "§a+" else "§c-"

    private fun group(n: Long) = String.format(java.util.Locale.ROOT, "%,d", n)

    private fun plural(n: Long) = if (n > 1) "s" else ""

    private fun num(s: String) = s.replace(",", "").substringBefore(".").toLongOrNull()
}
