package com.simplechat

import com.simplechat.engine.Channel
import com.simplechat.engine.ChatRules
import com.simplechat.engine.Verdict
import java.util.regex.Pattern

/**
 * Fusionne les 2 lignes Hoppity ("You found <nom> (<rareté>)!" puis "DUPLICATE/NEW RABBIT! …")
 * en une seule ligne compacte. État côté client (appelé par le mixin avant ChatRules).
 */
object HoppityCompact {

    private val FOUND = Pattern.compile(
        "^(?:HOPPITY'S HUNT )?You found (?:a |an )?(.+?) \\((COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL)\\)!?$",
        Pattern.CASE_INSENSITIVE)
    private val NEW = Pattern.compile("^NEW RABBIT!(?: .*)?$", Pattern.CASE_INSENSITIVE)
    private val DUP = Pattern.compile("^DUPLICATE RABBIT!(?: .*)?$", Pattern.CASE_INSENSITIVE)

    private var pending: String? = null

    /** Verdict Hoppity, ou null si non concerné / désactivé. `clean` = texte décoloré. */
    fun process(clean: String, enabled: Boolean): Verdict? {
        if (!enabled) { reset(); return null }
        if (ChatRules.classify(clean) != Channel.SYSTEM) return null
        val fm = FOUND.matcher(clean)
        if (fm.matches()) {
            pending = "${fm.group(1)} §7(${fm.group(2).lowercase()})"
            return Verdict.Hide // on masque la ligne verbeuse, on ressort tout sur la ligne rabbit
        }
        if (NEW.matcher(clean).matches()) {
            val p = pending; pending = null
            return Verdict.Replace(if (p != null) "§dNew rabbit §7- §f$p" else "§dNew rabbit")
        }
        if (DUP.matcher(clean).matches()) {
            val p = pending; pending = null
            return Verdict.Replace(if (p != null) "§dDuplicate rabbit §7- §f$p" else "§dDuplicate rabbit")
        }
        if (clean.isNotEmpty()) reset()
        return null
    }

    fun reset() { pending = null }
}
