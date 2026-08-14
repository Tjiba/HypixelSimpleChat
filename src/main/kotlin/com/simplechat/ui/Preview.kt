package com.simplechat.ui

import com.simplechat.BazaarSummary
import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.LegacyText
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Seg
import com.simplechat.engine.SelfPlayer
import com.simplechat.engine.Verdict
import com.simplechat.rules.Fmt
import com.simplechat.rules.Registry
import com.simplechat.rules.Rule

/**
 * Échantillons de chat rendus avec la config courante, pour l'aperçu live. Pur.
 * L'ordre vient de la liste de réglages affichée : une ligne d'aperçu suit son réglage.
 */
object Preview {

    // Canaux joueur : pas de règles derrière, seulement du formatage — exemples écrits à la main.
    // Un message d'un autre membre puis un du joueur : le pseudo du joueur porte sa couleur, celui
    // du membre celle de tout le monde — les deux lignes se comparent d'un coup d'œil.
    private fun guild(self: SelfPlayer?) = listOf(
        "Guild > BotName: G > DiscordUser: hey from discord",
        "Guild > [MVP++] BotName: [V1] DiscordUser: gg wp",
        "Guild > §b[MVP§c+§b] Player §7[Member]§f: gg",
        "Guild > ${who(self, "§b[MVP§c+§b] ", "Player")} §7[Member]§f: hey all",
        "Officer > §6[MVP§1++§6] Officer §e[Staff]§f: on it",
    )
    private val PARTY = listOf(
        "Party > §b[MVP§c+§b] Friend§8: §ron my way",
    )
    private fun public(self: SelfPlayer?) = listOf(
        "§8[§d330§8] §6⛃ §8[§6MVP§1++§8] §6Player§8:§r wanna trade?",
        "${level(self, "§8[§d330§8]")} §6⛃ ${who(self, "§8[§6MVP§1++§8] §6", "MeteoFrance")}§8:§r selling stuff",
    )

    /** Ce qu'Hypixel écrit devant un message, et rien d'autre : "[MVP++] Nom". */
    private val RANKED_NAME = Regex("^\\[[A-Z+]+] \\w{3,16}$")

    /**
     * Le joueur avec son rang Hypixel. La liste des joueurs ne le donne pas toujours : sur SkyBlock
     * elle sert aussi de tableau de bord et son entrée porte son niveau et son emblème à la place
     * du rang — recopiée telle quelle, la ligne sortirait avec deux niveaux et un emblème échoué
     * derrière le pseudo. Son pseudo garde alors sa place sur le rang de l'exemple : c'est lui
     * qu'il vient chercher dans l'aperçu, pas le rang.
     */
    private fun who(self: SelfPlayer?, sampleRank: String, sampleName: String): String {
        val player = self ?: return sampleRank + sampleName
        if (!RANKED_NAME.matches(ChatRules.clean(player.display))) return sampleRank + player.name
        return player.display
    }

    /** Le niveau que porte l'entrée de tab, "[372]", quand elle en porte un. */
    private val TAB_LEVEL = Regex("^\\[\\d{1,4}]")

    /**
     * Son niveau, couleur d'Hypixel comprise, repris de la liste des joueurs — c'est là que
     * SkyBlock l'écrit, à la place du rang. Sinon celui de l'exemple : les deux lignes de l'aperçu
     * ne doivent pas se ressembler au point qu'on ne voie plus laquelle parle de lui.
     */
    private fun level(self: SelfPlayer?, sample: String): String {
        val display = self?.display ?: return sample
        val tag = TAB_LEVEL.find(ChatRules.clean(display))?.value ?: return sample
        return Fmt.rawSpan(display, tag)
    }

    /** Catégorie factice : force l'aperçu à suivre les réglages passés, pas une page de canal. */
    const val SEARCH = "search"

    private val sampleByRule: Map<String, String> = Registry.rules.associate { it.id to it.sample }

    private val ruleIdsByGroup: Map<String, List<String>> =
        Registry.byGroup.entries.associate { (group, rules) -> group.id to rules.map { it.id } }

    private val ownerByRule: Map<String, String> = Registry.rules.associate { it.id to it.group.id }

    /**
     * Groupes qu'on ne peut pas déplier : l'aperçu est le seul endroit où voir tout ce qu'ils
     * couvrent. Les phrases qui ne produisent rien en sont exclues — celles laissées intactes
     * (OFF) et celles qui disparaissent toujours (compact vide).
     */
    private val wholePhrases: Map<String, List<String>> = Registry.groups.filterNot { it.split }
        .associate { group ->
            group.id to Registry.byGroup[group].orEmpty()
                .filter { it.default != RuleAction.OFF && !alwaysGone(it) }.map { it.sample }
        }

    /** Une phrase dont le compact est vide n'a pas d'aperçu : elle n'apparaît jamais en jeu. */
    private fun alwaysGone(rule: Rule): Boolean {
        val compact = rule.compact ?: return false
        val m = rule.match(ChatRules.clean(rule.sample), rule.sample) ?: return false
        return compact(m).isEmpty()
    }

