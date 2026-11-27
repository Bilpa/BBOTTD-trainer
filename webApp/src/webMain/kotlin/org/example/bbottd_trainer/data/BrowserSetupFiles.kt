package org.example.bbottd_trainer.data

import js.uri.encodeURIComponent
import kotlinx.coroutines.suspendCancellableCoroutine
import web.blob.text
import web.dom.document
import web.events.CANCEL
import web.events.CHANGE
import web.events.Event
import web.events.addEventListener
import web.file.File
import web.html.HtmlTagName
import web.html.InputType
import web.html.file
import kotlin.coroutines.resume

/**
 * Web [SetupFiles], shared by the JS and Wasm targets.
 *
 * Export downloads the file; the browser decides where it goes (usually the Downloads folder) or
 * asks, depending on its settings, and does not tell the page whether the user cancelled. Import
 * uses the browser's file picker.
 */
class BrowserSetupFiles : SetupFiles {
    override suspend fun export(suggestedFileName: String, text: String): FileTransferResult<Unit> =
        try {
            val link = document.createElement(HtmlTagName.a)
            // A data URL needs no object URL to clean up; setup files are small enough for it.
            link.href = "data:application/json;charset=utf-8," + encodeURIComponent(text)
            link.download = suggestedFileName
            document.body.append(link)
            link.click()
            link.remove()
            FileTransferResult.Done(Unit)
        } catch (e: Throwable) {
            FileTransferResult.Failed("the browser refused the download (${e.message ?: e::class.simpleName}).")
        }

    override suspend fun import(): FileTransferResult<String> {
        val file = chooseFile() ?: return FileTransferResult.Cancelled
        if (file.size > SetupFiles.MAX_IMPORT_BYTES.toDouble()) {
            return FileTransferResult.Failed(SetupFiles.TOO_LARGE_DETAIL)
        }
        return try {
            FileTransferResult.Done(file.text())
        } catch (e: Throwable) {
            FileTransferResult.Failed("the file could not be read (${e.message ?: e::class.simpleName}).")
        }
    }

    /**
     * Opens the browser's file picker and returns the chosen file, or `null` if it was cancelled.
     * Browsers only open the picker shortly after a user click, which is how this is reached.
     */
    private suspend fun chooseFile(): File? =
        suspendCancellableCoroutine { continuation ->
            val input = document.createElement(HtmlTagName.input)
            input.type = InputType.file
            input.accept = ".json,application/json"
            input.addEventListener(Event.CHANGE, { _: Event ->
                if (continuation.isActive) continuation.resume(input.files?.item(0))
            })
            // Fired when the picker is closed without a choice (supported by current browsers).
            input.addEventListener(Event.CANCEL, { _: Event ->
                if (continuation.isActive) continuation.resume(null)
            })
            input.click()
        }
}
