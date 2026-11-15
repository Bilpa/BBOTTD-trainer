package org.example.bbottd_trainer.presentation

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
    val selection: Selection? = null,
    /** A one-off message for the snackbar; cleared when the UI reports it as shown. */
    val message: EditorMessage? = null,
    val isDirty: Boolean = false,
    val dugoutsVisible: Boolean = true,
)

/** What the user has selected, to be moved by clicking a target. */
sealed interface Selection {
    data class Player(val playerId: PlayerId) : Selection

    data object Ball : Selection
}

/**
 * A message for the user, such as why a move failed. [id] is unique per message, so the same
 * text shown twice in a row is still shown twice.
 */
data class EditorMessage(
    val id: Long,
    val text: String,
)
