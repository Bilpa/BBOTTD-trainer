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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.domain.TeamColor
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.presentation.PlayerToken
import org.example.bbottd_trainer.presentation.label
import org.example.bbottd_trainer.ui.SelectionColor

/** The colour a team colour is displayed as. */
val TeamColor.displayColor: Color
    get() = when (this) {
        TeamColor.BLUE -> Color(0xFF1E5AA8)
        TeamColor.RED -> Color(0xFFC62828)
    }

/** Bright border marking a stunned player; yellow, so it differs from the cyan selection ring. */
private val StunnedBorderColor = Color(0xFFFFEB3B)

/** How much prone and stunned tokens are darkened towards black. */
private const val DOWNED_DARKENING = 0.5f

/**
 * A round player token filled with the team colour and showing the player's number, so the team
 * is visible on every token.
 *
 * Prone players are shaded darker; stunned players are shaded darker with a bright yellow border.
 * A selected token gets a thick cyan ring, which takes precedence over the stunned border.
 */
@Composable
fun PlayerTokenView(
    token: PlayerToken,
    size: Dp,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val numberText = token.number?.toString() ?: ""
    val isDowned = token.status == PlayerStatus.PRONE || token.status == PlayerStatus.STUNNED
    val fillColor = token.teamColor.displayColor.let {
        if (isDowned) lerp(it, Color.Black, DOWNED_DARKENING) else it
    }
    val border = when {
        selected -> Modifier.border(3.dp, SelectionColor, CircleShape)
        token.status == PlayerStatus.STUNNED -> Modifier.border(2.5.dp, StunnedBorderColor, CircleShape)
        else -> Modifier.border(1.dp, Color.White, CircleShape)
    }
    Box(
        modifier = modifier
            .size(size)
            .background(fillColor, CircleShape)
            .then(border)
            .semantics {
                contentDescription = buildString {
                    append(token.name)
                    token.number?.let { append(", number $it") }
                    append(", ${token.teamName} team")
                    if (isDowned) append(", ${token.status.label.lowercase()}")
                    if (selected) append(", selected")
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // The number is sized from the token (in dp, so it scales with the token, not with the
        // font-scale setting) and its line height equals its font size with the extra line
        // spacing trimmed. Otherwise the default ~24 sp line height pushes the number down and
        // clips it on small tokens.
        val fontSize = with(LocalDensity.current) { (size * NUMBER_SIZE_RATIO).toSp() }
        Text(
            text = numberText,
            style = TextStyle(
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                lineHeight = fontSize,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Number height as a share of the token diameter; leaves room for two digits. */
private const val NUMBER_SIZE_RATIO = 0.45f
