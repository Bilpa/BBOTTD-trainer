package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.data.InMemoryTextStore
import org.example.bbottd_trainer.data.SetupDecodeFailure
import org.example.bbottd_trainer.data.SetupSummary
import org.example.bbottd_trainer.data.StorageException
import org.example.bbottd_trainer.data.StorageResult
import org.example.bbottd_trainer.data.StoredSetupRepository
import org.example.bbottd_trainer.data.TextStore
import org.example.bbottd_trainer.data.UnreadableEntry
import org.example.bbottd_trainer.data.runSuspend
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.PlayerId
import org.example.bbottd_trainer.domain.PlayerStatus
import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The library of saved setups, as handled by [BoardStateHolder] and [SetupLibraryController]. */
class LibraryTest {
    private val store = InMemoryTextStore()
    private val repository = StoredSetupRepository(store)

    /** The initial sample with home player 1 placed at [x], so setups differ in content. */
    private fun saved(id: String, name: String, x: Int = 0): TacticalSetup {
        val setup = SampleSetups.initial.copy(id = SetupId(id), name = name).let {
            it.copy(
                home = it.home.copy(
                    players = it.home.players.map { player ->
                        if (player.id == PlayerId("home-1")) {
                            player.copy(status = PlayerStatus.ON_FIELD, position = BoardPosition(x, 20))
                        } else {
                            player
                        }
                    },
                ),
            )
        }
        runSuspend { repository.save(setup) }
        return setup
    }

    private fun stored(id: String): TacticalSetup? {
        var result: TacticalSetup? = null
        runSuspend { result = (repository.load(SetupId(id)) as? StorageResult.Success)?.value }
        return result
    }

    private fun lastOpened(): SetupId? {
        var result: SetupId? = null
        runSuspend { result = repository.lastOpenedSetupId() }
        return result
    }

    private fun holder() = testHolder(repository = repository)

    private val BoardStateHolder.library: LibraryState get() = assertNotNull(current.library)

    private fun BoardStateHolder.names(): List<String> = library.setups.map { it.name }

    private fun summary(id: String, name: String) = SetupSummary(SetupId(id), name)

    private fun lastMessage(holder: BoardStateHolder): String = assertNotNull(holder.current.message).text

    /** A small edit: home player 2 from the dugout onto the pitch. */
    private fun BoardStateHolder.edit() =
        click(BoardAction.DugoutPlayerClicked(PlayerId("home-2")), BoardAction.CellClicked(BoardPosition(9, 9)))

    // --- Showing the library -----------------------------------------------------------------

    @Test
    fun libraryListsSavedSetupsByName() {
        saved("b", "Bravo")
        saved("a", "alpha")
        val holder = holder()

        holder.click(BoardAction.LibraryRequested)

        assertEquals(listOf("alpha", "Bravo"), holder.names())
        assertFalse(holder.library.isBusy)
        assertNull(holder.library.listError)
    }

    @Test
    fun emptyLibraryHasNoSetups() {
        val holder = holder()
        holder.click(BoardAction.LibraryRequested)
        assertEquals(emptyList(), holder.library.setups)
    }

    @Test
    fun libraryIsReloadedEachTimeItIsShown() {
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.LibraryClosed)
        assertNull(holder.current.library)
        saved("a", "Later")

        holder.click(BoardAction.LibraryRequested)

