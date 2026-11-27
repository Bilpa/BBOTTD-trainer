package org.example.bbottd_trainer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.example.bbottd_trainer.data.SetupFiles
import org.example.bbottd_trainer.data.StoredEditorPreferences
import org.example.bbottd_trainer.data.StoredSetupRepository
import org.example.bbottd_trainer.data.TextStore
import org.example.bbottd_trainer.presentation.BoardStateHolder
import org.example.bbottd_trainer.ui.BoardScreen

/**
 * Shared root composable used by the Android, desktop and web entry points.
 *
 * Each entry point creates the [stateHolder] (see [rememberBoardStateHolder]) with its platform's
 * storage, so it can also send app-level actions such as a close request.
 *
 * The theme is the user's remembered choice, or the system's light/dark setting if there is none.
 */
@Composable
fun App(stateHolder: BoardStateHolder) {
    val state by stateHolder.state.collectAsState()
    val darkTheme = state.darkTheme ?: isSystemInDarkTheme()

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            BoardScreen(
                state = state,
                darkTheme = darkTheme,
                onAction = stateHolder::onAction,
                modifier = Modifier.safeContentPadding(),
            )
        }
    }
}

/**
 * Creates the editor's state holder once per composition, keeping saved setups and app settings
 * in [store] and exporting and importing setup files through [files]. It starts by reopening the
 * setup opened last.
 *
 * @param dugoutsVisibleByDefault whether the dugouts start visible. Android passes `false` because
 *   of limited screen space; the user can open them with the "Show dugouts" button.
 */
@Composable
fun rememberBoardStateHolder(
    store: TextStore,
    files: SetupFiles,
    dugoutsVisibleByDefault: Boolean = true,
): BoardStateHolder {
    val scope = rememberCoroutineScope()
    return remember(store) {
        BoardStateHolder(
            repository = StoredSetupRepository(store),
            preferences = StoredEditorPreferences(store),
            files = files,
            scope = scope,
            dugoutsVisible = dugoutsVisibleByDefault,
        )
    }
}
