package org.example.bbottd_trainer.data

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Runs the store contract against the Android directory store. Host tests run on the development
 * machine's JVM, not on Android, so this checks the code but not Android's file system.
 */
class DirectoryTextStoreTest : TextStoreContractTest() {
    private val root: File = Files.createTempDirectory("bbottd-android-store-test").toFile()

    /** A directory that does not exist yet, to check that the store creates it. */
    private val directory = File(File(root, "nested"), "setups")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    override fun withStore(block: suspend (TextStore) -> Unit) = runBlocking { block(DirectoryTextStore(directory)) }

    @Test
    fun noTemporaryFilesAreLeftBehind() = runBlocking {
        val store = DirectoryTextStore(directory)
        store.write("k", "one")
        store.write("k", "two")
        assertEquals(listOf("k.json"), directory.list().orEmpty().toList())
    }

    @Test
    fun ioErrorsBecomeStorageExceptions() {
        val blocked = File(root, "blocked").apply { writeText("not a directory") }
        val store = DirectoryTextStore(blocked)
        assertFailsWith<StorageException> { runBlocking { store.write("k", "text") } }
    }
}
