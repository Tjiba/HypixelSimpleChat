package com.simplechat

import java.util.regex.Pattern

/** Formatteur pur par canal → segments RGB (null = ne pas toucher). Aucun import Minecraft. */
object ChannelFormat {

    // Niveau public en tête, sur texte nettoyé : "[330] ".
    private val LEVEL_HEAD = Pattern.compile("^\\[(\\d{1,4})] ")

    // Couleur du niveau SkyBlock par palier de 40 (schéma officiel Hypixel).
    private val LEVEL_COLORS = intArrayOf(
        0xAAAAAA, // 0-39   gris
        0xFFFFFF, // 40-79  blanc
        0xFFFF55, // 80-119 jaune
        0x55FF55, // 120-159 vert
        0x00AA00, // 160-199 vert foncé
        0x55FFFF, // 200-239 aqua
        0x00AAAA, // 240-279 cyan
        0x5555FF, // 280-319 bleu
        0xFF55FF, // 320-359 rose
        0xAA00AA, // 360-399 violet
        0xFFAA00, // 400-439 or
        0xFF5555, // 440-479 rouge
        0xAA0000, // 480+    rouge foncé
    )

    private fun levelColor(level: Int): Int =
        LEVEL_COLORS[(level / 40).coerceIn(0, LEVEL_COLORS.size - 1)]

    // Rank de guilde en suffixe du nom : "Foo [Member]" -> nom + tag.
    private val GUILD_RANK_SUFFIX = Regex("^(.*?) (\\[[^\\]]+])$")
    private const val DIM = 0x555555
    private const val BRACKET = 0xAAAAAA // gris des crochets/emblème (seul le numéro de niveau est coloré)

    // Corps du message : garde les couleurs d'origine (recolor off) ou force messageColor.
    private fun messageSegs(rawMsg: String, style: ChannelStyle): List<Seg> =
        if (style.recolorMessage) listOf(Seg(ChatRules.clean(rawMsg), style.messageColor))
        else LegacyText.parse(rawMsg)

    // Regex portées de GuildZip.ChatFormatter (détection bridge guilde).
    private val BRIDGE_HEADER = Pattern.compile(
        "^(Guild|Officer|G|O) > (?:\\[([A-Z+]+)] )?([\\w]+)(?:\\s+\\[([A-Za-z0-9+_]+)])?:\\s*(.+)$")
    private val CHANNEL_MARKER = Pattern.compile("^([A-Z0-9]{1,2}) > .+")
    private val GUILD_VERSION_TAG = Pattern.compile("^\\[(V[123])]\\s+", Pattern.CASE_INSENSITIVE)

    // En-tête simple de canal (party/officer non-bridge) sur texte nettoyé.
    private val CHANNEL_HEAD = Pattern.compile("^(?:Guild|Officer|Party|G|O|P) > (.+)$")

    fun format(raw: String, channel: Channel, cfg: RuleConfig): List<Seg>? = when (channel) {
        Channel.PUBLIC -> formatPublic(raw, cfg)
        Channel.PARTY -> formatParty(raw, cfg)
        Channel.GUILD, Channel.OFFICER -> formatGuild(raw, channel, cfg)
        else -> null
    }

    private fun formatPublic(raw: String, cfg: RuleConfig): List<Seg>? {
        val clean = ChatRules.clean(raw)
        // Niveau [330] puis emblème optionnels (avant le rank), sur texte nettoyé.
        var rest = clean
        var level: String? = null
        val lm = LEVEL_HEAD.matcher(rest)
        if (lm.find()) { level = lm.group(1); rest = rest.substring(lm.end()) }
        // Emblème générique : un glyphe symbole en tête (pas '[', pas alphanum), suivi d'un espace.
        // Hypixel en a beaucoup (⛃ ☠ ⚑ ➷ …), pas seulement le pioche/coffre.
        var emblem: String? = null
        val first = rest.firstOrNull()
        if (first != null && first != '[' && !first.isLetterOrDigit() && first != '_') {
            val sp = rest.indexOf(' ')
            if (sp > 0) { emblem = rest.substring(0, sp); rest = rest.substring(sp + 1) }
        }

        val sep = rest.indexOf(": ")
        if (sep < 0) return null
        val nameHead = rest.substring(0, sep) // "[MVP++] MeteoFrance"
        val msg = rest.substring(sep + 2)

        val out = ArrayList<Seg>()
        // Niveau / emblème : couleur du palier SkyBlock (level/40).
        val lvlColor = levelColor(level?.toIntOrNull() ?: 0)
        if (level != null && !cfg.prefix.hideLevel) {
            out.add(Seg("[", BRACKET)); out.add(Seg(level, lvlColor)); out.add(Seg("]", BRACKET)); out.add(Seg(" ", null))
        }
        if (emblem != null && !cfg.prefix.hideEmblem) { out.addAll(rawColored(raw, emblem)); out.add(Seg(" ", null)) }
        out.addAll(rankNameSegs(raw, nameHead, cfg.publicStyle))
        out.add(Seg(": ", DIM))
        out.addAll(messageSegs(rawMessageSlice(raw, rest.substring(0, sep + 2), msg), cfg.publicStyle))
        return out
    }

