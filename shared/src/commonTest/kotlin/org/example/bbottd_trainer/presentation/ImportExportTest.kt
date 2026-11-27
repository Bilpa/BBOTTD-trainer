package org.example.bbottd_trainer.presentation

import org.example.bbottd_trainer.data.FileTransferResult
import org.example.bbottd_trainer.data.InMemoryTextStore
import org.example.bbottd_trainer.data.SetupCodec
import org.example.bbottd_trainer.data.SetupFiles
import org.example.bbottd_trainer.data.SetupSummary
import org.example.bbottd_trainer.data.StorageException
import org.example.bbottd_trainer.data.StorageResult
import org.example.bbottd_trainer.data.StoredSetupRepository
import org.example.bbottd_trainer.data.TextStore
import org.example.bbottd_trainer.data.runSuspend
import org.example.bbottd_trainer.domain.BallState
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

/** Exporting setups to files and importing them, through fake file dialogs. */
class ImportExportTest {
    /** File dialogs that record exports and return a prepared import result. */
    private class FakeSetupFiles : SetupFiles {
        val exported = mutableListOf<Pair<String, String>>()
        var exportResult: FileTransferResult<Unit> = FileTransferResult.Done(Unit)
        var importResult: FileTransferResult<String> = FileTransferResult.Cancelled

        override suspend fun export(suggestedFileName: String, text: String): FileTransferResult<Unit> {
            if (exportResult is FileTransferResult.Done) exported += suggestedFileName to text
            return exportResult
        }

        override suspend fun import(): FileTransferResult<String> = importResult
    }

    private val store = InMemoryTextStore()
    private val repository = StoredSetupRepository(store)
    private val files = FakeSetupFiles()

    /** A setup that uses several statuses and has the ball on a player. */
    private val mine: TacticalSetup = SampleSetups.initial.let { sample ->
        sample.copy(
            id = SetupId("mine"),
            name = "My OTT",
            notes = "Cage on the left",
            home = sample.home.copy(
                players = sample.home.players.map { player ->
                    when (player.id) {
                        PlayerId("home-1") -> player.copy(status = PlayerStatus.ON_FIELD, position = BoardPosition(3, 12))
                        PlayerId("home-2") -> player.copy(status = PlayerStatus.PRONE, position = BoardPosition(4, 13))
                        PlayerId("home-3") -> player.copy(status = PlayerStatus.CASUALTY)
                        else -> player
                    }
                },
            ),
            ball = BallState(BoardPosition(3, 12)),
        )
    }

    private fun holder(): BoardStateHolder {
        runSuspend { repository.save(mine) }
        return testHolder(repository = repository, files = files)
    }

    private fun savedSetups(): List<TacticalSetup> {
        val result = mutableListOf<TacticalSetup>()
        runSuspend {
            val library = (repository.list() as StorageResult.Success).value
            for (summary in library.setups) result += (repository.load(summary.id) as StorageResult.Success).value
        }
        return result
    }

    private fun lastMessage(holder: BoardStateHolder): String = assertNotNull(holder.current.message).text

    private val mineSummary = SetupSummary(SetupId("mine"), "My OTT")

    // --- Export ------------------------------------------------------------------------------

    @Test
    fun exportWritesTheSavedSetupAsASetupFile() {
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.ExportRequested(mineSummary))

