package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.domain.BallState
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.EditFailure
import org.example.bbottd_trainer.domain.Player
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.TeamColor
import org.example.bbottd_trainer.domain.TeamId
import org.example.bbottd_trainer.domain.onFieldPlayer
import org.example.bbottd_trainer.domain.reservePlayer
import org.example.bbottd_trainer.domain.setup
import org.example.bbottd_trainer.domain.team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardViewTest {

    private val knockedOutAwayPlayer =
        Player(PlayerId("a2"), "a2", 7, PlayerStatus.KNOCKED_OUT, position = null)

    private val mixedSetup = setup(
        home = team(TeamId.HOME, onFieldPlayer("h1", x = 7, y = 13), reservePlayer("h2")),
        away = team(TeamId.AWAY, onFieldPlayer("a1", x = 7, y = 12), knockedOutAwayPlayer),
    )

    @Test
    fun pitchTokensContainOnlyOnFieldPlayersAtTheirCells() {
        val tokens = mixedSetup.pitchTokens()

        assertEquals(setOf(BoardPosition(7, 13), BoardPosition(7, 12)), tokens.keys)
        assertEquals(PlayerId("h1"), tokens.getValue(BoardPosition(7, 13)).playerId)
        assertEquals(PlayerId("a1"), tokens.getValue(BoardPosition(7, 12)).playerId)
    }

    @Test
    fun tokensCarryTheOwningTeam() {
        val tokens = mixedSetup.pitchTokens()

        val home = tokens.getValue(BoardPosition(7, 13))
        assertEquals(TeamId.HOME, home.teamId)
        assertEquals(TeamColor.BLUE, home.teamColor)

        val away = tokens.getValue(BoardPosition(7, 12))
        assertEquals(TeamId.AWAY, away.teamId)
        assertEquals(TeamColor.RED, away.teamColor)
    }

    @Test
    fun dugoutHasOneSectionPerDugoutStatusInOrder() {
        val statuses = mixedSetup.dugoutSections(TeamId.HOME).map { it.status }

        assertEquals(
            listOf(
                PlayerStatus.RESERVE,
                PlayerStatus.KNOCKED_OUT,
                PlayerStatus.CASUALTY,
                PlayerStatus.SENT_OFF,
            ),
            statuses,
        )
    }

    @Test
    fun playersAppearOnlyInTheirOwnTeamsDugoutSection() {
        val home = mixedSetup.dugoutSections(TeamId.HOME).associate { it.status to it.players.map { p -> p.playerId } }
        val away = mixedSetup.dugoutSections(TeamId.AWAY).associate { it.status to it.players.map { p -> p.playerId } }

        assertEquals(listOf(PlayerId("h2")), home[PlayerStatus.RESERVE])
        assertEquals(emptyList(), home[PlayerStatus.KNOCKED_OUT])
        assertEquals(emptyList(), away[PlayerStatus.RESERVE])
        assertEquals(listOf(PlayerId("a2")), away[PlayerStatus.KNOCKED_OUT])
    }

    @Test
    fun onFieldPlayersAreNotInTheDugout() {
        val dugoutIds = (mixedSetup.dugoutSections(TeamId.HOME) + mixedSetup.dugoutSections(TeamId.AWAY))
            .flatMap { it.players }
            .map { it.playerId }

        assertTrue(PlayerId("h1") !in dugoutIds)
        assertTrue(PlayerId("a1") !in dugoutIds)
    }

    @Test
    fun statusMenuOffersOnlyStatusesValidWhereThePlayerIs() {
        val onPitch = listOf(PlayerStatus.ON_FIELD, PlayerStatus.PRONE, PlayerStatus.STUNNED)
        val inDugout = listOf(
            PlayerStatus.RESERVE,
            PlayerStatus.KNOCKED_OUT,
            PlayerStatus.CASUALTY,
            PlayerStatus.SENT_OFF,
        )

        for (status in onPitch) assertEquals(onPitch, statusMenuOptions(status), "$status")
        for (status in inDugout) assertEquals(inDugout, statusMenuOptions(status), "$status")
    }

    @Test
    fun doubleClickStatusTogglesStandingAndProneAndIgnoresTheDugout() {
        assertEquals(PlayerStatus.PRONE, doubleClickStatus(PlayerStatus.ON_FIELD))
        assertEquals(PlayerStatus.ON_FIELD, doubleClickStatus(PlayerStatus.PRONE))
        assertEquals(PlayerStatus.ON_FIELD, doubleClickStatus(PlayerStatus.STUNNED))
        for (status in PlayerStatus.entries.filter { !it.isOnPitch }) {
            assertEquals(null, doubleClickStatus(status), "$status")
        }
    }

    @Test
    fun tokensCarryThePlayersStatus() {
        val prone = Player(PlayerId("h1"), "h1", 1, PlayerStatus.PRONE, BoardPosition(3, 13))
        val result = setup(home = team(TeamId.HOME, prone))

        assertEquals(PlayerStatus.PRONE, result.pitchTokens().getValue(BoardPosition(3, 13)).status)
        assertEquals(
            PlayerStatus.KNOCKED_OUT,
            mixedSetup.dugoutSections(TeamId.AWAY).flatMap { it.players }.single().status,
        )
    }

    @Test
    fun withNothingSelectedThereAreNoTargets() {
        val state = BoardUiState(setup = mixedSetup)

        assertFalse(state.isCellTarget(BoardPosition(0, 0)))
        assertFalse(state.isDugoutTarget(TeamId.HOME))
        assertFalse(state.isOffPitchBallTarget)
    }

    @Test
    fun aSelectedPlayerTargetsEmptyCellsAndOnlyTheirOwnDugout() {
        val state = BoardUiState(setup = mixedSetup, selection = Selection.Player(PlayerId("h2")))

        assertTrue(state.isCellTarget(BoardPosition(0, 0)), "empty cell")
        assertFalse(state.isCellTarget(BoardPosition(7, 13)), "occupied by a teammate")
        assertFalse(state.isCellTarget(BoardPosition(7, 12)), "occupied by an opponent")
        assertTrue(state.isDugoutTarget(TeamId.HOME))
        assertFalse(state.isDugoutTarget(TeamId.AWAY))
        assertFalse(state.isOffPitchBallTarget)
    }

    @Test
    fun theSelectedBallTargetsEveryOtherCellIncludingPlayersAndTheOffPitchArea() {
        val onPitch = BoardUiState(
            setup = mixedSetup.copy(ball = BallState(BoardPosition(4, 4))),
            selection = Selection.Ball,
        )

        assertTrue(onPitch.isCellTarget(BoardPosition(0, 0)))
        assertTrue(onPitch.isCellTarget(BoardPosition(7, 13)), "a player's cell")
        assertFalse(onPitch.isCellTarget(BoardPosition(4, 4)), "where the ball already is")
        assertFalse(onPitch.isDugoutTarget(TeamId.HOME))
        assertTrue(onPitch.isOffPitchBallTarget)

        val offPitch = onPitch.copy(setup = mixedSetup)
        assertFalse(offPitch.isOffPitchBallTarget, "already off the pitch")
    }

    @Test
    fun failuresAreDescribedWithPlayerAndTeamNames() {
        val sample = SampleSetups.initial
        val home1 = PlayerId("home-1")

        assertEquals(
            "Cell (7, 12) is occupied by Away Player 10.",
            EditFailure.CellOccupied(BoardPosition(7, 12), PlayerId("away-10")).describe(sample),
        )
        assertEquals(
            "Home Player 1 can only go to the Home dugout.",
            EditFailure.NotOwnDugout(home1, TeamId.AWAY).describe(sample),
        )
        assertEquals(
            "Home Player 1 cannot be set to Prone there.",
            EditFailure.StatusNotAllowed(home1, PlayerStatus.PRONE).describe(sample),
        )
        assertEquals("(15, 0) is outside the pitch.", EditFailure.OutsidePitch(BoardPosition(15, 0)).describe(sample))
    }

    @Test
    fun proneAndStunnedPlayersAreOnThePitchNotInTheDugout() {
        val prone = Player(PlayerId("h1"), "h1", 1, PlayerStatus.PRONE, BoardPosition(3, 13))
        val stunned = Player(PlayerId("a1"), "a1", 1, PlayerStatus.STUNNED, BoardPosition(3, 12))
        val result = setup(home = team(TeamId.HOME, prone), away = team(TeamId.AWAY, stunned))

        assertEquals(setOf(BoardPosition(3, 13), BoardPosition(3, 12)), result.pitchTokens().keys)
        val dugoutIds = (result.dugoutSections(TeamId.HOME) + result.dugoutSections(TeamId.AWAY))
            .flatMap { it.players }
        assertTrue(dugoutIds.isEmpty())
    }

    @Test
    fun initialSampleShowsAll16PlayersOfEachTeamAsReservesAndAnEmptyPitch() {
        val sample = SampleSetups.initial

        assertTrue(sample.pitchTokens().isEmpty())
        for (teamId in TeamId.entries) {
            val sections = sample.dugoutSections(teamId).associate { it.status to it.players }
            assertEquals(16, sections.getValue(PlayerStatus.RESERVE).size, "$teamId reserves")
            assertEquals(0, sections.filterKeys { it != PlayerStatus.RESERVE }.values.sumOf { it.size }, "$teamId others")
        }
    }
}
