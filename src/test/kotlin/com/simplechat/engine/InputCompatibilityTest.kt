package com.simplechat.engine

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.input.KeyEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InputCompatibilityTest {
    @Test fun `named inputs resolve to the current backend codes`() {
        val keys = mapOf(
            "key.keyboard.f" to InputConstants.KEY_F,
            "key.keyboard.escape" to InputConstants.KEY_ESCAPE,
            "key.keyboard.enter" to InputConstants.KEY_RETURN,
            "key.keyboard.keypad.enter" to InputConstants.KEY_NUMPADENTER,
            "key.keyboard.backspace" to InputConstants.KEY_BACKSPACE,
            "key.mouse.left" to InputConstants.MOUSE_BUTTON_LEFT,
            "key.mouse.right" to InputConstants.MOUSE_BUTTON_RIGHT,
        )
        for ((name, code) in keys) assertEquals(code, InputConstants.getKey(name).value, name)
    }

    @Test fun `control detection uses current backend modifiers`() {
        val key = InputConstants.getKey("key.keyboard.f").value
        assertFalse(KeyEvent(key, 0, 0).hasControlDown())
        assertFalse(KeyEvent(key, 0, InputConstants.MOD_SHIFT).hasControlDown())
        assertFalse(KeyEvent(key, 0, InputConstants.MOD_ALT).hasControlDown())
        assertTrue(KeyEvent(key, 0, InputConstants.MOD_CONTROL).hasControlDown())
        assertTrue(KeyEvent(key, 0, InputConstants.MOD_CONTROL or InputConstants.MOD_SHIFT).hasControlDown())
    }
}
