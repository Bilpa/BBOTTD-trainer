package org.example.bbottd_trainer.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.bbottd_trainer.data.SetupRepository
import org.example.bbottd_trainer.data.StorageFailure
import org.example.bbottd_trainer.data.StorageResult
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.EditResult
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup
import org.example.bbottd_trainer.domain.TeamId
import org.example.bbottd_trainer.domain.changePlayerStatus
import org.example.bbottd_trainer.domain.moveBall
import org.example.bbottd_trainer.domain.movePlayerToCell
import org.example.bbottd_trainer.domain.movePlayerToDugout

/**
 * Holds the board screen's state and exposes it as a [StateFlow].
 *
 * This is a plain Kotlin class, not a lifecycle ViewModel. The UI observes [state] and sends
 * explicit [BoardAction]s to [onAction]; this class decides what a click means and applies the
 * domain editing operations. The UI never mutates the setup itself.
 *
 * Select-then-click rules:
 * - Clicking a player selects them; clicking the selected player again deselects them.
 * - With a player selected, clicking an empty cell or a dugout section moves them there. After a
 *   move to a cell the player stays selected; after a move into the dugout the selection clears.
 * - With the ball selected, clicking any cell (even a player's) moves the ball there, and clicking
 *   the off-pitch area takes it off the pitch; both clear the selection.
 * - A failed move keeps the selection and shows a message.
 * - [BoardAction.ClearSelection] (Escape, right-clicking an empty cell, clicking outside the
 *   pitch) deselects.
 *
 * Saving rules:
 * - Saving is explicit; nothing is saved automatically.
 * - Save writes a setup that is in the library under its ID. A setup that is not (the initial
 *   sample or a new setup) goes through "Save as", which asks for a name.
 * - Save as always creates a new library entry with a new ID; the editor continues with it.
 * - New and Close ask whether to save unsaved changes first (Save, Discard or Cancel).
 * - The setup saved or opened last is reopened at start; a new setup clears that record.
 * - Opening a setup from the library asks about unsaved changes like New does. Renaming,
 *   duplicating and deleting saved setups are handled by [SetupLibraryController].
 *
 * Storage runs in [scope]; its results come back as state updates.
 *
 * @param repository where setups are saved.
 * @param scope runs storage operations; usually the composition's scope.
 * @param dugoutsVisible whether the dugouts start visible. Android starts with them hidden because
 *   of limited screen space.
 * @param initialSetup the setup to start with. `null` (the default) reopens the setup opened last,
 *   or starts with the initial sample setup if there is none.
 */
