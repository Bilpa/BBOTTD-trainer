package org.example.bbottd_trainer.data

import kotlinx.coroutines.CancellationException
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup

/**
 * A [SetupRepository] that keeps each setup as one setup file (see [SetupCodec]) in a [TextStore].
 *
 * Layout of the store:
 * - `setup-<id>`: one entry per setup. The setup ID is escaped into a valid key (see [entryKeyOf]).
 * - `last-opened`: the plain ID of the setup opened last.
 *
 * The setup file inside an entry is the source of truth for its ID; an entry whose content has a
 * different ID than its key is treated as unreadable.
 */
class StoredSetupRepository(private val store: TextStore) : SetupRepository {
    override suspend fun list(): StorageResult<SetupLibrary> =
        storageCall {
            val setups = mutableListOf<SetupSummary>()
            val unreadable = mutableListOf<UnreadableEntry>()
            for (key in store.keys().filter { it.startsWith(SETUP_KEY_PREFIX) }.sorted()) {
                val text = store.read(key) ?: continue // Deleted meanwhile.
                when (val result = decodeEntry(key, text)) {
                    is SetupDecodeResult.Success -> setups += SetupSummary(result.setup.id, result.setup.name)
                    is SetupDecodeResult.Failure -> unreadable += UnreadableEntry(key, result.failure)
                }
            }
            SetupLibrary(
                setups = setups.sortedWith(compareBy<SetupSummary> { it.name.lowercase() }.thenBy { it.id.value }),
                unreadable = unreadable,
            )
        }

    override suspend fun load(id: SetupId): StorageResult<TacticalSetup> {
        val key = entryKeyOf(id)
        val text = when (val read = storageCall { store.read(key) }) {
            is StorageResult.Failure -> return read
            is StorageResult.Success -> read.value
        } ?: return StorageResult.Failure(StorageFailure.NotFound)

        return when (val decoded = decodeEntry(key, text)) {
            is SetupDecodeResult.Success -> StorageResult.Success(decoded.setup)
            is SetupDecodeResult.Failure -> StorageResult.Failure(StorageFailure.Unreadable(decoded.failure))
        }
    }

    override suspend fun save(setup: TacticalSetup): StorageResult<Unit> =
        storageCall { store.write(entryKeyOf(setup.id), SetupCodec.encode(setup)) }

    override suspend fun delete(id: SetupId): StorageResult<Unit> =
        storageCall { store.delete(entryKeyOf(id)) }

    override suspend fun deleteEntry(entryKey: String): StorageResult<Unit> =
        storageCall {
            // Never other entries, such as the last-opened record.
            require(entryKey.startsWith(SETUP_KEY_PREFIX)) { "Not a setup entry: $entryKey" }
            store.delete(entryKey)
        }

    override suspend fun lastOpenedSetupId(): SetupId? =
        when (val read = storageCall { store.read(LAST_OPENED_KEY) }) {
            is StorageResult.Success -> read.value?.takeIf { it.isNotEmpty() }?.let(::SetupId)
            is StorageResult.Failure -> null
        }

    override suspend fun setLastOpenedSetupId(id: SetupId?): StorageResult<Unit> =
        storageCall {
            if (id == null) store.delete(LAST_OPENED_KEY) else store.write(LAST_OPENED_KEY, id.value)
        }

    /** Decodes a stored entry, rejecting one whose content belongs to a different key. */
    private fun decodeEntry(key: String, text: String): SetupDecodeResult {
        val decoded = SetupCodec.decode(text)
        if (decoded is SetupDecodeResult.Success && entryKeyOf(decoded.setup.id) != key) {
            return SetupDecodeResult.Failure(
                SetupDecodeFailure.InvalidContent(
                    "Entry $key contains setup ${decoded.setup.id.value}, which belongs to another entry.",
                ),
            )
        }
        return decoded
    }

    private inline fun <T> storageCall(block: () -> T): StorageResult<T> =
        try {
            StorageResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StorageResult.Failure(StorageFailure.StorageError(e.message ?: e::class.simpleName ?: "Unknown error"))
        }

    companion object {
        private const val SETUP_KEY_PREFIX = "setup-"
        private const val LAST_OPENED_KEY = "last-opened"

        /**
         * The store key for a setup. Setup IDs are opaque and may come from imported files, so
         * they are escaped byte by byte in UTF-8: lowercase letters, digits and `-` are kept, and
         * every other byte becomes `_` followed by two hex digits. The mapping is one-to-one, and
         * keeps uppercase and lowercase IDs apart on case-insensitive file systems. An ID too long
         * for a valid key cannot be stored; saving it fails with a storage error.
         */
        internal fun entryKeyOf(id: SetupId): String =
            buildString {
                append(SETUP_KEY_PREFIX)
                for (byte in id.value.encodeToByteArray()) {
                    val char = (byte.toInt() and 0xFF).toChar()
                    if (char in 'a'..'z' || char in '0'..'9' || char == '-') {
                        append(char)
                    } else {
                        append('_')
                        append((byte.toInt() and 0xFF).toString(16).padStart(2, '0'))
                    }
                }
            }
    }
}
