package org.example.bbottd_trainer.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.domain.Team
import org.example.bbottd_trainer.presentation.BoardAction
import org.example.bbottd_trainer.presentation.BoardUiState
import org.example.bbottd_trainer.presentation.DragItem
import org.example.bbottd_trainer.presentation.DropTarget
import org.example.bbottd_trainer.presentation.Selection
import org.example.bbottd_trainer.presentation.asSelection
import org.example.bbottd_trainer.presentation.dugoutSections
import org.example.bbottd_trainer.presentation.isCellTarget
import org.example.bbottd_trainer.presentation.isDugoutTarget
import org.example.bbottd_trainer.presentation.isOffPitchBallTarget
import org.example.bbottd_trainer.presentation.isValidDrop
import org.example.bbottd_trainer.presentation.pitchTokens
import org.example.bbottd_trainer.presentation.tokenOf
import org.example.bbottd_trainer.ui.ball.BallTokenView
import org.example.bbottd_trainer.ui.ball.OffPitchBallArea
import org.example.bbottd_trainer.ui.drag.BoardDragState
import org.example.bbottd_trainer.ui.drag.rememberBoardDragState
import org.example.bbottd_trainer.ui.dugout.Dugout
import org.example.bbottd_trainer.ui.pitch.Pitch
import org.example.bbottd_trainer.ui.pitch.PitchDragFeedback
import org.example.bbottd_trainer.ui.player.PlayerTokenView
import kotlin.math.roundToInt

private val DugoutWidth = 168.dp
private val SnackbarBottomOffset = 72.dp
private val DraggedTokenSize = 32.dp

/**
 * The board editor screen: the home dugout left of the pitch and the away dugout right of it.
 *
 * Renders [state] and reports clicks, taps and keys through [onAction]; it never changes state
 * itself. Messages from the state holder are shown in a snackbar. Escape, a right-click on an
 * empty pitch cell and a click outside the pitch clear the selection.
 */
