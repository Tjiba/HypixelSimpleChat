package com.simplechat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClassifyTest {
    @Test fun `guild channel`() {
        assertEquals(Channel.GUILD, ChatRules.classify("Guild > [MVP+] Foo: hi"))
        assertEquals(Channel.GUILD, ChatRules.classify("G > V1 > LaZoulette : yo"))
    }
    @Test fun `officer channel`() {
        assertEquals(Channel.OFFICER, ChatRules.classify("Officer > Foo: hi"))
        assertEquals(Channel.OFFICER, ChatRules.classify("O > Foo: hi"))
    }
    @Test fun `party channel`() {
        assertEquals(Channel.PARTY, ChatRules.classify("Party > [MVP+] Foo: go"))
        assertEquals(Channel.PARTY, ChatRules.classify("P > Foo: go"))
    }
    @Test fun `whisper channel`() {
        assertEquals(Channel.WHISPER, ChatRules.classify("From [MVP+] Foo: yo"))
        assertEquals(Channel.WHISPER, ChatRules.classify("To [MVP+] Foo: yo"))
    }
    @Test fun `public channel`() {
        assertEquals(Channel.PUBLIC, ChatRules.classify("[221] ⛃ [MVP+] Milo: selling"))
        assertEquals(Channel.PUBLIC, ChatRules.classify("[MVP+] Foo: hey"))
    }
    @Test fun `system channel`() {
        assertEquals(Channel.SYSTEM, ChatRules.classify("You are playing on profile: Peach (Co-op)"))
        assertEquals(Channel.SYSTEM, ChatRules.classify("Sending to server mega8E..."))
    }
}
