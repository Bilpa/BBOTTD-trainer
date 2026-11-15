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

    /**
     * A dragged player or ball was dropped on a target. Applied with the same editing operations
     * as select-then-click.
     */
    data class Drop(val item: DragItem, val target: DropTarget) : BoardAction

    /** The ball token itself was clicked, on the pitch or in the off-pitch area. */
    data object BallClicked : BoardAction

    /** The off-pitch ball area below the pitch was clicked. */
    data object OffPitchBallAreaClicked : BoardAction

    /**
     * A player was double-clicked. On the pitch this toggles standing and prone (a stunned player
     * stands up); in the dugout it does nothing.
     */
    data class PlayerDoubleClicked(val playerId: PlayerId) : BoardAction

    /** Open the status context menu on a player (right-click or long-press). */
    data class OpenStatusMenu(val playerId: PlayerId) : BoardAction

    /** Close the status context menu without choosing anything. */
    data object DismissStatusMenu : BoardAction

    /** A status was chosen in the context menu. */
    data class SetPlayerStatus(val playerId: PlayerId, val status: PlayerStatus) : BoardAction

    /**
     * Clear the selection: Escape, a right-click on an empty pitch cell, or a click outside the
     * pitch.
     */
    data object ClearSelection : BoardAction

    /**
     * The user asked to reset to the initial sample setup. Resets at once if there are no unsaved
     * changes; otherwise asks for confirmation first.
     */
    data object ResetRequested : BoardAction

    /** The user confirmed discarding their changes and resetting. */
    data object ResetConfirmed : BoardAction

    /** The user cancelled the reset confirmation; nothing changes. */
    data object ResetCancelled : BoardAction

    /** The UI has shown the message with this [id]. */
    data class MessageShown(val id: Long) : BoardAction
}
