package org.example.bbottd_trainer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.data.SetupSummary
import org.example.bbottd_trainer.data.UnreadableEntry
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.presentation.BoardAction
import org.example.bbottd_trainer.presentation.LibraryItem
import org.example.bbottd_trainer.presentation.LibraryState
import org.example.bbottd_trainer.presentation.describe

private val ListMaxHeight = 420.dp
private val RowMinHeight = 48.dp

/**
 * The library of saved setups. Clicking a setup opens it; its "More" menu renames, duplicates or
 * deletes it. Entries that could not be read are listed with the reason and can be deleted.
 * Rename and delete confirmations are shown on top of the library.
 *
 * @param openSetupId the setup open in the editor, if it is in the library; marked in the list.
 */
@Composable
fun LibraryDialog(
    library: LibraryState,
    openSetupId: SetupId?,
    onAction: (BoardAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(BoardAction.LibraryClosed) },
        title = { Text("Saved setups") },
        text = {
            Column {
                // Reserves the same height whether or not it is showing, so the list does not jump.
                Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                    if (library.isBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(8.dp))
                LibraryList(library, openSetupId, onAction)
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(BoardAction.LibraryClosed) }) { Text("Close") }
        },
    )

    library.renaming?.let { summary ->
        NameDialog(
            title = "Rename setup",
            confirmLabel = "Rename",
            initialName = summary.name,
            onConfirm = { onAction(BoardAction.RenameConfirmed(it)) },
            onCancel = { onAction(BoardAction.RenameCancelled) },
        )
    }
    library.deleting?.let { item ->
        DeleteConfirmationDialog(
            item = item,
            onConfirm = { onAction(BoardAction.DeleteConfirmed) },
            onCancel = { onAction(BoardAction.DeleteCancelled) },
        )
    }
}

@Composable
private fun LibraryList(
    library: LibraryState,
    openSetupId: SetupId?,
    onAction: (BoardAction) -> Unit,
) {
    val listError = library.listError
    when {
        listError != null -> Text(listError, color = MaterialTheme.colorScheme.error)
        library.setups.isEmpty() && library.unreadable.isEmpty() && !library.isBusy ->
            Text("No saved setups yet. Use Save or Save as to add the current setup to the library.")
        else -> LazyColumn(modifier = Modifier.heightIn(max = ListMaxHeight)) {
            items(library.setups, key = { "setup:" + it.id.value }) { summary ->
                SetupRow(
                    summary = summary,
                    isOpen = summary.id == openSetupId,
                    enabled = !library.isBusy,
                    onAction = onAction,
                )
                HorizontalDivider()
            }
            items(library.unreadable, key = { "unreadable:" + it.entryKey }) { entry ->
                UnreadableRow(entry = entry, enabled = !library.isBusy, onAction = onAction)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SetupRow(
    summary: SetupSummary,
    isOpen: Boolean,
    enabled: Boolean,
    onAction: (BoardAction) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = RowMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = RowMinHeight)
                .clickable(enabled = enabled) { onAction(BoardAction.OpenSetupRequested(summary.id, summary.name)) }
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = summary.name,
                fontWeight = if (isOpen) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isOpen) {
                Text(
                    text = "Open in the editor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box {
            TextButton(onClick = { menuOpen = true }, enabled = enabled) { Text("More") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Rename…") },
                    onClick = {
                        menuOpen = false
                        onAction(BoardAction.RenameRequested(summary))
                    },
                )
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    onClick = {
                        menuOpen = false
                        onAction(BoardAction.DuplicateRequested(summary))
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete…") },
                    onClick = {
                        menuOpen = false
                        onAction(BoardAction.DeleteRequested(LibraryItem.Setup(summary)))
                    },
                )
            }
        }
    }
}

@Composable
private fun UnreadableRow(
    entry: UnreadableEntry,
    enabled: Boolean,
    onAction: (BoardAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = RowMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text("Unreadable entry", color = MaterialTheme.colorScheme.error)
            Text(
                text = "Cannot be opened: ${entry.failure.describe()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(
            onClick = { onAction(BoardAction.DeleteRequested(LibraryItem.Unreadable(entry))) },
            enabled = enabled,
        ) {
            Text("Delete")
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    item: LibraryItem,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val (title, text) = when (item) {
        is LibraryItem.Setup ->
            "Delete “${item.summary.name}”?" to "The saved setup is removed from the library. This cannot be undone."
        is LibraryItem.Unreadable ->
            "Delete the unreadable entry?" to "The damaged entry is removed. This cannot be undone."
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
