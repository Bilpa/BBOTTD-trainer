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
import org.example.bbottd_trainer.domain.changePlayerStatus
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
 * - With a player selected, clicking an empty cell or a dugout section moves them there. After a
 *   move to a cell the player stays selected; after a move into the dugout the selection clears.
 * - With the ball selected, clicking any cell (even a player's) moves the ball there, and clicking
 *   the off-pitch area takes it off the pitch; both clear the selection.
 * - A failed move keeps the selection and shows a message.
 * - [BoardAction.ClearSelection] (Escape, right-clicking an empty cell, clicking outside the
 *   pitch) deselects.
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
            is BoardAction.PlayerDoubleClicked -> onPlayerDoubleClicked(action.playerId)
            is BoardAction.OpenStatusMenu ->
                mutableState.update { it.copy(statusMenuPlayerId = action.playerId) }
            BoardAction.DismissStatusMenu -> closeStatusMenu()
            is BoardAction.SetPlayerStatus -> {
                closeStatusMenu()
                // A status change is not a move, so the current selection is kept.
                apply(
                    state.value.setup.changePlayerStatus(action.playerId, action.status),
                    clearSelection = false,
                )
            }
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
            // The player stays selected after moving to a cell, so they can be moved again.
            selection is Selection.Player ->
                apply(
                    current.setup.movePlayerToCell(selection.playerId, position),
                    clearSelection = false,
                )
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

    private fun onPlayerDoubleClicked(playerId: PlayerId) {
        val setup = state.value.setup
        val player = setup.findPlayer(playerId) ?: return
        // In the dugout a double-click has no effect.
        val newStatus = doubleClickStatus(player.status) ?: return
        // A status change, not a move: the selection is kept.
        apply(setup.changePlayerStatus(playerId, newStatus), clearSelection = false)
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

    private fun closeStatusMenu() {
        mutableState.update { it.copy(statusMenuPlayerId = null) }
    }

    /**
     * Applies an edit: on success, store the new setup (and clear the selection after a move);
     * on failure, explain why in a message.
     */
    private fun apply(result: EditResult, clearSelection: Boolean = true) {
        when (result) {
            is EditResult.Success -> mutableState.update {
                it.copy(
                    setup = result.setup,
                    selection = if (clearSelection) null else it.selection,
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
