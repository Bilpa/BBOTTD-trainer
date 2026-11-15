package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.TeamId

/**
 * Explicit user actions sent from the UI to [BoardStateHolder].
 *
 * Click actions describe what was clicked, not what should happen; the state holder decides
 * (select, move or report a problem) based on the current selection.
 */
sealed interface BoardAction {
    /** Show or hide both dugouts. */
    data class SetDugoutsVisible(val visible: Boolean) : BoardAction

    /** A pitch cell was clicked or tapped, whether or not a player or the ball is on it. */
    data class CellClicked(val position: BoardPosition) : BoardAction

    /** A player token in a dugout was clicked. */
    data class DugoutPlayerClicked(val playerId: PlayerId) : BoardAction

    /** A dugout section (outside its player tokens) was clicked. */
    data class DugoutSectionClicked(val team: TeamId, val status: PlayerStatus) : BoardAction

    /** The ball token itself was clicked, on the pitch or in the off-pitch area. */
    data object BallClicked : BoardAction

    /** The off-pitch ball area below the pitch was clicked. */
    data object OffPitchBallAreaClicked : BoardAction

    /** Clear the selection, for example with Escape. */
    data object ClearSelection : BoardAction

    /** The UI has shown the message with this [id]. */
    data class MessageShown(val id: Long) : BoardAction
}
