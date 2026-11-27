package org.example.bbottd_trainer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.example.bbottd_trainer.data.SetupRepository
import org.example.bbottd_trainer.presentation.BoardStateHolder
import org.example.bbottd_trainer.ui.BoardScreen

/**
 * Shared root composable used by the Android, desktop and web entry points.
 *
 * Each entry point creates the [stateHolder] (see [rememberBoardStateHolder]) with its platform's
 * storage, so it can also send app-level actions such as a close request.
 */
@Composable
fun App(stateHolder: BoardStateHolder) {
    val state by stateHolder.state.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BoardScreen(
                state = state,
                onAction = stateHolder::onAction,
                modifier = Modifier.safeContentPadding(),
            )
        }
    }
}

/**
 * Creates the editor's state holder once per composition, saving to [repository]. It starts by
 * reopening the setup opened last.
 *
 * @param dugoutsVisibleByDefault whether the dugouts start visible. Android passes `false` because
 *   of limited screen space; the user can open them with the "Show dugouts" button.
 */
@Composable
fun rememberBoardStateHolder(
    repository: SetupRepository,
    dugoutsVisibleByDefault: Boolean = true,
): BoardStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository) {
        BoardStateHolder(repository = repository, scope = scope, dugoutsVisible = dugoutsVisibleByDefault)
    }
}
