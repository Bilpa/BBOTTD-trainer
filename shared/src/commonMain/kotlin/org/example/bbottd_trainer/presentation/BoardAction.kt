package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.data.SetupSummary
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TeamId

/**
 * Explicit user actions sent from the UI to [BoardStateHolder].
 *
 * Click actions describe what was clicked, not what should happen; the state holder decides
 * (select, move or report a problem) based on the current selection.
 */
sealed interface BoardAction {
    /** Show or hide both dugouts. */
    data class SetDugoutsVisible(val visible: Boolean) : BoardAction

    /**
     * Switch to the dark ([dark] `true`) or light theme. The choice is remembered, and from then
     * on the system setting is no longer followed.
     */
    data class SetDarkTheme(val dark: Boolean) : BoardAction

    /** A pitch cell was clicked or tapped, whether or not a player or the ball is on it. */
    data class CellClicked(val position: BoardPosition) : BoardAction

    /** A player token in a dugout was clicked. */
    data class DugoutPlayerClicked(val playerId: PlayerId) : BoardAction

    /** A dugout section (outside its player tokens) was clicked. */
    data class DugoutSectionClicked(val team: TeamId, val status: PlayerStatus) : BoardAction

    /**
     * A dragged player or ball was dropped on a target. Applied with the same editing operations
     * as select-then-click.
     */
    data class Drop(val item: DragItem, val target: DropTarget) : BoardAction

    /** The ball token itself was clicked, on the pitch or in the off-pitch area. */
    data object BallClicked : BoardAction

    /** The off-pitch ball area below the pitch was clicked. */
    data object OffPitchBallAreaClicked : BoardAction

    /**
     * A player was double-clicked. On the pitch this toggles standing and prone (a stunned player
     * stands up); in the dugout it does nothing.
     */
    data class PlayerDoubleClicked(val playerId: PlayerId) : BoardAction

    /** Open the status context menu on a player (right-click or long-press). */
    data class OpenStatusMenu(val playerId: PlayerId) : BoardAction

    /** Close the status context menu without choosing anything. */
    data object DismissStatusMenu : BoardAction

    /** A status was chosen in the context menu. */
    data class SetPlayerStatus(val playerId: PlayerId, val status: PlayerStatus) : BoardAction

    /**
     * Clear the selection: Escape, a right-click on an empty pitch cell, or a click outside the
     * pitch.
     */
    data object ClearSelection : BoardAction

    /**
     * Start a new setup from the initial sample setup. If there are unsaved changes, the user is
     * asked first whether to save them.
     */
    data object NewRequested : BoardAction

    /**
     * Save the setup. A setup that is not in the library yet opens the "Save as" dialog instead,
     * since it needs a name.
     */
    data object SaveRequested : BoardAction

    /** Open the "Save as" dialog, to save the setup as a new library entry. */
    data object SaveAsRequested : BoardAction

    /** The "Save as" dialog was confirmed with this [name]. */
    data class SaveAsConfirmed(val name: String) : BoardAction

    /** The "Save as" dialog was cancelled; nothing is saved. */
    data object SaveAsCancelled : BoardAction

    /** "Save" in the unsaved-changes prompt: save, then continue with the pending action. */
    data object UnsavedChangesSave : BoardAction

    /** "Discard" in the unsaved-changes prompt: continue without saving. */
    data object UnsavedChangesDiscard : BoardAction

    /** "Cancel" in the unsaved-changes prompt: do nothing. */
    data object UnsavedChangesCancel : BoardAction

    /**
     * The user tried to close the app (desktop window). Approved at once if there are no unsaved
     * changes; otherwise the user is asked first. See [BoardUiState.closeApproved].
     */
    data object CloseRequested : BoardAction

    /** Show the library of saved setups; its list is loaded fresh each time. */
    data object LibraryRequested : BoardAction

    /** Close the library. */
    data object LibraryClosed : BoardAction

    /**
     * A setup in the library was chosen to open. If there are unsaved changes, the user is asked
     * first whether to save them.
     */
    data class OpenSetupRequested(val id: SetupId, val name: String) : BoardAction

    /** Show the rename dialog for a saved setup. */
    data class RenameRequested(val summary: SetupSummary) : BoardAction

    /** The rename dialog was confirmed with this [name]. */
    data class RenameConfirmed(val name: String) : BoardAction

    /** The rename dialog was cancelled. */
    data object RenameCancelled : BoardAction

    /** Save a copy of a saved setup under a new ID, named "<name> (copy)". It is not opened. */
    data class DuplicateRequested(val summary: SetupSummary) : BoardAction

    /** Ask for confirmation to delete a library item. */
    data class DeleteRequested(val item: LibraryItem) : BoardAction

    /** The delete confirmation was confirmed. */
    data object DeleteConfirmed : BoardAction

    /** The delete confirmation was cancelled. */
    data object DeleteCancelled : BoardAction

    /** Export a saved setup to a file the user chooses. */
    data class ExportRequested(val summary: SetupSummary) : BoardAction

    /**
     * Export the setup as it is in the editor, including unsaved changes, to a file the user
     * chooses. Works for setups that are not in the library too.
     */
    data object ExportCurrentSetupRequested : BoardAction

    /**
     * Import a setup file the user chooses into the library, under a new ID so that nothing in
     * the library is replaced. It is not opened.
     */
    data object ImportRequested : BoardAction

    /** The UI has shown the message with this [id]. */
    data class MessageShown(val id: Long) : BoardAction
}
