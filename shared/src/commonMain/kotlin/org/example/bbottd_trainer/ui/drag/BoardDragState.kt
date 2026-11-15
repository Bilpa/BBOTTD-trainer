package org.example.bbottd_trainer.ui.drag

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import org.example.bbottd_trainer.domain.Board
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.presentation.DragItem
import org.example.bbottd_trainer.presentation.DropTarget

/**
 * Drag-and-drop state for the board screen: what is being dragged, where the pointer is, and where
 * the drop targets are on screen.
 *
 * All positions are window pixels. This is UI geometry only; the editor state holder never sees
 * it, it only receives the resulting drop as an action.
 */
class BoardDragState {
    /** The item being dragged, or `null` when no drag is in progress. */
    var item: DragItem? by mutableStateOf(null)
        private set

    /** The pointer position during a drag, in window coordinates. */
    var pointer: Offset by mutableStateOf(Offset.Zero)
        private set

    // Drop-target bounds, kept up to date by the targets themselves while they are on screen.
    private var pitchBounds: Rect? = null
    private var offPitchBallAreaBounds: Rect? = null
    private val dugoutSectionBounds = mutableMapOf<DropTarget.DugoutSection, Rect>()

    fun start(item: DragItem, at: Offset) {
        this.item = item
        pointer = at
    }

    fun moveTo(position: Offset) {
        pointer = position
    }

    fun end() {
        item = null
    }

    /** The drop target under the pointer during a drag, or `null`. */
    val hoveredTarget: DropTarget?
        get() = if (item != null) targetAt(pointer) else null

    /** The drop target at [point] (window coordinates), or `null` if there is none. */
    fun targetAt(point: Offset): DropTarget? {
        pitchBounds?.let { bounds ->
            if (bounds.contains(point)) {
                val column = ((point.x - bounds.left) / (bounds.width / Board.WIDTH)).toInt()
                val row = ((point.y - bounds.top) / (bounds.height / Board.HEIGHT)).toInt()
                return DropTarget.Cell(
                    BoardPosition(
                        column.coerceIn(Board.columns),
                        row.coerceIn(Board.rows),
                    ),
                )
            }
        }
        dugoutSectionBounds.entries.firstOrNull { it.value.contains(point) }?.let { return it.key }
        if (offPitchBallAreaBounds?.contains(point) == true) return DropTarget.OffPitchBallArea
        return null
    }

    fun setPitchBounds(bounds: Rect) {
        pitchBounds = bounds
    }

    fun setOffPitchBallAreaBounds(bounds: Rect) {
        offPitchBallAreaBounds = bounds
    }

    fun setDugoutSectionBounds(section: DropTarget.DugoutSection, bounds: Rect?) {
        if (bounds == null) dugoutSectionBounds.remove(section) else dugoutSectionBounds[section] = bounds
    }
}

@Composable
fun rememberBoardDragState(): BoardDragState = remember { BoardDragState() }

/**
 * Makes this element draggable as [item]. Dropping it on a target calls [onDrop]; dropping it
 * anywhere else cancels the drag. A drag only starts after the pointer moves past the touch slop,
 * so clicks and taps still work.
 */
fun Modifier.dragSource(
    dragState: BoardDragState,
    item: DragItem,
    onDrop: (DragItem, DropTarget) -> Unit,
): Modifier = composed {
    val currentOnDrop by rememberUpdatedState(onDrop)
    // Read only inside the gesture callbacks, so updating it does not recompose.
    var originInWindow by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { originInWindow = it.positionInWindow() }
        .pointerInput(dragState, item) {
            // The pointer is tracked by its absolute position rather than by summing drag amounts:
            // the first drag amount repeats movement already included in the start position, so
            // summing would offset the dragged token from the pointer.
            detectDragGestures(
                onDragStart = { offset -> dragState.start(item, originInWindow + offset) },
                onDrag = { change, _ ->
                    change.consume()
                    dragState.moveTo(originInWindow + change.position)
                },
                onDragEnd = {
                    val target = dragState.targetAt(dragState.pointer)
                    dragState.end()
                    if (target != null) currentOnDrop(item, target)
                },
                onDragCancel = { dragState.end() },
            )
        }
}

/** Registers this element as the drop area of a dugout section while it is on screen. */
fun Modifier.dugoutSectionDropArea(
    dragState: BoardDragState,
    section: DropTarget.DugoutSection,
): Modifier = composed {
    // Hidden dugouts must not leave stale drop areas behind.
    DisposableEffect(dragState, section) {
        onDispose { dragState.setDugoutSectionBounds(section, null) }
    }
    onGloballyPositioned { dragState.setDugoutSectionBounds(section, it.boundsInWindow()) }
}

/** Registers this element as the pitch grid for drop hit-testing. */
fun Modifier.pitchDropArea(dragState: BoardDragState): Modifier =
    onGloballyPositioned { dragState.setPitchBounds(it.boundsInWindow()) }

/** Registers this element as the off-pitch ball area for drop hit-testing. */
fun Modifier.offPitchBallDropArea(dragState: BoardDragState): Modifier =
    onGloballyPositioned { dragState.setOffPitchBallAreaBounds(it.boundsInWindow()) }
