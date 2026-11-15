package org.example.bbottd_trainer.domain

// Editing operations on a TacticalSetup.
//
// These are editor and data-integrity rules, not Blood Bowl rules: any player may be moved to any
// empty cell. Each operation returns a new setup or an explicit failure; the input is never
// changed.

/** The outcome of an editing operation. */
sealed interface EditResult {
    data class Success(val setup: TacticalSetup) : EditResult

    data class Failure(val reason: EditFailure) : EditResult
}

/** Why an editing operation was rejected. The setup is unchanged in every case. */
sealed interface EditFailure {
    data class PlayerNotFound(val playerId: PlayerId) : EditFailure

    data class OutsidePitch(val position: BoardPosition) : EditFailure

    /** Another player already stands on [position]; it is never silently replaced. */
    data class CellOccupied(val position: BoardPosition, val occupantId: PlayerId) : EditFailure

    /** Players can only be moved into their own team's dugout. */
    data class NotOwnDugout(val playerId: PlayerId, val dugout: TeamId) : EditFailure

    /**
     * [status] is not valid where the player is or is going: on-pitch statuses need the pitch,
     * dugout statuses need the dugout.
     */
    data class StatusNotAllowed(val playerId: PlayerId, val status: PlayerStatus) : EditFailure
}

/**
 * Moves a player to [target] on the pitch.
 *
 * A player coming from the dugout arrives as [PlayerStatus.ON_FIELD]; a player already on the
 * pitch keeps their status (`ON_FIELD`, `PRONE` or `STUNNED`). Moving a player to the cell they
 * already occupy succeeds without changes. The ball never blocks a move.
 *
 * A player on the pitch who shares a cell with the ball carries it: the ball moves to [target]
 * with them. This is an editor convenience based only on the shared cell, whatever the player's
 * status; it does not apply Blood Bowl possession rules.
 */
fun TacticalSetup.movePlayerToCell(
    playerId: PlayerId,
    target: BoardPosition,
): EditResult {
    val player = findPlayer(playerId) ?: return failure(EditFailure.PlayerNotFound(playerId))
    if (!Board.contains(target)) return failure(EditFailure.OutsidePitch(target))

    val occupant = playerAt(target)
    if (occupant != null && occupant.id != playerId) {
        return failure(EditFailure.CellOccupied(target, occupant.id))
    }

    val status = if (player.isOnPitch) player.status else PlayerStatus.ON_FIELD
    val moved = replacePlayer(player.copy(status = status, position = target))
    // A dugout player has no position, so can never be carrying the ball.
    val carriesBall = player.position != null && player.position == ball.position
    return success(if (carriesBall) moved.copy(ball = BallState(target)) else moved)
}

/**
 * Moves a player into the [status] section of the [dugout] team's dugout, from the pitch or from
 * another dugout section. The player gets that status and loses their pitch position.
 */
fun TacticalSetup.movePlayerToDugout(
    playerId: PlayerId,
    dugout: TeamId,
    status: PlayerStatus,
): EditResult {
    val player = findPlayer(playerId) ?: return failure(EditFailure.PlayerNotFound(playerId))
    if (teamOf(playerId) != dugout) return failure(EditFailure.NotOwnDugout(playerId, dugout))
    if (status.isOnPitch) return failure(EditFailure.StatusNotAllowed(playerId, status))

    return success(replacePlayer(player.copy(status = status, position = null)))
}

/**
 * Changes a player's status where they are (the context-menu action).
 *
 * Only statuses valid for the player's current location are allowed: `ON_FIELD`, `PRONE` or
 * `STUNNED` on the pitch (position kept), or a dugout status in the dugout. Moving between the
 * pitch and the dugout is done with [movePlayerToCell] or [movePlayerToDugout].
 */
fun TacticalSetup.changePlayerStatus(
    playerId: PlayerId,
    status: PlayerStatus,
): EditResult {
    val player = findPlayer(playerId) ?: return failure(EditFailure.PlayerNotFound(playerId))
    if (status.isOnPitch != player.isOnPitch) {
        return failure(EditFailure.StatusNotAllowed(playerId, status))
    }

    return success(replacePlayer(player.copy(status = status)))
}

/**
 * Moves the ball to [target], or off the pitch when [target] is `null`. The ball may share a cell
 * with a player; it never moves players or implies possession.
 */
fun TacticalSetup.moveBall(target: BoardPosition?): EditResult {
    if (target != null && !Board.contains(target)) {
        return failure(EditFailure.OutsidePitch(target))
    }
    return success(copy(ball = BallState(target)))
}

/** Replaces the player with the same ID, keeping their place in their team's roster order. */
private fun TacticalSetup.replacePlayer(updated: Player): TacticalSetup =
    copy(home = home.replacePlayer(updated), away = away.replacePlayer(updated))

private fun Team.replacePlayer(updated: Player): Team =
    if (players.none { it.id == updated.id }) {
        this
    } else {
        copy(players = players.map { if (it.id == updated.id) updated else it })
    }

private fun success(setup: TacticalSetup): EditResult = EditResult.Success(setup)

private fun failure(reason: EditFailure): EditResult = EditResult.Failure(reason)
