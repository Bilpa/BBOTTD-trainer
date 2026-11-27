package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.data.InMemoryTextStore
import org.example.bbottd_trainer.data.StorageException
import org.example.bbottd_trainer.data.StorageResult
import org.example.bbottd_trainer.data.StoredSetupRepository
import org.example.bbottd_trainer.data.TextStore
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

/** Save, Save as, reopening at start, New and Close, as handled by [BoardStateHolder]. */
class SavingTest {
    private val store = InMemoryTextStore()
    private val repository = StoredSetupRepository(store)

    private fun holder() = testHolder(repository = repository)

    /** A small edit: home player 1 from the dugout onto the pitch. */
    private fun BoardStateHolder.edit(x: Int = 7, y: Int = 20) =
        click(BoardAction.DugoutPlayerClicked(PlayerId("home-1")), BoardAction.CellClicked(BoardPosition(x, y)))

    private fun BoardStateHolder.saveAs(name: String) =
        click(BoardAction.SaveAsRequested, BoardAction.SaveAsConfirmed(name))

    private fun stored(id: SetupId): TacticalSetup? {
        var result: TacticalSetup? = null
        runSuspend { result = (repository.load(id) as? StorageResult.Success)?.value }
        return result
    }

    private fun lastOpened(): SetupId? {
        var result: SetupId? = null
        runSuspend { result = repository.lastOpenedSetupId() }
        return result
    }

    private fun savedCount(): Int {
        var count = 0
        runSuspend { count = (repository.list() as StorageResult.Success).value.setups.size }
        return count
    }

    private fun lastMessage(holder: BoardStateHolder): String = assertNotNull(holder.current.message).text

    // --- Status at start ---------------------------------------------------------------------

    @Test
    fun withNothingSavedTheInitialSetupIsOpenAndNotInTheLibrary() {
        val state = holder().current
        assertEquals(SampleSetups.initial, state.setup)
        assertFalse(state.isInLibrary)
        assertFalse(state.isLoading)
        assertNull(state.message)
    }

    // --- Save as -----------------------------------------------------------------------------

    @Test
    fun saveAsOpensTheDialogWithTheCurrentName() {
        val holder = holder()
        holder.click(BoardAction.SaveAsRequested)
        assertEquals(SaveAsDialog(initialName = SampleSetups.initial.name), holder.current.saveAsDialog)
    }

    @Test
    fun saveAsStoresTheSetupUnderANewIdAndName() {
        val holder = holder()
        holder.edit()

        holder.saveAs("  My OTT  ")

        val state = holder.current
        assertNull(state.saveAsDialog)
        assertEquals("My OTT", state.setup.name, "the name is trimmed")
        assertNotEquals(SampleSetups.initial.id, state.setup.id)
        assertTrue(state.isInLibrary)
        assertFalse(state.isDirty)
        assertFalse(state.isSaving)
        assertEquals(state.setup, stored(state.setup.id))
        assertEquals("Saved “My OTT”.", lastMessage(holder))
    }

    @Test
    fun saveAsOfALibrarySetupMakesACopyAndLeavesTheOriginal() {
        val holder = holder()
        holder.saveAs("Original")
        val original = holder.current.setup
        holder.edit()

        holder.saveAs("Copy")

        assertNotEquals(original.id, holder.current.setup.id)
        assertEquals(original, stored(original.id), "the original is unchanged")
        assertEquals(2, savedCount())
    }

    @Test
    fun blankNameIsRefusedAndTheDialogStaysOpen() {
        val holder = holder()
        holder.click(BoardAction.SaveAsRequested, BoardAction.SaveAsConfirmed("   "))

        assertNotNull(holder.current.saveAsDialog)
        assertFalse(holder.current.isInLibrary)
        assertEquals(0, savedCount())
        assertEquals("Enter a name for the setup.", lastMessage(holder))
    }

    @Test
    fun cancellingSaveAsSavesNothing() {
        val holder = holder()
        holder.edit()
        holder.click(BoardAction.SaveAsRequested, BoardAction.SaveAsCancelled)

        assertNull(holder.current.saveAsDialog)
        assertTrue(holder.current.isDirty)
        assertEquals(0, savedCount())
    }

    // --- Save --------------------------------------------------------------------------------

    @Test
    fun saveOfASetupNotInTheLibraryAsksForAName() {
        val holder = holder()
        holder.click(BoardAction.SaveRequested)
        assertNotNull(holder.current.saveAsDialog)
        assertEquals(0, savedCount())
    }

    @Test
    fun saveOfALibrarySetupOverwritesItWithoutAsking() {
        val holder = holder()
        holder.saveAs("Mine")
        val id = holder.current.setup.id
        holder.edit()
        assertTrue(holder.current.isDirty)

        holder.click(BoardAction.SaveRequested)

        assertNull(holder.current.saveAsDialog)
        assertFalse(holder.current.isDirty)
        assertEquals(id, holder.current.setup.id, "same library entry")
        assertEquals(holder.current.setup, stored(id))
        assertEquals(1, savedCount())
    }

