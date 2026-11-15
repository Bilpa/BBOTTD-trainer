package org.example.bbottd_trainer.ui.dugout

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.TeamColor
import org.example.bbottd_trainer.domain.TeamId
import org.example.bbottd_trainer.presentation.DragItem
import org.example.bbottd_trainer.presentation.DropTarget
import org.example.bbottd_trainer.presentation.DugoutSection
import org.example.bbottd_trainer.presentation.label
import org.example.bbottd_trainer.ui.DRAGGED_ORIGINAL_ALPHA
import org.example.bbottd_trainer.ui.InvalidDropColor
import org.example.bbottd_trainer.ui.SelectionColor
import org.example.bbottd_trainer.ui.drag.BoardDragState
import org.example.bbottd_trainer.ui.drag.dragSource
import org.example.bbottd_trainer.ui.drag.dugoutSectionDropArea
import org.example.bbottd_trainer.ui.player.PlayerTokenView
import org.example.bbottd_trainer.ui.player.StatusMenu
import org.example.bbottd_trainer.ui.player.displayColor
import org.example.bbottd_trainer.ui.player.playerClicks

private val DugoutTokenSize = 30.dp
private val SectionShape = RoundedCornerShape(6.dp)

/**
 * One team's dugout: a section per dugout status, each listing that team's players.
 *
 * Each section is a click and drop target (moving the selected or dragged player into it),
 * highlighted while [isTarget]; each token can be clicked to select that player or dragged.
 * While dragging, the section under the pointer ([hoveredStatus]) gets a cyan border if the drop
 * would be accepted ([hoverValid]) and a red one if not.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Dugout(
    teamId: TeamId,
    teamName: String,
    teamColor: TeamColor,
    sections: List<DugoutSection>,
    modifier: Modifier = Modifier,
    selectedPlayerId: PlayerId? = null,
    isTarget: Boolean = false,
    onSectionClick: (PlayerStatus) -> Unit = {},
    onPlayerClick: (PlayerId) -> Unit = {},
    statusMenuPlayerId: PlayerId? = null,
    onPlayerContextMenu: (PlayerId) -> Unit = {},
    onStatusSelected: (PlayerId, PlayerStatus) -> Unit = { _, _ -> },
    onStatusMenuDismiss: () -> Unit = {},
    dragState: BoardDragState? = null,
    draggedPlayerId: PlayerId? = null,
    hoveredStatus: PlayerStatus? = null,
    hoverValid: Boolean = false,
    onDrop: (DragItem, DropTarget) -> Unit = { _, _ -> },
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$teamName dugout",
                style = MaterialTheme.typography.titleSmall,
                color = teamColor.displayColor,
            )
            for (section in sections) {
                val hovered = section.status == hoveredStatus
                val (borderWidth, borderColor) = when {
                    hovered && hoverValid -> 3.dp to SelectionColor
                    hovered -> 3.dp to InvalidDropColor
                    isTarget -> 2.dp to SelectionColor
                    else -> 1.dp to MaterialTheme.colorScheme.outlineVariant
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (dragState != null) {
                                Modifier.dugoutSectionDropArea(
                                    dragState,
                                    DropTarget.DugoutSection(teamId, section.status),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clip(SectionShape)
                        .border(width = borderWidth, color = borderColor, shape = SectionShape)
                        .clickable { onSectionClick(section.status) }
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${section.status.label} (${section.players.size})",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    FlowRow(
                        // At least one token row high, so an empty section is as tall as one
                        // holding a player and stays an easy target.
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = DugoutTokenSize),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (token in section.players) {
                            key(token.playerId) {
                                // The box anchors the status menu to the token.
                                Box {
                                    val dragged = token.playerId == draggedPlayerId
                                    PlayerTokenView(
                                        token = token,
                                        size = DugoutTokenSize,
                                        selected = token.playerId == selectedPlayerId,
                                        modifier = Modifier
                                            .then(if (dragged) Modifier.alpha(DRAGGED_ORIGINAL_ALPHA) else Modifier)
                                            .then(
                                                if (dragState != null) {
                                                    Modifier.dragSource(
                                                        dragState,
                                                        DragItem.Player(token.playerId),
                                                        onDrop,
                                                    )
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .playerClicks(
                                                onClick = { onPlayerClick(token.playerId) },
                                                onContextMenu = { onPlayerContextMenu(token.playerId) },
                                            ),
                                    )
                                    if (token.playerId == statusMenuPlayerId) {
                                        StatusMenu(
                                            current = token.status,
                                            onSelect = { onStatusSelected(token.playerId, it) },
                                            onDismiss = onStatusMenuDismiss,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
