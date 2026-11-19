package org.example.bbottd_trainer.data

import web.storage.localStorage

/**
 * Web [TextStore] backed by the browser's `localStorage`, shared by the JS and Wasm targets.
 *
 * `localStorage` belongs to the whole site (origin), so every key is prefixed with [KEY_PREFIX] and
 * entries of other apps on the same origin (for example other projects on `localhost:8080`) are
 * ignored. The browser or the user may clear this storage at any time; export is the way to keep
 * setups safe.
 */
class LocalStorageTextStore : TextStore {
    override suspend fun keys(): Set<String> =
        browserStorage {
            (0 until localStorage.length)
                .mapNotNull { localStorage.key(it) }
                .filter { it.startsWith(KEY_PREFIX) }
                .map { it.removePrefix(KEY_PREFIX) }
                .filter { TextStore.isValidKey(it) }
                .toSet()
        }

    override suspend fun read(key: String): String? =
        browserStorage { localStorage.getItem(KEY_PREFIX + key) }

    override suspend fun write(key: String, text: String) {
        require(TextStore.isValidKey(key)) { "Invalid key: $key" }
        // A single setItem either stores the whole text or throws (for example when the quota is
        // exceeded), so no partial text is left behind.
        browserStorage { localStorage.setItem(KEY_PREFIX + key, text) }
    }

    override suspend fun delete(key: String) {
        browserStorage { localStorage.removeItem(KEY_PREFIX + key) }
    }

    /**
     * Converts browser errors (quota exceeded, storage disabled by privacy settings) to
     * [StorageException]. On Wasm, JavaScript errors only arrive as [Throwable], hence the broad
     * catch.
     */
    private inline fun <T> browserStorage(block: () -> T): T =
        try {
            block()
        } catch (e: Throwable) {
            throw StorageException("Browser storage failed: ${e.message ?: e::class.simpleName}", e)
        }

    private companion object {
        const val KEY_PREFIX = "bbottd-trainer/"
    }
}
