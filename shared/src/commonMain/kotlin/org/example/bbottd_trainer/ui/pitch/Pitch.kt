package org.example.bbottd_trainer.ui.pitch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.domain.Board
import org.example.bbottd_trainer.domain.BoardPosition

private val CentreFieldColor = Color(0xFF3F8A3F)
private val WideZoneColor = Color(0xFF357535)
private val EndZoneColor = Color(0xFF24502A)
private val GridLineColor = Color(0x40FFFFFF)
private val LineOfScrimmageColor = Color.White

/**
 * The 15 × 26 pitch, drawn north (`y = 0`) at the top, with square cells scaled to fit the
 * available space.
 */
@Composable
fun Pitch(
    modifier: Modifier = Modifier,
    markings: PitchMarkings = PitchMarkings.STANDARD,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.TopCenter) {
        val cellSize = minOf(maxWidth / Board.WIDTH, maxHeight / Board.HEIGHT)

        Column(
            modifier = Modifier.semantics {
                contentDescription = "Pitch, ${Board.WIDTH} columns by ${Board.HEIGHT} rows"
            },
        ) {
            for (y in Board.rows) {
                Row {
                    for (x in Board.columns) {
                        PitchCell(
                            area = markings.areaOf(BoardPosition(x, y)),
                            lineOfScrimmageBelow = markings.hasLineOfScrimmageBelow(y),
                            size = cellSize,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PitchCell(
    area: PitchArea,
    lineOfScrimmageBelow: Boolean,
    size: Dp,
) {
    Spacer(
        modifier = Modifier
            .size(size)
            .background(area.color)
            .drawBehind {
                drawRect(color = GridLineColor, style = Stroke(width = 1.dp.toPx()))
                if (lineOfScrimmageBelow) {
                    // Drawn just inside the cell so the next row's background does not cover it.
                    val strokeWidth = 3.dp.toPx()
                    val y = this.size.height - strokeWidth / 2
                    drawLine(
                        color = LineOfScrimmageColor,
                        start = Offset(0f, y),
                        end = Offset(this.size.width, y),
                        strokeWidth = strokeWidth,
                    )
                }
            },
    )
}

private val PitchArea.color: Color
    get() = when (this) {
        PitchArea.END_ZONE -> EndZoneColor
        PitchArea.WIDE_ZONE -> WideZoneColor
        PitchArea.CENTRE_FIELD -> CentreFieldColor
    }
