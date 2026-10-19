package com.simplechat.config

import com.simplechat.engine.PersonalRule

object PersonalRules : HscCategory("Custom Rules") {
    var rules by string("[]") { name = "Personal rules" }

    fun list(): List<PersonalRule> = PersonalRule.decode(rules)
    fun replace(values: List<PersonalRule>) { rules = PersonalRule.encode(values) }
}

/** Les mêmes champs et widgets que le reste du menu, pour une règle ajoutée par le joueur. */
class PersonalRuleSettings(rule: PersonalRule) : ConfigGroup() {
    var enabled by boolean(rule.enabled) { name = "Enabled" }
    var text by string(rule.text) { name = "Contains text"; description = "Plain text to find in the original message. Empty rules do nothing" }
    var action by enum(rule.action) { name = "Action"; description = "Highlight matching text or hide matching messages. First matching personal rule wins" }
    var color by color(rule.color) { name = "Highlight color" }
    var bold by boolean(rule.bold) { name = "Bold" }
    var italic by boolean(rule.italic) { name = "Italic" }
    var underlined by boolean(rule.underlined) { name = "Underlined" }
    var channel by enum(rule.channel) { name = "Channel"; description = "Guild includes officer chat and bridge messages" }
    var caseSensitive by boolean(rule.caseSensitive) {
        name = "Case sensitive"
        description = "Match uppercase and lowercase exactly. Off: 'test' matches 'test', 'Test' and 'TEST'. On: 'test' only matches 'test'. Leave off to ignore case"
    }

    fun rule() = PersonalRule(text, enabled, action, color, channel, caseSensitive, bold, italic, underlined)
}
