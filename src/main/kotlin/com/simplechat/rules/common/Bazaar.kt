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
        rule("bazaar-sell-offer", RuleAction.HIDE,
            "^\\[Bazaar] Submitting sell offer",
            compact = { "§6BZ §7· selling…" },
            sample = "§6[Bazaar] §7Submitting sell offer...",
            title = "Submitting sell offer")
        rule("bazaar-buy-order", RuleAction.HIDE,
            "^\\[Bazaar] Submitting buy order",
            compact = { "§6BZ §7· buying…" },
            sample = "§6[Bazaar] §7Submitting buy order...",
            title = "Submitting buy order")
        rule("bazaar-order-setup", RuleAction.HIDE,
            "^(?:Buy Order|Sell Offer) Setup! (.+)$",
            compact = { "§6BZ §a✔ ${Fmt.rawSpan(it.raw, it[1])}" },
            sample = "§6Buy Order Setup! §a64x §fEnchanted Cobblestone",
            title = "Order set up")
        rule("bazaar-instant-buy", RuleAction.HIDE,
            "^\\[Bazaar] Executing instant buy",
            compact = { "§6BZ §7· buying…" },
            sample = "§6[Bazaar] §7Executing instant buy...",
            title = "Executing instant buy")
        rule("bazaar-instant-sell", RuleAction.HIDE,
            "^\\[Bazaar] Executing instant sell",
            compact = { "§6BZ §7· selling…" },
            sample = "§6[Bazaar] §7Executing instant sell...",
            title = "Executing instant sell")
        rule("bazaar-bought", RuleAction.HIDE,
            "^\\[Bazaar] Bought ([\\d,]+)x (.+) for ([\\d,.]+) coins!",
            compact = { "§6BZ §a+ ${Fmt.rawColor(it.raw, it[2])}${it[1]}x ${it[2]} §7· §c-${Fmt.shortNum(it[3])}" },
            sample = "§6[Bazaar] §fBought §a64x §fRaw Cod §ffor §620,422 coins§f!",
            title = "Instant buy filled")
        rule("bazaar-sold", RuleAction.HIDE,
            "^\\[Bazaar] Sold ([\\d,]+)x (.+) for ([\\d,.]+) coins!",
            compact = { "§6BZ §c- ${Fmt.rawColor(it.raw, it[2])}${it[1]}x ${it[2]} §7· §a+${Fmt.shortNum(it[3])}" },
            sample = "§6[Bazaar] §fSold §a399x §fRuby Veilshroom §ffor §6391,539 coins§f!",
            title = "Instant sell filled")
        rule("bazaar-claiming", RuleAction.HIDE,
            "^\\[Bazaar] Claiming order",
            compact = { "§6BZ §7· claiming…" },
            sample = "§6[Bazaar] §7Claiming order...",
            title = "Claiming an order")
        rule("bazaar-claimed", RuleAction.HIDE,
            "^\\[Bazaar] Claimed ([\\d,.]+) coins from selling ([\\d,]+)x (.+) at ",
            compact = { "§6BZ §a+${Fmt.shortNum(it[1])} §7· ${Fmt.rawColor(it.raw, it[3])}${it[2]}x ${it[3]}" },
            sample = "§6[Bazaar] §fClaimed §61,387,133 coins §ffrom selling §a1x §fFuming Potato Book §fat §61,401,145 each§f!",
            title = "Order claimed")
        rule("auction-escrow", RuleAction.HIDE,
            "^Putting item in escrow",
            compact = { "§7Escrow…" },
            sample = "§7Putting item in escrow...",
            title = "Putting item in escrow")
        rule("auction-setup", RuleAction.HIDE,
            "^Setting up the auction",
            compact = { "§6AH §7· setup…" },
            sample = "§7Setting up the auction...",
            title = "Setting up the auction")
        // Filet, en dernier : toute autre ligne [Bazaar] obéit quand même au groupe.
        rule("bazaar-other", RuleAction.HIDE,
            "^\\[Bazaar] ",
            sample = "§6[Bazaar] §7Something new from Hypixel",
            title = "Other Bazaar lines")
    }
}
