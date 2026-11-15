package org.example.bbottd_trainer.domain

/**
 * The unit users create, edit and save: two teams, their players and the ball.
 *
 * Invariants, checked on construction:
 * - [home] has [TeamId.HOME] and [away] has [TeamId.AWAY].
 * - Player IDs are unique across both teams.
 * - No two players occupy the same pitch cell.
 */
data class TacticalSetup(
    val id: SetupId,
    val name: String,
    val home: Team,
    val away: Team,
    val ball: BallState,
    val notes: String = "",
) {
    init {
        require(home.id == TeamId.HOME) { "The home team must have ID HOME (was ${home.id})." }
        require(away.id == TeamId.AWAY) { "The away team must have ID AWAY (was ${away.id})." }

        val duplicateIds = players.map { it.id }.duplicates()
        require(duplicateIds.isEmpty()) {
            "Player IDs must be unique; duplicated: ${duplicateIds.joinToString { it.value }}."
        }

        val sharedCells = players.mapNotNull { it.position }.duplicates()
        require(sharedCells.isEmpty()) {
            "Only one player may occupy a cell; shared: ${sharedCells.joinToString()}."
        }
    }

    /** All players of both teams, home first. */
    val players: List<Player>
        get() = home.players + away.players

    fun team(teamId: TeamId): Team =
        when (teamId) {
            TeamId.HOME -> home
            TeamId.AWAY -> away
        }
}

private fun <T> List<T>.duplicates(): Set<T> =
    groupingBy { it }.eachCount().filterValues { it > 1 }.keys
