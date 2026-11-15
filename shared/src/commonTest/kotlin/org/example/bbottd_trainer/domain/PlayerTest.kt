package org.example.bbottd_trainer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerTest {

    private val dugoutStatuses = PlayerStatus.entries - PlayerStatus.ON_FIELD

    @Test
    fun onFieldPlayerHasPosition() {
        val player = onFieldPlayer("p1", x = 7, y = 13)

        assertTrue(player.isOnPitch)
        assertEquals(BoardPosition(7, 13), player.position)
    }

    @Test
    fun onFieldPlayerWithoutPositionIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            Player(PlayerId("p1"), "p1", null, PlayerStatus.ON_FIELD, position = null)
        }
    }

    @Test
    fun onFieldPlayerOutsideThePitchIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            Player(PlayerId("p1"), "p1", null, PlayerStatus.ON_FIELD, BoardPosition(15, 0))
        }
        assertFailsWith<IllegalArgumentException> {
            Player(PlayerId("p1"), "p1", null, PlayerStatus.ON_FIELD, BoardPosition(0, 26))
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
    }
}
