package org.example.bbottd_trainer.ui.ball

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.ui.SelectionColor

private val BallColor = Color(0xFFE0A000)
private val BallOutline = Color(0xFF5A3A00)

/**
 * The ball marker: a gold circle with a dark outline, distinct from the team-coloured players.
 * When selected it gets a thick cyan ring.
 */
@Composable
fun BallTokenView(
    size: Dp,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(BallColor, CircleShape)
            .then(
                if (selected) {
                    Modifier.border(3.dp, SelectionColor, CircleShape)
                } else {
                    Modifier.border(1.5.dp, BallOutline, CircleShape)
                },
            )
            .semantics { contentDescription = if (selected) "Ball, selected" else "Ball" },
    )
}