    private fun formatParty(raw: String, cfg: RuleConfig): List<Seg>? {
        val clean = ChatRules.clean(raw)
        val m = CHANNEL_HEAD.matcher(clean)
        if (!m.matches()) return null
        val rest = m.group(1) // "[MVP+] Foo: go next"
        val sep = rest.indexOf(": ")
        if (sep < 0) return null
        val nameHead = rest.substring(0, sep)               // "[MVP+] Foo"
        val msg = rest.substring(sep + 2)
        val out = ArrayList<Seg>()
        out.add(Seg(cfg.partyPrefix, cfg.partyStyle.prefixColor))
        out.add(Seg(" > ", DIM))
        out.addAll(rankNameSegs(raw, nameHead, cfg.partyStyle))
        out.add(Seg(" : ", DIM))
        out.addAll(messageSegs(rawMessageSlice(raw, rest.substring(0, sep + 2), msg), cfg.partyStyle))
        return out
    }

    // Portage de GuildZip.ChatFormatter : relais bridge (V1/V2/V3, alias) OU message guilde normal (rank+nom).
    private fun formatGuild(raw: String, channel: Channel, cfg: RuleConfig): List<Seg>? {
        val clean = ChatRules.clean(raw)
        val head = CHANNEL_HEAD.matcher(clean)
        if (!head.matches()) return null
        val isOff = channel == Channel.OFFICER
        val prefix = if (isOff) cfg.bridge.officerPrefix else cfg.bridge.guildPrefix
        val prefixColor = if (isOff) cfg.bridge.officerPrefixColor else cfg.bridge.guildPrefixColor

        // Bridge Discord uniquement si le payload porte un marker de canal ou un tag version.
        val m = BRIDGE_HEADER.matcher(clean)
        if (m.matches()) {
            val payload = m.group(5)
            if (hasChannelMarker(payload) || hasGuildVersionTag(payload)) {
                val botFilter = cfg.bridge.botMcName.ifEmpty { null }
                if (botFilter != null && !botFilter.equals(m.group(3), ignoreCase = true)) return null
                return bridgeSegs(raw, payload, prefix, prefixColor, cfg)
            }
        }

        // Message guilde normal : comme public/party, rank + nom en couleur du rank.
        if (!cfg.bridge.formatAllGuild) return null
        return channelBody(prefix, prefixColor, cfg.guildStyle, raw, head.group(1))
    }

    // Formatage d'un relais bridge Discord : "prefix > alias/version > name : message".
    // Le nom passe par rankNameSegs (rank Hypixel + rank guilde + couleur du rank, mêmes toggles).
    private fun bridgeSegs(raw: String, payload: String, prefix: String, prefixColor: Int, cfg: RuleConfig): List<Seg>? {
        var cleaned = stripLeadingNonVersionTag(payload)
        var guildVersion: String? = null
        val vm = GUILD_VERSION_TAG.matcher(cleaned)
        if (vm.find()) {
            guildVersion = vm.group(1).uppercase()
            cleaned = cleaned.substring(vm.end()).trim()
        }
        cleaned = stripLeadingNonVersionTag(cleaned)

        val marker = extractChannelMarker(cleaned)
        if (marker != null && cleaned.startsWith("$marker > ")) cleaned = cleaned.substring(marker.length + 3)

        val discord: String
        val message: String
        var sep = cleaned.indexOf(": ")
        if (sep >= 0) {
            discord = cleaned.substring(0, sep).trim()
            message = cleaned.substring(sep + 2).trim()
        } else {
            sep = cleaned.indexOf(" > ")
            if (sep < 0) return null
            discord = cleaned.substring(0, sep).trim()
            message = cleaned.substring(sep + 3).trim()
        }
        if (discord.isEmpty() || message.isEmpty()) return null

        val useVersion = cfg.bridge.versionTagsEnabled && !guildVersion.isNullOrBlank()
        val versionOrAlias = if (useVersion) guildVersion!! else cfg.bridge.botAlias
        val voaColor = versionOrAliasColor(versionOrAlias, useVersion, cfg)

        // Pseudo avec rank ([MVP+] …) -> logique rank ; sinon pseudo Discord -> couleur bridge custom.
        val nameSegs = if (discord.contains("] ")) rankNameSegs(raw, discord, cfg.guildStyle)
        else listOf(Seg(discord, cfg.bridge.bridgeNameColor))

        return listOf(Seg(prefix, prefixColor), Seg(" > ", DIM),
            Seg(versionOrAlias, voaColor), Seg(" > ", DIM)) +
            nameSegs +
            Seg(" : ", DIM) +
            messageSegs(message, cfg.guildStyle)
    }

