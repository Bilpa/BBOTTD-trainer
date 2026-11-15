package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.domain.BallState
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.TacticalSetup
import org.example.bbottd_trainer.domain.TeamId
import org.example.bbottd_trainer.domain.onFieldPlayer
import org.example.bbottd_trainer.domain.reservePlayer
import org.example.bbottd_trainer.domain.setup
import org.example.bbottd_trainer.domain.team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardStateHolderTest {

    /** h1 on the pitch at (7, 13), h2 a home reserve, a1 on the pitch at (7, 12). Ball off pitch. */
    private val start = setup(
        home = team(TeamId.HOME, onFieldPlayer("h1", x = 7, y = 13), reservePlayer("h2")),
        away = team(TeamId.AWAY, onFieldPlayer("a1", x = 7, y = 12)),
    )

    private fun holder(setup: TacticalSetup = start) = BoardStateHolder(initialSetup = setup)

    private fun BoardStateHolder.click(vararg actions: BoardAction) = actions.forEach(::onAction)

    private val BoardStateHolder.current get() = state.value

    private fun BoardStateHolder.positionOf(id: String): BoardPosition? =
        current.setup.findPlayer(PlayerId(id))?.position

    private fun BoardStateHolder.statusOf(id: String): PlayerStatus? =
        current.setup.findPlayer(PlayerId(id))?.status

    // --- Initial state ---------------------------------------------------------------------

    @Test
    fun startsWithTheInitialSampleSetupByDefault() {
        assertEquals(SampleSetups.initial, BoardStateHolder().current.setup)
    }

    @Test
    fun startsWithNothingSelectedNoMessageAndNoUnsavedChanges() {
        val state = holder().current

        assertNull(state.selection)
        assertNull(state.message)
        assertFalse(state.isDirty)
    }

    // --- Dugout visibility -----------------------------------------------------------------

    @Test
    fun dugoutsAreVisibleByDefaultAndCanStartHidden() {
        assertTrue(BoardStateHolder().current.dugoutsVisible)
        assertFalse(BoardStateHolder(dugoutsVisible = false).current.dugoutsVisible)
    }

    @Test
    fun setDugoutsVisibleShowsAndHidesThemWithoutMarkingDirty() {
        val holder = BoardStateHolder(dugoutsVisible = false)

        holder.click(BoardAction.SetDugoutsVisible(true))
        assertTrue(holder.current.dugoutsVisible)
        holder.click(BoardAction.SetDugoutsVisible(false))
        assertFalse(holder.current.dugoutsVisible)
        assertFalse(holder.current.isDirty)
    }

    // --- Selecting -------------------------------------------------------------------------

    @Test
    fun clickingAPlayersCellSelectsThemAndClickingAgainDeselects() {
        val holder = holder()

        holder.click(BoardAction.CellClicked(BoardPosition(7, 13)))
        assertEquals(Selection.Player(PlayerId("h1")), holder.current.selection)

        holder.click(BoardAction.CellClicked(BoardPosition(7, 13)))
        assertNull(holder.current.selection)
    }

    @Test
    fun clickingADugoutPlayerSelectsThem() {
        val holder = holder()

        holder.click(BoardAction.DugoutPlayerClicked(PlayerId("h2")))

        assertEquals(Selection.Player(PlayerId("h2")), holder.current.selection)
    }

    @Test
    fun clickingAnotherPlayerSwitchesTheSelectionInsteadOfMoving() {
        val holder = holder()

        holder.click(
            BoardAction.CellClicked(BoardPosition(7, 13)),
            BoardAction.CellClicked(BoardPosition(7, 12)),
        )

        assertEquals(Selection.Player(PlayerId("a1")), holder.current.selection)
        assertEquals(BoardPosition(7, 13), holder.positionOf("h1"))
        assertNull(holder.current.message)
    }

    @Test
    fun clickingAnEmptyCellWithNothingSelectedDoesNothing() {
        val holder = holder()

        holder.click(BoardAction.CellClicked(BoardPosition(0, 0)))

        assertEquals(start, holder.current.setup)
        assertNull(holder.current.selection)
        assertNull(holder.current.message)
    }

    @Test
    fun escapeClearsTheSelection() {
        val holder = holder()

        holder.click(BoardAction.DugoutPlayerClicked(PlayerId("h2")), BoardAction.ClearSelection)

        assertNull(holder.current.selection)
    }

    // --- Moving players --------------------------------------------------------------------

    @Test
    fun selectedDugoutPlayerMovesToAnEmptyCellAsOnField() {
        val holder = holder()

        holder.click(
            BoardAction.DugoutPlayerClicked(PlayerId("h2")),
            BoardAction.CellClicked(BoardPosition(3, 20)),
        )

        assertEquals(BoardPosition(3, 20), holder.positionOf("h2"))
        assertEquals(PlayerStatus.ON_FIELD, holder.statusOf("h2"))
        assertNull(holder.current.selection, "selection cleared after a move")
        assertTrue(holder.current.isDirty)
    }

    @Test
    fun selectedPitchPlayerMovesToAnotherCell() {
        val holder = holder()

        holder.click(
            BoardAction.CellClicked(BoardPosition(7, 13)),
            BoardAction.CellClicked(BoardPosition(1, 1)),
        )

        assertEquals(BoardPosition(1, 1), holder.positionOf("h1"))
    }

    @Test
    fun selectedPlayerMovesIntoTheClickedDugoutSection() {
        val holder = holder()

        holder.click(
            BoardAction.CellClicked(BoardPosition(7, 13)),
            BoardAction.DugoutSectionClicked(TeamId.HOME, PlayerStatus.KNOCKED_OUT),
        )

        assertEquals(PlayerStatus.KNOCKED_OUT, holder.statusOf("h1"))
        assertNull(holder.positionOf("h1"))
        assertNull(holder.current.selection)
    }

    @Test
    fun moveIntoTheOtherTeamsDugoutShowsAMessageAndKeepsTheSelection() {
        val holder = holder()

        holder.click(
            BoardAction.CellClicked(BoardPosition(7, 13)),
            BoardAction.DugoutSectionClicked(TeamId.AWAY, PlayerStatus.RESERVE),
        )

        assertEquals(BoardPosition(7, 13), holder.positionOf("h1"))
        assertEquals(Selection.Player(PlayerId("h1")), holder.current.selection)
        assertEquals("h1 can only go to the HOME dugout.", holder.current.message?.text)
        assertFalse(holder.current.isDirty)
    }

    @Test
    fun dugoutSectionClickWithNothingSelectedDoesNothing() {
        val holder = holder()

        holder.click(BoardAction.DugoutSectionClicked(TeamId.HOME, PlayerStatus.CASUALTY))

        assertEquals(start, holder.current.setup)
        assertNull(holder.current.message)
    }

    // --- Moving the ball -------------------------------------------------------------------

    @Test
    fun ballInTheOffPitchAreaCanBeSelectedByClickingTheAreaOrTheBall() {
        val viaArea = holder().apply { click(BoardAction.OffPitchBallAreaClicked) }
        val viaBall = holder().apply { click(BoardAction.BallClicked) }

        assertEquals(Selection.Ball, viaArea.current.selection)
        assertEquals(Selection.Ball, viaBall.current.selection)
    }

    @Test
    fun selectedBallMovesToAnEmptyCellOrOntoAPlayer() {
        val holder = holder()

        holder.click(BoardAction.BallClicked, BoardAction.CellClicked(BoardPosition(4, 4)))
        assertEquals(BoardPosition(4, 4), holder.current.setup.ball.position)
        assertNull(holder.current.selection)

        holder.click(BoardAction.BallClicked, BoardAction.CellClicked(BoardPosition(7, 13)))
        assertEquals(BoardPosition(7, 13), holder.current.setup.ball.position)
        assertEquals(BoardPosition(7, 13), holder.positionOf("h1"), "player not moved")
    }

    @Test
    fun clickingTheBallsCellSelectsTheBallWhenNoPlayerIsThere() {
        val holder = holder(start.copy(ball = BallState(BoardPosition(4, 4))))

        holder.click(BoardAction.CellClicked(BoardPosition(4, 4)))

        assertEquals(Selection.Ball, holder.current.selection)
    }

    @Test
    fun selectedBallGoesBackOffThePitchViaTheOffPitchArea() {
        val holder = holder(start.copy(ball = BallState(BoardPosition(4, 4))))

        holder.click(BoardAction.BallClicked, BoardAction.OffPitchBallAreaClicked)

        assertNull(holder.current.setup.ball.position)
        assertNull(holder.current.selection)
    }

    @Test
    fun ballCannotGoIntoADugout() {
        val holder = holder()

        holder.click(
            BoardAction.BallClicked,
            BoardAction.DugoutSectionClicked(TeamId.HOME, PlayerStatus.RESERVE),
        )

        assertEquals("The ball cannot be put in a dugout.", holder.current.message?.text)
        assertEquals(Selection.Ball, holder.current.selection)
    }

    @Test
    fun aPlayerCannotGoIntoTheOffPitchBallArea() {
        val holder = holder()

        holder.click(
            BoardAction.DugoutPlayerClicked(PlayerId("h2")),
            BoardAction.OffPitchBallAreaClicked,
        )

        assertEquals("Only the ball can be placed here.", holder.current.message?.text)
        assertNull(holder.positionOf("h2"))
    }

    // --- Messages and unsaved changes ------------------------------------------------------

    @Test
    fun eachMessageGetsANewIdAndShownClearsOnlyThatMessage() {
        val holder = holder()
        holder.click(BoardAction.BallClicked, BoardAction.DugoutSectionClicked(TeamId.HOME, PlayerStatus.RESERVE))
        val first = assertNotNull(holder.current.message)

        holder.click(BoardAction.DugoutSectionClicked(TeamId.HOME, PlayerStatus.RESERVE))
        val second = assertNotNull(holder.current.message)
        assertNotEquals(first.id, second.id, "same text, new message")

        holder.click(BoardAction.MessageShown(first.id))
        assertEquals(second, holder.current.message, "an older id does not clear a newer message")

        holder.click(BoardAction.MessageShown(second.id))
        assertNull(holder.current.message)
    }

    @Test
    fun aMoveThatChangesNothingDoesNotMarkTheSetupDirty() {
        val holder = holder()

        // A reserve moved into the Reserves section they are already in.
        holder.click(
            BoardAction.DugoutPlayerClicked(PlayerId("h2")),
            BoardAction.DugoutSectionClicked(TeamId.HOME, PlayerStatus.RESERVE),
        )

        assertEquals(start, holder.current.setup)
        assertFalse(holder.current.isDirty)
        assertNull(holder.current.selection)
    }
}
