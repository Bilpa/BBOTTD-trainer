package org.example.bbottd_trainer.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.example.bbottd_trainer.domain.BallState
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.Player
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup
import org.example.bbottd_trainer.domain.TeamId
import org.example.bbottd_trainer.domain.onFieldPlayer
import org.example.bbottd_trainer.domain.reservePlayer
import org.example.bbottd_trainer.domain.team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SetupCodecTest {
    private fun player(id: String, status: PlayerStatus, position: BoardPosition?, number: Int? = 7) =
        Player(id = PlayerId(id), name = "Name of $id", number = number, status = status, position = position)

    /** A setup that uses every status, both colours, a missing number and the ball on the pitch. */
    private val varied: TacticalSetup =
        TacticalSetup(
            id = SetupId("varied"),
            name = "Varied \"setup\" – ünïcode",
            home = team(
                TeamId.HOME,
                player("h1", PlayerStatus.ON_FIELD, BoardPosition(0, 0)),
                player("h2", PlayerStatus.PRONE, BoardPosition(14, 25)),
                player("h3", PlayerStatus.STUNNED, BoardPosition(7, 12), number = null),
                player("h4", PlayerStatus.RESERVE, null),
            ),
            away = team(
                TeamId.AWAY,
                player("a1", PlayerStatus.KNOCKED_OUT, null),
                player("a2", PlayerStatus.CASUALTY, null),
                player("a3", PlayerStatus.SENT_OFF, null),
            ),
            ball = BallState(position = BoardPosition(7, 12)),
            notes = "Line one\nLine two",
        )

    private fun decodeSuccess(text: String): TacticalSetup =
        assertIs<SetupDecodeResult.Success>(SetupCodec.decode(text)).setup

    private fun decodeFailure(text: String): SetupDecodeFailure =
        assertIs<SetupDecodeResult.Failure>(SetupCodec.decode(text)).failure

    /** The encoded JSON of [varied] with [change] applied to its root object. */
    private fun encodedWith(change: (MutableMap<String, JsonElement>) -> Unit): String {
        val root = Json.parseToJsonElement(SetupCodec.encode(varied)).jsonObject.toMutableMap()
        change(root)
        return JsonObject(root).toString()
    }

    // Round trips.

    @Test
    fun variedSetupSurvivesRoundTrip() {
        assertEquals(varied, decodeSuccess(SetupCodec.encode(varied)))
    }

    @Test
    fun initialSampleSurvivesRoundTrip() {
        assertEquals(SampleSetups.initial, decodeSuccess(SetupCodec.encode(SampleSetups.initial)))
    }

    @Test
    fun ballOffPitchSurvivesRoundTrip() {
        val setup = varied.copy(ball = BallState(position = null))
        assertEquals(setup, decodeSuccess(SetupCodec.encode(setup)))
    }

    @Test
    fun emptyTeamsSurviveRoundTrip() {
        val setup = varied.copy(home = team(TeamId.HOME), away = team(TeamId.AWAY))
        assertEquals(setup, decodeSuccess(SetupCodec.encode(setup)))
    }

    @Test
    fun playerOrderIsPreserved() {
        val setup = varied.copy(home = team(TeamId.HOME, reservePlayer("z"), reservePlayer("a"), reservePlayer("m")))
        val decoded = decodeSuccess(SetupCodec.encode(setup))
        assertEquals(listOf("z", "a", "m"), decoded.home.players.map { it.id.value })
    }

    // Written form.

    @Test
    fun encodedFileHasMarkerAndCurrentVersion() {
        val root = Json.parseToJsonElement(SetupCodec.encode(varied)).jsonObject
        assertEquals(JsonPrimitive(SetupCodec.FORMAT_MARKER), root["format"])
        assertEquals(JsonPrimitive(1), root["formatVersion"])
        assertEquals(1, SetupCodec.CURRENT_VERSION)
    }

    @Test
    fun encodedFileMatchesVersion1Layout() {
        val setup = TacticalSetup(
            id = SetupId("s"),
            name = "N",
            home = team(TeamId.HOME, onFieldPlayer("h", x = 1, y = 2)),
            away = team(TeamId.AWAY, reservePlayer("a")),
            ball = BallState(position = null),
            notes = "",
        )
        val expected = """
            {"format":"bbottd-setup","formatVersion":1,"setup":{"id":"s","name":"N","notes":"",
            "home":{"name":"HOME","color":"BLUE","players":[{"id":"h","name":"h","number":null,
            "status":"ON_FIELD","position":{"x":1,"y":2}}]},
            "away":{"name":"AWAY","color":"RED","players":[{"id":"a","name":"a","number":null,
            "status":"RESERVE","position":null}]},
            "ball":{"position":null}}}
        """.trimIndent().replace("\n", "")
        // Compare as parsed JSON, so formatting (pretty printing) does not matter.
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(SetupCodec.encode(setup)))
    }

    @Test
    fun handWrittenVersion1FileIsRead() {
        val text = """
            {
              "format": "bbottd-setup",
              "formatVersion": 1,
              "setup": {
                "id": "hand", "name": "Hand-written", "notes": "n",
                "home": { "name": "Home", "color": "BLUE", "players": [
                  { "id": "h1", "name": "Blitzer", "number": 4, "status": "PRONE", "position": { "x": 3, "y": 20 } }
                ] },
                "away": { "name": "Away", "color": "RED", "players": [] },
                "ball": { "position": { "x": 3, "y": 20 } }
              }
            }
        """.trimIndent()
        val setup = decodeSuccess(text)
        assertEquals(SetupId("hand"), setup.id)
        val blitzer = setup.home.players.single()
        assertEquals(PlayerStatus.PRONE, blitzer.status)
        assertEquals(BoardPosition(3, 20), blitzer.position)
        assertEquals(4, blitzer.number)
        assertEquals(BoardPosition(3, 20), setup.ball.position)
    }

    // Not a setup file.

    @Test
    fun malformedJsonIsNotJson() {
        assertEquals(SetupDecodeFailure.NotJson, decodeFailure("{ \"format\": "))
        assertEquals(SetupDecodeFailure.NotJson, decodeFailure(""))
        assertEquals(SetupDecodeFailure.NotJson, decodeFailure("not json at all"))
    }

    @Test
    fun jsonThatIsNotAnObjectIsNotJson() {
        assertEquals(SetupDecodeFailure.NotJson, decodeFailure("[1, 2, 3]"))
        assertEquals(SetupDecodeFailure.NotJson, decodeFailure("42"))
    }

    @Test
    fun missingOrWrongMarkerIsNotASetupFile() {
        assertEquals(SetupDecodeFailure.NotASetupFile, decodeFailure("""{"name": "something else"}"""))
        assertEquals(SetupDecodeFailure.NotASetupFile, decodeFailure(encodedWith { it["format"] = JsonPrimitive("other-app") }))
        assertEquals(SetupDecodeFailure.NotASetupFile, decodeFailure(encodedWith { it["format"] = JsonPrimitive(1) }))
    }

    // Versions.

    @Test
    fun newerVersionIsUnsupported() {
        val text = encodedWith { it["formatVersion"] = JsonPrimitive(2) }
        assertEquals(SetupDecodeFailure.UnsupportedVersion(2), decodeFailure(text))
    }

    @Test
    fun newerVersionIsReportedEvenIfItsContentDiffers() {
        val text = """{"format": "bbottd-setup", "formatVersion": 7, "somethingNew": {}}"""
        assertEquals(SetupDecodeFailure.UnsupportedVersion(7), decodeFailure(text))
    }

    @Test
    fun zeroVersionIsUnsupported() {
        assertEquals(SetupDecodeFailure.UnsupportedVersion(0), decodeFailure(encodedWith { it["formatVersion"] = JsonPrimitive(0) }))
    }

    @Test
    fun missingOrNonIntegerVersionIsInvalidContent() {
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(encodedWith { it.remove("formatVersion") }))
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(encodedWith { it["formatVersion"] = JsonPrimitive("1") }))
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(encodedWith { it["formatVersion"] = JsonPrimitive(1.5) }))
    }

    // Damaged content.

    private fun encodedWithReplacement(old: String, new: String, setup: TacticalSetup = varied): String {
        val text = SetupCodec.encode(setup)
        assertTrue(old in text, "Test setup error: \"$old\" not found in encoded text.")
        return text.replaceFirst(old, new)
    }

    @Test
    fun missingSetupIsInvalidContent() {
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(encodedWith { it.remove("setup") }))
    }

    @Test
    fun missingFieldIsInvalidContent() {
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(encodedWithReplacement("\"notes\":", "\"unused\":")))
    }

    @Test
    fun unknownFieldIsInvalidContent() {
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(encodedWith { it["extra"] = JsonPrimitive(true) }))
    }

    @Test
    fun wrongValueTypeIsInvalidContent() {
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(encodedWithReplacement("\"number\": 7", "\"number\": \"seven\"")))
    }

    @Test
    fun unknownStatusIsInvalidContent() {
        val failure = decodeFailure(encodedWithReplacement("\"PRONE\"", "\"FLYING\""))
        assertIs<SetupDecodeFailure.InvalidContent>(failure)
        assertTrue("FLYING" in failure.detail)
    }

    @Test
    fun unknownColourIsInvalidContent() {
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(encodedWithReplacement("\"BLUE\"", "\"GREEN\"")))
    }

    @Test
    fun lowercaseStatusIsInvalidContent() {
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(encodedWithReplacement("\"PRONE\"", "\"prone\"")))
    }

    @Test
    fun onPitchPlayerWithoutPositionIsInvalidContent() {
        val setup = varied.copy(home = team(TeamId.HOME, onFieldPlayer("h", x = 1, y = 2)))
        val damaged = SetupCodec.encode(setup).replace(Regex("\"position\": \\{[^}]*\\}"), "\"position\": null")
        assertTrue("\"x\"" !in damaged, "Test setup error: the position was not removed.")
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(damaged))
    }

    @Test
    fun positionOutsidePitchIsInvalidContent() {
        val setup = varied.copy(home = team(TeamId.HOME, onFieldPlayer("h", x = 1, y = 2)))
        val damaged = encodedWithReplacement("\"x\": 1", "\"x\": 15", setup)
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(damaged))
    }

    @Test
    fun twoPlayersOnOneCellIsInvalidContent() {
        val setup = varied.copy(
            home = team(TeamId.HOME, onFieldPlayer("h", x = 1, y = 2)),
            away = team(TeamId.AWAY, onFieldPlayer("a", x = 3, y = 2)),
        )
        val damaged = encodedWithReplacement("\"x\": 3", "\"x\": 1", setup)
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(damaged))
    }

    @Test
    fun duplicatePlayerIdsAreInvalidContent() {
        val setup = varied.copy(home = team(TeamId.HOME, reservePlayer("x"), reservePlayer("y")))
        val damaged = encodedWithReplacement("\"id\": \"y\"", "\"id\": \"x\"", setup)
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(damaged))
    }

    @Test
    fun ballOutsidePitchIsInvalidContent() {
        val setup = varied.copy(
            home = team(TeamId.HOME),
            away = team(TeamId.AWAY),
            ball = BallState(position = BoardPosition(4, 5)),
        )
        val damaged = encodedWithReplacement("\"y\": 5", "\"y\": 26", setup)
        assertIs<SetupDecodeFailure.InvalidContent>(decodeFailure(damaged))
    }
}
