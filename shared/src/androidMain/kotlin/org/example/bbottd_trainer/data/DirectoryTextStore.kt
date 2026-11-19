package org.example.bbottd_trainer.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Android [TextStore]: one UTF-8 file `<key>.json` per key in [directory], which is created when
 * first written to.
 *
 * Writes go to a temporary file that is then renamed over the target (an atomic replace on
 * Android's file systems), so a crash or full storage never leaves a half-written setup. It uses
 * `java.io.File` because `java.nio.file` needs API 26 and the minimum is 24. (Desktop has its own
 * class of the same name.)
 */
class DirectoryTextStore(private val directory: File) : TextStore {
    override suspend fun keys(): Set<String> =
        io {
            directory.listFiles().orEmpty()
                .filter { it.isFile && it.name.endsWith(EXTENSION) }
                .map { it.name.removeSuffix(EXTENSION) }
                .filter { TextStore.isValidKey(it) }
                .toSet()
        }

    override suspend fun read(key: String): String? =
        io {
            if (!TextStore.isValidKey(key)) return@io null
            val file = fileOf(key)
            if (file.isFile) file.readText() else null
        }

    override suspend fun write(key: String, text: String) {
        require(TextStore.isValidKey(key)) { "Invalid key: $key" }
        io {
            if (!directory.isDirectory && !directory.mkdirs()) {
                throw IOException("Cannot create directory $directory")
            }
            val temporary = File.createTempFile("tmp-$key-", ".tmp", directory)
            try {
                temporary.writeText(text)
                val target = fileOf(key)
                // On Android, rename replaces the target atomically. The fallback is only for
                // file systems that refuse to rename over an existing file, such as Windows
                // when the host tests run there.
                val replaced = temporary.renameTo(target) || (target.delete() && temporary.renameTo(target))
                if (!replaced) throw IOException("Cannot replace $target")
            } finally {
                temporary.delete()
            }
        }
    }

    override suspend fun delete(key: String) {
        if (!TextStore.isValidKey(key)) return
        io {
            val file = fileOf(key)
            if (file.exists() && !file.delete()) throw IOException("Cannot delete $file")
        }
    }

    private fun fileOf(key: String): File = File(directory, key + EXTENSION)

    /** Runs file I/O off the main thread, converting I/O errors to [StorageException]. */
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

/** The setup repository of the Android app, in the app's private internal storage. */
fun androidSetupRepository(context: Context): SetupRepository =
    StoredSetupRepository(DirectoryTextStore(File(context.filesDir, "setups")))
