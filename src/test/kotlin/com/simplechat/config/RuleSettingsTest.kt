package com.simplechat.config

import com.google.gson.JsonParser
import com.simplechat.engine.RuleAction
import com.simplechat.rules.Registry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Reprise des configs d'avant les réglages par phrase : sans elle, un joueur qui avait mis
 * « Dungeons » sur GREY repartirait sur les défauts sans être prévenu.
 */
class RuleSettingsTest {

    init { RuleSettings.register() }

    private val dungeons = Registry.groups.first { it.id == "dungeons" }

    @Test fun `a legacy group value is passed down to its phrases`() {
        RuleSettings.migrate(JsonParser.parseString("""{"SkyBlock":{"dungeons":"GREY"}}""").asJsonObject)
        val actions = RuleSettings.overrides()
        for (rule in RuleSettings.perPhrase(dungeons)) {
            assertEquals(RuleAction.GREY, actions[rule.id], "${rule.id} n'a pas repris la valeur du groupe")
        }
    }

    @Test fun `a phrase already saved keeps its own value`() {
        SkyBlockCleanup.entries["puzzle-solved"]!!.setEnum(RuleAction.COMPACT)
        RuleSettings.migrate(
            JsonParser.parseString("""{"SkyBlock":{"dungeons":"HIDE","puzzle-solved":"COMPACT"}}""").asJsonObject)
        assertEquals(RuleAction.COMPACT, RuleSettings.overrides()["puzzle-solved"])
    }

    @Test fun `a group covering a single rule keeps its own setting`() {
        val boss = Registry.groups.first { it.id == "boss" }
        assertEquals(emptyList<Any>(), RuleSettings.perPhrase(boss))
        assertEquals(boss.default, RuleSettings.groupActions()["boss"])
    }
}
