package com.simplechat

import com.simplechat.engine.Verdict
import com.simplechat.rules.Registry

/**
 * Journal des décisions du mod, activé par `/hsc debug`. Répond à « pourquoi je ne vois plus
 * cette ligne » : verdict, règle responsable, et repli en (xN) le cas échéant.
 * Les `§` sortent en `&` pour être copiables depuis le log.
 */
object Debug {

    @JvmStatic
    var enabled = false

    @JvmStatic
    fun log(legacy: String, clean: String, verdict: Verdict) {
        if (!enabled) return
        val rule = Registry.find(clean, legacy)?.first
        SimpleChatMod.LOGGER.info("[hsc] {} rule={} | {}",
            verdict.javaClass.simpleName, rule?.id ?: "-", legacy.replace('§', '&'))
    }

    /** Un garde-fou du mixin a annulé le verdict pour ne pas casser le message. */
    @JvmStatic
    fun logGuard(reason: String, legacy: String) {
        if (!enabled) return
        SimpleChatMod.LOGGER.info("[hsc] Guard ({}) kept as-is | {}", reason, legacy.replace('§', '&'))
    }

    /** Ce que le mod affiche réellement, juste avant de le rendre. Départage un bug d'ici d'un
     *  autre mod qui rajouterait quelque chose après nous. */
    @JvmStatic
    fun logRendered(text: String) {
        if (!enabled) return
        SimpleChatMod.LOGGER.info("[hsc] Rendered | {}", text.replace('§', '&'))
    }

    @JvmStatic
    fun logCollapsed(clean: String, count: Int) {
        if (!enabled) return
        SimpleChatMod.LOGGER.info("[hsc] Collapsed x{} | {}", count, clean)
    }
}
