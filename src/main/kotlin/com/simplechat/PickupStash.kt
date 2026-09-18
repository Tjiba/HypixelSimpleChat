package com.simplechat

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import com.simplechat.rules.Registry
import com.simplechat.rules.common.Economy
import java.util.regex.Pattern

/**
 * Fusionne les trois lignes du Pickup Stash : montant, nombre de types, puis bouton cliquable.
 * L'état côté client est nécessaire : Hypixel les envoie comme trois messages indépendants.
 */
object PickupStash {

    private val STASHED = Pattern.compile("^You have ([\\d,]+) (items|materials) stashed away!$")
    private val TYPES = Pattern.compile("^\\(This totals ([\\d,]+) types? of (items?|materials?) stashed!\\)$")
    private val PICK_UP = Pattern.compile("^>>> CLICK HERE to pick them up! <<<$")

    /** Réglage commun au rappel fusionné et aux messages de récupération. */
    const val SETTING = "pickup-stash"

    private var amount: String? = null
    private var kind: String? = null
    private var types: String? = null
    private var padding = false
    private val pickup = PickupBatch()

    /** Verdict d'une ligne Pickup Stash, ou null si elle n'en fait pas partie. */
    fun process(clean: String, cfg: RuleConfig, raw: String = clean): Verdict? {
        val action = action(cfg)
        val trim = action == RuleAction.COMPACT || action == RuleAction.COMPACT_GREY || action == RuleAction.HIDE
        if (clean.isEmpty() && trim && padding) return Verdict.Hide
        padding = trim && isLine(clean)
        if (action != RuleAction.COMPACT && action != RuleAction.COMPACT_GREY) {
            clearBlock()
            pickup.reset()
            if (!isLine(clean)) return null
            return when (action) {
                RuleAction.GREY -> Verdict.Replace("§8" + clean)
                RuleAction.HIDE -> Verdict.Hide
                else -> Verdict.Pass
            }
        }

        // "From stash:" ressemble à un MP pour ChatRules : ces règles passent avant son garde-fou.
        if (isPickup(clean)) {
            clearBlock()
            return pickup.process(clean, raw, cfg, action)
        }
        pickup.reset()

        val stashed = STASHED.matcher(clean)
        if (stashed.matches()) {
            amount = stashed.group(1)
            kind = stashed.group(2)
            types = null
            return Verdict.Hide
        }
        val typeCount = TYPES.matcher(clean)
        if (amount != null && typeCount.matches()
            && typeCount.group(2).removeSuffix("s") == kind?.removeSuffix("s")) {
            types = typeCount.group(1)
            return Verdict.Hide
        }
        if (amount != null && PICK_UP.matcher(clean).matches()) {
            val result = if (types == null) Verdict.Pass
            else Verdict.Replace(line(action, amount!!, kind!!, types!!, cfg))
            clearBlock()
            return result
        }
        if (amount != null) clearBlock()
        return null
    }

    /** Aperçu isolé : ne touche jamais au bloc Pickup Stash en cours. */
    fun preview(cfg: RuleConfig): List<Pair<String, Verdict>> {
        val samples = listOf(
            "§7You have §b14,431 §7materials stashed away!",
            "§7(This totals §b16 §7types of materials stashed!)",
            "§e>>> CLICK HERE to pick them up! <<<",
        )
        val action = action(cfg)
        val reminder = if (action == RuleAction.COMPACT || action == RuleAction.COMPACT_GREY)
            listOf(samples.last() to Verdict.Replace(line(action, "14,431", "materials", "16", cfg)))
        else samples.map { raw ->
            raw to when (action) {
                RuleAction.GREY -> Verdict.Replace("§8" + ChatRules.clean(raw))
                RuleAction.HIDE -> Verdict.Hide
                else -> Verdict.Pass
            }
        }
        val pickups = Registry.byGroup[Economy.PICKUP_STASH].orEmpty()
        if (action == RuleAction.COMPACT || action == RuleAction.COMPACT_GREY) {
            val sample = PickupBatch()
            val merged = pickups.map { rule -> sample.process(ChatRules.clean(rule.sample), rule.sample, cfg, action) }.last()
            return reminder + (pickups.last().sample to merged)
        }
        return reminder + pickups.map { rule ->
            rule.sample to if (action == RuleAction.OFF) Verdict.Pass
            else Registry.match(ChatRules.clean(rule.sample), rule.sample, cfg) ?: Verdict.Pass
        }
    }

