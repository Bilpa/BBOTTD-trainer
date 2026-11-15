package org.example.bbottd_trainer.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Calls [onRightClick] when the secondary (right) mouse button is pressed.
 *
 * The press is caught in the initial pass and consumed, so a click handler further down the
 * modifier chain does not also treat it as a normal click.
 */
fun Modifier.onRightClick(onRightClick: () -> Unit): Modifier =
    pointerInput(onRightClick) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                    event.changes.forEach { it.consume() }
                    onRightClick()
                }
            }
        }
    }

/**
 * Click handling for a player token: a normal click or tap calls [onClick]; a right-click (mouse)
 * or a long-press (touch) calls [onContextMenu]; a double-click calls [onDoubleClick] if given.
 *
 * When [onDoubleClick] is set, a single click is reported only after the double-click interval
 * has passed without a second click, and a double-click does not also count as a single click.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.playerClicks(
    onClick: () -> Unit,
    onContextMenu: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
): Modifier =
    this
        .onRightClick(onContextMenu)
        .combinedClickable(
            onClick = onClick,
            onLongClick = onContextMenu,
            onDoubleClick = onDoubleClick,
        )
