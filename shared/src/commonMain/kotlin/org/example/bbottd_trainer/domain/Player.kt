package org.example.bbottd_trainer.domain

/**
 * A player's status, which also decides where they are. These are editor categories, not a rules
 * implementation.
 *
 * @property isOnPitch `true` for the statuses of a player on the pitch (`ON_FIELD`, `PRONE`,
 *   `STUNNED`), who must have a position. Every other status puts the player in their own team's
 *   dugout, with no position.
 */
enum class PlayerStatus(val isOnPitch: Boolean) {
    ON_FIELD(isOnPitch = true),
    PRONE(isOnPitch = true),
    STUNNED(isOnPitch = true),
    RESERVE(isOnPitch = false),
    KNOCKED_OUT(isOnPitch = false),
    CASUALTY(isOnPitch = false),
    SENT_OFF(isOnPitch = false),
}

/**
 * A player in a tactical setup.
 *
 * Invariant: [position] is non-null and on the pitch exactly when [status] is an on-pitch status
 * ([PlayerStatus.isOnPitch]); otherwise the player is in the dugout and [position] is `null`.
 * The player's team is the [Team] that owns them, so it is not stored here.
 */
data class Player(
    val id: PlayerId,
    val name: String,
    val number: Int?,
    val status: PlayerStatus,
    val position: BoardPosition?,
) {
    init {
        if (status.isOnPitch) {
            requireNotNull(position) { "Player ${id.value} is $status but has no position." }
            require(Board.contains(position)) {
                "Player ${id.value} is at $position, which is outside the pitch."
            }
        } else {
            require(position == null) {
                "Player ${id.value} has status $status, so must not have a position (was $position)."
            }
        }
    }

    val isOnPitch: Boolean
        get() = status.isOnPitch
}