    // Formate "[rank] Name: message" (couleurs rank gardées) préfixé du canal.
    private fun channelBody(prefix: String, prefixColor: Int, style: ChannelStyle, raw: String, cleanRest: String): List<Seg>? {
        val sep = cleanRest.indexOf(": ")
        if (sep < 0) return null
        val nameHead = cleanRest.substring(0, sep) // "[rank] Name" (texte nettoyé)
        val msg = cleanRest.substring(sep + 2)
        val out = ArrayList<Seg>()
        out.add(Seg(prefix, prefixColor))
        out.add(Seg(" > ", DIM))
        out.addAll(rankNameSegs(raw, nameHead, style))
        out.add(Seg(" : ", DIM))
        out.addAll(messageSegs(rawMessageSlice(raw, cleanRest.substring(0, sep + 2), msg), style))
        return out
    }

    // Retourne la portion de raw (avec §) correspondant au message, localisée après cleanHead.
    private fun rawMessageSlice(raw: String, cleanHead: String, cleanMsg: String): String {
        val start = indexOfClean(raw, cleanHead)
        if (start < 0) return cleanMsg
        val headRaw = rawSliceForClean(raw, start, cleanHead.length)
        return raw.substring(start + headRaw.length)
    }

    // Rank Hypixel + nom (couleur du rank) + rank de guilde. Chaque partie togglable ; couleurs d'origine gardées.
    private fun rankNameSegs(raw: String, nameHead: String, style: ChannelStyle): List<Seg> {
        // 1. Détacher un éventuel rank de guilde en suffixe " [xxx]" (marche même sans rank Hypixel).
        val gm = GUILD_RANK_SUFFIX.find(nameHead)
        val core = gm?.groupValues?.get(1) ?: nameHead   // "[MVP+] Name" ou "Name"
        val guildTag = gm?.groupValues?.get(2) ?: ""      // "[Member]"

        // 2. Couleur du rank/nom depuis le raw.
        val coreStart = indexOfClean(raw, core)
        val coreSlice = if (coreStart < 0) core else coloredSlice(raw, coreStart, core.length)
        // Couleur du rank = couleur du NOM lui-même (dernier fragment coloré), pas des crochets §8.
        val rankColor = LegacyText.parse(coreSlice).lastOrNull { it.color != null }?.color ?: BRACKET
        val nameColor = if (style.recolorName) style.nameColor else rankColor

        // 3. Rank Hypixel [MVP+] optionnel en tête de core.
        val out = ArrayList<Seg>()
        val rankEnd = core.indexOf("] ")
        if (rankEnd >= 0) {
            val rankTxt = core.substring(0, rankEnd + 1)
            val nameTxt = core.substring(rankEnd + 2)
            if (style.showRank) {
                val rankSlice = if (coreStart < 0) rankTxt else coloredSlice(raw, coreStart, rankTxt.length)
                out.addAll(LegacyText.parse(rankSlice))
                out.add(Seg(" ", DIM))
            }
            out.add(Seg(nameTxt, nameColor))
        } else {
            out.add(Seg(core, nameColor))
        }

        // 4. Rank de guilde en suffixe (couleur d'origine).
        if (style.showGuildRank && guildTag.isNotEmpty()) {
            out.add(Seg(" ", null))
            out.addAll(rawColored(raw, guildTag))
        }
        return out
    }

