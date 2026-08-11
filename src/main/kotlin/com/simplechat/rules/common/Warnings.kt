package com.simplechat.rules.common

import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Refus du serveur : trop vite, en combat, mauvais outil. */
object Warnings {

    val WARNINGS = Group(
        id = "warnings",
        title = "Warnings",
        category = Category.SKYBLOCK,
        section = "COMBAT",
        default = RuleAction.HIDE,
        description = "Too fast, can't use in combat, tree regenerating, wrong tool",
    )

    val rules = rules(WARNINGS) {
        rule("commands-too-fast", RuleAction.HIDE,
            "^You are sending commands too fast! Please slow down\\.",
            sample = "§cYou are sending commands too fast! Please slow down.",
            title = "Commands too fast")
        rule("in-combat", RuleAction.HIDE,
            "^You can't use this while in combat!",
            sample = "§cYou can't use this while in combat!",
            title = "Blocked while in combat")
        rule("slow-down", RuleAction.HIDE,
            "^Whoa! Slow down there!",
            sample = "§cWhoa! Slow down there!",
            title = "Slow down")
        rule("tree-regenerating", RuleAction.HIDE,
            "^You cannot damage a tree while it is regenerating!",
            sample = "§cYou cannot damage a tree while it is regenerating!",
            title = "Tree regenerating")
        rule("tree-toughness", RuleAction.HIDE,
            "^The toughness of this tree is way too high!",
            sample = "§cThe toughness of this tree is way too high!",
            title = "Tree too tough")
        rule("axes-only", RuleAction.HIDE,
            "^Monsters around here can only take damage from Axes!",
            sample = "§cMonsters around here can only take damage from Axes!",
            title = "Axes only")
    }
}
