package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.data.SetupDecodeFailure
import org.example.bbottd_trainer.data.StorageFailure
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

/** Explains a storage failure, to follow "Could not save …:" or "Could not open …:". */
fun StorageFailure.describe(): String =
    when (this) {
        StorageFailure.NotFound -> "it is no longer in the library."
        is StorageFailure.Unreadable -> failure.describe()
        is StorageFailure.StorageError -> "the storage reported an error ($detail)."
    }

/** Explains why a text could not be read as a setup. */
fun SetupDecodeFailure.describe(): String =
    when (this) {
        SetupDecodeFailure.NotJson -> "the file is damaged (it is not valid JSON)."
        SetupDecodeFailure.NotASetupFile -> "it is not a BBOTTD setup file."
        is SetupDecodeFailure.UnsupportedVersion ->
            "it was saved in format version $version, which this version of the app cannot read. " +
                "A newer version of the app may be able to open it."
        is SetupDecodeFailure.InvalidContent -> "the file is damaged ($detail)."
    }