    @Test
    fun editsAfterSavingMarkTheSetupDirty() {
        val holder = holder()
        holder.saveAs("Mine")
        holder.edit()
        assertTrue(holder.current.isDirty)
    }

    // --- Reopening at start ------------------------------------------------------------------

    @Test
    fun theSavedSetupIsReopenedAtTheNextStart() {
        val first = holder()
        first.edit()
        first.saveAs("Mine")
        val saved = first.current.setup

        val restarted = holder()

        assertEquals(saved, restarted.current.setup)
        assertTrue(restarted.current.isInLibrary)
        assertFalse(restarted.current.isDirty)
        assertNull(restarted.current.message)
    }

    @Test
    fun unsavedChangesAreNotReopened() {
        val first = holder()
        first.saveAs("Mine")
        val saved = first.current.setup
        first.edit()

        assertEquals(saved, holder().current.setup)
    }

    @Test
    fun aNewSetupIsNotReopenedAfterwards() {
        val first = holder()
        first.saveAs("Mine")
        first.click(BoardAction.NewRequested)

        assertNull(lastOpened())
        assertEquals(SampleSetups.initial, holder().current.setup)
    }

    @Test
    fun aDeletedLastSetupStartsWithTheSampleQuietly() {
        val first = holder()
        first.saveAs("Mine")
        runSuspend { repository.delete(first.current.setup.id) }

        val restarted = holder()

        assertEquals(SampleSetups.initial, restarted.current.setup)
        assertFalse(restarted.current.isInLibrary)
        assertNull(restarted.current.message)
        assertNull(lastOpened(), "the missing setup is forgotten")
    }

    @Test
    fun aDamagedLastSetupStartsWithTheSampleAndSaysWhy() {
        val first = holder()
        first.saveAs("Mine")
        val id = first.current.setup.id
        runSuspend { store.write("setup-${id.value}", "{ damaged") }

        val restarted = holder()

        assertEquals(SampleSetups.initial, restarted.current.setup)
        assertEquals(
            "Could not open the last setup: the file is damaged (it is not valid JSON).",
            lastMessage(restarted),
        )
        assertNull(lastOpened(), "it is not tried again at the next start")
    }

    @Test
    fun aSetupFromANewerAppVersionSaysSo() {
        runSuspend {
            store.write("setup-future", """{"format": "bbottd-setup", "formatVersion": 3}""")
            repository.setLastOpenedSetupId(SetupId("future"))
        }

        val message = lastMessage(holder())

        assertTrue("format version 3" in message, message)
    }

    // --- Unsaved-changes prompt with Save ------------------------------------------------------

    @Test
    fun savingFromThePromptSavesThenStartsTheNewSetup() {
        val holder = holder()
        holder.saveAs("Mine")
        holder.edit()
        val edited = holder.current.setup

        holder.click(BoardAction.NewRequested, BoardAction.UnsavedChangesSave)

        assertEquals(edited, stored(edited.id))
        assertEquals(SampleSetups.initial, holder.current.setup)
        assertFalse(holder.current.isDirty)
    }

    @Test
    fun savingANeverSavedSetupFromThePromptAsksForANameFirst() {
        val holder = holder()
        holder.edit()

        holder.click(BoardAction.NewRequested, BoardAction.UnsavedChangesSave)
        assertNotNull(holder.current.saveAsDialog)
        assertNull(holder.current.unsavedChangesPrompt)

        holder.click(BoardAction.SaveAsConfirmed("Kept"))

        assertEquals(1, savedCount())
        assertEquals(SampleSetups.initial, holder.current.setup, "then the new setup starts")
    }

    @Test
    fun cancellingTheNameDialogFromThePromptKeepsTheChanges() {
        val holder = holder()
        holder.edit()
        val edited = holder.current.setup

        holder.click(BoardAction.NewRequested, BoardAction.UnsavedChangesSave, BoardAction.SaveAsCancelled)

        assertEquals(edited, holder.current.setup)
        assertTrue(holder.current.isDirty)

        // A later Save as no longer continues with the cancelled New.
        holder.saveAs("Later")
        assertEquals(edited.copy(id = holder.current.setup.id, name = "Later"), holder.current.setup)
    }

    // --- Close -------------------------------------------------------------------------------

    @Test
    fun closeWithoutUnsavedChangesIsApprovedAtOnce() {
        val holder = holder()
        holder.click(BoardAction.CloseRequested)
        assertTrue(holder.current.closeApproved)
    }

    @Test
    fun closeWithUnsavedChangesAsksFirst() {
        val holder = holder()
        holder.edit()
        holder.click(BoardAction.CloseRequested)

        assertEquals(PendingAction.Close, holder.current.unsavedChangesPrompt)
        assertFalse(holder.current.closeApproved)
    }

    @Test
    fun discardingOnCloseApprovesIt() {
        val holder = holder()
        holder.edit()
        holder.click(BoardAction.CloseRequested, BoardAction.UnsavedChangesDiscard)
        assertTrue(holder.current.closeApproved)
        assertEquals(0, savedCount())
    }

