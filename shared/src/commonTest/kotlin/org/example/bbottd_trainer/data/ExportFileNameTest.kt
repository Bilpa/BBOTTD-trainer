package org.example.bbottd_trainer.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ExportFileNameTest {
    @Test
    fun plainNameGetsJsonExtension() {
        assertEquals("Blitz OTT.json", exportFileName("Blitz OTT"))
    }

    @Test
    fun charactersNotAllowedInFileNamesAreReplaced() {
        assertEquals("a_b_c_d_e_f_g_h_i.json", exportFileName("a<b>c:d\"e/f\\g|h?i"))
        assertEquals("star_.json", exportFileName("star*"))
        assertEquals("tab_x.json", exportFileName("tab\tx"))
    }

    @Test
    fun unicodeIsKept() {
        assertEquals("Ünïcode – 🏈.json", exportFileName("Ünïcode – 🏈"))
    }

    @Test
    fun surroundingSpacesAndDotsAreRemoved() {
        assertEquals("name.json", exportFileName("  ..name.. "))
    }

    @Test
    fun emptyOrUnusableNameBecomesSetup() {
        assertEquals("setup.json", exportFileName(""))
        assertEquals("setup.json", exportFileName("   "))
        assertEquals("setup.json", exportFileName("..."))
    }

    @Test
    fun longNameIsShortened() {
        assertEquals("x".repeat(80) + ".json", exportFileName("x".repeat(200)))
    }
}
