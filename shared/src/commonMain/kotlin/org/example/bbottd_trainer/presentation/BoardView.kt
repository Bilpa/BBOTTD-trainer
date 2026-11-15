package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.Player
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.TacticalSetup
import org.example.bbottd_trainer.domain.Team
import org.example.bbottd_trainer.domain.TeamColor
import org.example.bbottd_trainer.domain.TeamId

/**
 * What the UI needs to draw one player. The team is taken from the team that owns the player, so
 * every token can show which team it belongs to.
 */
data class PlayerToken(
    val playerId: PlayerId,
    val name: String,
    val number: Int?,
    val teamId: TeamId,
    val teamName: String,
    val teamColor: TeamColor,
)

/** One group in a team's dugout, such as its reserves. */
data class DugoutSection(
    val status: PlayerStatus,
    val players: List<PlayerToken>,
)

/** Dugout sections in display order: every status except [PlayerStatus.ON_FIELD]. */
val DUGOUT_STATUSES: List<PlayerStatus> = PlayerStatus.entries.filter { it != PlayerStatus.ON_FIELD }

/** Tokens of all players on the pitch, by the cell they occupy. */
fun TacticalSetup.pitchTokens(): Map<BoardPosition, PlayerToken> =
    listOf(home, away)
        .flatMap { team ->
            team.players.mapNotNull { player ->
                player.position?.let { position -> position to player.toToken(team) }
            }
        }
        .toMap()

/**
 * The sections of one team's dugout, always one per dugout status (possibly empty), so the UI
 * layout does not jump when players move in or out.
 */
fun TacticalSetup.dugoutSections(teamId: TeamId): List<DugoutSection> {
    val team = team(teamId)
    return DUGOUT_STATUSES.map { status ->
        DugoutSection(
            status = status,
            players = team.players.filter { it.status == status }.map { it.toToken(team) },
        )
    }
}

private fun Player.toToken(team: Team): PlayerToken =
    PlayerToken(
        playerId = id,
        name = name,
        number = number,
        teamId = team.id,
        teamName = team.name,
        teamColor = team.color,
    )
