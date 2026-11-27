package org.example.bbottd_trainer.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import org.example.bbottd_trainer.presentation.PendingAction

/**
 * Asks whether to save unsaved changes before [pending] continues: Save, Discard or Cancel.
 * Dismissing the dialog (clicking outside it, Escape or Back) cancels.
 */
@Composable
fun UnsavedChangesDialog(
    setupName: String,
    pending: PendingAction,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    val (title, consequence) = when (pending) {
        PendingAction.NewSetup ->
            "Save changes before starting a new setup?" to "a new setup is started"
        PendingAction.Close -> "Save changes before closing?" to "the app closes"
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Text("“$setupName” has unsaved changes. If you discard them, they are lost when $consequence.")
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
            TextButton(onClick = onDiscard) { Text("Discard") }
        },
    )
}

/**
 * Asks for the name to save the setup under. The name field starts with [initialName], selected,
 * so typing replaces it. Save is disabled while the name is blank.
 */
@Composable
fun SaveAsDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    // The text being typed is local to the dialog until Save sends it as an action.
    var name by remember {
        mutableStateOf(TextFieldValue(initialName, selection = TextRange(0, initialName.length)))
    }
    val canSave = name.text.isNotBlank()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Save setup as") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSave) onSave(name.text) }),
                modifier = Modifier.focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.text) }, enabled = canSave) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