    @Test
    fun cancellingOnCloseKeepsTheAppOpen() {
        val holder = holder()
        holder.edit()
        holder.click(BoardAction.CloseRequested, BoardAction.UnsavedChangesCancel)
        assertFalse(holder.current.closeApproved)
        assertTrue(holder.current.isDirty)
    }

    @Test
    fun savingOnCloseSavesThenApprovesIt() {
        val holder = holder()
        holder.edit()
        holder.click(BoardAction.CloseRequested, BoardAction.UnsavedChangesSave, BoardAction.SaveAsConfirmed("Before closing"))

        assertTrue(holder.current.closeApproved)
        assertEquals(1, savedCount())
        assertEquals(holder.current.setup.id, lastOpened())
    }

    // --- Storage failures --------------------------------------------------------------------

    private class BrokenWrites(private val delegate: TextStore = InMemoryTextStore()) : TextStore by delegate {
        override suspend fun write(key: String, text: String) = throw StorageException("Quota exceeded")
    }

    @Test
    fun aFailedSaveKeepsTheChangesAndSaysWhy() {
        val holder = testHolder(repository = StoredSetupRepository(BrokenWrites()))
        holder.edit()
        val edited = holder.current.setup

        holder.saveAs("Mine")

        assertEquals(edited, holder.current.setup)
        assertFalse(holder.current.isInLibrary)
        assertTrue(holder.current.isDirty)
        assertFalse(holder.current.isSaving)
        assertEquals(
            "Could not save “Mine”: the storage reported an error (Quota exceeded).",
            lastMessage(holder),
        )
    }

    @Test
    fun aFailedSaveOnCloseDoesNotClose() {
        val holder = testHolder(repository = StoredSetupRepository(BrokenWrites()))
        holder.edit()

        holder.click(BoardAction.CloseRequested, BoardAction.UnsavedChangesSave, BoardAction.SaveAsConfirmed("Mine"))

        assertFalse(holder.current.closeApproved)
        assertTrue(holder.current.isDirty)
    }

    @Test
    fun aFailedSaveOnNewDoesNotStartTheNewSetup() {
        val holder = testHolder(repository = StoredSetupRepository(BrokenWrites()))
        holder.edit()
        val edited = holder.current.setup

        holder.click(BoardAction.NewRequested, BoardAction.UnsavedChangesSave, BoardAction.SaveAsConfirmed("Mine"))

        assertEquals(edited, holder.current.setup)
    }

    @Test
    fun anUnreachableStoreStillStartsWithTheSample() {
        class Unreachable : TextStore {
            override suspend fun keys(): Set<String> = throw StorageException("Storage disabled")
            override suspend fun read(key: String): String = throw StorageException("Storage disabled")
            override suspend fun write(key: String, text: String) = throw StorageException("Storage disabled")
            override suspend fun delete(key: String) = throw StorageException("Storage disabled")
        }

        val holder = testHolder(repository = StoredSetupRepository(Unreachable()))

        assertEquals(SampleSetups.initial, holder.current.setup)
        assertFalse(holder.current.isLoading)
    }

    // --- While storage is busy ---------------------------------------------------------------

    @Test
    fun editsAreIgnoredWhileTheLastSetupIsOpening() {
        val gate = GatedRepository(repository)
        val holder = testHolder(repository = gate)
        assertTrue(holder.current.isLoading)

        holder.edit()
        holder.click(BoardAction.SetDugoutsVisible(false))

        assertEquals(SampleSetups.initial, holder.current.setup, "the edit was ignored")
        assertFalse(holder.current.dugoutsVisible, "view settings still work")

        gate.open()
        assertFalse(holder.current.isLoading)
    }

    @Test
    fun editsMadeWhileSavingStayUnsaved() {
        val gate = GatedRepository(repository, gateLoads = false)
        val holder = testHolder(repository = gate)
        holder.saveAs("Mine")
        gate.open()

        gate.closeGate()
        holder.click(BoardAction.SaveRequested)
        assertTrue(holder.current.isSaving)
        holder.click(BoardAction.SetPlayerStatus(PlayerId("home-1"), PlayerStatus.KNOCKED_OUT))
        gate.open()

        assertFalse(holder.current.isSaving)
        assertTrue(holder.current.isDirty, "the status change was made after the save started")
        assertEquals(PlayerStatus.RESERVE, stored(holder.current.setup.id)?.findPlayer(PlayerId("home-1"))?.status)
    }

    @Test
    fun saveNewAndCloseWaitWhileSaving() {
        val gate = GatedRepository(repository, gateLoads = false)
        val holder = testHolder(repository = gate)
        holder.saveAs("Mine")
        gate.open()
        holder.edit()

        gate.closeGate()
        holder.click(BoardAction.SaveRequested)
        holder.click(BoardAction.NewRequested, BoardAction.CloseRequested, BoardAction.SaveAsRequested)

        assertNull(holder.current.unsavedChangesPrompt)
        assertNull(holder.current.saveAsDialog)
        assertFalse(holder.current.closeApproved)
        gate.open()
    }
}
