package org.example.bbottd_trainer.ui.pitch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.presentation.PlayerToken
import org.example.bbottd_trainer.ui.TargetCellOverlay
import org.example.bbottd_trainer.ui.ball.BallTokenView
import org.example.bbottd_trainer.ui.player.PlayerTokenView
import org.example.bbottd_trainer.ui.player.StatusMenu
import org.example.bbottd_trainer.ui.player.onRightClick
import org.example.bbottd_trainer.ui.player.playerClicks

private val CentreFieldColor = Color(0xFF3F8A3F)
private val WideZoneColor = Color(0xFF357535)
private val EndZoneColor = Color(0xFF24502A)
private val GridLineColor = Color(0x40FFFFFF)
private val LineOfScrimmageColor = Color.White

/**
 * The 15 × 26 pitch, drawn north (`y = 0`) at the top, with square cells scaled to fit the
 * available space. Every cell can be clicked or tapped; [isTarget] cells are highlighted as places
 * the current selection can be moved to.
 */
@Composable
fun Pitch(
    modifier: Modifier = Modifier,
    tokens: Map<BoardPosition, PlayerToken> = emptyMap(),
    ballPosition: BoardPosition? = null,
    selectedPlayerId: PlayerId? = null,
    ballSelected: Boolean = false,
    isTarget: (BoardPosition) -> Boolean = { false },
    onCellClick: (BoardPosition) -> Unit = {},
    onBallClick: () -> Unit = {},
    statusMenuPlayerId: PlayerId? = null,
    onPlayerContextMenu: (PlayerId) -> Unit = {},
    onPlayerDoubleClick: (PlayerId) -> Unit = {},
    onEmptyCellRightClick: () -> Unit = {},
    onStatusSelected: (PlayerId, PlayerStatus) -> Unit = { _, _ -> },
    onStatusMenuDismiss: () -> Unit = {},
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
                        val position = BoardPosition(x, y)
                        val token = tokens[position]
                        PitchCell(
                            area = markings.areaOf(position),
                            lineOfScrimmageBelow = markings.hasLineOfScrimmageBelow(y),
                            size = cellSize,
                            token = token,
                            tokenSelected = token != null && token.playerId == selectedPlayerId,
                            hasBall = position == ballPosition,
                            ballSelected = ballSelected,
                            isTarget = isTarget(position),
                            onClick = { onCellClick(position) },
                            onBallClick = onBallClick,
                            statusMenuOpen = token != null && token.playerId == statusMenuPlayerId,
                            onContextMenu = { token?.let { onPlayerContextMenu(it.playerId) } },
                            onDoubleClick = { token?.let { onPlayerDoubleClick(it.playerId) } },
                            onEmptyCellRightClick = onEmptyCellRightClick,
                            onStatusSelected = { status ->
                                token?.let { onStatusSelected(it.playerId, status) }
                            },
                            onStatusMenuDismiss = onStatusMenuDismiss,
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
    token: PlayerToken?,
    tokenSelected: Boolean,
    hasBall: Boolean,
    ballSelected: Boolean,
    isTarget: Boolean,
    onClick: () -> Unit,
    onBallClick: () -> Unit,
    statusMenuOpen: Boolean,
    onContextMenu: () -> Unit,
    onDoubleClick: () -> Unit,
    onEmptyCellRightClick: () -> Unit,
    onStatusSelected: (PlayerStatus) -> Unit,
    onStatusMenuDismiss: () -> Unit,
) {
    // A cell with a player also opens the player's status menu (right-click or long-press) and
    // toggles standing/prone on a double-click.
    val clicks = if (token != null) {
        Modifier.playerClicks(
            onClick = onClick,
            onContextMenu = onContextMenu,
            onDoubleClick = onDoubleClick,
        )
    } else {
        // A right-click on an empty cell deselects.
        Modifier
            .onRightClick(onEmptyCellRightClick)
            .clickable(onClick = onClick)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(area.color)
            .drawBehind {
                if (isTarget) drawRect(color = TargetCellOverlay)
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
            }
            .then(clicks),
    ) {
        if (token != null) {
            PlayerTokenView(token = token, size = size * 0.85f, selected = tokenSelected)
            if (statusMenuOpen) {
                StatusMenu(
                    current = token.status,
                    onSelect = onStatusSelected,
                    onDismiss = onStatusMenuDismiss,
                )
            }
        }
        if (hasBall) {
            // The ball has its own click target, so it can be selected even on a player's cell.
            val ballModifier = Modifier.clickable(onClick = onBallClick)
            if (token == null) {
                BallTokenView(size = size * 0.55f, selected = ballSelected, modifier = ballModifier)
            } else {
                // Shares the cell with a player: smaller, in the corner, so both stay visible.
                BallTokenView(
                    size = size * 0.4f,
                    selected = ballSelected,
                    modifier = Modifier.align(Alignment.BottomEnd).then(ballModifier),
                )
            }
        }
    }
}

private val PitchArea.color: Color
    get() = when (this) {
        PitchArea.END_ZONE -> EndZoneColor
        PitchArea.WIDE_ZONE -> WideZoneColor
        PitchArea.CENTRE_FIELD -> CentreFieldColor
    }
