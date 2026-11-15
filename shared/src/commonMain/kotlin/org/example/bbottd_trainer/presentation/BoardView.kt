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
    val status: PlayerStatus,
)

/**
 * The status a double-click gives a player: standing ↔ prone, and a stunned player stands up.
 * `null` for players in the dugout, where a double-click has no effect.
 */
fun doubleClickStatus(current: PlayerStatus): PlayerStatus? =
    when (current) {
        PlayerStatus.ON_FIELD -> PlayerStatus.PRONE
        PlayerStatus.PRONE, PlayerStatus.STUNNED -> PlayerStatus.ON_FIELD
        PlayerStatus.RESERVE, PlayerStatus.KNOCKED_OUT, PlayerStatus.CASUALTY, PlayerStatus.SENT_OFF -> null
    }

/**
 * The statuses offered in a player's context menu: only those valid where the player is now
 * (standing, prone or stunned on the pitch; the four dugout statuses in the dugout).
 */
fun statusMenuOptions(current: PlayerStatus): List<PlayerStatus> =
    PlayerStatus.entries.filter { it.isOnPitch == current.isOnPitch }

/** One group in a team's dugout, such as its reserves. */
data class DugoutSection(
    val status: PlayerStatus,
    val players: List<PlayerToken>,
)

/** Dugout sections in display order: every status that is not an on-pitch status. */
val DUGOUT_STATUSES: List<PlayerStatus> = PlayerStatus.entries.filter { !it.isOnPitch }

/** The token for one player, or `null` if no player has this ID. */
fun TacticalSetup.tokenOf(playerId: PlayerId): PlayerToken? {
    val teamId = teamOf(playerId) ?: return null
    val team = team(teamId)
    return team.players.firstOrNull { it.id == playerId }?.toToken(team)
}

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

/**
 * Whether clicking [position] would move the current selection there, for target highlighting.
 * A selected player can go to any empty cell; the ball can go to any cell.
 */
fun BoardUiState.isCellTarget(position: BoardPosition): Boolean =
    when (selection) {
        Selection.Ball -> position != setup.ball.position
        is Selection.Player -> setup.playerAt(position) == null
        null -> false
    }

/** Whether the selected player can be moved into [team]'s dugout (only their own team's). */
fun BoardUiState.isDugoutTarget(team: TeamId): Boolean =
    when (val current = selection) {
        is Selection.Player -> setup.teamOf(current.playerId) == team
        Selection.Ball, null -> false
    }

/** Whether the ball is selected and can be taken off the pitch. */
val BoardUiState.isOffPitchBallTarget: Boolean
    get() = selection == Selection.Ball && setup.ball.isOnPitch

private fun Player.toToken(team: Team): PlayerToken =
    PlayerToken(
        playerId = id,
        name = name,
        number = number,
        teamId = team.id,
        teamName = team.name,
        teamColor = team.color,
        status = status,
    )
