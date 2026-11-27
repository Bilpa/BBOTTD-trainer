package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.data.InMemoryTextStore
import org.example.bbottd_trainer.data.StoredEditorPreferences
import org.example.bbottd_trainer.data.StoredSetupRepository
import org.example.bbottd_trainer.data.runSuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeTest {
    private val store = InMemoryTextStore()
    private val preferences = StoredEditorPreferences(store)

    private fun holder() = testHolder(preferences = preferences)

    @Test
    fun followsTheSystemThemeByDefault() {
        assertNull(holder().current.darkTheme)
    }

    @Test
    fun aRememberedChoiceIsUsedAtStart() {
        runSuspend { preferences.setDarkTheme(true) }
        assertEquals(true, holder().current.darkTheme)
    }

    @Test
    fun theToggleChangesAndRemembersTheTheme() {
        val holder = holder()

        holder.click(BoardAction.SetDarkTheme(true))
        assertEquals(true, holder.current.darkTheme)
        assertEquals(true, holder().current.darkTheme, "remembered at the next start")

        holder.click(BoardAction.SetDarkTheme(false))
        assertEquals(false, holder().current.darkTheme)
    }

    @Test
    fun changingTheThemeIsNotAnUnsavedChange() {
        val holder = holder()
        holder.click(BoardAction.SetDarkTheme(true))
        assertFalse(holder.current.isDirty)
    }

    @Test
    fun theThemeCanChangeWhileTheLastSetupIsOpening() {
        val gate = GatedRepository(StoredSetupRepository(store))
        val holder = testHolder(repository = gate, preferences = preferences)
        assertTrue(holder.current.isLoading)

        holder.click(BoardAction.SetDarkTheme(true))

        assertEquals(true, holder.current.darkTheme)
        gate.open()
    }
}
