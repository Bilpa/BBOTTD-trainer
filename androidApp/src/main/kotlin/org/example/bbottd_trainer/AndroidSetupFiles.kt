package org.example.bbottd_trainer

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.bbottd_trainer.data.FileTransferResult
import org.example.bbottd_trainer.data.SetupFiles
import java.io.IOException

/**
 * Android [SetupFiles], using the system file picker (the Storage Access Framework), so the user
 * can choose any location, including cloud storage apps.
 *
 * The pickers are activity results, so their launchers must be registered in composition; this
 * creates them and returns the [SetupFiles] that uses them.
 */
@Composable
fun rememberAndroidSetupFiles(): SetupFiles {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val files = remember(resolver) { AndroidSetupFiles(resolver) }
    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(JSON_MIME_TYPE),
    ) { uri -> files.onPicked(uri) }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> files.onPicked(uri) }
    SideEffect {
        files.createLauncher = createLauncher
        files.openLauncher = openLauncher
    }
    return files
}

private const val JSON_MIME_TYPE = "application/json"

/**
 * The [SetupFiles] behind [rememberAndroidSetupFiles]. Only one picker is open at a time, since
 * the library is busy while an export or import runs.
 *
 * If the activity is recreated while a picker is open (for example when the screen rotates), the
 * result goes to the new activity, and the waiting export or import is never completed.
 */
private class AndroidSetupFiles(private val resolver: ContentResolver) : SetupFiles {
    var createLauncher: ActivityResultLauncher<String>? = null
    var openLauncher: ActivityResultLauncher<Array<String>>? = null
    private var pending: CompletableDeferred<Uri?>? = null

    fun onPicked(uri: Uri?) {
        pending?.complete(uri)
        pending = null
    }

    override suspend fun export(suggestedFileName: String, text: String): FileTransferResult<Unit> {
        val launcher = createLauncher ?: return notReady()
        val uri = pick { launcher.launch(suggestedFileName) } ?: return FileTransferResult.Cancelled
        return io {
            // "wt" truncates, so a shorter text does not leave the end of an older file behind.
            val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open the file for writing")
            stream.use { it.write(text.encodeToByteArray()) }
            FileTransferResult.Done(Unit)
        }
    }

    override suspend fun import(): FileTransferResult<String> {
        val launcher = openLauncher ?: return notReady()
        // Some file managers do not report .json as application/json, so plain text is allowed too.
        val uri = pick { launcher.launch(arrayOf(JSON_MIME_TYPE, "text/plain", "application/octet-stream")) }
            ?: return FileTransferResult.Cancelled
        return io {
            val stream = resolver.openInputStream(uri) ?: throw IOException("Cannot open the file")
            stream.use { input ->
                // Reads at most one byte more than allowed, to detect a file that is too large.
                val bytes = input.readNBytesCompat(SetupFiles.MAX_IMPORT_BYTES.toInt() + 1)
                if (bytes.size > SetupFiles.MAX_IMPORT_BYTES) {
                    FileTransferResult.Failed(SetupFiles.TOO_LARGE_DETAIL)
                } else {
                    FileTransferResult.Done(bytes.decodeToString())
                }
            }
        }
    }

    private suspend fun pick(launch: () -> Unit): Uri? {
        val result = CompletableDeferred<Uri?>()
        pending = result
        launch()
        return result.await()
    }

    private fun <T> notReady(): FileTransferResult<T> = FileTransferResult.Failed("the file picker is not ready yet.")

    private suspend fun <T> io(block: () -> FileTransferResult<T>): FileTransferResult<T> =
        withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: IOException) {
                FileTransferResult.Failed(e.message ?: e::class.simpleName ?: "Unknown error")
            } catch (e: SecurityException) {
                FileTransferResult.Failed(e.message ?: "Access denied")
            }
        }
}

/** `InputStream.readNBytes` needs API 33; this reads up to [limit] bytes on any version. */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(8 * 1024)
    while (buffer.size() < limit) {
        val read = read(chunk, 0, minOf(chunk.size, limit - buffer.size()))
        if (read < 0) break
        buffer.write(chunk, 0, read)
    }
    return buffer.toByteArray()
}
