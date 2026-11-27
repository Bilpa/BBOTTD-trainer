package org.example.bbottd_trainer.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.bbottd_trainer.data.FileTransferResult
import org.example.bbottd_trainer.data.SetupCodec
import org.example.bbottd_trainer.data.SetupDecodeResult
import org.example.bbottd_trainer.data.SetupFiles
import org.example.bbottd_trainer.data.SetupRepository
import org.example.bbottd_trainer.data.StorageResult
import org.example.bbottd_trainer.data.exportFileName
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup

/**
 * The library part of [BoardStateHolder]: listing saved setups; renaming, duplicating and
 * deleting them; and exporting them to and importing them from files. Opening a setup stays in the state holder, because it may first have to ask
 * about unsaved changes.
 *
 * It updates [BoardUiState.library] in the holder's [state], and keeps the editor consistent when
 * the open setup is renamed (the editor shows the new name) or deleted (the editor keeps the
 * content, now not in the library).
 */
internal class SetupLibraryController(
    private val state: MutableStateFlow<BoardUiState>,
    private val repository: SetupRepository,
    private val files: SetupFiles,
    private val scope: CoroutineScope,
    private val showMessage: (String) -> Unit,
) {
    fun show() {
        state.update { it.copy(library = LibraryState()) }
        refresh()
    }

    fun close() {
        state.update { it.copy(library = null) }
    }

    /** Reloads the list, keeping the library's dialogs as they are. */
    fun refresh() {
        updateLibrary { it.copy(isBusy = true) }
        scope.launch {
            val result = repository.list()
            updateLibrary { library ->
                when (result) {
                    is StorageResult.Success -> library.copy(
                        setups = result.value.setups,
                        unreadable = result.value.unreadable,
                        isBusy = false,
                        listError = null,
                    )
                    is StorageResult.Failure -> library.copy(
                        isBusy = false,
                        listError = "The saved setups could not be listed: ${result.failure.describe()}",
                    )
                }
            }
        }
    }

    fun onAction(action: BoardAction) {
        when (action) {
            is BoardAction.RenameRequested -> updateLibrary { it.copy(renaming = action.summary) }
            is BoardAction.RenameConfirmed -> rename(action.name)
            BoardAction.RenameCancelled -> updateLibrary { it.copy(renaming = null) }
            is BoardAction.DuplicateRequested -> duplicate(action.summary.id)
            is BoardAction.DeleteRequested -> updateLibrary { it.copy(deleting = action.item) }
            BoardAction.DeleteConfirmed -> delete()
            BoardAction.DeleteCancelled -> updateLibrary { it.copy(deleting = null) }
            is BoardAction.ExportRequested -> export(action.summary.id)
            BoardAction.ExportCurrentSetupRequested -> exportCurrentSetup()
            BoardAction.ImportRequested -> import()
            else -> error("Not a library action: $action")
        }
    }

    private fun rename(name: String) {
        val library = state.value.library ?: return
        val summary = library.renaming ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            showMessage("Enter a name for the setup.")
            return
        }
        if (library.isBusy) return
        updateLibrary { it.copy(renaming = null) }
        runOperation {
            // Renames the saved version only; unsaved edits in the editor stay unsaved.
            val saved = when (val loaded = repository.load(summary.id)) {
                is StorageResult.Success -> loaded.value
                is StorageResult.Failure -> {
                    showMessage("Could not rename “${summary.name}”: ${loaded.failure.describe()}")
                    return@runOperation
                }
            }
            when (val result = repository.save(saved.copy(name = trimmed))) {
                is StorageResult.Success -> {
                    state.update { current ->
                        if (current.isInLibrary && current.setup.id == summary.id) {
                            current.copy(setup = current.setup.copy(name = trimmed))
                        } else {
                            current
                        }
                    }
                    showMessage("Renamed to “$trimmed”.")
                }
                is StorageResult.Failure ->
                    showMessage("Could not rename “${summary.name}”: ${result.failure.describe()}")
            }
        }
    }

    private fun duplicate(id: SetupId) {
        if (state.value.library?.isBusy != false) return
        runOperation {
            val original = when (val loaded = repository.load(id)) {
                is StorageResult.Success -> loaded.value
                is StorageResult.Failure -> {
                    showMessage("Could not duplicate the setup: ${loaded.failure.describe()}")
                    return@runOperation
                }
            }
            val copy = original.copy(id = SetupId.generate(), name = "${original.name} (copy)")
            when (val result = repository.save(copy)) {
                is StorageResult.Success -> showMessage("Saved a copy as “${copy.name}”.")
                is StorageResult.Failure ->
                    showMessage("Could not duplicate “${original.name}”: ${result.failure.describe()}")
            }
        }
    }

    private fun delete() {
        val library = state.value.library ?: return
        val item = library.deleting ?: return
        if (library.isBusy) return
        updateLibrary { it.copy(deleting = null) }
        runOperation {
            when (item) {
                is LibraryItem.Setup -> {
                    val summary = item.summary
                    when (val result = repository.delete(summary.id)) {
                        is StorageResult.Success -> {
                            onSetupDeleted(summary.id)
                            showMessage("Deleted “${summary.name}”.")
                        }
                        is StorageResult.Failure ->
                            showMessage("Could not delete “${summary.name}”: ${result.failure.describe()}")
                    }
                }
                is LibraryItem.Unreadable ->
                    when (val result = repository.deleteEntry(item.entry.entryKey)) {
                        is StorageResult.Success -> showMessage("Deleted the unreadable entry.")
                        is StorageResult.Failure ->
                            showMessage("Could not delete the entry: ${result.failure.describe()}")
                    }
            }
        }
    }

    /** Exports the saved version of a setup; unsaved edits in the editor are not included. */
    private fun export(id: SetupId) {
        if (!canStartFileTransfer()) return
        runFileTransfer {
            val setup = when (val loaded = repository.load(id)) {
                is StorageResult.Success -> loaded.value
                is StorageResult.Failure -> {
                    showMessage("Could not export the setup: ${loaded.failure.describe()}")
                    return@runFileTransfer
                }
            }
            writeExport(setup)
        }
    }

    /** Exports the setup as it is in the editor, including unsaved changes. */
    private fun exportCurrentSetup() {
        if (!canStartFileTransfer()) return
        val setup = state.value.setup
        runFileTransfer { writeExport(setup) }
    }

    private suspend fun writeExport(setup: TacticalSetup) {
        when (val result = files.export(exportFileName(setup.name), SetupCodec.encode(setup))) {
            is FileTransferResult.Done -> showMessage("Exported \u201C${setup.name}\u201D.")
            FileTransferResult.Cancelled -> Unit
            is FileTransferResult.Failed ->
                showMessage("Could not export \u201C${setup.name}\u201D: ${result.detail}")
        }
    }

    /**
     * Imports a setup file into the library. It always gets a new ID, so importing a file twice,
     * or importing an export of a setup that is still in the library, adds a copy and never
     * replaces a saved setup.
     */
    private fun import() {
        if (!canStartFileTransfer()) return
        runFileTransfer {
            val text = when (val chosen = files.import()) {
                is FileTransferResult.Done -> chosen.value
                FileTransferResult.Cancelled -> return@runFileTransfer
                is FileTransferResult.Failed -> {
                    showMessage("Could not import the file: ${chosen.detail}")
                    return@runFileTransfer
                }
            }
            val setup = when (val decoded = SetupCodec.decode(text)) {
                is SetupDecodeResult.Success -> decoded.setup.copy(id = SetupId.generate())
                is SetupDecodeResult.Failure -> {
                    showMessage("Could not import the file: ${decoded.failure.describe()}")
                    return@runFileTransfer
                }
            }
            when (val result = repository.save(setup)) {
                is StorageResult.Success -> showMessage("Imported \u201C${setup.name}\u201D into the library.")
                is StorageResult.Failure ->
                    showMessage("Could not import \u201C${setup.name}\u201D: ${result.failure.describe()}")
            }
        }
    }

    /**
     * The open setup stays in the editor when its saved version is deleted, but is no longer in
     * the library: Save then asks for a name, and it is not reopened at the next start.
     */
    private suspend fun onSetupDeleted(id: SetupId) {
        val current = state.value
        if (current.isInLibrary && current.setup.id == id) {
            state.update { it.copy(isInLibrary = false) }
            repository.setLastOpenedSetupId(null)
        }
    }

    /**
     * Export and import can start from the File menu (library closed) or from the library; only
     * one file transfer runs at a time, and not while the library is busy.
     */
    private fun canStartFileTransfer(): Boolean {
        val current = state.value
        return !current.isTransferringFile && current.library?.isBusy != true
    }

    /** Runs an export or import marked as a file transfer, and as a library operation. */
    private fun runFileTransfer(block: suspend () -> Unit) {
        state.update { it.copy(isTransferringFile = true) }
        runOperation {
            try {
                block()
            } finally {
                state.update { it.copy(isTransferringFile = false) }
            }
        }
    }

    /** Runs a library operation with the library marked busy, then reloads the list. */
    private fun runOperation(block: suspend () -> Unit) {
        updateLibrary { it.copy(isBusy = true) }
        scope.launch {
            block()
            refresh()
        }
    }

    private fun updateLibrary(change: (LibraryState) -> LibraryState) {
        state.update { current -> current.library?.let { current.copy(library = change(it)) } ?: current }
    }
}
