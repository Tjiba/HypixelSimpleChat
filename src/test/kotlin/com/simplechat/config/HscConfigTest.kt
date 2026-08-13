package com.simplechat.config

import com.simplechat.engine.RuleAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * Un réglage changé doit survivre au redémarrage : tout ce qui s'écrit doit se relire,
 * y compris les entrées de règles déclarées à l'exécution.
 */
class HscConfigTest {

    private class Cat : HscCategory("Cat") {
        var mode by enum(ChatMode.COMPACT) { name = "Mode" }
        var tint by color(0x112233) { name = "Tint" }
    }

    private class Cfg(dir: Path) : HscConfig("simplechat/config", dir) {
        var flag by boolean(true) { name = "Flag" }
        var count by int(3) { name = "Count" }
        var label by string("hi") { name = "Label" }
        val cat = Cat()

        init {
            category(cat)
            cat.addEnum("some-rule", RuleAction.COMPACT, "Some rule")
        }
    }

    @Test fun `every kind survives a save and a reload`(@TempDir dir: Path) {
        Cfg(dir).apply {
            flag = false
            count = 42
            label = "changed"
            cat.mode = ChatMode.VANILLA
            cat.tint = 0xAABBCC
            cat.entries["some-rule"]!!.setEnum(RuleAction.HIDE)
            save()
        }

        Cfg(dir).apply {
            load()
            assertEquals(false, flag)
            assertEquals(42, count)
            assertEquals("changed", label)
            assertEquals(ChatMode.VANILLA, cat.mode)
            assertEquals(0xAABBCC, cat.tint)
            assertEquals(RuleAction.HIDE, cat.entries["some-rule"]!!.getEnum())
        }
    }

    @Test fun `an interrupted save leaves the previous file intact`(@TempDir dir: Path) {
        Cfg(dir).apply { count = 42; save() }
        // Le .tmp d'un save tué ne doit ni rester en place ni être relu.
        Files.writeString(dir.resolve("simplechat/config.json.tmp"), "{ tronqu")

        Cfg(dir).apply { load(); assertEquals(42, count) }
    }

    @Test fun `an unreadable config is backed up before the defaults overwrite it`(@TempDir dir: Path) {
        Cfg(dir).apply { count = 42; save() }
        val file = dir.resolve("simplechat/config.json")
        Files.writeString(file, "{ \"count\": 42")

        Cfg(dir).apply { load(); assertEquals(3, count) }
        assertTrue(Files.exists(dir.resolve("simplechat/config.json.bak")), "pas de copie de secours")
        assertFalse(Files.readString(file).isEmpty())
    }
}
