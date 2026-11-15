package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.TeamId
import org.example.bbottd_trainer.domain.reservePlayer
import org.example.bbottd_trainer.domain.setup
import org.example.bbottd_trainer.domain.team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardStateHolderTest {

    @Test
    fun startsWithTheInitialSampleSetupByDefault() {
        val holder = BoardStateHolder()

        assertEquals(SampleSetups.initial, holder.state.value.setup)
    }

    @Test
    fun startsWithTheGivenSetup() {
        val custom = setup(home = team(TeamId.HOME, reservePlayer("h1")))

        val holder = BoardStateHolder(initialSetup = custom)

        assertEquals(custom, holder.state.value.setup)
    }

    @Test
    fun startsWithNoSelectionAndNoUnsavedChanges() {
        val state = BoardStateHolder().state.value

        assertNull(state.selectedPlayerId)
        assertNull(state.selectedPosition)
        assertFalse(state.isDirty)
    }

    @Test
    fun dugoutsAreVisibleByDefault() {
        assertTrue(BoardStateHolder().state.value.dugoutsVisible)
    }

    @Test
    fun dugoutsCanStartHidden() {
        assertFalse(BoardStateHolder(dugoutsVisible = false).state.value.dugoutsVisible)
    }

    @Test
    fun setDugoutsVisibleShowsAndHidesThem() {
        val holder = BoardStateHolder(dugoutsVisible = false)

        holder.onAction(BoardAction.SetDugoutsVisible(true))
        assertTrue(holder.state.value.dugoutsVisible)

        holder.onAction(BoardAction.SetDugoutsVisible(false))
        assertFalse(holder.state.value.dugoutsVisible)
    }

    @Test
    fun showingDugoutsDoesNotChangeTheSetupOrMarkItDirty() {
        val holder = BoardStateHolder(dugoutsVisible = false)

        holder.onAction(BoardAction.SetDugoutsVisible(true))

        assertEquals(SampleSetups.initial, holder.state.value.setup)
        assertFalse(holder.state.value.isDirty)
    }
}
