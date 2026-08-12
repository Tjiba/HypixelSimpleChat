package com.simplechat.rules.lobby

import com.simplechat.engine.LegacyText
import com.simplechat.engine.RuleAction
import com.simplechat.rules.Category
import com.simplechat.rules.Group
import com.simplechat.rules.rules

/** Arrivées dans un lobby. */
object Joins {

    val LOBBY_JOIN = Group(
        id = "lobbyJoin",
        title = "Lobby join/leave",
        category = Category.LOBBY,
        section = "",
        default = RuleAction.HIDE,
        description = "OFF = as-is · GREY = dimmed · COMPACT = reformat · COMPACT_GREY = reformat + dimmed · HIDE = remove",
    )

    val rules = rules(LOBBY_JOIN) {
        // Compact fait sur le raw : garde la couleur du rank, retire les flèches et le suffixe.
        rule("lobby-join", RuleAction.HIDE,
            "^(>+ )?\\[?[A-Za-z+]* ?[\\w+]* ?]?.* joined the lobby!",
            compact = {
                it.raw.replaceFirst(Regex("^(?:\\s|>|${LegacyText.CODE})+"), "")
                    .replaceFirst(Regex("(?:${LegacyText.CODE})?\\s*joined the lobby!.*$"), "")
                    .trim() + " §8joined"
            },
            sample = "§b[MVP§c+§b] Notch §ejoined the lobby!")
    }
}