    private fun action(cfg: RuleConfig): RuleAction {
        if (!cfg.masterEnabled || !cfg.skyblockEnabled) return RuleAction.OFF
        return cfg.groupActions[SETTING] ?: Economy.PICKUP_STASH.default
    }

    /** Les lignes initiales peuvent être cliquables elles aussi : leur Hide est volontaire. */
    fun isLine(clean: String): Boolean =
        STASHED.matcher(clean).matches() || TYPES.matcher(clean).matches() || PICK_UP.matcher(clean).matches()
            || isPickup(clean)

    private fun isPickup(clean: String): Boolean =
        Registry.byGroup[Economy.PICKUP_STASH].orEmpty().any { it.match(clean, clean) != null }

    fun trimsPadding(clean: String, cfg: RuleConfig): Boolean {
        val action = action(cfg)
        if (action != RuleAction.COMPACT && action != RuleAction.COMPACT_GREY && action != RuleAction.HIDE) return false
        return isLine(clean)
    }

    internal fun reset() {
        padding = false
        clearBlock()
        pickup.reset()
    }

    fun stale(): String? = pickup.stale()

    fun displayed(rendered: String) = pickup.displayed(rendered)

    /** Comme les lots Bazaar, chaque ligne remplace la précédente : un bloc incomplet reste visible. */
    private class PickupBatch {
        private val items = LinkedHashSet<String>()
        private var quantity: String? = null
        private var remaining: String? = null
        private var shown: String? = null
        private var stale: String? = null
        private var pending = false

        fun process(clean: String, raw: String, cfg: RuleConfig, action: RuleAction): Verdict {
            val (rule, match) = Registry.find(clean, raw, Registry.byGroup[Economy.PICKUP_STASH].orEmpty())
                ?: return Verdict.Pass
            if (remaining != null || (rule.id != "stash-remaining" && quantity != null)) reset()
            val part = rule.compact?.invoke(match)?.removePrefix("§bStash §7· ") ?: raw
            when (rule.id) {
                "stash-item" -> items.add(part)
                "stash-picked-up" -> quantity = part
                "stash-remaining" -> remaining = part
            }
            val details = listOfNotNull(quantity, remaining).joinToString(" §7· ")
            val lines = listOf(items.joinToString(" §7· "), details).filter { it.isNotEmpty() }
            val full = lines.joinToString("\n") { "§bStash §7· $it" }
            stale = shown
            pending = true
            return Verdict.Replace(if (action == RuleAction.COMPACT_GREY) full.lines().joinToString("\n") { "§8" + ChatRules.clean(it) }
                else ChatRules.theme(full, cfg))
        }

        fun stale(): String? {
            val s = stale
            stale = null
            return s
        }

        fun displayed(rendered: String) {
            if (!pending) return
            pending = false
            shown = rendered
        }

        fun reset() {
            items.clear()
            quantity = null; remaining = null; shown = null; stale = null; pending = false
        }
    }

    private fun clearBlock() {
        amount = null; kind = null; types = null
    }

    private fun line(action: RuleAction, amount: String, kind: String, types: String, cfg: RuleConfig): String {
        val typeLabel = if (types == "1") "type" else "types"
        val full = "§bStash §7· §b$amount §f$kind §7· §b$types §f$typeLabel §6[PICK UP]"
        return if (action == RuleAction.COMPACT_GREY) "§8" + ChatRules.clean(full)
        else ChatRules.theme(full, cfg)
    }
}
