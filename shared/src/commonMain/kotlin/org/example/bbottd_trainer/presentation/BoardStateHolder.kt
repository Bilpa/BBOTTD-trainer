package org.example.bbottd_trainer.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.TacticalSetup

/**
 * Holds the board screen's state and exposes it as a [StateFlow].
 *
 * This is a plain Kotlin class, not a lifecycle ViewModel. The UI observes [state] and sends
 * explicit [BoardAction]s to [onAction]; it never mutates the setup itself. Editing actions
 * (selection, moving players and the ball, reset) are added in Milestone 2.
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

    val state: StateFlow<BoardUiState> = mutableState.asStateFlow()

    fun onAction(action: BoardAction) {
        when (action) {
            is BoardAction.SetDugoutsVisible ->
                mutableState.update { it.copy(dugoutsVisible = action.visible) }
        }
    }
}
