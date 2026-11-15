package org.example.bbottd_trainer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TacticalSetupTest {

    @Test
    fun homeTeamMustHaveHomeId() {
        assertFailsWith<IllegalArgumentException> {
            setup(home = team(TeamId.AWAY), away = team(TeamId.AWAY))
        }
    }

    @Test
    fun awayTeamMustHaveAwayId() {
        assertFailsWith<IllegalArgumentException> {
            setup(home = team(TeamId.HOME), away = team(TeamId.HOME))
        }
    }

    @Test
    fun playerIdsMustBeUniqueWithinATeam() {
        assertFailsWith<IllegalArgumentException> {
            setup(home = team(TeamId.HOME, reservePlayer("p1"), reservePlayer("p1")))
        }
    }

    @Test
    fun playerIdsMustBeUniqueAcrossTeams() {
        assertFailsWith<IllegalArgumentException> {
            setup(
                home = team(TeamId.HOME, reservePlayer("p1")),
                away = team(TeamId.AWAY, reservePlayer("p1")),
            )
        }
    }

    @Test
    fun twoPlayersMayNotOccupyTheSameCell() {
        assertFailsWith<IllegalArgumentException> {
            setup(
                home = team(TeamId.HOME, onFieldPlayer("h1", x = 7, y = 12)),
                away = team(TeamId.AWAY, onFieldPlayer("a1", x = 7, y = 12)),
            )
        }
    }

    @Test
    fun playersOnDifferentCellsAreAccepted() {
        val result = setup(
            home = team(TeamId.HOME, onFieldPlayer("h1", x = 7, y = 13), reservePlayer("h2")),
            away = team(TeamId.AWAY, onFieldPlayer("a1", x = 7, y = 12), reservePlayer("a2")),
        )

        assertEquals(4, result.players.size)
    }

    @Test
    fun ballMayShareACellWithAPlayer() {
        val result = setup(
            home = team(TeamId.HOME, onFieldPlayer("h1", x = 7, y = 13)),
            ball = BallState(BoardPosition(7, 13)),
        )

        assertTrue(result.ball.isOnPitch)
    }

    @Test
    fun ballOutsideThePitchIsRejected() {
        assertFailsWith<IllegalArgumentException> { BallState(BoardPosition(-1, 0)) }
        assertFailsWith<IllegalArgumentException> { BallState(BoardPosition(0, 26)) }
    }

    @Test
    fun ballCanBeOffThePitch() {
        assertFalse(BallState(position = null).isOnPitch)
    }

    @Test
    fun teamLookupReturnsTheMatchingSide() {
        val result = setup(
            home = team(TeamId.HOME, reservePlayer("h1")),
            away = team(TeamId.AWAY, reservePlayer("a1")),
        )

        assertEquals(result.home, result.team(TeamId.HOME))
        assertEquals(result.away, result.team(TeamId.AWAY))
    }

    @Test
    fun playersListsHomeThenAway() {
        val result = setup(
            home = team(TeamId.HOME, reservePlayer("h1")),
            away = team(TeamId.AWAY, reservePlayer("a1")),
        )

        assertEquals(listOf(PlayerId("h1"), PlayerId("a1")), result.players.map { it.id })
    }
}
