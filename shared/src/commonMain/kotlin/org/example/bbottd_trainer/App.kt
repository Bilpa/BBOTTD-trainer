package org.example.bbottd_trainer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.example.bbottd_trainer.presentation.BoardStateHolder
import org.example.bbottd_trainer.ui.BoardScreen

/** Shared root composable used by the Android, desktop and web entry points. */
@Composable
@Preview
fun App() {
    val stateHolder = remember { BoardStateHolder() }
    val state by stateHolder.state.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BoardScreen(
                state = state,
                modifier = Modifier.safeContentPadding(),
            )
        }
    }
}
