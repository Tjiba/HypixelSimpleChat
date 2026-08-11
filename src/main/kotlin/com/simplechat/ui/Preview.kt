package com.simplechat.ui

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.LegacyText
import com.simplechat.engine.Seg
import com.simplechat.engine.Verdict
import com.simplechat.rules.Registry

/**
 * Échantillons de chat rendus avec la config courante, pour l'aperçu live. Pur.
 * L'ordre vient de la liste de réglages affichée : une ligne d'aperçu suit son réglage.
 */
object Preview {

    // Canaux joueur : pas de règles derrière, seulement du formatage — exemples écrits à la main.
    private val GUILD = listOf(
        "Guild > BotName: G > DiscordUser: hey from discord",
        "Guild > [MVP++] BotName: [V1] DiscordUser: gg wp",
        "Guild > §b[MVP§c+§b] Player §7[Member]§f: hey all",
        "Officer > §6[MVP§1++§6] Officer §e[Staff]§f: on it",
    )
    private val PARTY = listOf(
        "Party > §b[MVP§c+§b] Friend§8: §ron my way",
    )
    private val PUBLIC = listOf(
        "§8[§d330§8] §6⛃ §8[§6MVP§1++§8] §6MeteoFrance§8:§r selling stuff",
    )

    /** Catégorie factice : force l'aperçu à suivre les réglages passés, pas une page de canal. */
    const val SEARCH = "search"

    private val sampleByRule: Map<String, String> = Registry.rules.associate { it.id to it.sample }

    private val ruleIdsByGroup: Map<String, List<String>> =
        Registry.byGroup.entries.associate { (group, rules) -> group.id to rules.map { it.id } }

    /**
     * Exemples des réglages [ids], dans leur ordre d'affichage. Un réglage de phrase donne sa
     * ligne ; un réglage de groupe donne toutes les siennes, sauf si ses phrases sont listées
     * juste après — ce sont elles qui portent l'aperçu. Un réglage sans règle est sauté.
     */
    internal fun samplesFor(ids: List<String>): List<String> = blocksFor(ids).flatten()

    private val ownerByRule: Map<String, String> = Registry.rules.associate { it.id to it.group.id }

    /**
     * Un bloc par réglage affiché : les phrases d'un même groupe déplié forment un seul bloc,
     * un groupe replié n'en montre qu'un exemple. Une ligne d'aperçu par ligne de réglage.
     */
    private fun blocksFor(ids: List<String>): List<List<String>> {
        val listed = ids.toSet()
        val blocks = ArrayList<List<String>>()
        var phrases = ArrayList<String>()
        var owner: String? = null

        fun flush() {
            if (phrases.isNotEmpty()) blocks.add(phrases)
            phrases = ArrayList(); owner = null
        }

        for (id in ids) {
            val sample = sampleByRule[id]
            if (sample != null) {
                if (ownerByRule[id] != owner) { flush(); owner = ownerByRule[id] }
                phrases.add(sample)
                continue
            }
            flush()
            val ruleIds = ruleIdsByGroup[id] ?: continue
            // Groupe déplié : ce sont ses phrases qui parlent, pas lui.
            if (ruleIds.none { it in listed }) blocks.add(listOf(sampleByRule.getValue(ruleIds.first())))
        }
        flush()
        return blocks
    }

    // Un message masqué est montré barré : l'aperçu sert justement à voir ce qui disparaît.
    private fun render(cfg: RuleConfig, raw: String): List<Seg> =
        when (val v = ChatRules.evaluate(raw, cfg)) {
            is Verdict.Segments -> v.segs
            is Verdict.Compact -> LegacyText.parse(v.shortLegacy)
            is Verdict.Replace -> LegacyText.parse(v.legacy)
            Verdict.Hide -> listOf(Seg(ChatRules.clean(raw), 0x555555, strikethrough = true))
            Verdict.Pass -> LegacyText.parse(raw)
        }

    private val TS_FMT = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Lignes rendues pour la page affichée : [ids] = les réglages listés à partir du premier
     * visible, dans l'ordre. [limit] borne le travail, l'aperçu est de toute façon rogné.
     */
    fun forSettings(cfg: RuleConfig, categoryId: String?, ids: List<String>, limit: Int = 30): List<List<Seg>> {
        val blocks = when (categoryId) {
            "Guild Chat" -> listOf(GUILD)
            "Party Chat" -> listOf(PARTY)
            "Public Chat" -> listOf(PUBLIC)
            null -> listOf(PUBLIC, PARTY, GUILD) // page racine : réglages globaux, les 3 canaux
            else -> capped(blocksFor(ids), limit)
        }
        val ts = if (cfg.showTimestamps) Seg("[${java.time.LocalTime.now().format(TS_FMT)}] ", cfg.timestampColor) else null

        // Ligne vide entre deux blocs : on voit d'un coup d'œil à quel réglage chaque ligne appartient.
        val lines = ArrayList<List<Seg>>()
        for (block in blocks) {
            if (lines.isNotEmpty()) lines.add(emptyList())
            for (raw in block) {
                val segs = render(cfg, raw)
                lines.add(if (ts == null) segs else listOf(ts) + segs)
            }
        }
        return lines
    }

    /** Coupe à [limit] messages au total, sans casser un bloc en cours. */
    private fun capped(blocks: List<List<String>>, limit: Int): List<List<String>> {
        val kept = ArrayList<List<String>>()
        var count = 0
        for (block in blocks) {
            if (count >= limit) break
            kept.add(block)
            count += block.size
        }
        return kept
    }
}
