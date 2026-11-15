package org.example.bbottd_trainer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BBOTTD-trainer",
    ) {
        App()
    }
}