package org.example.bbottd_trainer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerTest {

    private val onPitchStatuses = PlayerStatus.entries.filter { it.isOnPitch }
    private val dugoutStatuses = PlayerStatus.entries.filter { !it.isOnPitch }

    @Test
    fun onFieldProneAndStunnedAreTheOnPitchStatuses() {
        assertEquals(
            listOf(PlayerStatus.ON_FIELD, PlayerStatus.PRONE, PlayerStatus.STUNNED),
            onPitchStatuses,
        )
        assertEquals(
            listOf(
                PlayerStatus.RESERVE,
                PlayerStatus.KNOCKED_OUT,
                PlayerStatus.CASUALTY,
                PlayerStatus.SENT_OFF,
            ),
            dugoutStatuses,
        )
    }

    @Test
    fun everyOnPitchStatusHasAPosition() {
        for (status in onPitchStatuses) {
            val player = Player(PlayerId("p1"), "p1", null, status, BoardPosition(7, 13))

            assertTrue(player.isOnPitch, "$status")
            assertEquals(BoardPosition(7, 13), player.position, "$status")
        }
    }

    @Test
    fun onPitchStatusWithoutPositionIsRejected() {
        for (status in onPitchStatuses) {
            assertFailsWith<IllegalArgumentException>("$status") {
                Player(PlayerId("p1"), "p1", null, status, position = null)
            }
        }
    }

    @Test
    fun onPitchStatusOutsideThePitchIsRejected() {
        for (status in onPitchStatuses) {
            assertFailsWith<IllegalArgumentException>("$status, east") {
                Player(PlayerId("p1"), "p1", null, status, BoardPosition(15, 0))
            }
            assertFailsWith<IllegalArgumentException>("$status, south") {
                Player(PlayerId("p1"), "p1", null, status, BoardPosition(0, 26))
            }
        }
    }

    @Test
    fun everyDugoutStatusHasNoPosition() {
        for (status in dugoutStatuses) {
            val player = Player(PlayerId("p1"), "p1", null, status, position = null)

            assertFalse(player.isOnPitch, "$status")
            assertNull(player.position, "$status")
        }
    }

    @Test
    fun dugoutStatusWithPositionIsRejected() {
        for (status in dugoutStatuses) {
            assertFailsWith<IllegalArgumentException>("$status") {
                Player(PlayerId("p1"), "p1", null, status, BoardPosition(0, 0))
            }
        }
    }

    @Test
    fun changingBetweenOnPitchStatusesKeepsThePosition() {
        val standing = onFieldPlayer("p1", x = 7, y = 13)

        val prone = standing.copy(status = PlayerStatus.PRONE)
        val stunned = prone.copy(status = PlayerStatus.STUNNED)

        assertEquals(BoardPosition(7, 13), prone.position)
        assertEquals(BoardPosition(7, 13), stunned.position)
        assertTrue(stunned.isOnPitch)
    }

    @Test
    fun movingPlayerKeepsTheirId() {
        val player = onFieldPlayer("p1", x = 0, y = 0)

        val moved = player.copy(position = BoardPosition(5, 5))
        val benched = player.copy(status = PlayerStatus.RESERVE, position = null)

        assertEquals(PlayerId("p1"), moved.id)
        assertEquals(PlayerId("p1"), benched.id)
    }

    @Test
    fun invalidCopyIsRejected() {
        val player = onFieldPlayer("p1", x = 0, y = 0)

        // copy() runs the same checks as the constructor.
        assertFailsWith<IllegalArgumentException> {
            player.copy(status = PlayerStatus.RESERVE)
        }
        assertFailsWith<IllegalArgumentException> {
            player.copy(status = PlayerStatus.STUNNED, position = null)
        }
    }
}
