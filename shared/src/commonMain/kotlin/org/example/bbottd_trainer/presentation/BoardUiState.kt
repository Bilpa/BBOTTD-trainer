package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.data.SetupSummary
import org.example.bbottd_trainer.data.UnreadableEntry
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup

/**
 * Everything the board screen renders.
 *
 * [setup] is the persisted tactical data. The other properties are transient editor state and are
 * never saved with the setup.
 */
data class BoardUiState(
    val setup: TacticalSetup,
    val selection: Selection? = null,
    /** A one-off message for the snackbar; cleared when the UI reports it as shown. */
    val message: EditorMessage? = null,
    /** Whether [setup] has changes that are not saved. */
    val isDirty: Boolean = false,
    /**
     * Whether [setup] is saved in the library under its ID. A setup that is not (the initial
     * sample, or a new setup) is saved with "Save as", which asks for a name.
     */
    val isInLibrary: Boolean = false,
    /** Opening the last opened setup at start; editing is ignored until it is done. */
    val isLoading: Boolean = false,
    /** A save is in progress; further saves, New and Close wait until it is done. */
    val isSaving: Boolean = false,
    val dugoutsVisible: Boolean = true,
    /** The player whose status context menu is open, if any. */
    val statusMenuPlayerId: PlayerId? = null,
    /**
     * When set, the "Save changes?" prompt is showing; the action runs once the user saves or
     * discards the changes.
     */
    val unsavedChangesPrompt: PendingAction? = null,
    /** When set, the "Save as" dialog is showing. */
    val saveAsDialog: SaveAsDialog? = null,
    /** When set, the library of saved setups is showing. */
    val library: LibraryState? = null,
    /**
     * The user may close the app: there were no unsaved changes, or they were saved or
     * discarded. The platform entry point closes the window when this becomes `true`.
     */
    val closeApproved: Boolean = false,
)

/** An action that would lose unsaved changes, so it waits for the user's answer. */
sealed interface PendingAction {
    /** Start a new setup from the initial sample setup. */
    data object NewSetup : PendingAction

    /** Close the app. */
    data object Close : PendingAction

    /** Open the saved setup [id], called [name]. */
    data class Open(val id: SetupId, val name: String) : PendingAction
}

/**
 * The library of saved setups, while it is showing.
 *
 * @property setups the saved setups, sorted by name.
 * @property unreadable stored entries that could not be read, such as damaged files.
 * @property isBusy the list is loading, or a setup is being opened, renamed, duplicated or
 *   deleted; further operations wait until it is done.
 * @property listError why the list could not be loaded, if it could not.
 * @property renaming the setup whose rename dialog is showing.
 * @property deleting the item whose delete confirmation is showing.
 */
data class LibraryState(
    val setups: List<SetupSummary> = emptyList(),
    val unreadable: List<UnreadableEntry> = emptyList(),
    val isBusy: Boolean = true,
    val listError: String? = null,
    val renaming: SetupSummary? = null,
    val deleting: LibraryItem? = null,
)

/** Something listed in the library: a saved setup, or an entry that could not be read. */
sealed interface LibraryItem {
    data class Setup(val summary: SetupSummary) : LibraryItem

    data class Unreadable(val entry: UnreadableEntry) : LibraryItem
}

/** The "Save as" dialog, which asks for the setup's name; it starts with [initialName]. */
data class SaveAsDialog(
    val initialName: String,
)

/** What the user has selected, to be moved by clicking a target. */
sealed interface Selection {
    data class Player(val playerId: PlayerId) : Selection

    data object Ball : Selection
}

/**
 * A message for the user, such as why a move failed. [id] is unique per message, so the same
 * text shown twice in a row is still shown twice.
 */
data class EditorMessage(
    val id: Long,
    val text: String,
)
