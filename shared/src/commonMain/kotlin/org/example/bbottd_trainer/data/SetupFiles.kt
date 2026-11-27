package org.example.bbottd_trainer.data

/** The outcome of letting the user export to, or import from, a file. */
sealed interface FileTransferResult<out T> {
    data class Done<out T>(val value: T) : FileTransferResult<T>

    /** The user closed the file dialog without choosing a file. */
    data object Cancelled : FileTransferResult<Nothing>

    /** The file could not be written or read; [detail] says why. */
    data class Failed(val detail: String) : FileTransferResult<Nothing>
}

/**
 * Setup files chosen by the user, for export and import. Each platform implements it with its
 * own file dialogs. The texts are setup files as written by [SetupCodec]; this interface does not
 * look at their content.
 *
 * Neither operation throws for file problems; they return [FileTransferResult.Failed].
 */
interface SetupFiles {
    /**
     * Lets the user save [text] as a file, suggesting [suggestedFileName]. On the web the file is
     * downloaded, and the browser decides where it goes, so `Done` means the download started.
     */
    suspend fun export(suggestedFileName: String, text: String): FileTransferResult<Unit>

    /**
     * Lets the user choose a file and returns its text. Files larger than [MAX_IMPORT_BYTES] are
     * refused, since a setup file is only a few KB.
     */
    suspend fun import(): FileTransferResult<String>

    companion object {
        /** Far larger than any setup file, but small enough to read into memory safely. */
        const val MAX_IMPORT_BYTES = 5L * 1024 * 1024

        /** The message for a file larger than [MAX_IMPORT_BYTES]. */
        const val TOO_LARGE_DETAIL = "the file is too large to be a setup file."
    }
}

/** [SetupFiles] for where no file dialogs are available, such as tests and previews. */
object UnavailableSetupFiles : SetupFiles {
    private const val DETAIL = "files cannot be exported or imported here."

    override suspend fun export(suggestedFileName: String, text: String): FileTransferResult<Unit> =
        FileTransferResult.Failed(DETAIL)

    override suspend fun import(): FileTransferResult<String> = FileTransferResult.Failed(DETAIL)
}

/**
 * A file name for exporting a setup called [setupName]: the name with characters that are not
 * allowed in file names on some system replaced by `_`, shortened to a sensible length, plus
 * `.json`. A name with nothing usable left becomes `setup.json`.
 */
fun exportFileName(setupName: String): String {
    val cleaned = setupName
        .map { if (it in FORBIDDEN_FILE_NAME_CHARACTERS || it.code < 32) '_' else it }
        .joinToString("")
        .trim()
        .trim('.', ' ')
        .take(MAX_FILE_NAME_BASE_LENGTH)
        .trim()
    val base = cleaned.ifEmpty { "setup" }
    return "$base.json"
}

/** Characters that Windows, macOS or Linux do not allow in file names. */
private const val FORBIDDEN_FILE_NAME_CHARACTERS = "<>:\"/\\|?*"

private const val MAX_FILE_NAME_BASE_LENGTH = 80
