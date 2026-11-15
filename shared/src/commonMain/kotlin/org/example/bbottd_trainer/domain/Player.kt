package org.example.bbottd_trainer.domain

/**
 * Where a player is. `ON_FIELD` means on the pitch; every other status means the player is in
 * their own team's dugout. These are editor categories, not a rules implementation.
 */
enum class PlayerStatus {
    ON_FIELD,
    RESERVE,
    KNOCKED_OUT,
    CASUALTY,
    SENT_OFF,
}

/**
 * A player in a tactical setup.
 *
 * Invariant: [position] is non-null and on the pitch exactly when [status] is
 * [PlayerStatus.ON_FIELD]; otherwise the player is in the dugout and [position] is `null`.
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
        if (status == PlayerStatus.ON_FIELD) {
            requireNotNull(position) { "Player ${id.value} is ON_FIELD but has no position." }
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
        get() = status == PlayerStatus.ON_FIELD
}
