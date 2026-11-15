package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.TacticalSetup

/**
 * Everything the board screen renders.
 *
 * [setup] is the persisted tactical data. The other properties are transient editor state and are
 * never saved with the setup.
 */
data class BoardUiState(
    val setup: TacticalSetup,
    val selectedPlayerId: PlayerId? = null,
    val selectedPosition: BoardPosition? = null,
    val isDirty: Boolean = false,
)
