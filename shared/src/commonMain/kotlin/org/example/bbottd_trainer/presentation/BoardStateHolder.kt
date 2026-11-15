package org.example.bbottd_trainer.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.EditResult
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.TacticalSetup
import org.example.bbottd_trainer.domain.TeamId
import org.example.bbottd_trainer.domain.moveBall
import org.example.bbottd_trainer.domain.movePlayerToCell
import org.example.bbottd_trainer.domain.movePlayerToDugout

/**
 * Holds the board screen's state and exposes it as a [StateFlow].
 *
 * This is a plain Kotlin class, not a lifecycle ViewModel. The UI observes [state] and sends
 * explicit [BoardAction]s to [onAction]; this class decides what a click means and applies the
 * domain editing operations. The UI never mutates the setup itself.
 *
 * Select-then-click rules:
 * - Clicking a player selects them; clicking the selected player again deselects them.
 * - With a player selected, clicking an empty cell or a dugout section moves them there.
 * - With the ball selected, clicking any cell (even a player's) moves the ball there, and clicking
 *   the off-pitch area takes it off the pitch.
 * - A successful move clears the selection; a failed move keeps it and shows a message.
 *
 * @param dugoutsVisible whether the dugouts start visible. Android starts with them hidden because
 *   of limited screen space.
 */
class BoardStateHolder(
    initialSetup: TacticalSetup = SampleSetups.initial,
    dugoutsVisible: Boolean = true,
) {
    private val mutableState = MutableStateFlow(
        BoardUiState(setup = initialSetup, dugoutsVisible = dugoutsVisible),
    )
    private var nextMessageId = 0L

    val state: StateFlow<BoardUiState> = mutableState.asStateFlow()

    fun onAction(action: BoardAction) {
        when (action) {
            is BoardAction.SetDugoutsVisible ->
                mutableState.update { it.copy(dugoutsVisible = action.visible) }
            is BoardAction.CellClicked -> onCellClicked(action.position)
            is BoardAction.DugoutPlayerClicked -> togglePlayerSelection(action.playerId)
            is BoardAction.DugoutSectionClicked -> onDugoutSectionClicked(action.team, action.status)
            BoardAction.BallClicked -> toggleBallSelection()
            BoardAction.OffPitchBallAreaClicked -> onOffPitchBallAreaClicked()
            BoardAction.ClearSelection -> select(null)
            is BoardAction.MessageShown -> mutableState.update {
                if (it.message?.id == action.id) it.copy(message = null) else it
            }
        }
    }

    private fun onCellClicked(position: BoardPosition) {
        val current = state.value
        val selection = current.selection
        val occupant = current.setup.playerAt(position)
        when {
            selection == Selection.Ball -> apply(current.setup.moveBall(position))
            occupant != null -> togglePlayerSelection(occupant.id)
            selection is Selection.Player ->
                apply(current.setup.movePlayerToCell(selection.playerId, position))
            current.setup.ball.position == position -> toggleBallSelection()
            // Nothing selected and an empty cell: nothing to do.
        }
    }

    private fun onDugoutSectionClicked(team: TeamId, status: PlayerStatus) {
        val current = state.value
        when (val selection = current.selection) {
            is Selection.Player ->
                apply(current.setup.movePlayerToDugout(selection.playerId, team, status))
            Selection.Ball -> showMessage("The ball cannot be put in a dugout.")
            null -> Unit
        }
    }

    private fun onOffPitchBallAreaClicked() {
        val current = state.value
        when (current.selection) {
            Selection.Ball -> apply(current.setup.moveBall(null))
            is Selection.Player -> showMessage("Only the ball can be placed here.")
            null -> if (!current.setup.ball.isOnPitch) select(Selection.Ball)
        }
    }

    private fun togglePlayerSelection(playerId: PlayerId) {
        val target = Selection.Player(playerId)
        select(if (state.value.selection == target) null else target)
    }

    private fun toggleBallSelection() {
        select(if (state.value.selection == Selection.Ball) null else Selection.Ball)
    }

    private fun select(selection: Selection?) {
        mutableState.update { it.copy(selection = selection) }
    }

    /** Applies an edit: on success, store the new setup and clear the selection; else explain. */
    private fun apply(result: EditResult) {
        when (result) {
            is EditResult.Success -> mutableState.update {
                it.copy(
                    setup = result.setup,
                    selection = null,
                    isDirty = it.isDirty || result.setup != it.setup,
                )
            }
            is EditResult.Failure -> showMessage(result.reason.describe(state.value.setup))
        }
    }

    private fun showMessage(text: String) {
        val message = EditorMessage(id = nextMessageId++, text = text)
        mutableState.update { it.copy(message = message) }
    }
}
