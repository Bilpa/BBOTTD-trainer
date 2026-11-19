package org.example.bbottd_trainer.data

/**
 * A minimal store of named texts: the only part of setup storage that differs per platform
 * (a directory of files on desktop and Android, `localStorage` on the web).
 *
 * Keys are chosen by [StoredSetupRepository] and always match [TextStore.isValidKey], so stores
 * can use them directly as file names or storage keys.
 *
 * Every operation may throw [StorageException] when the platform storage fails (for example a
 * full disk or browser storage quota). Implementations convert platform errors to it.
 */
interface TextStore {
    /** All keys that currently have a text. */
    suspend fun keys(): Set<String>

    /** The text stored under [key], or `null` if there is none. */
    suspend fun read(key: String): String?

    /** Stores [text] under [key], replacing any earlier text. Must not leave a partial text. */
    suspend fun write(key: String, text: String)

    /** Removes the text under [key]. Does nothing if there is none. */
    suspend fun delete(key: String)

    companion object {
        private val validKey = Regex("[a-z0-9_-]{1,200}")

        /**
         * Keys are lowercase letters, digits, `_` and `-`, at most 200 characters: safe as file
         * names on every platform, including case-insensitive file systems.
         */
        fun isValidKey(key: String): Boolean = validKey.matches(key)
    }
}

/** The platform storage failed; [message] describes the platform error. */
class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** A [TextStore] that keeps texts in memory only. For tests, and as a fallback without storage. */
class InMemoryTextStore : TextStore {
    private val texts = mutableMapOf<String, String>()

    override suspend fun keys(): Set<String> = texts.keys.toSet()

    override suspend fun read(key: String): String? = texts[key]

    override suspend fun write(key: String, text: String) {
        require(TextStore.isValidKey(key)) { "Invalid key: $key" }
        texts[key] = text
    }

    override suspend fun delete(key: String) {
        texts.remove(key)
    }
}
