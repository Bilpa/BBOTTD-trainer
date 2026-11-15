package org.example.bbottd_trainer.domain

/** The side a team plays on. Home defends the south end zone (`y = 25`), away the north (`y = 0`). */
enum class TeamId {
    HOME,
    AWAY,
}

/** Team colour as a plain domain value; the UI maps it to the colour it displays. */
enum class TeamColor {
    BLUE,
    RED,
}

/**
 * One side of a tactical setup.
 *
 * The team owns its players: this is the only record of which team a player belongs to.
 */
data class Team(
    val id: TeamId,
    val name: String,
    val color: TeamColor,
    val players: List<Player>,
)
