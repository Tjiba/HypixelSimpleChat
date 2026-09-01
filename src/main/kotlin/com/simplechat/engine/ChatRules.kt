package com.simplechat.engine

import com.simplechat.CompactRules
import com.simplechat.config.RuleConfig
import com.simplechat.rules.Registry
import java.util.regex.Pattern

/** Moteur pur : texte brut Hypixel -> Verdict. Aucun import Minecraft. */
object ChatRules {

    private val COLOR_CODE = Pattern.compile(LegacyText.CODE)

    /** Retire les codes couleur/format et compacte les espaces. */
    fun clean(raw: String): String =
        COLOR_CODE.matcher(raw).replaceAll("").replace(Regex("\\s+"), " ").trim()

    // Avertissement anti-phishing greffé par Hypixel aux messages contenant "discord" (suffixe inline).
    private val DISCORD_WARNING = Pattern.compile(
        "(?:${LegacyText.CODE}|\\s)*Please be mindful of Discord links in chat as they may pose a security risk\\.?(?:${LegacyText.CODE}|\\s)*$",
        Pattern.CASE_INSENSITIVE)

    /** Retire l'avertissement Discord greffé par Hypixel, en gardant le reste du message. */
    fun stripDiscordWarning(raw: String): String =
        DISCORD_WARNING.matcher(raw).replaceFirst("")

    /** Clé de collapse « intelligente » : normalise les nombres pour fusionner
     *  "hit 3 for 2,200 damage" et "hit 4 for 999 damage" en une seule ligne comptée. */
    fun collapseKey(clean: String): String = clean.replace(Regex("\\d[\\d,.]*"), "#")

    // Jamais touché : MP + co-op.
    private val HARD_PASS = Pattern.compile("^(Co-op) > .*|^(From|To) .+", Pattern.DOTALL)

    // Détection de canal sur texte nettoyé.
    private val CH_GUILD = Pattern.compile("^(Guild|G) > .+")
    private val CH_OFFICER = Pattern.compile("^(Officer|O) > .+")
    private val CH_PARTY = Pattern.compile("^(Party|P) > .+")
    private val CH_WHISPER = Pattern.compile("^(From|To) .+")
    /** Les PNJ des Catacombes parlent sans rang : "Mort: Good luck." passerait pour un message de
     *  joueur. Liste partagée avec les règles Dungeons qui les masquent. */
    const val DUNGEON_SPEAKERS = "Mort|Fairy|The Watcher"

    /** Les boss non plus n'ont ni rang ni tag depuis qu'Hypixel a lâché « [BOSS] ». Un étage des
     *  Catacombes pour la règle Dungeons, le reste de SkyBlock pour la règle Combat. */
    const val FLOOR_BOSSES = "Bonzo|Scarf|The Professor|Thorn|Livid|Sadan|Necron|Maxor|Storm|Goldor"
    const val WORLD_BOSSES = "Arachne|Endstone Protector|Barbarian Duke X|Mage Outlaw|Ashfang|" +
        "Bladesoul|Magma Boss|Kuudra|Headless Horseman|Sea Emperor|Water Hydra|Reindrake|Grim Reaper"

    private val CH_SPEAKER = Pattern.compile(
        "^(?:\\[(?:BOSS|NPC)] )?(?:Master )?(?:$DUNGEON_SPEAKERS|$FLOOR_BOSSES|$WORLD_BOSSES): ")

    // Message joueur public : (préfixe niveau/emblème optionnel) rank/nom puis ": ".
    // [NPC] est un dialogue SYSTEM, pas un rank -> exclu via lookahead.
    private val CH_PUBLIC = Pattern.compile("^(?!\\[NPC] )(?!\\[BOSS] )(?!\\[STATUE] )(?:\\[\\d{1,4}] )?(?:[^\\[\\w\\s]\\S* )?(?:\\[[A-Za-z+]+] )?[\\w]+: .+")

    fun classify(clean: String): Channel = when {
        CH_GUILD.matcher(clean).find() -> Channel.GUILD
        CH_OFFICER.matcher(clean).find() -> Channel.OFFICER
        CH_PARTY.matcher(clean).find() -> Channel.PARTY
        CH_WHISPER.matcher(clean).find() -> Channel.WHISPER
        CH_PUBLIC.matcher(clean).find() && !CH_SPEAKER.matcher(clean).find() -> Channel.PUBLIC
        else -> Channel.SYSTEM
    }

    fun evaluate(raw: String, cfg: RuleConfig): Verdict {
        if (!cfg.masterEnabled) return Verdict.Pass
        val clean = clean(raw)
        if (clean.isEmpty()) return Verdict.Pass
        // Les MP ne sont jamais touchés, sauf sourdine : elle couvre les deux sens.
        if (HARD_PASS.matcher(clean).matches())
            return if (Mute.matches(clean, Channel.WHISPER, cfg)) Verdict.Hide else Verdict.Pass

        when (classify(clean)) {
            Channel.WHISPER -> return Verdict.Pass
            Channel.GUILD, Channel.OFFICER, Channel.PARTY, Channel.PUBLIC -> {
                val ch = classify(clean)
                // Avant le mode : un joueur en sourdine reste masqué même en affichage vanilla.
                if (Mute.matches(clean, ch, cfg)) return Verdict.Hide
                val style = when (ch) {
                    Channel.PARTY -> cfg.partyStyle
                    Channel.PUBLIC -> cfg.publicStyle
                    else -> cfg.guildStyle
                }
                if (!style.enabled || !style.compact) return Verdict.Pass
                ChannelFormat.format(raw, ch, cfg)?.let { return Verdict.Segments(it) }
                return Verdict.Pass
            }
            Channel.SYSTEM -> { /* règles v1 ci-dessous */ }
        }

        // Compact + survol (style SkyHanni) : passe avant le registre, il a son propre réglage.
        CompactRules.compact(clean, cfg)?.let { return it }
        // Une règle du registre qui matche décide seule (OFF = laisser tel quel, sans repli).
        Registry.match(clean, raw, cfg)?.let { return it }
        // Patterns saisis par le joueur (toujours masqués).
        if (CustomHide.matches(raw, cfg)) return Verdict.Hide
        return Verdict.Pass
    }

    /** Thème compact : recolore les mots blancs (§f) de NOS compacts avec la couleur choisie. */
    internal fun theme(s: String, cfg: RuleConfig): String =
        if (!cfg.compactTheme) s else s.replace("§f", "§#%06X".format(cfg.compactThemeColor and 0xFFFFFF))
}