        val (fileName, text) = files.exported.single()
        assertEquals("My OTT.json", fileName)
        assertEquals(SetupCodec.encode(mine), text)
        assertEquals("Exported “My OTT”.", lastMessage(holder))
        assertFalse(holder.current.library!!.isBusy)
    }

    @Test
    fun exportUsesTheSavedVersionNotUnsavedEdits() {
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(mine.id, mine.name))
        holder.click(BoardAction.PlayerDoubleClicked(PlayerId("home-1")))
        assertTrue(holder.current.isDirty)

        holder.click(BoardAction.LibraryRequested, BoardAction.ExportRequested(mineSummary))

        assertEquals(SetupCodec.encode(mine), files.exported.single().second)
    }

    @Test
    fun cancelledExportSaysNothing() {
        val holder = holder()
        files.exportResult = FileTransferResult.Cancelled
        holder.click(BoardAction.LibraryRequested, BoardAction.ExportRequested(mineSummary))
        assertNull(holder.current.message)
    }

    @Test
    fun failedExportSaysWhy() {
        val holder = holder()
        files.exportResult = FileTransferResult.Failed("the disk is full.")
        holder.click(BoardAction.LibraryRequested, BoardAction.ExportRequested(mineSummary))
        assertEquals("Could not export “My OTT”: the disk is full.", lastMessage(holder))
    }

    // --- Import ------------------------------------------------------------------------------

    @Test
    fun importAddsTheSetupToTheLibraryUnderANewId() {
        val holder = holder()
        val other = mine.copy(id = SetupId("from-elsewhere"), name = "Shared OTT")
        files.importResult = FileTransferResult.Done(SetupCodec.encode(other))

        holder.click(BoardAction.LibraryRequested, BoardAction.ImportRequested)

        val imported = savedSetups().single { it.name == "Shared OTT" }
        assertNotEquals(SetupId("from-elsewhere"), imported.id)
        assertEquals(other.copy(id = imported.id), imported, "everything but the ID is kept")
        assertEquals(listOf("My OTT", "Shared OTT"), holder.current.library!!.setups.map { it.name })
        assertEquals("Imported “Shared OTT” into the library.", lastMessage(holder))
    }

    @Test
    fun exportThenImportRoundTripsTheSetup() {
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.ExportRequested(mineSummary))
        files.importResult = FileTransferResult.Done(files.exported.single().second)

        holder.click(BoardAction.ImportRequested)

        val setups = savedSetups()
        assertEquals(2, setups.size, "the import is a copy; the original is not replaced")
        val copy = setups.single { it.id != mine.id }
        assertEquals(mine.copy(id = copy.id), copy)
        assertEquals(mine, setups.single { it.id == mine.id })
    }

    @Test
    fun importDoesNotChangeTheEditor() {
        val holder = holder()
        val before = holder.current
        files.importResult = FileTransferResult.Done(SetupCodec.encode(mine))

        holder.click(BoardAction.LibraryRequested, BoardAction.ImportRequested)

        assertEquals(before.setup, holder.current.setup)
        assertEquals(before.isDirty, holder.current.isDirty)
    }

    @Test
    fun cancelledImportChangesNothing() {
        val holder = holder()
        files.importResult = FileTransferResult.Cancelled
        holder.click(BoardAction.LibraryRequested, BoardAction.ImportRequested)
        assertEquals(1, savedSetups().size)
        assertNull(holder.current.message)
    }

    @Test
    fun malformedFileIsRefusedWithTheReason() {
        val holder = holder()
        files.importResult = FileTransferResult.Done("{ not json")
        holder.click(BoardAction.LibraryRequested, BoardAction.ImportRequested)

        assertEquals("Could not import the file: the file is damaged (it is not valid JSON).", lastMessage(holder))
        assertEquals(1, savedSetups().size)
    }

    @Test
    fun otherJsonFileIsRefused() {
        val holder = holder()
        files.importResult = FileTransferResult.Done("""{"name": "a shopping list"}""")
        holder.click(BoardAction.LibraryRequested, BoardAction.ImportRequested)
        assertEquals("Could not import the file: it is not a BBOTTD setup file.", lastMessage(holder))
    }

    @Test
    fun fileFromANewerVersionIsRefusedWithTheVersion() {
        val holder = holder()
        files.importResult = FileTransferResult.Done("""{"format": "bbottd-setup", "formatVersion": 4}""")
        holder.click(BoardAction.LibraryRequested, BoardAction.ImportRequested)
        val message = lastMessage(holder)
        assertTrue("format version 4" in message, message)
        assertEquals(1, savedSetups().size)
    }

    @Test
    fun fileThatBreaksADomainRuleIsRefused() {
        val holder = holder()
        // Two players on one cell.
        val text = SetupCodec.encode(mine).replace("\"x\": 4,\n", "\"x\": 3,\n").replace("\"y\": 13", "\"y\": 12")
        files.importResult = FileTransferResult.Done(text)
        holder.click(BoardAction.LibraryRequested, BoardAction.ImportRequested)
        assertTrue(lastMessage(holder).startsWith("Could not import the file: the file is damaged"), lastMessage(holder))
        assertEquals(1, savedSetups().size)
    }

    @Test
    fun aFileThatCannotBeReadSaysWhy() {
        val holder = holder()
        files.importResult = FileTransferResult.Failed(SetupFiles.TOO_LARGE_DETAIL)
        holder.click(BoardAction.LibraryRequested, BoardAction.ImportRequested)
        assertEquals("Could not import the file: the file is too large to be a setup file.", lastMessage(holder))
    }

    // --- From the File menu (library closed) --------------------------------------------------

    @Test
    fun importWorksWithTheLibraryClosed() {
        val holder = holder()
        files.importResult = FileTransferResult.Done(SetupCodec.encode(mine.copy(name = "From the menu")))

        holder.click(BoardAction.ImportRequested)

        assertNull(holder.current.library, "the library is not opened")
        assertTrue(savedSetups().any { it.name == "From the menu" })
        assertEquals("Imported “From the menu” into the library.", lastMessage(holder))
        assertFalse(holder.current.isTransferringFile)
    }

    @Test
    fun exportOfTheCurrentSetupIncludesUnsavedEdits() {
        val holder = holder()
        holder.click(BoardAction.LibraryRequested, BoardAction.OpenSetupRequested(mine.id, mine.name))
        holder.click(BoardAction.PlayerDoubleClicked(PlayerId("home-1")))
        val edited = holder.current.setup

        holder.click(BoardAction.ExportCurrentSetupRequested)

        assertEquals("My OTT.json" to SetupCodec.encode(edited), files.exported.single())
        assertTrue(holder.current.isDirty, "exporting does not save")
        assertEquals(mine, savedSetups().single(), "the saved version is unchanged")
    }

    @Test
    fun aSetupThatWasNeverSavedCanBeExported() {
        val holder = holder()
        holder.click(BoardAction.NewRequested)
        assertFalse(holder.current.isInLibrary)

        holder.click(BoardAction.ExportCurrentSetupRequested)

        assertEquals(SetupCodec.encode(SampleSetups.initial), files.exported.single().second)
    }

    @Test
    fun onlyOneFileTransferRunsAtATime() {
        val gated = GatedSetupFiles()
        runSuspend { repository.save(mine) }
        val holder = testHolder(repository = repository, files = gated)

        holder.click(BoardAction.ExportCurrentSetupRequested)
        assertTrue(holder.current.isTransferringFile)
        holder.click(BoardAction.ImportRequested, BoardAction.ExportCurrentSetupRequested)
        assertEquals(1, gated.calls, "the second and third requests are ignored")

        gated.finish()
        assertFalse(holder.current.isTransferringFile)
        holder.click(BoardAction.ImportRequested)
        assertEquals(2, gated.calls, "a new transfer can start once the first is done")
        gated.finish()
    }

    /** File dialogs that stay open until [finish] is called, then report a cancel. */
    private class GatedSetupFiles : SetupFiles {
        var calls = 0
        private var gate = kotlinx.coroutines.CompletableDeferred<Unit>()

        fun finish() {
            gate.complete(Unit)
            gate = kotlinx.coroutines.CompletableDeferred()
        }

        override suspend fun export(suggestedFileName: String, text: String): FileTransferResult<Unit> {
            calls++
            gate.await()
            return FileTransferResult.Cancelled
        }

        override suspend fun import(): FileTransferResult<String> {
            calls++
            gate.await()
            return FileTransferResult.Cancelled
        }
    }

    @Test
    fun aFailedSaveOfTheImportSaysWhy() {
        class NoWrites(private val delegate: TextStore = InMemoryTextStore()) : TextStore by delegate {
            override suspend fun write(key: String, text: String) = throw StorageException("Quota exceeded")
        }
        val holder = testHolder(repository = StoredSetupRepository(NoWrites()), files = files)
        files.importResult = FileTransferResult.Done(SetupCodec.encode(mine))

        holder.click(BoardAction.LibraryRequested, BoardAction.ImportRequested)

        assertEquals(
            "Could not import “My OTT”: the storage reported an error (Quota exceeded).",
            lastMessage(holder),
        )
    }
}