@Composable
fun BoardScreen(
    state: BoardUiState,
    onAction: (BoardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val setup = state.setup
    val selectedPlayerId = (state.selection as? Selection.Player)?.playerId
    val ballSelected = state.selection == Selection.Ball

    // Drag-and-drop: while dragging, targets are highlighted as if the dragged item were
    // selected, and the target under the pointer shows whether the drop would be accepted.
    val dragState = rememberBoardDragState()
    val dragItem = dragState.item
    val targetState = if (dragItem != null) state.copy(selection = dragItem.asSelection()) else state
    val hoveredTarget = dragState.hoveredTarget
    val hoverValid = dragItem != null && hoveredTarget != null && state.isValidDrop(dragItem, hoveredTarget)
    val onDrop: (DragItem, DropTarget) -> Unit = { item, target -> onAction(BoardAction.Drop(item, target)) }
    var screenOriginInWindow by remember { mutableStateOf(Offset.Zero) }

    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message.text)
            onAction(BoardAction.MessageShown(message.id))
        }
    }

    // Focus the screen so Escape works before anything has been clicked.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { screenOriginInWindow = it.positionInWindow() }
            // Taps that reach the background (outside the pitch and every other click target)
            // deselect. A plain tap detector rather than `clickable`: `clickable` would merge the
            // whole screen's semantics into one node, which hides everything from screen readers
            // and crashes on the web when the snackbar's pane title is merged in.
            .pointerInput(onAction) {
                detectTapGestures(onTap = { onAction(BoardAction.ClearSelection) })
            }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                    onAction(BoardAction.ClearSelection)
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditorToolbar(state, onAction)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.dugoutsVisible) {
                    TeamDugout(state, targetState, setup.home, onAction, dragState, hoveredTarget, hoverValid, onDrop)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pitch(
                        tokens = setup.pitchTokens(),
                        ballPosition = setup.ball.position,
                        selectedPlayerId = selectedPlayerId,
                        ballSelected = ballSelected,
                        isTarget = targetState::isCellTarget,
                        onCellClick = { onAction(BoardAction.CellClicked(it)) },
                        onBallClick = { onAction(BoardAction.BallClicked) },
                        statusMenuPlayerId = state.statusMenuPlayerId,
                        onPlayerContextMenu = { onAction(BoardAction.OpenStatusMenu(it)) },
                        onPlayerDoubleClick = { onAction(BoardAction.PlayerDoubleClicked(it)) },
                        onEmptyCellRightClick = { onAction(BoardAction.ClearSelection) },
                        onStatusSelected = { playerId, status ->
                            onAction(BoardAction.SetPlayerStatus(playerId, status))
                        },
                        onStatusMenuDismiss = { onAction(BoardAction.DismissStatusMenu) },
                        dragState = dragState,
                        dragFeedback = PitchDragFeedback(
                            hoveredCell = (hoveredTarget as? DropTarget.Cell)?.position,
                            hoverValid = hoverValid,
                            draggedItem = dragItem,
                        ),
                        onDrop = onDrop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    OffPitchBallArea(
                        ballIsOffPitch = !setup.ball.isOnPitch,
                        ballSelected = ballSelected,
                        isTarget = targetState.isOffPitchBallTarget,
                        onClick = { onAction(BoardAction.OffPitchBallAreaClicked) },
                        dragState = dragState,
                        ballDragged = dragItem == DragItem.Ball,
                        hovered = hoveredTarget == DropTarget.OffPitchBallArea,
                        hoverValid = hoverValid,
                        onDrop = onDrop,
                    )
                }
                if (state.dugoutsVisible) {
                    TeamDugout(state, targetState, setup.away, onAction, dragState, hoveredTarget, hoverValid, onDrop)
                }
            }
        }

        // The dragged item follows the pointer, centred on it.
        if (dragItem != null) {
            val draggedToken = (dragItem as? DragItem.Player)?.let { setup.tokenOf(it.playerId) }
            val sizePx = with(LocalDensity.current) { DraggedTokenSize.toPx() }
            val pointer = dragState.pointer - screenOriginInWindow
            Box(
                modifier = Modifier.offset {
                    IntOffset(
                        (pointer.x - sizePx / 2).roundToInt(),
                        (pointer.y - sizePx / 2).roundToInt(),
                    )
                },
            ) {
                if (draggedToken != null) {
                    PlayerTokenView(token = draggedToken, size = DraggedTokenSize, selected = true)
                } else {
                    BallTokenView(size = DraggedTokenSize * 0.7f, selected = true)
                }
            }
        }

        state.unsavedChangesPrompt?.let { pending ->
            UnsavedChangesDialog(
                setupName = setup.name,
                pending = pending,
                onSave = { onAction(BoardAction.UnsavedChangesSave) },
                onDiscard = { onAction(BoardAction.UnsavedChangesDiscard) },
                onCancel = { onAction(BoardAction.UnsavedChangesCancel) },
            )
        }
        state.saveAsDialog?.let { dialog ->
            SaveAsDialog(
                initialName = dialog.initialName,
                onSave = { onAction(BoardAction.SaveAsConfirmed(it)) },
                onCancel = { onAction(BoardAction.SaveAsCancelled) },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            // Raised so it does not cover the off-pitch ball area at the bottom.
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = SnackbarBottomOffset),
        )
    }
}

@Composable
private fun TeamDugout(
    state: BoardUiState,
    targetState: BoardUiState,
    team: Team,
    onAction: (BoardAction) -> Unit,
    dragState: BoardDragState,
    hoveredTarget: DropTarget?,
    hoverValid: Boolean,
    onDrop: (DragItem, DropTarget) -> Unit,
) {
    val hoveredSection = (hoveredTarget as? DropTarget.DugoutSection)?.takeIf { it.team == team.id }
    Dugout(
        teamId = team.id,
        teamName = team.name,
        teamColor = team.color,
        sections = state.setup.dugoutSections(team.id),
        selectedPlayerId = (state.selection as? Selection.Player)?.playerId,
        isTarget = targetState.isDugoutTarget(team.id),
        dragState = dragState,
        draggedPlayerId = (dragState.item as? DragItem.Player)?.playerId,
        hoveredStatus = hoveredSection?.status,
        hoverValid = hoverValid,
        onDrop = onDrop,
        onSectionClick = { status -> onAction(BoardAction.DugoutSectionClicked(team.id, status)) },
        onPlayerClick = { playerId -> onAction(BoardAction.DugoutPlayerClicked(playerId)) },
        statusMenuPlayerId = state.statusMenuPlayerId,
        onPlayerContextMenu = { onAction(BoardAction.OpenStatusMenu(it)) },
        onStatusSelected = { playerId, status ->
            onAction(BoardAction.SetPlayerStatus(playerId, status))
        },
        onStatusMenuDismiss = { onAction(BoardAction.DismissStatusMenu) },
        modifier = Modifier
            .width(DugoutWidth)
            .fillMaxHeight(),
    )
}