    /**
     * Un bloc par réglage affiché : les phrases d'un même groupe déplié forment un seul bloc,
     * un groupe replié n'en montre qu'un exemple. Une ligne d'aperçu par ligne de réglage.
     */
    private fun blocksFor(ids: List<String>, batch: Line? = null): List<List<Msg>> {
        val listed = ids.toSet()
        val blocks = ArrayList<List<Msg>>()
        var phrases = ArrayList<String>()
        var owner: String? = null

        fun flush() {
            if (phrases.isNotEmpty()) blocks.add(msgs(phrases))
            phrases = ArrayList(); owner = null
        }

        for (id in ids) {
            // Le lot Bazaar n'a pas de règle : c'est son réglage de couleur qui porte son aperçu.
            if (id == BazaarSummary.SETTING && batch != null) {
                flush()
                blocks.add(listOf(Msg.Ready(batch)))
                continue
            }
            val sample = sampleByRule[id]
            if (sample != null) {
                if (ownerByRule[id] != owner) { flush(); owner = ownerByRule[id] }
                phrases.add(sample)
                continue
            }
            flush()
            val ruleIds = ruleIdsByGroup[id] ?: continue
            // Groupe déplié : ce sont ses phrases qui parlent, pas lui.
            if (ruleIds.none { it in listed }) blocks.add(msgs(
                wholePhrases[id]?.takeIf { it.isNotEmpty() }
                    ?: listOf(sampleByRule.getValue(ruleIds.first()))))
        }
        flush()
        return blocks
    }

    /** Une ligne d'aperçu : ses segments, et le survol qu'elle porte en jeu (null = aucun). */
    data class Line(val segs: List<Seg>, val hover: String? = null)

    /** Ce qu'un réglage met dans l'aperçu : une phrase à passer aux règles, ou une ligne déjà
     *  faite pour ce qui n'en a pas — le lot Bazaar. Le rendu attend le rognage. */
    private sealed class Msg {
        class Sample(val raw: String) : Msg()
        class Ready(val line: Line) : Msg()
    }

    private fun msgs(raws: List<String>): List<Msg> = raws.map { Msg.Sample(it) }

    /** Le lot Bazaar tel qu'il sortirait en jeu, survol compris : il ne passe par aucune règle. */
    private fun bazaarBatch(cfg: RuleConfig): Line = BazaarSummary.preview(cfg)
        .let { Line(LegacyText.parse(it.shortLegacy), it.hoverLegacy) }

    // Un message masqué est montré barré : l'aperçu sert justement à voir ce qui disparaît.
    private fun render(cfg: RuleConfig, raw: String): Line =
        when (val v = ChatRules.evaluate(raw, cfg)) {
            is Verdict.Segments -> Line(v.segs)
            is Verdict.Compact -> Line(LegacyText.parse(v.shortLegacy), v.hoverLegacy)
            is Verdict.Replace -> Line(LegacyText.parse(v.legacy))
            Verdict.Hide -> Line(listOf(Seg(ChatRules.clean(raw), 0x555555, strikethrough = true)))
            Verdict.Pass -> Line(LegacyText.parse(raw))
        }

    private val TS_FMT = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Lignes rendues pour la page affichée : [ids] = les réglages listés à partir du premier
     * visible, dans l'ordre. [limit] borne le travail, l'aperçu est de toute façon rogné.
     */
    fun forSettings(cfg: RuleConfig, categoryId: String?, ids: List<String>, limit: Int = 30): List<Line> {
        val blocks = when (categoryId) {
            "Guild Chat" -> listOf(msgs(guild(cfg.self)))
            "Party Chat" -> listOf(msgs(PARTY))
            "Public Chat" -> listOf(msgs(public(cfg.self)))
            // page racine : réglages globaux, les 3 canaux
            null -> listOf(msgs(public(cfg.self)), msgs(PARTY), msgs(guild(cfg.self)))
            else -> capped(blocksFor(ids, bazaarBatch(cfg)), limit)
        }
        val ts = if (cfg.showTimestamps) Seg("[${java.time.LocalTime.now().format(TS_FMT)}] ", cfg.timestampColor) else null

        // Ligne vide entre deux blocs : on voit d'un coup d'œil à quel réglage chaque ligne appartient.
        val lines = ArrayList<Line>()
        for (block in blocks) {
            if (lines.isNotEmpty()) lines.add(Line(emptyList()))
            for (msg in block) {
                val line = when (msg) {
                    is Msg.Sample -> render(cfg, msg.raw)
                    is Msg.Ready -> msg.line
                }
                lines.add(if (ts == null) line else line.copy(segs = listOf(ts) + line.segs))
            }
        }
        return lines
    }

    /** Coupe à [limit] messages au total, sans casser un bloc en cours. */
    private fun capped(blocks: List<List<Msg>>, limit: Int): List<List<Msg>> {
        val kept = ArrayList<List<Msg>>()
        var count = 0
        for (block in blocks) {
            if (count >= limit) break
            kept.add(block)
            count += block.size
        }
        return kept
    }
}
