package org.example.bbottd_trainer.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.TacticalSetup

/**
 * Holds the board screen's state and exposes it as a [StateFlow].
 *
 * This is a plain Kotlin class, not a lifecycle ViewModel. The UI observes [state] and will send
 * explicit actions to this class; it never mutates the setup itself. Editing actions (selection,
 * moving players and the ball, reset) are added in Milestone 2.
 */
class BoardStateHolder(
    initialSetup: TacticalSetup = SampleSetups.initial,
) {
    private val mutableState = MutableStateFlow(BoardUiState(setup = initialSetup))

    val state: StateFlow<BoardUiState> = mutableState.asStateFlow()
}
