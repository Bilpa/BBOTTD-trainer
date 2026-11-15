package org.example.bbottd_trainer.ui.pitch

import org.example.bbottd_trainer.domain.Board
import org.example.bbottd_trainer.domain.BoardPosition

/** The marked area a pitch cell belongs to. Used for drawing only. */
enum class PitchArea {
    END_ZONE,
    WIDE_ZONE,
    CENTRE_FIELD,
}

/**
 * Pitch markings, as presentation configuration.
 *
 * Editing operations must not depend on these: a player may be placed in any empty cell whatever
 * its area. The defaults are the standard layout from `docs/DOMAIN_MODEL.md`.
 */
data class PitchMarkings(
    val endZoneRows: Set<Int> = setOf(0, Board.HEIGHT - 1),
    val wideZoneColumns: Set<Int> = (0..3).toSet() + (11..14).toSet(),
    /** The line of scrimmage is drawn between this row and the next (rows 12 and 13 by default). */
    val lineOfScrimmageAfterRow: Int = 12,
) {
    /** End zones take precedence over wide zones in the corner cells. */
    fun areaOf(position: BoardPosition): PitchArea =
        when {
            position.y in endZoneRows -> PitchArea.END_ZONE
            position.x in wideZoneColumns -> PitchArea.WIDE_ZONE
            else -> PitchArea.CENTRE_FIELD
        }

    fun hasLineOfScrimmageBelow(row: Int): Boolean = row == lineOfScrimmageAfterRow

    companion object {
        val STANDARD = PitchMarkings()
    }
}
