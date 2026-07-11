package com.simplechat

import com.simplechat.ui.RcWidgets
import com.simplechat.ui.WidgetKind
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RcWidgetsKindTest {
    @Test fun `boolean is toggle`() =
        assertEquals(WidgetKind.TOGGLE, RcWidgets.kindOf(EntryType.BOOLEAN, hasColor = false, hasRange = false))

    @Test fun `enum is dropdown`() =
        assertEquals(WidgetKind.DROPDOWN, RcWidgets.kindOf(EntryType.ENUM, hasColor = false, hasRange = false))

    @Test fun `string is textbox`() =
        assertEquals(WidgetKind.TEXT, RcWidgets.kindOf(EntryType.STRING, hasColor = false, hasRange = false))

    @Test fun `int with color is color`() =
        assertEquals(WidgetKind.COLOR, RcWidgets.kindOf(EntryType.INTEGER, hasColor = true, hasRange = false))

    @Test fun `int with range is slider`() =
        assertEquals(WidgetKind.SLIDER, RcWidgets.kindOf(EntryType.INTEGER, hasColor = false, hasRange = true))

    @Test fun `plain int is number`() =
        assertEquals(WidgetKind.NUMBER, RcWidgets.kindOf(EntryType.INTEGER, hasColor = false, hasRange = false))

    @Test fun `color flag wins over range`() =
        assertEquals(WidgetKind.COLOR, RcWidgets.kindOf(EntryType.INTEGER, hasColor = true, hasRange = true))

    @Test fun `object is unsupported`() =
        assertEquals(WidgetKind.UNSUPPORTED, RcWidgets.kindOf(EntryType.OBJECT, hasColor = false, hasRange = false))

    @Test fun `non-int numeric is unsupported`() =
        assertEquals(WidgetKind.UNSUPPORTED, RcWidgets.kindOf(EntryType.DOUBLE, hasColor = false, hasRange = false))
}
