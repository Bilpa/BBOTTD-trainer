package org.example.bbottd_trainer.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.example.bbottd_trainer.domain.BallState
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.Player
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup
import org.example.bbottd_trainer.domain.Team
import org.example.bbottd_trainer.domain.TeamColor
import org.example.bbottd_trainer.domain.TeamId

/** Why a text could not be read as a setup. */
sealed interface SetupDecodeFailure {
    /** The text is not valid JSON, or not a JSON object. */
    data object NotJson : SetupDecodeFailure

    /** Valid JSON, but without the setup file marker: some other kind of file. */
    data object NotASetupFile : SetupDecodeFailure

    /** A setup file, but of a format version this app cannot read (usually a newer app's). */
    data class UnsupportedVersion(val version: Int) : SetupDecodeFailure

    /**
     * A setup file of a supported version whose content is damaged: a missing or unexpected
     * field, a value of the wrong type or an unknown value, or a setup that breaks a domain rule
     * (such as two players on one cell). [detail] is a technical description for diagnostics.
     */
    data class InvalidContent(val detail: String) : SetupDecodeFailure
}

sealed interface SetupDecodeResult {
    data class Success(val setup: TacticalSetup) : SetupDecodeResult

    data class Failure(val failure: SetupDecodeFailure) : SetupDecodeResult
}

/**
 * Converts tactical setups to and from the versioned JSON setup file format.
 *
 * Writing always uses the current version, [CURRENT_VERSION]. Reading checks the format marker and
 * version first, then decodes the content, then rebuilds the domain objects, whose own checks
 * reject setups that break a domain rule. Decoding never throws; every problem is a
 * [SetupDecodeResult.Failure].
 */
object SetupCodec {
    /** Identifies a file as a BBOTTD setup file, so other JSON files are rejected clearly. */
    const val FORMAT_MARKER = "bbottd-setup"

    const val CURRENT_VERSION = 1

    private const val FORMAT_FIELD = "format"
    private const val VERSION_FIELD = "formatVersion"

    private val json = Json { prettyPrint = true }

    fun encode(setup: TacticalSetup): String =
        json.encodeToString(
            SetupFileV1.serializer(),
            SetupFileV1(
                format = FORMAT_MARKER,
                formatVersion = CURRENT_VERSION,
                setup = setup.toV1(),
            ),
        )

    fun decode(text: String): SetupDecodeResult {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return SetupDecodeResult.Failure(SetupDecodeFailure.NotJson)

        val marker = (root[FORMAT_FIELD] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (marker != FORMAT_MARKER) {
            return SetupDecodeResult.Failure(SetupDecodeFailure.NotASetupFile)
        }

        val version = (root[VERSION_FIELD] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
            ?: return SetupDecodeResult.Failure(
                SetupDecodeFailure.InvalidContent("Missing or non-integer $VERSION_FIELD."),
            )

        return when (version) {
            1 -> decodeV1(root)
            else -> SetupDecodeResult.Failure(SetupDecodeFailure.UnsupportedVersion(version))
        }
    }

    private fun decodeV1(root: JsonObject): SetupDecodeResult =
        try {
            val file = json.decodeFromJsonElement(SetupFileV1.serializer(), root)
            SetupDecodeResult.Success(file.setup.toDomain())
        } catch (e: SerializationException) {
            invalid(e)
        } catch (e: IllegalArgumentException) {
            // Thrown by the domain constructors' checks, and by unknown enum values below.
            invalid(e)
        }

    private fun invalid(e: Exception): SetupDecodeResult =
        SetupDecodeResult.Failure(
            SetupDecodeFailure.InvalidContent(e.message ?: e::class.simpleName ?: "Unknown error"),
        )
}

// Domain → file format.

private fun TacticalSetup.toV1(): SetupV1 =
    SetupV1(
        id = id.value,
        name = name,
        notes = notes,
        home = home.toV1(),
        away = away.toV1(),
        ball = BallV1(position = ball.position?.toV1()),
    )

private fun Team.toV1(): TeamV1 =
    TeamV1(name = name, color = color.fileValue, players = players.map { it.toV1() })

private fun Player.toV1(): PlayerV1 =
    PlayerV1(
        id = id.value,
        name = name,
        number = number,
        status = status.fileValue,
        position = position?.toV1(),
    )

private fun BoardPosition.toV1(): PositionV1 = PositionV1(x = x, y = y)

// File format → domain. Domain constructors re-check every invariant.

private fun SetupV1.toDomain(): TacticalSetup =
    TacticalSetup(
        id = SetupId(id),
        name = name,
        home = home.toDomain(TeamId.HOME),
        away = away.toDomain(TeamId.AWAY),
        ball = BallState(position = ball.position?.toDomain()),
        notes = notes,
    )

private fun TeamV1.toDomain(teamId: TeamId): Team =
    Team(
        id = teamId,
        name = name,
        color = teamColorFromFile(color),
        players = players.map { it.toDomain() },
    )

private fun PlayerV1.toDomain(): Player =
    Player(
        id = PlayerId(id),
        name = name,
        number = number,
        status = playerStatusFromFile(status),
        position = position?.toDomain(),
    )

private fun PositionV1.toDomain(): BoardPosition = BoardPosition(x = x, y = y)

// Fixed file values for enums, so renaming a Kotlin enum constant cannot change the file format.

private val TeamColor.fileValue: String
    get() = when (this) {
        TeamColor.BLUE -> "BLUE"
        TeamColor.RED -> "RED"
    }

private val PlayerStatus.fileValue: String
    get() = when (this) {
        PlayerStatus.ON_FIELD -> "ON_FIELD"
        PlayerStatus.PRONE -> "PRONE"
        PlayerStatus.STUNNED -> "STUNNED"
        PlayerStatus.RESERVE -> "RESERVE"
        PlayerStatus.KNOCKED_OUT -> "KNOCKED_OUT"
        PlayerStatus.CASUALTY -> "CASUALTY"
        PlayerStatus.SENT_OFF -> "SENT_OFF"
    }

private fun teamColorFromFile(value: String): TeamColor =
    TeamColor.entries.firstOrNull { it.fileValue == value }
        ?: throw IllegalArgumentException("Unknown team colour \"$value\".")

private fun playerStatusFromFile(value: String): PlayerStatus =
    PlayerStatus.entries.firstOrNull { it.fileValue == value }
        ?: throw IllegalArgumentException("Unknown player status \"$value\".")
