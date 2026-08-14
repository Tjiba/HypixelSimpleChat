package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Fmt
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Bazaar et Hôtel des ventes : les lignes de confirmation d'ordres. */
object Bazaar {

    val BAZAAR = Group(
        id = "bazaar",
        title = "Bazaar / Auction House",
        category = Category.SKYBLOCK,
        section = "ECONOMY",
        default = RuleAction.HIDE,
        description = "Escrow, submitting offers, order setup spam",
    )

    val rules = rules(BAZAAR) {
        // Sept lignes d'attente pour la même chose, chacune suivie de son résultat : elles n'ont
        // rien à dire que la ligne d'après ne dise mieux. Compact vide pour qu'elles disparaissent
        // aussi quand le groupe est en COMPACT.
        rule("bazaar-progress", RuleAction.HIDE,
            "^(?:\\[Bazaar] (?:Submitting (?:sell offer|buy order)|Executing instant (?:buy|sell)|Claiming order)" +
                "|Putting item in escrow|Setting up the auction)",
            compact = { "" },
            sample = "§6[Bazaar] §7Submitting sell offer...",
            title = "Order in progress")
        rule("bazaar-order-setup", RuleAction.HIDE,
            "^(?:Buy Order|Sell Offer) Setup! (.+)$",
            compact = { "§6BZ §a✔ ${Fmt.rawSpan(it.raw, it[1])}" },
            sample = "§6Buy Order Setup! §a64x §fEnchanted Cobblestone",
            title = "Order set up")
        rule("bazaar-bought", RuleAction.HIDE,
            "^\\[Bazaar] Bought ([\\d,]+)x (.+) for ([\\d,.]+) coins!",
            compact = { "§6BZ §a+ §f${it[1]}x ${Fmt.rawColor(it.raw, it[2])}${it[2]} §7· §c-${Fmt.shortNum(it[3])}" },
            sample = "§6[Bazaar] §fBought §a64x §fRaw Cod §ffor §620,422 coins§f!",
            title = "Instant buy filled")
        rule("bazaar-sold", RuleAction.HIDE,
            "^\\[Bazaar] Sold ([\\d,]+)x (.+) for ([\\d,.]+) coins!",
            compact = { "§6BZ §c- §f${it[1]}x ${Fmt.rawColor(it.raw, it[2])}${it[2]} §7· §a+${Fmt.shortNum(it[3])}" },
            sample = "§6[Bazaar] §fSold §a399x §fRuby Veilshroom §ffor §6391,539 coins§f!",
            title = "Instant sell filled")
        rule("bazaar-claimed", RuleAction.HIDE,
            "^\\[Bazaar] Claimed ([\\d,.]+) coins from selling ([\\d,]+)x (.+) at ",
            compact = { "§6BZ §a+${Fmt.shortNum(it[1])} §7· §f${it[2]}x ${Fmt.rawColor(it.raw, it[3])}${it[3]}" },
            sample = "§6[Bazaar] §fClaimed §61,387,133 coins §ffrom selling §a1x §fFuming Potato Book §fat §61,401,145 each§f!",
            title = "Order claimed")
        // Filet, en dernier : toute autre ligne [Bazaar] obéit quand même au groupe.
        rule("bazaar-other", RuleAction.HIDE,
            "^\\[Bazaar] ",
            sample = "§6[Bazaar] §7Something new from Hypixel",
            title = "Other Bazaar lines")
    }
}
