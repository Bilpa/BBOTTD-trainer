package org.example.bbottd_trainer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardTest {

    @Test
    fun pitchIs15ColumnsBy26RowsWith390Cells() {
        assertEquals(15, Board.WIDTH)
        assertEquals(26, Board.HEIGHT)
        assertEquals(390, Board.CELL_COUNT)
        assertEquals(390, Board.columns.count() * Board.rows.count())
    }

    @Test
    fun containsAllFourCorners() {
        assertTrue(Board.contains(BoardPosition(0, 0)))
        assertTrue(Board.contains(BoardPosition(14, 0)))
        assertTrue(Board.contains(BoardPosition(0, 25)))
        assertTrue(Board.contains(BoardPosition(14, 25)))
    }

    @Test
    fun rejectsPositionsJustOutsideEachEdge() {
        assertFalse(Board.contains(BoardPosition(-1, 0)), "west of the pitch")
        assertFalse(Board.contains(BoardPosition(15, 0)), "east of the pitch")
        assertFalse(Board.contains(BoardPosition(0, -1)), "north of the pitch")
        assertFalse(Board.contains(BoardPosition(0, 26)), "south of the pitch")
    }

    @Test
    fun outOfBoundsPositionsCanBeRepresented() {
        // Needed so that an out-of-bounds move target can be reported rather than crashing.
        val position = BoardPosition(99, -5)

        assertEquals(99, position.x)
        assertEquals(-5, position.y)
    }
}
