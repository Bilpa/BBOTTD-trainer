package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.TeamId

/** What is being dragged. */
sealed interface DragItem {
    data class Player(val playerId: PlayerId) : DragItem

    data object Ball : DragItem
}

/** Where a dragged item can be dropped. */
sealed interface DropTarget {
    data class Cell(val position: BoardPosition) : DropTarget

    data class DugoutSection(val team: TeamId, val status: PlayerStatus) : DropTarget

    data object OffPitchBallArea : DropTarget
}

/** The selection equivalent to dragging [this], used to show the same target highlights. */
fun DragItem.asSelection(): Selection =
    when (this) {
        is DragItem.Player -> Selection.Player(playerId)
        DragItem.Ball -> Selection.Ball
    }

/**
 * Whether dropping [item] on [target] would be accepted, for the hover feedback while dragging.
 * Mirrors the editing rules: players go to empty cells (or their own) and their own team's dugout;
 * the ball goes to any cell or the off-pitch area.
 */
fun BoardUiState.isValidDrop(item: DragItem, target: DropTarget): Boolean =
    when (item) {
        is DragItem.Player -> when (target) {
            is DropTarget.Cell -> setup.playerAt(target.position)?.id.let { it == null || it == item.playerId }
            is DropTarget.DugoutSection -> setup.teamOf(item.playerId) == target.team
            DropTarget.OffPitchBallArea -> false
        }
        DragItem.Ball -> when (target) {
            is DropTarget.Cell, DropTarget.OffPitchBallArea -> true
            is DropTarget.DugoutSection -> false
        }
    }
