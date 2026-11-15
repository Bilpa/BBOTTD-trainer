package org.example.bbottd_trainer

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BBOTTD-trainer",
        // Large enough for both dugouts and a readable pitch; still fits a 1920 × 1080 screen.
        state = rememberWindowState(
            size = DpSize(1280.dp, 900.dp),
            position = WindowPosition(Alignment.Center),
        ),
    ) {
        App()
    }
}
