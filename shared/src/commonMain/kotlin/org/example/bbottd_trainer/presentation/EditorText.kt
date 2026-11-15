package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.EditFailure
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.TacticalSetup

/** The user-facing name of a status, as used for dugout sections and messages. */
val PlayerStatus.label: String
    get() = when (this) {
        PlayerStatus.ON_FIELD -> "Standing"
        PlayerStatus.PRONE -> "Prone"
        PlayerStatus.STUNNED -> "Stunned"
        PlayerStatus.RESERVE -> "Reserves"
        PlayerStatus.KNOCKED_OUT -> "Knocked out"
        PlayerStatus.CASUALTY -> "Casualties"
        PlayerStatus.SENT_OFF -> "Sent off"
    }

/** Pitch coordinates as shown to the user, e.g. "(7, 12)". */
fun BoardPosition.label(): String = "($x, $y)"

/** Explains why an edit was rejected, using names from [setup]. */
fun EditFailure.describe(setup: TacticalSetup): String =
    when (this) {
        is EditFailure.PlayerNotFound -> "That player is not in this setup."
        is EditFailure.OutsidePitch -> "${position.label()} is outside the pitch."
        is EditFailure.CellOccupied ->
            "Cell ${position.label()} is occupied by ${setup.playerName(occupantId)}."
        is EditFailure.NotOwnDugout -> {
            val ownTeam = setup.teamOf(playerId)?.let { setup.team(it).name }
            "${setup.playerName(playerId)} can only go to the ${ownTeam ?: "own"} dugout."
        }
        is EditFailure.StatusNotAllowed ->
            "${setup.playerName(playerId)} cannot be set to ${status.label} there."
    }

private fun TacticalSetup.playerName(playerId: PlayerId): String =
    findPlayer(playerId)?.name ?: "another player"
