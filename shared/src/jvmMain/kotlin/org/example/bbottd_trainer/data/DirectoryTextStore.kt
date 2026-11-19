package org.example.bbottd_trainer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Desktop [TextStore]: one UTF-8 file `<key>.json` per key in [directory], which is created when
 * first written to.
 *
 * Writes go to a temporary file that then replaces the target, so a crash or full disk never
 * leaves a half-written setup. (Android has its own class of the same name.)
 */
class DirectoryTextStore(private val directory: Path) : TextStore {
    override suspend fun keys(): Set<String> =
        io {
            if (!directory.exists()) return@io emptySet()
            directory.listDirectoryEntries("*$EXTENSION")
                .filter { it.isRegularFile() }
                .map { it.name.removeSuffix(EXTENSION) }
                .filter { TextStore.isValidKey(it) }
                .toSet()
        }

    override suspend fun read(key: String): String? =
        io {
            if (!TextStore.isValidKey(key)) return@io null
            val file = fileOf(key)
            if (file.isRegularFile()) file.readText() else null
        }

    override suspend fun write(key: String, text: String) {
        require(TextStore.isValidKey(key)) { "Invalid key: $key" }
        io {
            Files.createDirectories(directory)
            val temporary = Files.createTempFile(directory, "tmp-$key-", ".tmp")
            try {
                temporary.writeText(text)
                moveReplacing(temporary, fileOf(key))
            } finally {
                temporary.deleteIfExists()
            }
        }
    }

    override suspend fun delete(key: String) {
        if (!TextStore.isValidKey(key)) return
        io { fileOf(key).deleteIfExists() }
    }

    private fun fileOf(key: String): Path = directory.resolve(key + EXTENSION)

    private fun moveReplacing(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /** Runs file I/O off the UI thread, converting I/O errors to [StorageException]. */
    private suspend fun <T> io(block: () -> T): T =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: IOException) {
                throw StorageException("${e::class.simpleName}: ${e.message}", e)
            }
        }

    private companion object {
        const val EXTENSION = ".json"
    }
}
