package org.example.bbottd_trainer.presentation

/** Explicit user actions sent from the UI to [BoardStateHolder]. */
sealed interface BoardAction {
    /** Show or hide both dugouts. */
    data class SetDugoutsVisible(val visible: Boolean) : BoardAction
}
