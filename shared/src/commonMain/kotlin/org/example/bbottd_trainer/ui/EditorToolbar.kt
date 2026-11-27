package org.example.bbottd_trainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.presentation.BoardAction
import org.example.bbottd_trainer.presentation.BoardUiState

private val NameBlockWidth = 220.dp

/**
 * The bar above the board: the setup's name and save status, and the New, Save, Save as and
 * dugout buttons. It wraps onto more lines on narrow screens.
 */
@Composable
fun EditorToolbar(
    state: BoardUiState,
    onAction: (BoardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Saving and opening are disabled while storage is busy, so they cannot overlap.
    val storageIdle = !state.isLoading && !state.isSaving
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
        OutlinedButton(onClick = { onAction(BoardAction.NewRequested) }, enabled = storageIdle) {
            Text("New")
        }
        OutlinedButton(onClick = { onAction(BoardAction.SaveRequested) }, enabled = storageIdle) {
            Text("Save")
        }
        OutlinedButton(onClick = { onAction(BoardAction.SaveAsRequested) }, enabled = storageIdle) {
            Text("Save as…")
        }
        OutlinedButton(onClick = { onAction(BoardAction.SetDugoutsVisible(!state.dugoutsVisible)) }) {
            Text(if (state.dugoutsVisible) "Hide dugouts" else "Show dugouts")
        }
    }
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
