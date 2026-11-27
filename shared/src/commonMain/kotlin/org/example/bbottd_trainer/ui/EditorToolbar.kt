package org.example.bbottd_trainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.presentation.BoardAction
import org.example.bbottd_trainer.presentation.BoardUiState

private val NameBlockWidth = 220.dp

/**
 * The bar above the board: the setup's name and save status, the File menu, and the dugout and
 * theme buttons. It wraps onto more lines on narrow screens.
 */
@Composable
fun EditorToolbar(
    state: BoardUiState,
    darkTheme: Boolean,
    onAction: (BoardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Saving, opening and file transfers are disabled while storage is busy, so they cannot overlap.
    val storageIdle = !state.isLoading && !state.isSaving && !state.isTransferringFile
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        // A fixed width, so the buttons do not move when the status text changes.
        Column(modifier = Modifier.width(NameBlockWidth)) {
            Text(
                text = state.setup.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = saveStatus(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FileMenu(storageIdle = storageIdle, onAction = onAction)
        OutlinedButton(onClick = { onAction(BoardAction.SetDugoutsVisible(!state.dugoutsVisible)) }) {
            Text(if (state.dugoutsVisible) "Hide dugouts" else "Show dugouts")
        }
        OutlinedButton(onClick = { onAction(BoardAction.SetDarkTheme(!darkTheme)) }) {
            Text(if (darkTheme) "Light mode" else "Dark mode")
        }
    }
}

/**
 * The File button and its menu: New, Save, Save as, Library, Import and Export. The items are
 * disabled while storage is busy (opening, saving, exporting or importing), so storage operations
 * cannot overlap. Export writes the setup as it is in the editor, unsaved changes included.
 */
@Composable
private fun FileMenu(
    storageIdle: Boolean,
    onAction: (BoardAction) -> Unit,
) {
    // Whether the menu is open is purely visual, so it is kept here rather than in the state.
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("File") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FileMenuItem("New", storageIdle) {
                expanded = false
                onAction(BoardAction.NewRequested)
            }
            FileMenuItem("Save", storageIdle) {
                expanded = false
                onAction(BoardAction.SaveRequested)
            }
            FileMenuItem("Save as…", storageIdle) {
                expanded = false
                onAction(BoardAction.SaveAsRequested)
            }
            FileMenuItem("Library…", storageIdle) {
                expanded = false
                onAction(BoardAction.LibraryRequested)
            }
            HorizontalDivider()
            FileMenuItem("Import…", storageIdle) {
                expanded = false
                onAction(BoardAction.ImportRequested)
            }
            FileMenuItem("Export…", storageIdle) {
                expanded = false
                onAction(BoardAction.ExportCurrentSetupRequested)
            }
        }
    }
}

@Composable
private fun FileMenuItem(label: String, enabled: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = onClick, enabled = enabled)
}

/** A short line under the setup name saying whether it is saved. */
internal fun saveStatus(state: BoardUiState): String =
    when {
        state.isLoading -> "Opening…"
        state.isSaving -> "Saving…"
        !state.isInLibrary && state.isDirty -> "Not saved • unsaved changes"
        !state.isInLibrary -> "Not saved"
        state.isDirty -> "Unsaved changes"
        else -> "Saved"
    }
