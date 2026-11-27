package org.example.bbottd_trainer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException

/**
 * Desktop [SetupFiles], using the operating system's own file dialogs (through AWT's
 * [FileDialog]).
 *
 * @param owner the app window, so the dialogs open on top of it and block it while open.
 */
class DesktopSetupFiles(private val owner: () -> Frame?) : SetupFiles {
    override suspend fun export(suggestedFileName: String, text: String): FileTransferResult<Unit> {
        val chosen = chooseFile("Export setup", FileDialog.SAVE, suggestedFileName)
            ?: return FileTransferResult.Cancelled
        // The dialog asks before replacing the chosen file. If ".json" is added here, the file
        // with the extension is replaced without asking; users rarely remove the suggested one.
        val target = if (chosen.name.endsWith(JSON_EXTENSION, ignoreCase = true)) {
            chosen
        } else {
            File(chosen.parentFile, chosen.name + JSON_EXTENSION)
        }
        return io {
            target.writeText(text)
            FileTransferResult.Done(Unit)
        }
    }

    override suspend fun import(): FileTransferResult<String> {
        val chosen = chooseFile("Import setup", FileDialog.LOAD, "*$JSON_EXTENSION")
            ?: return FileTransferResult.Cancelled
        return io {
            if (chosen.length() > SetupFiles.MAX_IMPORT_BYTES) {
                FileTransferResult.Failed(SetupFiles.TOO_LARGE_DETAIL)
            } else {
                FileTransferResult.Done(chosen.readText())
            }
        }
    }

    /**
     * Shows a modal file dialog and returns the chosen file, or `null` if it was cancelled. AWT
     * dialogs must be shown on the UI thread, which blocks here until the dialog closes.
     */
    private suspend fun chooseFile(title: String, mode: Int, initialFile: String): File? =
        withContext(Dispatchers.Main) {
            val dialog = FileDialog(owner(), title, mode)
            // On Windows the "*.json" file name acts as the filter; elsewhere the filter does.
            dialog.file = initialFile
            if (mode == FileDialog.LOAD) {
                dialog.setFilenameFilter { _, name -> name.endsWith(JSON_EXTENSION, ignoreCase = true) }
            }
            dialog.isVisible = true
            val directory = dialog.directory
            val name = dialog.file
            dialog.dispose()
            if (directory == null || name == null) null else File(directory, name)
        }

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

    private companion object {
        const val JSON_EXTENSION = ".json"
    }
}
