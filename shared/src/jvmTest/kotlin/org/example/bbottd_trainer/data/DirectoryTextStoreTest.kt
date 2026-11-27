package org.example.bbottd_trainer.data

import kotlinx.coroutines.runBlocking
import org.example.bbottd_trainer.domain.SampleSetups
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@OptIn(ExperimentalPathApi::class)
class DirectoryTextStoreTest : TextStoreContractTest() {
    private val root: Path = Files.createTempDirectory("bbottd-store-test")

    /** A directory that does not exist yet, to check that the store creates it. */
    private val directory: Path = root.resolve("nested").resolve("saved_data")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    override fun withStore(block: suspend (TextStore) -> Unit) = runBlocking { block(DirectoryTextStore(directory)) }

    @Test
    fun eachKeyIsOneJsonFile() = runBlocking {
        DirectoryTextStore(directory).write("setup-a", "content")
        assertEquals(listOf("setup-a.json"), directory.listDirectoryEntries().map { it.name })
        assertEquals("content", directory.resolve("setup-a.json").readText())
    }

    @Test
    fun noTemporaryFilesAreLeftBehind() = runBlocking {
        val store = DirectoryTextStore(directory)
        store.write("k", "one")
        store.write("k", "two")
        assertEquals(listOf("k.json"), directory.listDirectoryEntries().map { it.name })
    }

    @Test
    fun otherFilesInTheDirectoryAreIgnored() = runBlocking {
        directory.createDirectories()
        directory.resolve("notes.txt").writeText("x")
        directory.resolve("Upper.json").writeText("x")
        directory.resolve("leftover.tmp").writeText("x")
        directory.resolve("folder.json").createDirectories()
        assertEquals(emptySet(), DirectoryTextStore(directory).keys())
    }

    @Test
    fun textSurvivesANewStoreInstance() = runBlocking {
        // Stands in for an app restart: a new store over the same directory.
        DirectoryTextStore(directory).write("k", "kept")
        assertEquals("kept", DirectoryTextStore(directory).read("k"))
    }

    @Test
    fun repositoryRoundTripsThroughFiles() = runBlocking {
        val repository = StoredSetupRepository(DirectoryTextStore(directory))
        repository.save(SampleSetups.initial)
        repository.setLastOpenedSetupId(SampleSetups.initial.id)

        val restarted = StoredSetupRepository(DirectoryTextStore(directory))
        assertEquals(SampleSetups.initial, assertIs<StorageResult.Success<*>>(restarted.load(SampleSetups.initial.id)).value)
        assertEquals(SampleSetups.initial.id, restarted.lastOpenedSetupId())
    }

    @Test
    fun ioErrorsBecomeStorageExceptions() {
        // A regular file where the directory should be makes every write fail.
        val blocked = root.resolve("blocked")
        blocked.writeText("not a directory")
        val store = DirectoryTextStore(blocked)
        assertFailsWith<StorageException> { runBlocking { store.write("k", "text") } }
    }

    @Test
    fun ioErrorsAreReportedByTheRepository() = runBlocking<Unit> {
        val blocked = root.resolve("blocked")
        blocked.writeText("not a directory")
        val result = StoredSetupRepository(DirectoryTextStore(blocked)).save(SampleSetups.initial)
        assertIs<StorageFailure.StorageError>(assertIs<StorageResult.Failure>(result).failure)
    }
}

class DesktopDataDirectoryTest {
    @Test
    fun windowsUsesAppData() {
        val directory = desktopDataDirectory(
            osName = "Windows 11",
            environment = mapOf("APPDATA" to "C:\\Users\\u\\AppData\\Roaming"),
            userHome = "C:\\Users\\u",
        )
        assertEquals(Path("C:\\Users\\u\\AppData\\Roaming", "BBOTTD-trainer", "saved_data"), directory)
    }

    @Test
    fun windowsWithoutAppDataFallsBackToTheUsualPlace() {
        val directory = desktopDataDirectory(osName = "Windows 10", environment = emptyMap(), userHome = "home")
        assertEquals(Path("home", "AppData", "Roaming", "BBOTTD-trainer", "saved_data"), directory)
    }

    @Test
    fun macUsesApplicationSupport() {
        val directory = desktopDataDirectory(osName = "Mac OS X", environment = emptyMap(), userHome = "home")
        assertEquals(Path("home", "Library", "Application Support", "BBOTTD-trainer", "saved_data"), directory)
    }

    @Test
    fun linuxUsesXdgDataHome() {
        val directory = desktopDataDirectory(osName = "Linux", environment = mapOf("XDG_DATA_HOME" to "xdg"), userHome = "home")
        assertEquals(Path("xdg", "BBOTTD-trainer", "saved_data"), directory)
    }

    @Test
    fun linuxWithoutXdgDataHomeUsesLocalShare() {
        val directory = desktopDataDirectory(osName = "Linux", environment = emptyMap(), userHome = "home")
        assertEquals(Path("home", ".local", "share", "BBOTTD-trainer", "saved_data"), directory)
    }
}
