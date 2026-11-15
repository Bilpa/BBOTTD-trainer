package org.example.bbottd_trainer.domain

/**
 * The ball marker. It is independent of players: it may share a cell with a player, and moving it
 * never moves a player or implies possession.
 *
 * A `null` [position] means the ball is off the pitch.
 */
data class BallState(
    val position: BoardPosition?,
) {
    init {
        require(position == null || Board.contains(position)) {
            "The ball is at $position, which is outside the pitch."
        }
    }

    val isOnPitch: Boolean
        get() = position != null
}