        assertEquals(listOf("Later"), holder.names())
    }

    @Test
    fun unreadableEntriesAreListedSeparately() {
        saved("a", "Good")
        runSuspend { store.write("setup-bad", "{ damaged") }
        val holder = holder()

        holder.click(BoardAction.LibraryRequested)

        assertEquals(listOf("Good"), holder.names())
        assertEquals(listOf(UnreadableEntry("setup-bad", SetupDecodeFailure.NotJson)), holder.library.unreadable)
    }

    @Test
    fun aListFailureIsShownInTheLibrary() {
        class Unlistable : TextStore by InMemoryTextStore() {
            override suspend fun keys(): Set<String> = throw StorageException("Storage disabled")
        }
        val holder = testHolder(repository = StoredSetupRepository(Unlistable()))

        holder.click(BoardAction.LibraryRequested)

        assertEquals(
            "The saved setups could not be listed: the storage reported an error (Storage disabled).",
            holder.library.listError,
        )
    }

    // --- Opening -----------------------------------------------------------------------------

    @Test
    fun openingASetupLoadsItAndClosesTheLibrary() {
        val other = saved("other", "Other", x = 5)
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(other.id, other.name))

        assertEquals(other, holder.current.setup)
        assertTrue(holder.current.isInLibrary)
        assertFalse(holder.current.isDirty)
        assertNull(holder.current.library)
        assertEquals(other.id, lastOpened(), "it is reopened at the next start")
    }

    @Test
    fun openingClearsSelectionAndMenus() {
        val other = saved("other", "Other")
        val holder = holder()
        holder.click(
            BoardAction.DugoutPlayerClicked(PlayerId("home-3")),
            BoardAction.OpenStatusMenu(PlayerId("home-3")),
            BoardAction.LibraryRequested,
            BoardAction.OpenSetupRequested(other.id, other.name),
        )
        assertNull(holder.current.selection)
        assertNull(holder.current.statusMenuPlayerId)
    }

    @Test
    fun openingWithUnsavedChangesAsksFirst() {
        val other = saved("other", "Other")
        val holder = holder()
        holder.edit()
        val edited = holder.current.setup

        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(other.id, other.name))

        assertEquals(PendingAction.Open(other.id, "Other"), holder.current.unsavedChangesPrompt)
        assertEquals(edited, holder.current.setup)
        assertNotNull(holder.current.library, "the library stays open behind the prompt")
    }

    @Test
    fun discardingOpensTheOtherSetup() {
        val other = saved("other", "Other", x = 5)
        val holder = holder()
        holder.edit()
        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.OpenSetupRequested(other.id, other.name),
            BoardAction.UnsavedChangesDiscard,
        )
        assertEquals(other, holder.current.setup)
    }

    @Test
    fun savingFromThePromptSavesThenOpens() {
        val mine = saved("mine", "Mine")
        val other = saved("other", "Other", x = 5)
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(mine.id, mine.name))
        holder.edit()
        val edited = holder.current.setup

        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.OpenSetupRequested(other.id, other.name),
            BoardAction.UnsavedChangesSave,
        )

        assertEquals(edited, stored("mine"))
        assertEquals(other, holder.current.setup)
    }

    @Test
    fun cancellingKeepsTheEditsAndTheLibrary() {
        val other = saved("other", "Other")
        val holder = holder()
        holder.edit()
        val edited = holder.current.setup
        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.OpenSetupRequested(other.id, other.name),
            BoardAction.UnsavedChangesCancel,
        )
        assertEquals(edited, holder.current.setup)
        assertNotNull(holder.current.library)
    }

    @Test
    fun openingTheUnchangedOpenSetupJustClosesTheLibrary() {
        val mine = saved("mine", "Mine")
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(mine.id, mine.name))

        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(mine.id, mine.name))

        assertNull(holder.current.library)
        assertNull(holder.current.unsavedChangesPrompt)
    }

    @Test
    fun openingTheChangedOpenSetupAsksThenRevertsToTheSavedVersion() {
        val mine = saved("mine", "Mine")
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(mine.id, mine.name))
        holder.edit()

        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(mine.id, mine.name))
        assertEquals(PendingAction.Open(mine.id, "Mine"), holder.current.unsavedChangesPrompt)
        holder.click(BoardAction.UnsavedChangesDiscard)

        assertEquals(mine, holder.current.setup)
        assertFalse(holder.current.isDirty)
    }

    @Test
    fun aSetupThatCannotBeOpenedLeavesTheEditorUnchanged() {
        runSuspend { store.write("setup-bad", "{ damaged") }
        val holder = holder()
        val before = holder.current.setup
        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(SetupId("bad"), "Bad"))

        assertEquals(before, holder.current.setup)
        assertEquals("Could not open “Bad”: the file is damaged (it is not valid JSON).", lastMessage(holder))
        assertNotNull(holder.current.library, "the library stays open")
    }

    // --- Rename ------------------------------------------------------------------------------

    @Test
    fun renameRenamesTheSavedSetup() {
        val mine = saved("mine", "Mine")
        val holder = holder()
        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.RenameRequested(summary("mine", "Mine")),
        )
        assertEquals(summary("mine", "Mine"), holder.library.renaming)

        holder.click(BoardAction.RenameConfirmed("  Renamed  "))

        assertNull(holder.library.renaming)
        assertEquals(mine.copy(name = "Renamed"), stored("mine"))
        assertEquals(listOf("Renamed"), holder.names())
        assertEquals("Renamed to “Renamed”.", lastMessage(holder))
    }

    @Test
    fun renamingTheOpenSetupUpdatesTheEditorAndKeepsUnsavedEdits() {
        val mine = saved("mine", "Mine")
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(mine.id, mine.name))
        holder.edit()
        val edited = holder.current.setup

        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.RenameRequested(summary("mine", "Mine")),
            BoardAction.RenameConfirmed("New name"),
        )

        assertEquals(edited.copy(name = "New name"), holder.current.setup)
        assertTrue(holder.current.isDirty, "the edits are still unsaved")
        assertEquals(mine.copy(name = "New name"), stored("mine"), "only the name of the saved version changed")
    }

    @Test
    fun renamingAnotherSetupLeavesTheEditorAlone() {
        saved("other", "Other")
        val holder = holder()
        val before = holder.current.setup
        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.RenameRequested(summary("other", "Other")),
            BoardAction.RenameConfirmed("Changed"),
        )
        assertEquals(before, holder.current.setup)
    }

    @Test
    fun blankRenameIsRefusedAndTheDialogStaysOpen() {
        saved("mine", "Mine")
        val holder = holder()
        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.RenameRequested(summary("mine", "Mine")),
            BoardAction.RenameConfirmed("  "),
        )
        assertNotNull(holder.library.renaming)
        assertEquals("Mine", stored("mine")?.name)
    }

    @Test
    fun cancelledRenameChangesNothing() {
        saved("mine", "Mine")
        val holder = holder()
        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.RenameRequested(summary("mine", "Mine")),
            BoardAction.RenameCancelled,
        )
        assertNull(holder.library.renaming)
        assertEquals("Mine", stored("mine")?.name)
    }

    // --- Duplicate ---------------------------------------------------------------------------

    @Test
    fun duplicateSavesACopyUnderANewIdWithoutOpeningIt() {
        val mine = saved("mine", "Mine", x = 4)
        val holder = holder()
        val before = holder.current.setup

        holder.click(BoardAction.LibraryRequested, BoardAction.DuplicateRequested(summary("mine", "Mine")))

        assertEquals(listOf("Mine", "Mine (copy)"), holder.names())
        val copyId = holder.library.setups.single { it.name == "Mine (copy)" }.id
        assertNotEquals(mine.id, copyId)
        assertEquals(mine.copy(id = copyId, name = "Mine (copy)"), stored(copyId.value))
        assertEquals(mine, stored("mine"), "the original is unchanged")
        assertEquals(before, holder.current.setup, "the copy is not opened")
    }

    // --- Delete ------------------------------------------------------------------------------

    @Test
    fun deleteAsksFirst() {
        saved("mine", "Mine")
        val holder = holder()
        val item = LibraryItem.Setup(summary("mine", "Mine"))
        holder.click(BoardAction.LibraryRequested, BoardAction.DeleteRequested(item))

        assertEquals(item, holder.library.deleting)
        assertNotNull(stored("mine"))
    }

    @Test
    fun confirmedDeleteRemovesTheSetup() {
        saved("mine", "Mine")
        saved("other", "Other")
        val holder = holder()
        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.DeleteRequested(LibraryItem.Setup(summary("mine", "Mine"))),
            BoardAction.DeleteConfirmed,
        )

        assertNull(stored("mine"))
        assertEquals(listOf("Other"), holder.names())
        assertNull(holder.library.deleting)
        assertEquals("Deleted “Mine”.", lastMessage(holder))
    }

    @Test
    fun cancelledDeleteKeepsTheSetup() {
        saved("mine", "Mine")
        val holder = holder()
        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.DeleteRequested(LibraryItem.Setup(summary("mine", "Mine"))),
            BoardAction.DeleteCancelled,
        )
        assertNotNull(stored("mine"))
        assertNull(holder.library.deleting)
    }

    @Test
    fun deletingTheOpenSetupKeepsItsContentAsNotSaved() {
        val mine = saved("mine", "Mine", x = 6)
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(mine.id, mine.name))

        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.DeleteRequested(LibraryItem.Setup(summary("mine", "Mine"))),
            BoardAction.DeleteConfirmed,
        )

        assertEquals(mine, holder.current.setup, "the content stays in the editor")
        assertFalse(holder.current.isInLibrary)
        assertNull(lastOpened())

        // Save now asks for a name, since the setup is no longer in the library.
        holder.click(BoardAction.LibraryClosed, BoardAction.SaveRequested)
        assertNotNull(holder.current.saveAsDialog)
    }

    @Test
    fun unreadableEntryCanBeDeleted() {
        runSuspend { store.write("setup-bad", "{ damaged") }
        val holder = holder()
        val entry = UnreadableEntry("setup-bad", SetupDecodeFailure.NotJson)
        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.DeleteRequested(LibraryItem.Unreadable(entry)),
            BoardAction.DeleteConfirmed,
        )
        assertEquals(emptyList(), holder.library.unreadable)
        assertEquals("Deleted the unreadable entry.", lastMessage(holder))
    }

    @Test
    fun aFailedDeleteSaysWhyAndKeepsTheSetup() {
        class NoDeletes(private val delegate: TextStore = InMemoryTextStore()) : TextStore by delegate {
            override suspend fun delete(key: String) = throw StorageException("Read-only")
        }
        val failing = StoredSetupRepository(NoDeletes())
        runSuspend { failing.save(SampleSetups.initial.copy(id = SetupId("mine"), name = "Mine")) }
        val holder = testHolder(repository = failing)

        holder.click(
            BoardAction.LibraryRequested,
            BoardAction.DeleteRequested(LibraryItem.Setup(summary("mine", "Mine"))),
            BoardAction.DeleteConfirmed,
        )

        assertEquals(listOf("Mine"), holder.names())
        assertEquals(
            "Could not delete “Mine”: the storage reported an error (Read-only).",
            lastMessage(holder),
        )
    }
}
