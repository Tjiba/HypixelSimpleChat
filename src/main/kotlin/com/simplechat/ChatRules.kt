package com.simplechat

import java.util.regex.Pattern

/** Moteur pur : texte brut Hypixel -> Verdict. Aucun import Minecraft. */
object ChatRules {

    private val COLOR_CODE = Pattern.compile("[§&][0-9a-fk-orA-FK-OR]")

    /** Retire les codes couleur/format et compacte les espaces. */
    fun clean(raw: String): String =
        COLOR_CODE.matcher(raw).replaceAll("").replace(Regex("\\s+"), " ").trim()

    // Avertissement anti-phishing greffé par Hypixel aux messages contenant "discord" (suffixe inline).
    private val DISCORD_WARNING = Pattern.compile(
        "(?:[§&][0-9a-fk-orA-FK-OR]|\\s)*Please be mindful of Discord links in chat as they may pose a security risk\\.?(?:[§&][0-9a-fk-orA-FK-OR]|\\s)*$",
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
    // Message joueur public : (préfixe niveau/emblème optionnel) rank/nom puis ": ".
    // [NPC] est un dialogue SYSTEM, pas un rank -> exclu via lookahead.
    private val CH_PUBLIC = Pattern.compile("^(?!\\[NPC] )(?!\\[BOSS] )(?!\\[STATUE] )(?:\\[\\d{1,4}] )?(?:[^\\[\\w\\s]\\S* )?(?:\\[[A-Za-z+]+] )?[\\w]+: .+")

    // Lignes de boss / invocations (masquées via le toggle SkyBlock "Hide boss messages").
    private val BOSS_LINE = Pattern.compile("^\\[(?:BOSS|STATUE)] |^ *[A-Z][A-Z ]+ DOWN!| has spawned!$")

    fun classify(clean: String): Channel = when {
        CH_GUILD.matcher(clean).find() -> Channel.GUILD
        CH_OFFICER.matcher(clean).find() -> Channel.OFFICER
        CH_PARTY.matcher(clean).find() -> Channel.PARTY
        CH_WHISPER.matcher(clean).find() -> Channel.WHISPER
        CH_PUBLIC.matcher(clean).find() -> Channel.PUBLIC
        else -> Channel.SYSTEM
    }

    fun evaluate(raw: String, cfg: RuleConfig): Verdict {
        if (!cfg.masterEnabled) return Verdict.Pass
        val clean = clean(raw)
        if (clean.isEmpty()) return Verdict.Pass
        if (HARD_PASS.matcher(clean).matches()) return Verdict.Pass

        when (classify(clean)) {
            Channel.WHISPER -> return Verdict.Pass
            Channel.GUILD, Channel.OFFICER, Channel.PARTY, Channel.PUBLIC -> {
                val ch = classify(clean)
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

        // Une règle v1 curée qui matche décide seule (OFF = laisser tel quel, sans repli hide-useless).
        val rule = RULES.firstOrNull { it.matches(clean) }
        if (rule != null) {
            if (rule.category == Category.LOBBY && !cfg.lobbyEnabled) return Verdict.Pass
            if (rule.category == Category.SKYBLOCK && !cfg.skyblockEnabled) return Verdict.Pass
            if (rule.category == Category.SYSTEM && !cfg.systemEnabled) return Verdict.Pass
            return when (cfg.actionOf(rule.id, rule.default)) {
                RuleAction.OFF -> Verdict.Pass
                RuleAction.HIDE -> Verdict.Hide
                RuleAction.GREY -> Verdict.Replace("§8" + clean)
                RuleAction.COMPACT -> Verdict.Replace(rule.beautify(clean, raw))
                RuleAction.COMPACT_GREY -> Verdict.Replace("§8" + clean(rule.beautify(clean, raw)))
            }
        }
        // Compact + survol (style SkyHanni).
        CompactRules.compact(clean, cfg)?.let { return it }
        // Groupes de spam : action par groupe (COMPACT/GREY pour les groupes = message nettoyé §7/§8).
        HideUseless.matchedGroup(raw, cfg)?.let { group ->
            return when (cfg.hideGroupActions[group] ?: HideAction.HIDE) {
                HideAction.OFF -> Verdict.Pass
                HideAction.HIDE -> Verdict.Hide
                HideAction.GREY -> Verdict.Replace("§8" + clean)
                HideAction.COMPACT -> Verdict.Replace(raw) // compact = garde les couleurs d'origine
            }
        }
        // Patterns custom (toujours masqués).
        if (HideUseless.matchesCustom(raw, cfg)) return Verdict.Hide
        return Verdict.Pass
    }

    enum class Category { LOBBY, SKYBLOCK, SYSTEM }

    class Rule(
        val id: String,
        val category: Category,
        val default: RuleAction,
        private val match: Pattern,
        val beautify: (clean: String, raw: String) -> String = { _, raw -> raw }, // défaut compact = couleurs d'origine
    ) {
        fun matches(clean: String) = match.matcher(clean).find()
    }

    private val RULES: List<Rule> = listOf(
        Rule("lobby-join", Category.LOBBY, RuleAction.HIDE,
            Pattern.compile("^(>+ )?\\[?[A-Za-z+]* ?[\\w+]* ?]?.* joined the lobby!"),
            beautify = { _, raw ->
                raw.replaceFirst(Regex("^(?:\\s|>|§.)+"), "")
                    .replaceFirst(Regex("(?:§.)?\\s*joined the lobby!.*$"), "")
                    .trim() + " §8joined"
            }),
        Rule("booster-activated", Category.LOBBY, RuleAction.COMPACT,
            Pattern.compile("^Activated your booster\\. You now have .* of ([0-9.]+)x coins\\."),
            beautify = { c, _ ->
                val mult = Regex("of ([0-9.]+)x").find(c)?.groupValues?.get(1) ?: "?"
                val hours = Regex("have (\\w+) hours?").find(c)?.groupValues?.get(1) ?: ""
                "§eBooster §f${mult}x§e coins §7· ${wordToHours(hours)}"
            }),
        Rule("mystery-reward", Category.LOBBY, RuleAction.COMPACT,
            Pattern.compile("^You have claimed a (\\[.+?]) reward card!"),
            beautify = { c, _ ->
                val item = Regex("claimed a (\\[.+?]) reward card").find(c)?.groupValues?.get(1) ?: "reward"
                "§6Daily reward §7· §f$item"
            }),
        Rule("claimed-currency", Category.LOBBY, RuleAction.COMPACT,
            Pattern.compile("^You have successfully claimed .+ and .+!"),
            beautify = { c, _ ->
                val body = c.removePrefix("You have successfully claimed ").removeSuffix("!")
                "§aClaimed §f" + body.replace(" and ", "§a, §f")
            }),
        Rule("npc-dialog", Category.SKYBLOCK, RuleAction.GREY,
            Pattern.compile("^\\[NPC] "),
            beautify = { _, raw -> raw.replaceFirst(Regex("\\[NPC]\\s*"), "") }),
        Rule("boss", Category.SKYBLOCK, RuleAction.HIDE, BOSS_LINE,
            beautify = { _, raw -> raw.replaceFirst(Regex("\\[(?:BOSS|STATUE)]\\s*"), "") }),
        Rule("radiating-generosity", Category.LOBBY, RuleAction.COMPACT,
            Pattern.compile("^You are still radiating with "),
            beautify = { _, _ -> "§eRadiating generosity" }),
        Rule("playtime-ticket", Category.LOBBY, RuleAction.COMPACT,
            Pattern.compile("^PLAYTIME! You gained "),
            beautify = { c, _ ->
                val item = c.substringAfter("You gained ").removeSuffix("!")
                    .removePrefix("a ").removePrefix("Playtime ").trim()
                "§ePlaytime §7· §a+$item"
            }),
        Rule("reward-link", Category.LOBBY, RuleAction.COMPACT,
            Pattern.compile("Click the link to visit our website and claim your reward"),
            beautify = { c, _ ->
                val link = c.substringAfter("reward: ", "").trim()
                if (link.isEmpty()) "§6Claim reward" else "§6Claim reward: §b$link"
            }),
        Rule("morph-wardrobe", Category.LOBBY, RuleAction.COMPACT,
            Pattern.compile("^(You are now morphed|You selected .+ Cloak|Reset your (Cloak|Morph)|Morph reset\\.|Right-Click with the .* to activate)"),
            beautify = { c, _ ->
                when {
                    c.startsWith("You are now morphed") -> {
                        // "morphed into a Cow!" ou "morphed as a Cow Morph for 5 minutes."
                        val m = Regex("morphed (?:into|as) (?:an? )?(.+?)(?: Morph)?(?: for .+)?[!.]*$")
                            .find(c)?.groupValues?.get(1) ?: c
                        "§7Morphed §8→ §f$m"
                    }
                    c.startsWith("You selected") -> {
                        val name = c.substringAfter("You selected ").removeSuffix("!")
                            .removePrefix("the ").removeSuffix(" Cloak").trim()
                        "§7Cloak §8→ §f$name"
                    }
                    c.startsWith("Morph reset") -> "§7Reset morph"
                    c.startsWith("Reset your") ->
                        "§7Reset " + c.substringAfter("Reset your ").removeSuffix("!").trim().lowercase()
                    else -> {
                        val item = Regex("Right-Click with the (.+?)(?: selected)? to activate").find(c)?.groupValues?.get(1)
                        if (item != null) "§7Right-click §f$item §7to activate" else "§7$c"
                    }
                }
            }),
        Rule("sacks", Category.SKYBLOCK, RuleAction.GREY,
            Pattern.compile("^\\[Sacks] \\+"),
            beautify = { c, _ -> "§a" + c.removePrefix("[Sacks] ") }),
        Rule("loot-share", Category.SKYBLOCK, RuleAction.COMPACT,
            Pattern.compile("^LOOT SHARE You received loot for assisting "),
            beautify = { c, _ ->
                val who = Regex("assisting (.+?)!*$").find(c)?.groupValues?.get(1) ?: "?"
                "§6Loot share §7· §f$who"
            }),
        Rule("profile-id", Category.SYSTEM, RuleAction.COMPACT,
            Pattern.compile("^Profile ID: "),
            beautify = { c, _ -> "§8" + c.removePrefix("Profile ID: ") }),
        Rule("server-routing", Category.SYSTEM, RuleAction.COMPACT,
            Pattern.compile("^(Sending to server .+\\.\\.\\.|Warping\\.\\.\\.|Request join for Hub #\\d+ \\(.+\\)\\.\\.\\.|Sending you to .+!)"),
            beautify = { c, _ ->
                val srv = Regex("Sending to server (.+?)\\.\\.\\.").find(c)?.groupValues?.get(1)
                if (srv != null) "§8→ §7$srv" else "§7Warping..."
            }),
        Rule("pet-spawn", Category.LOBBY, RuleAction.COMPACT,
            Pattern.compile("^(Spawned|Despawned) your .+"),
            beautify = { c, _ ->
                val name = c.substringAfter("your ").removeSuffix("!").trim()
                if (c.startsWith("Spawned")) "§a+ §f$name" else "§c- §f$name"
            }),
        // Pets SkyBlock : même logique +/- que le lobby, mais la couleur de rareté du raw est gardée.
        Rule("pet-summon", Category.SKYBLOCK, RuleAction.COMPACT,
            Pattern.compile("^You (summoned|despawned) your .+"),
            beautify = { c, raw ->
                val name = raw.substringAfter("your ").trim().replace(Regex("(?:§.)*!(?:§.)*\\s*$"), "")
                if (c.startsWith("You summoned")) "§a+ §r$name" else "§c- §r$name"
            }),
        Rule("damage-spam", Category.SKYBLOCK, RuleAction.GREY,
            Pattern.compile("^Your (.+?) hit \\d+ enem(y|ies) for [0-9,.]+ damage\\."),
            beautify = { c, _ ->
                val ability = Regex("Your (.+?) hit").find(c)?.groupValues?.get(1) ?: "?"
                val dmg = Regex("for ([0-9,.]+) damage").find(c)?.groupValues?.get(1) ?: ""
                "§6$ability §7· §c${shortNum(dmg)}"
            }),
        Rule("kill-combo", Category.SKYBLOCK, RuleAction.GREY,
            Pattern.compile("^\\+\\d+ Kill Combo "),
            beautify = { c, _ ->
                val combo = Regex("\\+(\\d+) Kill Combo").find(c)?.groupValues?.get(1) ?: "?"
                val bonus = c.substringAfter("Kill Combo").replace("✯", "").replace(Regex("\\s+"), " ").trim()
                "§6+$combo Combo §7$bonus"
            }),
        Rule("mob-ability", Category.SKYBLOCK, RuleAction.GREY,
            Pattern.compile("^.+ used .+ on you hitting you for [0-9,.]+ damage"),
            beautify = { c, _ ->
                val ability = Regex("used (.+?) on you").find(c)?.groupValues?.get(1) ?: "?"
                val dmg = Regex("for ([0-9,.]+) damage").find(c)?.groupValues?.get(1) ?: ""
                "§c$ability §7· §f-${shortNum(dmg)}"
            }),
        Rule("rare-reward", Category.SKYBLOCK, RuleAction.COMPACT,
            Pattern.compile("^RARE REWARD! .+ found a .+ in their .+ Chest!"),
            beautify = { c, _ ->
                val who = Regex("RARE REWARD! (.+?) found a").find(c)?.groupValues?.get(1) ?: "?"
                val item = Regex("found a (.+?) in their").find(c)?.groupValues?.get(1) ?: ""
                val chest = Regex("in their (.+?) Chest").find(c)?.groupValues?.get(1) ?: ""
                "§6§lRARE REWARD §r§f$who §7· §f$item §7in §f$chest Chest"
            }),
        Rule("gexp", Category.SKYBLOCK, RuleAction.COMPACT,
            Pattern.compile("^You earned [\\d,]+ GEXP \\+ [\\d,]+ Event EXP from playing"),
            beautify = { c, _ ->
                val gexp = Regex("earned ([\\d,]+) GEXP").find(c)?.groupValues?.get(1) ?: "?"
                val event = Regex("\\+ ([\\d,]+) Event EXP").find(c)?.groupValues?.get(1) ?: "?"
                "§2$gexp GEXP §a+ §e$event Event EXP"
            }),
    )

    /** "2,637,430.3" -> "2.6M". Abrège les gros nombres pour les lignes compactes. */
    private fun shortNum(raw: String): String {
        val n = raw.replace(",", "").substringBefore(".").toLongOrNull() ?: return raw
        val l = java.util.Locale.ROOT
        return when {
            n >= 1_000_000_000 -> String.format(l, "%.1fB", n / 1_000_000_000.0)
            n >= 1_000_000 -> String.format(l, "%.1fM", n / 1_000_000.0)
            n >= 1_000 -> String.format(l, "%.1fk", n / 1_000.0)
            else -> n.toString()
        }
    }

    private fun wordToHours(word: String): String = when (word.lowercase()) {
        "one" -> "1h"; "two" -> "2h"; "three" -> "3h"; "four" -> "4h"
        "five" -> "5h"; "six" -> "6h"; else -> word.toIntOrNull()?.let { "${it}h" } ?: word
    }
}
