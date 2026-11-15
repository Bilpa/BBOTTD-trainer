package org.example.bbottd_trainer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SetupEditingTest {

    private fun player(id: String, status: PlayerStatus, x: Int? = null, y: Int? = null): Player =
        Player(
            id = PlayerId(id),
            name = id,
            number = null,
            status = status,
            position = if (x != null && y != null) BoardPosition(x, y) else null,
        )

    /**
     * Home: h1 on the pitch at (7, 13), h2 prone at (5, 14), h3 stunned at (9, 14), h4 a reserve.
     * Away: a1 on the pitch at (7, 12), a2 a reserve. The ball is off the pitch.
     */
    private val start = setup(
        home = team(
            TeamId.HOME,
            player("h1", PlayerStatus.ON_FIELD, 7, 13),
            player("h2", PlayerStatus.PRONE, 5, 14),
            player("h3", PlayerStatus.STUNNED, 9, 14),
            player("h4", PlayerStatus.RESERVE),
        ),
        away = team(
            TeamId.AWAY,
            player("a1", PlayerStatus.ON_FIELD, 7, 12),
            player("a2", PlayerStatus.RESERVE),
        ),
    )

    private fun EditResult.setup(): TacticalSetup = assertIs<EditResult.Success>(this).setup

    private fun EditResult.failure(): EditFailure = assertIs<EditResult.Failure>(this).reason

    private fun TacticalSetup.player(id: String): Player =
        assertNotNull(findPlayer(PlayerId(id)), "player $id")

    // --- Moving to a pitch cell -------------------------------------------------------------

    @Test
    fun dugoutPlayerMovedToEmptyCellArrivesOnField() {
        val result = start.movePlayerToCell(PlayerId("h4"), BoardPosition(3, 20)).setup()

        val moved = result.player("h4")
        assertEquals(PlayerStatus.ON_FIELD, moved.status)
        assertEquals(BoardPosition(3, 20), moved.position)
        assertEquals(PlayerId("h4"), moved.id)
    }

    @Test
    fun dugoutPlayerWithAnyDugoutStatusArrivesOnField() {
        val knockedOut = setup(home = team(TeamId.HOME, player("h1", PlayerStatus.KNOCKED_OUT)))

        val result = knockedOut.movePlayerToCell(PlayerId("h1"), BoardPosition(0, 0)).setup()

        assertEquals(PlayerStatus.ON_FIELD, result.player("h1").status)
    }

    @Test
    fun playerMovedWithinThePitchKeepsTheirStatus() {
        val afterProne = start.movePlayerToCell(PlayerId("h2"), BoardPosition(1, 1)).setup()
        val afterStunned = start.movePlayerToCell(PlayerId("h3"), BoardPosition(2, 2)).setup()
        val afterStanding = start.movePlayerToCell(PlayerId("h1"), BoardPosition(3, 3)).setup()

        assertEquals(PlayerStatus.PRONE, afterProne.player("h2").status)
        assertEquals(BoardPosition(1, 1), afterProne.player("h2").position)
        assertEquals(PlayerStatus.STUNNED, afterStunned.player("h3").status)
        assertEquals(PlayerStatus.ON_FIELD, afterStanding.player("h1").status)
    }

    @Test
    fun moveOntoAnotherPlayerIsRejectedAndNothingChanges() {
        val ownTeammate = start.movePlayerToCell(PlayerId("h1"), BoardPosition(5, 14))
        val opponent = start.movePlayerToCell(PlayerId("h4"), BoardPosition(7, 12))

        assertEquals(
            EditFailure.CellOccupied(BoardPosition(5, 14), PlayerId("h2")),
            ownTeammate.failure(),
        )
        assertEquals(
            EditFailure.CellOccupied(BoardPosition(7, 12), PlayerId("a1")),
            opponent.failure(),
        )
        assertEquals(BoardPosition(7, 13), start.player("h1").position, "input setup unchanged")
    }

    @Test
    fun moveToOwnCellSucceedsWithoutChanges() {
        val result = start.movePlayerToCell(PlayerId("h1"), BoardPosition(7, 13)).setup()

        assertEquals(start, result)
    }

    @Test
    fun moveOutsideThePitchIsRejected() {
        assertEquals(
            EditFailure.OutsidePitch(BoardPosition(15, 0)),
            start.movePlayerToCell(PlayerId("h1"), BoardPosition(15, 0)).failure(),
        )
        assertEquals(
            EditFailure.OutsidePitch(BoardPosition(0, -1)),
            start.movePlayerToCell(PlayerId("h4"), BoardPosition(0, -1)).failure(),
        )
    }

    @Test
    fun moveOfUnknownPlayerIsRejected() {
        assertEquals(
            EditFailure.PlayerNotFound(PlayerId("nobody")),
            start.movePlayerToCell(PlayerId("nobody"), BoardPosition(0, 0)).failure(),
        )
    }

    @Test
    fun theBallDoesNotBlockAMove() {
        val withBall = start.moveBall(BoardPosition(4, 4)).setup()

        val result = withBall.movePlayerToCell(PlayerId("h4"), BoardPosition(4, 4)).setup()

        assertEquals(BoardPosition(4, 4), result.player("h4").position)
        assertEquals(BoardPosition(4, 4), result.ball.position)
    }

    @Test
    fun movingKeepsRosterOrderAndOtherPlayers() {
        val result = start.movePlayerToCell(PlayerId("h4"), BoardPosition(3, 20)).setup()

        assertEquals(start.home.players.map { it.id }, result.home.players.map { it.id })
        assertEquals(start.away, result.away)
    }

    // --- Moving to a dugout section --------------------------------------------------------

    @Test
    fun pitchPlayerMovedToDugoutSectionGetsThatStatusAndNoPosition() {
        for (status in listOf(
            PlayerStatus.RESERVE,
            PlayerStatus.KNOCKED_OUT,
            PlayerStatus.CASUALTY,
            PlayerStatus.SENT_OFF,
        )) {
            val result = start.movePlayerToDugout(PlayerId("h1"), TeamId.HOME, status).setup()

            assertEquals(status, result.player("h1").status, "$status")
            assertNull(result.player("h1").position, "$status")
        }
    }

    @Test
    fun proneOrStunnedPlayerCanBeMovedToTheDugout() {
        val result = start
            .movePlayerToDugout(PlayerId("h2"), TeamId.HOME, PlayerStatus.KNOCKED_OUT).setup()
            .movePlayerToDugout(PlayerId("h3"), TeamId.HOME, PlayerStatus.CASUALTY).setup()

        assertEquals(PlayerStatus.KNOCKED_OUT, result.player("h2").status)
        assertEquals(PlayerStatus.CASUALTY, result.player("h3").status)
    }

    @Test
    fun dugoutPlayerCanMoveToAnotherSection() {
        val result = start.movePlayerToDugout(PlayerId("h4"), TeamId.HOME, PlayerStatus.SENT_OFF).setup()

        assertEquals(PlayerStatus.SENT_OFF, result.player("h4").status)
        assertNull(result.player("h4").position)
    }

    @Test
    fun movingToTheDugoutFreesTheCell() {
        val result = start
            .movePlayerToDugout(PlayerId("a1"), TeamId.AWAY, PlayerStatus.RESERVE).setup()
            .movePlayerToCell(PlayerId("h4"), BoardPosition(7, 12)).setup()

        assertEquals(BoardPosition(7, 12), result.player("h4").position)
    }

    @Test
    fun moveIntoTheOtherTeamsDugoutIsRejected() {
        assertEquals(
            EditFailure.NotOwnDugout(PlayerId("h1"), TeamId.AWAY),
            start.movePlayerToDugout(PlayerId("h1"), TeamId.AWAY, PlayerStatus.RESERVE).failure(),
        )
    }

    @Test
    fun onPitchStatusIsRejectedForTheDugout() {
        for (status in listOf(PlayerStatus.ON_FIELD, PlayerStatus.PRONE, PlayerStatus.STUNNED)) {
            assertEquals(
                EditFailure.StatusNotAllowed(PlayerId("h1"), status),
                start.movePlayerToDugout(PlayerId("h1"), TeamId.HOME, status).failure(),
                "$status",
            )
        }
    }

    @Test
    fun moveToDugoutOfUnknownPlayerIsRejected() {
        assertEquals(
            EditFailure.PlayerNotFound(PlayerId("nobody")),
            start.movePlayerToDugout(PlayerId("nobody"), TeamId.HOME, PlayerStatus.RESERVE).failure(),
        )
    }

    // --- Changing status (context menu) ----------------------------------------------------

    @Test
    fun pitchPlayerCanBeMadeProneStunnedOrStandingInPlace() {
        for (status in listOf(PlayerStatus.PRONE, PlayerStatus.STUNNED, PlayerStatus.ON_FIELD)) {
            val result = start.changePlayerStatus(PlayerId("h1"), status).setup()

            assertEquals(status, result.player("h1").status, "$status")
            assertEquals(BoardPosition(7, 13), result.player("h1").position, "$status")
        }
    }

    @Test
    fun pitchPlayerCannotGetADugoutStatusFromTheMenu() {
        assertEquals(
            EditFailure.StatusNotAllowed(PlayerId("h1"), PlayerStatus.RESERVE),
            start.changePlayerStatus(PlayerId("h1"), PlayerStatus.RESERVE).failure(),
        )
    }

    @Test
    fun dugoutPlayerCanChangeBetweenDugoutStatuses() {
        val result = start.changePlayerStatus(PlayerId("h4"), PlayerStatus.CASUALTY).setup()

        assertEquals(PlayerStatus.CASUALTY, result.player("h4").status)
        assertNull(result.player("h4").position)
    }

    @Test
    fun dugoutPlayerCannotGetAnOnPitchStatus() {
        for (status in listOf(PlayerStatus.ON_FIELD, PlayerStatus.PRONE, PlayerStatus.STUNNED)) {
            assertEquals(
                EditFailure.StatusNotAllowed(PlayerId("h4"), status),
                start.changePlayerStatus(PlayerId("h4"), status).failure(),
                "$status",
            )
        }
    }

    @Test
    fun statusChangeOfUnknownPlayerIsRejected() {
        assertEquals(
            EditFailure.PlayerNotFound(PlayerId("nobody")),
            start.changePlayerStatus(PlayerId("nobody"), PlayerStatus.PRONE).failure(),
        )
    }

    // --- Moving the ball -------------------------------------------------------------------

    @Test
    fun ballCanBePlacedMovedAndTakenOffThePitch() {
        val placed = start.moveBall(BoardPosition(7, 0)).setup()
        val moved = placed.moveBall(BoardPosition(7, 25)).setup()
        val removed = moved.moveBall(null).setup()

        assertEquals(BoardPosition(7, 0), placed.ball.position)
        assertEquals(BoardPosition(7, 25), moved.ball.position)
        assertNull(removed.ball.position)
    }

    @Test
    fun ballCanShareACellWithAPlayerWithoutMovingThem() {
        val result = start.moveBall(BoardPosition(7, 13)).setup()

        assertEquals(BoardPosition(7, 13), result.ball.position)
        assertEquals(start.players, result.players)
    }

    @Test
    fun ballOutsideThePitchIsRejected() {
        assertEquals(
            EditFailure.OutsidePitch(BoardPosition(-1, 5)),
            start.moveBall(BoardPosition(-1, 5)).failure(),
        )
    }

    // --- Lookups ---------------------------------------------------------------------------

    @Test
    fun lookupsFindPlayersTeamsAndOccupants() {
        assertEquals(TeamId.HOME, start.teamOf(PlayerId("h4")))
        assertEquals(TeamId.AWAY, start.teamOf(PlayerId("a1")))
        assertNull(start.teamOf(PlayerId("nobody")))
        assertEquals(PlayerId("a1"), start.playerAt(BoardPosition(7, 12))?.id)
        assertNull(start.playerAt(BoardPosition(0, 0)))
    }
}