    // Segments d'un fragment nettoyé en gardant ses couleurs d'origine dans raw.
    private fun rawColored(raw: String, clean: String): List<Seg> {
        val start = indexOfClean(raw, clean)
        val slice = if (start < 0) clean else coloredSlice(raw, start, clean.length)
        return LegacyText.parse(slice)
    }

    // Tranche raw pour [len] chars nettoyés, préfixée du code couleur actif juste avant [start]
    // (Hypixel place souvent le §couleur AVANT le "[" -> sinon la 1re partie sort blanche).
    private fun coloredSlice(raw: String, start: Int, len: Int): String =
        activeColorBefore(raw, start) + rawSliceForClean(raw, start, len)

    private val COLOR_CHARS = "0123456789abcdef"

    private fun activeColorBefore(raw: String, pos: Int): String {
        var i = pos - 1
        while (i >= 1) {
            if (raw[i - 1] == '§' || raw[i - 1] == '&') {
                val c = raw[i].lowercaseChar()
                if (c in COLOR_CHARS) return "§" + raw[i]
                if (c == 'r') return "" // reset : pas de couleur héritée
            }
            i--
        }
        return ""
    }

    // Index dans raw où le texte nettoyé nameHead commence (en ignorant les codes couleur).
    private fun indexOfClean(raw: String, needle: String): Int {
        if (needle.isEmpty()) return -1
        var i = 0
        while (i < raw.length) {
            if (matchesCleanAt(raw, i, needle)) return i
            i++
        }
        return -1
    }

    private fun matchesCleanAt(raw: String, start: Int, needle: String): Boolean {
        var ri = start; var ni = 0
        while (ni < needle.length) {
            if (ri >= raw.length) return false
            val ch = raw[ri]
            if ((ch == '§' || ch == '&') && ri + 1 < raw.length) { ri += 2; continue }
            if (ch != needle[ni]) return false
            ri++; ni++
        }
        return true
    }

    private fun rawSliceForClean(raw: String, start: Int, len: Int): String {
        var ri = start; var count = 0
        while (ri < raw.length && count < len) {
            val ch = raw[ri]
            if ((ch == '§' || ch == '&') && ri + 1 < raw.length) { ri += 2; continue }
            ri++; count++
        }
        return raw.substring(start, ri)
    }

    private fun versionOrAliasColor(versionOrAlias: String, useVersion: Boolean, cfg: RuleConfig): Int {
        if (!useVersion) return cfg.bridge.bridgeAliasColor and 0xFFFFFF
        return when (versionOrAlias.uppercase()) {
            "V1" -> cfg.bridge.v1Color and 0xFFFFFF
            "V2" -> cfg.bridge.v2Color and 0xFFFFFF
            "V3" -> cfg.bridge.v3Color and 0xFFFFFF
            else -> cfg.bridge.v1Color and 0xFFFFFF
        }
    }

    private fun extractChannelMarker(payload: String?): String? {
        if (payload.isNullOrEmpty()) return null
        val m = CHANNEL_MARKER.matcher(stripLeadingNonVersionTag(payload))
        return if (m.matches()) m.group(1) else null
    }

    private fun hasChannelMarker(payload: String?): Boolean {
        if (payload.isNullOrEmpty()) return false
        var cleaned = stripLeadingNonVersionTag(payload)
        if (cleaned.startsWith("[")) {
            val end = cleaned.indexOf("] ")
            if (end > 0 && end + 2 <= cleaned.length) {
                val after = cleaned.substring(end + 2)
                if (after.contains(": ")) return true
                cleaned = after
            }
        }
        return CHANNEL_MARKER.matcher(cleaned).matches()
    }

    private fun hasGuildVersionTag(payload: String?): Boolean {
        if (payload.isNullOrEmpty()) return false
        return GUILD_VERSION_TAG.matcher(stripLeadingNonVersionTag(payload)).find()
    }

    private fun stripLeadingNonVersionTag(payload: String): String {
        if (payload.isEmpty() || !payload.startsWith("[")) return payload
        val end = payload.indexOf("] ")
        if (end <= 0 || end + 2 > payload.length) return payload
        val tag = payload.substring(1, end)
        if (tag.equals("V1", true) || tag.equals("V2", true) || tag.equals("V3", true)) return payload
        return payload.substring(end + 2)
    }
}
