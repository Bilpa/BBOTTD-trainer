package org.example.bbottd_trainer

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.bbottd_trainer.data.desktopTextStore
import org.example.bbottd_trainer.presentation.BoardAction

fun main() = application {
    val store = remember { desktopTextStore() }
    val stateHolder = rememberBoardStateHolder(store)
    val state by stateHolder.state.collectAsState()

    // Closing the window is a request: with unsaved changes the app asks first, and closes once
    // the changes are saved or discarded.
    LaunchedEffect(state.closeApproved) {
        if (state.closeApproved) exitApplication()
    }

    Window(
        onCloseRequest = { stateHolder.onAction(BoardAction.CloseRequested) },
        title = "BBOTTD-trainer",
        // Large enough for both dugouts and a readable pitch; still fits a 1920 × 1080 screen.
        state = rememberWindowState(
            size = DpSize(1280.dp, 900.dp),
            position = WindowPosition(Alignment.Center),
        ),
    ) {
        App(stateHolder)
    }
}
