package org.example.bbottd_trainer.ui.ball

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val BallSlotSize = 30.dp

/**
 * The place below the pitch where the ball sits while it is off the pitch (its position is
 * `null`). The area is always shown so the layout stays stable; its slot is empty while the ball
 * is on the pitch.
 */
@Composable
fun OffPitchBallArea(
    ballIsOffPitch: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (ballIsOffPitch) "Ball (off pitch)" else "Ball (on pitch)",
                style = MaterialTheme.typography.labelMedium,
            )
            Box(
                modifier = Modifier
                    .size(BallSlotSize)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (ballIsOffPitch) {
                    BallTokenView(size = BallSlotSize - 6.dp)
                }
            }
        }
    }
}
