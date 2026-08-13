package com.simplechat.engine

import com.simplechat.config.RuleConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChannelFormatTest {
    private val cfg = RuleConfig.DEFAULT

    @Test fun `public keeps rank colors and original message color`() {
        val c = cfg.copy(publicStyle = cfg.publicStyle.copy(showRank = true))
        val segs = ChannelFormat.format("[221] ⛃ §b[MVP§c+§b] Milo§f: selling", Channel.PUBLIC, c)!!
        // recolor off par défaut : "selling" garde sa couleur d'origine (aucun code dans le
        // sous-texte du message -> null = couleur héritée), pas messageColor.
        assertEquals("selling", segs.last().text)
        assertEquals(null, segs.last().color)
        // rank affiché avec couleur d'origine (§b) quand showRank on
        assert(segs.any { it.text.contains("MVP") && it.color == 0x55FFFF })
    }

    // Le niveau garde la couleur qu'Hypixel lui donne. Le palier (level/40) ne sert que si le
    // message ne la porte pas — ici 100 tomberait sur le jaune du palier, pas sur ce vert.
    @Test fun `level keeps hypixel color`() {
        val segs = ChannelFormat.format("§8[§a100§8] §b[MVP§c+§b] Milo§f: hi", Channel.PUBLIC, cfg)!!
        assertEquals(0x55FF55, segs.first { it.text == "100" }.color)
    }

    @Test fun `level falls back to the tier color`() {
        val segs = ChannelFormat.format("[100] §b[MVP§c+§b] Milo§f: hi", Channel.PUBLIC, cfg)!!
        assertEquals(0xFFFF55, segs.first { it.text == "100" }.color)
    }

    @Test fun `public recolor on forces message color`() {
        val c = cfg.copy(publicStyle = cfg.publicStyle.copy(recolorMessage = true, showRank = true))
        val segs = ChannelFormat.format("[221] ⛃ §b[MVP§c+§b] Milo§f: selling", Channel.PUBLIC, c)!!
        assertEquals("selling", segs.last().text)
        assertEquals(0xAAAAAA, segs.last().color) // publicStyle.messageColor
        assert(segs.any { it.text.contains("MVP") && it.color == 0x55FFFF })
    }

    @Test fun `public hides emblem by default keeps level dimmed off`() {
        // prefix par défaut: hideEmblem=true, hideLevel=false, dim=true -> "[221]" grisé, pas de ⛃
        val segs = ChannelFormat.format("[221] ⛃ §b[MVP§c+§b] Milo§f: hi", Channel.PUBLIC, cfg)!!
        val joined = segs.joinToString("") { it.text }
        assert(!joined.contains("⛃"))
        assert(joined.contains("[221]"))
    }

    @Test fun `public hideLevel removes level`() {
        val c = cfg.copy(prefix = cfg.prefix.copy(hideLevel = true, hideEmblem = true))
        val segs = ChannelFormat.format("[221] ⛃ §b[MVP§c+§b] Milo§f: hi", Channel.PUBLIC, c)!!
        val joined = segs.joinToString("") { it.text }
        assert(!joined.contains("221"))
        assert(!joined.contains("⛃"))
    }

    @Test fun `party compacts and keeps original message color`() {
        val segs = ChannelFormat.format("Party > §b[MVP§c+§b] Foo§f: go next", Channel.PARTY, cfg)!!
        // recolor off : message garde sa couleur d'origine (aucun code dans le sous-texte -> null)
        assertEquals("go next", segs.last().text)
        assertEquals(null, segs.last().color)
        assert(segs.any { it.text.contains("Foo") })
    }

    @Test fun `party recolor on forces message color`() {
        val c = cfg.copy(partyStyle = cfg.partyStyle.copy(recolorMessage = true))
        val segs = ChannelFormat.format("Party > §b[MVP§c+§b] Foo§f: go next", Channel.PARTY, c)!!
        assertEquals("go next", segs.last().text)
        assertEquals(0x55FFFF, segs.last().color) // partyStyle.messageColor
        assert(segs.any { it.text.contains("Foo") })
    }

    @Test fun `guild bridge V1 formatted like GuildZip`() {
        val segs = ChannelFormat.format("Guild > [MVP++] BotName: G > V1 > User: hello", Channel.GUILD, cfg)!!
        val joined = segs.joinToString("") { it.text }
        assert(joined.contains("V1"))          // tag version affiché
        assert(joined.contains("User"))         // pseudo discord
        assertEquals("hello", segs.last().text) // message
        assertEquals(null, segs.last().color)   // recolor off : couleur d'origine (null)
    }

    @Test fun `guild bridge recolor on forces message color`() {
        val c = cfg.copy(guildStyle = cfg.guildStyle.copy(recolorMessage = true))
        val segs = ChannelFormat.format("Guild > [MVP++] BotName: G > V1 > User: hello", Channel.GUILD, c)!!
        assertEquals("hello", segs.last().text)
        assertEquals(0x55FF55, segs.last().color) // guildStyle.messageColor
    }

    @Test fun `guild message shows mc rank name and guild rank`() {
        val segs = ChannelFormat.format("Guild > §b[MVP§c+§b] Foo §7[Member]§f: gg", Channel.GUILD, cfg)!!
        val joined = segs.joinToString("") { it.text }
        assert(joined.contains("[MVP+]"))   // rank MC
        assert(joined.contains("Foo"))       // nom
        assert(joined.contains("[Member]"))  // rank de guilde gardé
        assertEquals("gg", segs.last().text)
    }

    @Test fun `guild no hypixel rank keeps guild rank separate`() {
        val segs = ChannelFormat.format("Guild > §7firestar04 §7[Membre]§f: yo", Channel.GUILD, cfg)!!
        val joined = segs.joinToString("") { it.text }
        assert(joined.contains("firestar04"))
        assert(joined.contains("[Membre]"))
        assertEquals("yo", segs.last().text)
    }

    @Test fun `guild simple message`() {
        val segs = ChannelFormat.format("Guild > [MVP+] Foo: gg wp", Channel.GUILD, cfg)!!
        assertEquals("gg wp", segs.last().text)
        assertEquals(null, segs.last().color) // recolor off
        assert(segs.any { it.text.contains("Foo") })
    }

    private val self = cfg.copy(self = SelfPlayer("non00w", highlightColor = 0xFFAA00))

    @Test fun `own name is highlighted in guild party and public`() {
        val cases = listOf(
            "Guild > §b[MVP§c+§b] non00w§f: gg" to Channel.GUILD,
            "Party > §b[MVP§c+§b] non00w§f: go" to Channel.PARTY,
            "§b[MVP§c+§b] non00w§f: hi" to Channel.PUBLIC,
        )
        for ((raw, channel) in cases) {
            val segs = ChannelFormat.format(raw, channel, self)!!
            assertEquals(0xFFAA00, segs.first { it.text == "non00w" }.color, raw)
        }
    }

    @Test fun `other players keep their rank color`() {
        val segs = ChannelFormat.format("Guild > §b[MVP§c+§b] Foo§f: gg", Channel.GUILD, self)!!
        assertEquals(0x55FFFF, segs.first { it.text == "Foo" }.color)
    }

    // Le repère ne sert à rien s'il porte la couleur de tout le monde.
    @Test fun `own name wins over recolorName`() {
        val c = self.copy(guildStyle = self.guildStyle.copy(recolorName = true, nameColor = 0xFFFFFF))
        val segs = ChannelFormat.format("Guild > §b[MVP§c+§b] non00w§f: gg", Channel.GUILD, c)!!
        assertEquals(0xFFAA00, segs.first { it.text == "non00w" }.color)
    }
}
