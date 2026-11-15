package org.example.bbottd_trainer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.presentation.label
import org.example.bbottd_trainer.presentation.statusMenuOptions

/**
 * The context menu for setting a player's status. It lists only the statuses valid where the
 * player is, with the current one marked by a dot and bold text. Place it inside the token's box so
 * it opens there.
 */
@Composable
fun StatusMenu(
    current: PlayerStatus,
    onSelect: (PlayerStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        for (status in statusMenuOptions(current)) {
            val isCurrent = status == current
            DropdownMenuItem(
                text = {
                    Text(
                        text = status.label,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                // A drawn dot rather than a check-mark character: the web build's default font
                // has no glyph for one.
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .then(
                                if (isCurrent) {
                                    Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                },
                onClick = { onSelect(status) },
            )
        }
    }
}
