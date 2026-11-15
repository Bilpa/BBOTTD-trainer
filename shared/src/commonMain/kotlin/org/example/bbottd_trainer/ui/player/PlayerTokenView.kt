package org.example.bbottd_trainer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.bbottd_trainer.domain.TeamColor
import org.example.bbottd_trainer.presentation.PlayerToken

/** The colour a team colour is displayed as. */
val TeamColor.displayColor: Color
    get() = when (this) {
        TeamColor.BLUE -> Color(0xFF1E5AA8)
        TeamColor.RED -> Color(0xFFC62828)
    }

/**
 * A round player token filled with the team colour and showing the player's number, so the team
 * is visible on every token.
 */
@Composable
fun PlayerTokenView(
    token: PlayerToken,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val numberText = token.number?.toString() ?: ""
    Box(
        modifier = modifier
            .size(size)
            .background(token.teamColor.displayColor, CircleShape)
            .border(1.dp, Color.White, CircleShape)
            .semantics {
                contentDescription = buildString {
                    append(token.name)
                    token.number?.let { append(", number $it") }
                    append(", ${token.teamName} team")
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = numberText,
            color = Color.White,
            fontSize = (size.value * 0.45f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
