package org.example.bbottd_trainer.ui.ball

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BallColor = Color(0xFFE0A000)
private val BallOutline = Color(0xFF5A3A00)

/** The ball marker: a gold circle with a dark outline, distinct from the team-coloured players. */
@Composable
fun BallTokenView(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(BallColor, CircleShape)
            .border(1.5.dp, BallOutline, CircleShape)
            .semantics { contentDescription = "Ball" },
    )
}
