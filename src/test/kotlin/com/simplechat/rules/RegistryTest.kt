package com.simplechat.rules

import com.simplechat.config.RuleConfig
import com.simplechat.engine.ChatRules
import com.simplechat.engine.RuleAction
import com.simplechat.engine.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Garde-fous du registre : s'appliquent à toutes les règles au fur et à mesure du portage. */
class RegistryTest {

    @Test fun `ids uniques`() {
        val rules = Registry.rules.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue(rules.isEmpty(), "ids de règles en double : $rules")
        val groups = Registry.groups.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue(groups.isEmpty(), "ids de groupes en double : $groups")
    }

    @Test fun `tout groupe utilisé est déclaré`() {
        val missing = Registry.rules.map { it.group }.filter { it !in Registry.groups }.map { it.id }.toSet()
        assertTrue(missing.isEmpty(), "groupes absents de Registry.groups : $missing")
    }

    // Une règle qui partage son réglage avec d'autres a sa propre ligne dans le menu : sans
    // libellé, elle s'y afficherait sous son identifiant technique.
    @Test fun `toute règle affichée seule a un libellé`() {
        val nameless = Registry.byGroup.values
            .filter { it.size > 1 }.flatten()
            .filter { it.title.isNullOrBlank() }.map { it.id }
        assertTrue(nameless.isEmpty(), "règles sans libellé : $nameless")
    }

    @Test fun `chaque règle est déclenchée par son exemple`() {
        for (rule in Registry.rules) {
            assertTrue(rule.sample.isNotBlank(), "${rule.id} : exemple vide")
            val clean = ChatRules.clean(rule.sample)
            assertNotNull(rule.match(clean, rule.sample), "${rule.id} : son exemple ne le déclenche pas — $clean")
        }
    }

    @Test fun `aucune règle n'est étouffée par une règle placée avant elle`() {
        for ((i, rule) in Registry.rules.withIndex()) {
            val clean = ChatRules.clean(rule.sample)
            val stealer = Registry.find(clean, rule.sample, Registry.rules.subList(0, i))?.first
            assertNull(stealer, "${rule.id} est étouffé par ${stealer?.id}")
        }
    }

    // Le détecteur ci-dessus doit vraiment détecter : la générique est déclarée avant la précise.
    @Test fun `le détecteur d'étouffement fonctionne`() {
        val group = Group("test", "Test", Category.SKYBLOCK, "TEST", RuleAction.HIDE)
        val list = rules(group) {
            rule("generic", RuleAction.HIDE, "damage", sample = "Something hit you for 5 damage")
            rule("precise", RuleAction.HIDE, "^Kuudra hit you for", sample = "Kuudra hit you for 5 damage")
        }
        assertEquals("generic", Registry.find("Kuudra hit you for 5 damage", "", list)?.first?.id)
    }

    @Test fun `une phrase réglée à part échappe au réglage de son groupe`() {
        val cfg = RuleConfig.DEFAULT.copy(
            groupActions = mapOf("dungeons" to RuleAction.HIDE),
            actions = mapOf("puzzle-solved" to RuleAction.COMPACT),
        )
        assertEquals(Verdict.Replace("§aPuzzle ✔ §7Timo solved Water Board!"),
            ChatRules.evaluate("§ePUZZLE SOLVED! Timo solved Water Board!", cfg))
        // ses voisines de groupe ne bougent pas
        assertEquals(Verdict.Hide, ChatRules.evaluate("§8A §5Wither Key §8was picked up!", cfg))
    }

    // "BEEHEEMOTH DOWN!" tombe dans le générique "… DOWN!" du réglage Boss : seule sa place
    // avant Combat dans le registre le laisse passer intact.
    @Test fun `beeheemoth down échappe au réglage Boss`() {
        val cfg = RuleConfig.DEFAULT.copy(groupActions = mapOf("boss" to RuleAction.HIDE))
        assertEquals(Verdict.Pass, ChatRules.evaluate("§6§lBEEHEEMOTH DOWN!", cfg))
        assertEquals(Verdict.Hide, ChatRules.evaluate("§cARACHNE DOWN!", cfg))
    }

    @Test fun `registre vide, pas de verdict, l'ancien moteur enchaîne`() {
        assertNull(Registry.match("zzz qqq 12345", "zzz qqq 12345", RuleConfig.DEFAULT))
    }
}
