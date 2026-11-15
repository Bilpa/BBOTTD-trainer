package org.example.bbottd_trainer.domain

/**
 * Dimensions of the pitch grid.
 *
 * The pitch is oriented north–south: `x` is the column (west to east, `0..14`) and `y` is the row
 * (north to south, `0..25`). `y = 0` is the north end zone and `y = 25` the south end zone.
 */
object Board {
    const val WIDTH = 15
    const val HEIGHT = 26
    const val CELL_COUNT = WIDTH * HEIGHT

    val columns: IntRange = 0 until WIDTH
    val rows: IntRange = 0 until HEIGHT

    /** The single bounds check for pitch positions; do not duplicate it elsewhere. */
    fun contains(position: BoardPosition): Boolean = position.x in columns && position.y in rows
}

/**
 * A cell on the pitch, in zero-based grid coordinates.
 *
 * Any coordinates can be represented, so that an out-of-bounds target can be reported as an editor
 * failure instead of crashing. Use [Board.contains] to check whether a position is on the pitch.
 */
data class BoardPosition(
    val x: Int,
    val y: Int,
)
