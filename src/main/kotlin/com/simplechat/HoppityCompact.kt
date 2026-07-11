package com.simplechat

import java.util.regex.Pattern

/**
 * Fusionne les 2 lignes Hoppity ("You found <nom> (<rareté>)!" puis "DUPLICATE/NEW RABBIT! …")
 * en une seule ligne compacte. État côté client (appelé par le mixin avant ChatRules).
 */
object HoppityCompact {

    private val FOUND = Pattern.compile(
        "You found (?:a |an )?(.+?) \\((COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL)\\)",
        Pattern.CASE_INSENSITIVE)
    private val NEW = Pattern.compile("NEW RABBIT!", Pattern.CASE_INSENSITIVE)
    private val DUP = Pattern.compile("DUPLICATE RABBIT!", Pattern.CASE_INSENSITIVE)

    private var pending: String? = null

    /** Verdict Hoppity, ou null si non concerné / désactivé. `clean` = texte décoloré. */
    fun process(clean: String, enabled: Boolean): Verdict? {
        if (!enabled) return null
        val fm = FOUND.matcher(clean)
        if (fm.find()) {
            pending = "${fm.group(1)} §7(${fm.group(2).lowercase()})"
            return Verdict.Hide // on masque la ligne verbeuse, on ressort tout sur la ligne rabbit
        }
        if (NEW.matcher(clean).find()) {
            val p = pending; pending = null
            return Verdict.Replace(if (p != null) "§dNew rabbit §7- §f$p" else "§dNew rabbit")
        }
        if (DUP.matcher(clean).find()) {
            val p = pending; pending = null
            return Verdict.Replace(if (p != null) "§dDuplicate rabbit §7- §f$p" else "§dDuplicate rabbit")
        }
        return null
    }
}
