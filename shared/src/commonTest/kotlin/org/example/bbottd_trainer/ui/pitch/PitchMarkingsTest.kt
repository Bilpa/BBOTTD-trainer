package org.example.bbottd_trainer.ui.pitch

import org.example.bbottd_trainer.domain.Board
import org.example.bbottd_trainer.domain.BoardPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PitchMarkingsTest {

    private val markings = PitchMarkings.STANDARD

    private val allPositions = Board.rows.flatMap { y -> Board.columns.map { x -> BoardPosition(x, y) } }

    @Test
    fun pitchRowsAndColumnsGive390DistinctCells() {
        assertEquals(390, allPositions.size)
        assertEquals(390, allPositions.toSet().size)
    }

    @Test
    fun endZonesAreTheNorthAndSouthRows() {
        for (x in Board.columns) {
            assertEquals(PitchArea.END_ZONE, markings.areaOf(BoardPosition(x, 0)), "north, x=$x")
            assertEquals(PitchArea.END_ZONE, markings.areaOf(BoardPosition(x, 25)), "south, x=$x")
        }
    }

    @Test
    fun wideZonesAreTheOuterFourColumnsOnEachSide() {
        for (y in 1..24) {
            for (x in (0..3) + (11..14)) {
                assertEquals(PitchArea.WIDE_ZONE, markings.areaOf(BoardPosition(x, y)), "x=$x, y=$y")
            }
        }
    }

    @Test
    fun centreFieldIsBetweenTheWideZones() {
        for (y in 1..24) {
            for (x in 4..10) {
                assertEquals(PitchArea.CENTRE_FIELD, markings.areaOf(BoardPosition(x, y)), "x=$x, y=$y")
            }
        }
    }

    @Test
    fun areaSizesAddUpToTheWholePitch() {
        val counts = allPositions.groupingBy { markings.areaOf(it) }.eachCount()

        assertEquals(2 * 15, counts[PitchArea.END_ZONE])
        assertEquals(8 * 24, counts[PitchArea.WIDE_ZONE])
        assertEquals(7 * 24, counts[PitchArea.CENTRE_FIELD])
    }

    @Test
    fun lineOfScrimmageLiesBetweenRows12And13() {
        assertTrue(markings.hasLineOfScrimmageBelow(12))
        assertEquals(listOf(12), Board.rows.filter { markings.hasLineOfScrimmageBelow(it) })
        assertFalse(markings.hasLineOfScrimmageBelow(13))
    }
}