class BoardStateHolder(
    private val repository: SetupRepository,
    private val scope: CoroutineScope,
    dugoutsVisible: Boolean = true,
    initialSetup: TacticalSetup? = null,
) {
    private val mutableState = MutableStateFlow(
        BoardUiState(
            setup = initialSetup ?: SampleSetups.initial,
            dugoutsVisible = dugoutsVisible,
            isLoading = initialSetup == null,
        ),
    )
    private var nextMessageId = 0L

    /** What to do once the setup is saved through the "Save as" dialog, if anything. */
    private var afterSaveAs: PendingAction? = null

    private val library = SetupLibraryController(mutableState, repository, scope) { showMessage(it) }

    val state: StateFlow<BoardUiState> = mutableState.asStateFlow()

    init {
        if (initialSetup == null) scope.launch { openLastSetup() }
    }

    fun onAction(action: BoardAction) {
        // Until the last setup is open, edits would be overwritten by it, so only view and
        // app-level actions are handled.
        if (state.value.isLoading && !action.isAllowedWhileLoading()) return
        when (action) {
            is BoardAction.SetDugoutsVisible ->
                mutableState.update { it.copy(dugoutsVisible = action.visible) }
            is BoardAction.CellClicked -> onCellClicked(action.position)
            is BoardAction.DugoutPlayerClicked -> togglePlayerSelection(action.playerId)
            is BoardAction.DugoutSectionClicked -> onDugoutSectionClicked(action.team, action.status)
            is BoardAction.Drop -> onDrop(action.item, action.target)
            BoardAction.BallClicked -> toggleBallSelection()
            BoardAction.OffPitchBallAreaClicked -> onOffPitchBallAreaClicked()
            is BoardAction.PlayerDoubleClicked -> onPlayerDoubleClicked(action.playerId)
            is BoardAction.OpenStatusMenu ->
                mutableState.update { it.copy(statusMenuPlayerId = action.playerId) }
            BoardAction.DismissStatusMenu -> closeStatusMenu()
            is BoardAction.SetPlayerStatus -> {
                closeStatusMenu()
                // A status change is not a move, so the current selection is kept.
                apply(
                    state.value.setup.changePlayerStatus(action.playerId, action.status),
                    clearSelection = false,
                )
            }
            BoardAction.ClearSelection -> select(null)
            BoardAction.NewRequested -> runAfterAskingToSave(PendingAction.NewSetup)
            BoardAction.CloseRequested -> runAfterAskingToSave(PendingAction.Close)
            BoardAction.SaveRequested -> save(then = null)
            BoardAction.SaveAsRequested -> openSaveAsDialog(then = null)
            is BoardAction.SaveAsConfirmed -> onSaveAsConfirmed(action.name)
            BoardAction.SaveAsCancelled -> {
                afterSaveAs = null
                mutableState.update { it.copy(saveAsDialog = null) }
            }
            BoardAction.UnsavedChangesSave -> {
                val pending = state.value.unsavedChangesPrompt
                mutableState.update { it.copy(unsavedChangesPrompt = null) }
                save(then = pending)
            }
            BoardAction.UnsavedChangesDiscard -> {
                val pending = state.value.unsavedChangesPrompt
                mutableState.update { it.copy(unsavedChangesPrompt = null) }
                if (pending != null) run(pending)
            }
            BoardAction.UnsavedChangesCancel ->
                mutableState.update { it.copy(unsavedChangesPrompt = null) }
            BoardAction.LibraryRequested -> library.show()
            BoardAction.LibraryClosed -> library.close()
            is BoardAction.OpenSetupRequested -> onOpenSetupRequested(action.id, action.name)
            is BoardAction.RenameRequested,
            is BoardAction.RenameConfirmed,
            BoardAction.RenameCancelled,
            is BoardAction.DuplicateRequested,
            is BoardAction.DeleteRequested,
            BoardAction.DeleteConfirmed,
            BoardAction.DeleteCancelled,
            -> library.onAction(action)
            is BoardAction.MessageShown -> mutableState.update {
                if (it.message?.id == action.id) it.copy(message = null) else it
            }
        }
    }

    private fun onCellClicked(position: BoardPosition) {
        val current = state.value
        val selection = current.selection
        val occupant = current.setup.playerAt(position)
        when {
            selection == Selection.Ball -> apply(current.setup.moveBall(position))
            occupant != null -> togglePlayerSelection(occupant.id)
            // The player stays selected after moving to a cell, so they can be moved again.
            selection is Selection.Player ->
                apply(
                    current.setup.movePlayerToCell(selection.playerId, position),
                    clearSelection = false,
                )
            current.setup.ball.position == position -> toggleBallSelection()
            // Nothing selected and an empty cell: nothing to do.
        }
    }

    private fun onDugoutSectionClicked(team: TeamId, status: PlayerStatus) {
        val current = state.value
        when (val selection = current.selection) {
            is Selection.Player ->
                apply(current.setup.movePlayerToDugout(selection.playerId, team, status))
            Selection.Ball -> showMessage("The ball cannot be put in a dugout.")
            null -> Unit
        }
    }

    private fun onOffPitchBallAreaClicked() {
        val current = state.value
        when (current.selection) {
            Selection.Ball -> apply(current.setup.moveBall(null))
            is Selection.Player -> showMessage("Only the ball can be placed here.")
            null -> if (!current.setup.ball.isOnPitch) select(Selection.Ball)
        }
    }

    /**
     * Applies a drag-and-drop with the same operations and messages as select-then-click. A player
     * dropped on a cell ends up selected (as after a click-move); other drops clear the selection.
     */
    private fun onDrop(item: DragItem, target: DropTarget) {
        val setup = state.value.setup
        when (item) {
            is DragItem.Player -> when (target) {
                is DropTarget.Cell -> {
                    val moved = apply(setup.movePlayerToCell(item.playerId, target.position))
                    if (moved) select(Selection.Player(item.playerId))
                }
                is DropTarget.DugoutSection ->
                    apply(setup.movePlayerToDugout(item.playerId, target.team, target.status))
                DropTarget.OffPitchBallArea -> showMessage("Only the ball can be placed here.")
            }
            DragItem.Ball -> when (target) {
                is DropTarget.Cell -> apply(setup.moveBall(target.position))
                DropTarget.OffPitchBallArea -> apply(setup.moveBall(null))
                is DropTarget.DugoutSection -> showMessage("The ball cannot be put in a dugout.")
            }
        }
    }

    private fun onPlayerDoubleClicked(playerId: PlayerId) {
        val setup = state.value.setup
        val player = setup.findPlayer(playerId) ?: return
        // In the dugout a double-click has no effect.
        val newStatus = doubleClickStatus(player.status) ?: return
        // A status change, not a move: the selection is kept.
        apply(setup.changePlayerStatus(playerId, newStatus), clearSelection = false)
    }

    private fun togglePlayerSelection(playerId: PlayerId) {
        val target = Selection.Player(playerId)
        select(if (state.value.selection == target) null else target)
    }

    private fun toggleBallSelection() {
        select(if (state.value.selection == Selection.Ball) null else Selection.Ball)
    }

    private fun select(selection: Selection?) {
        mutableState.update { it.copy(selection = selection) }
    }

    // --- Saving and opening ------------------------------------------------------------------

    /** Runs [action] at once if nothing would be lost; otherwise asks whether to save first. */
    private fun runAfterAskingToSave(action: PendingAction) {
        val current = state.value
        when {
            current.isSaving -> Unit // The user can try again once the save is done.
            current.isDirty -> mutableState.update { it.copy(unsavedChangesPrompt = action) }
            else -> run(action)
        }
    }

    private fun run(action: PendingAction) {
        when (action) {
            PendingAction.NewSetup -> startNewSetup()
            PendingAction.Close -> mutableState.update { it.copy(closeApproved = true) }
            is PendingAction.Open -> openSetup(action.id, action.name)
        }
    }

    private fun onOpenSetupRequested(id: SetupId, name: String) {
        val current = state.value
        if (current.library?.isBusy == true) return
        if (current.isInLibrary && current.setup.id == id && !current.isDirty) {
            // Already open, unchanged: nothing to load.
            library.close()
            return
        }
        // Opening the open setup while it has unsaved changes goes back to its saved version,
        // after the same question as for any other setup.
        runAfterAskingToSave(PendingAction.Open(id, name))
    }

    /**
     * Loads the saved setup [id] into the editor and closes the library. It becomes the setup to
     * reopen at start. If it cannot be loaded, the user is told why and the editor is unchanged.
     */
    private fun openSetup(id: SetupId, name: String) {
        mutableState.update { it.copy(library = it.library?.copy(isBusy = true)) }
        scope.launch {
            when (val result = repository.load(id)) {
                is StorageResult.Success -> {
                    mutableState.update {
                        it.copy(
                            setup = result.value,
                            isInLibrary = true,
                            isDirty = false,
                            selection = null,
                            statusMenuPlayerId = null,
                            library = null,
                        )
                    }
                    repository.setLastOpenedSetupId(id)
                }
                is StorageResult.Failure -> {
                    showMessage("Could not open “$name”: ${result.failure.describe()}")
                    library.refresh()
                }
            }
        }
    }

    /**
     * Replaces the setup with the initial sample setup, not yet in the library, and clears the
     * selection, any open menu and the unsaved-changes flag. Keeps view settings such as dugout
     * visibility.
     */
    private fun startNewSetup() {
        mutableState.update {
            it.copy(
                setup = SampleSetups.initial,
                selection = null,
                statusMenuPlayerId = null,
                isDirty = false,
                isInLibrary = false,
            )
        }
        // The new setup is not saved, so there is nothing to reopen at the next start.
        scope.launch { repository.setLastOpenedSetupId(null) }
    }

    /** Saves the setup, then runs [then] if the save succeeds. */
    private fun save(then: PendingAction?) {
        val current = state.value
        when {
            current.isSaving || current.isLoading -> Unit
            !current.isInLibrary -> openSaveAsDialog(then)
            else -> write(current.setup, then)
        }
    }

    private fun openSaveAsDialog(then: PendingAction?) {
        if (state.value.isSaving) return
        afterSaveAs = then
        mutableState.update { it.copy(saveAsDialog = SaveAsDialog(initialName = it.setup.name)) }
    }

    private fun onSaveAsConfirmed(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            // The dialog stays open; the UI also disables its Save button for a blank name.
            showMessage("Enter a name for the setup.")
            return
        }
        val then = afterSaveAs
        afterSaveAs = null
        mutableState.update { it.copy(saveAsDialog = null) }
        write(state.value.setup.copy(id = SetupId.generate(), name = trimmed), then)
    }

    /**
     * Writes [target] to the library. On success the editor continues with [target]'s ID and name
     * (keeping any edits made while saving), it becomes the setup to reopen at start, and [then]
     * runs. On failure the user is told why, and [then] is dropped, so nothing is lost.
     */
    private fun write(target: TacticalSetup, then: PendingAction?) {
        mutableState.update { it.copy(isSaving = true) }
        scope.launch {
            val result = repository.save(target)
            if (result is StorageResult.Success) repository.setLastOpenedSetupId(target.id)
            mutableState.update { current ->
                when (result) {
                    is StorageResult.Success -> {
                        val setup = current.setup.copy(id = target.id, name = target.name)
                        current.copy(
                            setup = setup,
                            isSaving = false,
                            isInLibrary = true,
                            isDirty = setup != target,
                        )
                    }
                    is StorageResult.Failure -> current.copy(isSaving = false)
                }
            }
            when (result) {
                is StorageResult.Success -> {
                    showMessage("Saved \u201C${target.name}\u201D.")
                    if (then != null) run(then)
                }
                is StorageResult.Failure ->
                    showMessage("Could not save \u201C${target.name}\u201D: ${result.failure.describe()}")
            }
        }
    }

    /** Opens the setup recorded as opened last; keeps the initial sample setup if there is none. */
    private suspend fun openLastSetup() {
        val id = repository.lastOpenedSetupId()
        val result = id?.let { repository.load(it) }
        mutableState.update { current ->
            if (result is StorageResult.Success) {
                current.copy(setup = result.value, isInLibrary = true, isLoading = false)
            } else {
                current.copy(isLoading = false)
            }
        }
        if (result is StorageResult.Failure) {
            val failure = result.failure
            // A setup that is gone or cannot be read will not open next time either.
            if (failure !is StorageFailure.StorageError) repository.setLastOpenedSetupId(null)
            // A deleted setup needs no message: the user deleted it.
            if (failure != StorageFailure.NotFound) {
                showMessage("Could not open the last setup: ${failure.describe()}")
            }
        }
    }

    private fun closeStatusMenu() {
        mutableState.update { it.copy(statusMenuPlayerId = null) }
    }

    /**
     * Applies an edit: on success, store the new setup (and clear the selection after a move);
     * on failure, explain why in a message. Returns whether the edit succeeded.
     */
    private fun apply(result: EditResult, clearSelection: Boolean = true): Boolean =
        when (result) {
            is EditResult.Success -> {
                mutableState.update {
                    it.copy(
                        setup = result.setup,
                        selection = if (clearSelection) null else it.selection,
                        isDirty = it.isDirty || result.setup != it.setup,
                    )
                }
                true
            }
            is EditResult.Failure -> {
                showMessage(result.reason.describe(state.value.setup))
                false
            }
        }

    private fun BoardAction.isAllowedWhileLoading(): Boolean =
        this is BoardAction.SetDugoutsVisible ||
            this is BoardAction.MessageShown ||
            this == BoardAction.CloseRequested

    private fun showMessage(text: String) {
        val message = EditorMessage(id = nextMessageId++, text = text)
        mutableState.update { it.copy(message = message) }
    }
}
