package com.simplechat.rules

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import com.simplechat.rules.common.Abilities
import com.simplechat.rules.common.Bazaar
import com.simplechat.rules.common.Combat
import com.simplechat.rules.common.Drops
import com.simplechat.rules.common.Economy
import com.simplechat.rules.common.Events
import com.simplechat.rules.common.Misc
import com.simplechat.rules.common.Npc
import com.simplechat.rules.common.Pets
import com.simplechat.rules.common.Slayer
import com.simplechat.rules.common.Warnings
import com.simplechat.rules.islands.Dungeons
import com.simplechat.rules.islands.Foraging
import com.simplechat.rules.lobby.Boosters
import com.simplechat.rules.lobby.Joins
import com.simplechat.rules.lobby.Morphs
import com.simplechat.rules.lobby.Notifications
import com.simplechat.rules.lobby.Rewards
import com.simplechat.rules.system.Server
import com.simplechat.rules.system.Transitions

/**
 * Toutes les règles du mod, dans l'ordre d'évaluation : premier match gagne.
 * Le spécifique passe avant le générique — règles précises d'abord, groupes de spam ensuite.
 * Ce qui n'est pas encore porté retombe sur l'ancien moteur, dans ChatRules.
 */
object Registry {

    val groups: List<Group> = listOf(
        Joins.LOBBY_JOIN,
        Boosters.BOOSTER, Boosters.GENEROSITY, Boosters.PLAYTIME, Boosters.REWARD_LINK,
        Rewards.MYSTERY, Rewards.CLAIMED,
        Morphs.MORPH,
        Npc.NPC_DIALOG, Npc.ABIPHONE,
        Pets.PET_SPAWN, Pets.PET_SUMMON,
        Combat.BOSS, Combat.DAMAGE, Combat.KILL_COMBO, Combat.MOB_ABILITY,
        Economy.SACKS, Economy.LOOT_SHARE, Economy.GEXP, Economy.RARE_REWARD,
        Server.PROFILE_ID, Server.ROUTING,
        Transitions.TRANSITIONS, Notifications.NOTIFICATIONS, Dungeons.DUNGEONS,
        Foraging.TREE_GIFT, Foraging.FLOOR_DROP, Foraging.PETALFALL, Foraging.WOODPECKER, Foraging.TIMBER,
        Foraging.TORRHUS, Foraging.HIVE,
        Foraging.APPEARED, Foraging.SHARDS, Foraging.HUNTING, Foraging.HONEY_TREE,
        Foraging.SAFARI_ENTRY, Foraging.SAFARI_CAPTURE, Foraging.SAFARI_MANAGER, Foraging.SAFARI_SUMMARY,
        Foraging.SAFARI_MILESTONES, Foraging.SAFARI_DISABLED, Foraging.BIRDFEEDER,
        Abilities.ABILITIES, Combat.COMBAT_HEAL, Drops.SKYBLOCK_XP, Drops.REWARDS,
        Misc.MISC, Bazaar.BAZAAR, Slayer.SLAYER, Events.EVENTS, Warnings.WARNINGS,
    )

    val rules: List<Rule> =
        // Règles précises : un réglage chacune, elles décident avant les groupes.
        // Garde-fou : aucune règle ne doit avaler un gain d'XP SkyBlock.
        Drops.skyblockXp +
            Joins.rules + Boosters.rules + Rewards.rules + Morphs.rules +
            Pets.rules +
            // Avant Combat : son générique "… DOWN!" avalerait le "BEEHEEMOTH DOWN!" de Torrhus.
            // Avant Npc : ses PNJ nommés ont leur propre réglage, "[NPC] " générique les avalerait.
            Foraging.rules + Npc.rules + Combat.rules + Economy.rules + Server.rules +
            // Groupes de spam : un réglage pour plusieurs messages.
            Transitions.rules + Notifications.rules + Dungeons.rules +
            Abilities.rules + Combat.spam + Drops.rules +
            Misc.rules + Bazaar.rules + Slayer.rules + Events.rules + Warnings.rules

    /** Règles de chaque groupe, dans l'ordre d'évaluation. */
    val byGroup: Map<Group, List<Rule>> = rules.groupBy { it.group }

    /** Première règle qui matche, ou null. [list] permet de tester un sous-ensemble. */
    fun find(clean: String, raw: String, list: List<Rule> = rules): Pair<Rule, Match>? {
        for (rule in list) {
            val m = rule.match(clean, raw) ?: continue
            return rule to m
        }
        return null
    }

    /** Verdict de la première règle qui matche, ou null si aucune (l'appelant enchaîne). */
    fun match(clean: String, raw: String, cfg: RuleConfig): Verdict? {
        val (rule, m) = find(clean, raw) ?: return null
        if (!enabled(rule.group.category, cfg)) return Verdict.Pass
        val short = rule.compact?.invoke(m)
        // Compact vide = rien à montrer : la phrase disparaît même si son groupe est en COMPACT.
        // C'est ce qui permet à un réglage unique de cacher une ligne et d'en raccourcir d'autres.
        return when (actionOf(rule, cfg)) {
            RuleAction.OFF -> Verdict.Pass
            RuleAction.HIDE -> Verdict.Hide
            RuleAction.GREY -> Verdict.Replace("§8" + clean)
            RuleAction.COMPACT ->
                if (short?.isEmpty() == true) Verdict.Hide
                else Verdict.Replace(short?.let { ChatRules.theme(it, cfg) } ?: raw)
            RuleAction.COMPACT_GREY ->
                if (short?.isEmpty() == true) Verdict.Hide
                else Verdict.Replace("§8" + ChatRules.clean(short ?: raw))
        }
    }

    /** Override par règle, sinon réglage du groupe, sinon défaut de la règle. */
    fun actionOf(rule: Rule, cfg: RuleConfig): RuleAction =
        cfg.actions[rule.id] ?: cfg.groupActions[rule.group.id] ?: rule.default

    private fun enabled(category: Category, cfg: RuleConfig): Boolean = when (category) {
        Category.LOBBY -> cfg.lobbyEnabled
        Category.SKYBLOCK -> cfg.skyblockEnabled
        Category.SYSTEM -> cfg.systemEnabled
    }
}
