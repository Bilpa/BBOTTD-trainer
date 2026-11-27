package org.example.bbottd_trainer.data

import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup

/** A saved setup as shown in the library, without loading all of it into the editor. */
data class SetupSummary(
    val id: SetupId,
    val name: String,
)

/**
 * A stored entry that could not be read as a setup, for example a damaged file.
 *
 * @property entryKey the storage key of the entry, since its setup ID may be unknown.
 */
data class UnreadableEntry(
    val entryKey: String,
    val failure: SetupDecodeFailure,
)

/** The saved setups, and any stored entries that could not be read. */
data class SetupLibrary(
    val setups: List<SetupSummary>,
    val unreadable: List<UnreadableEntry>,
)

/** Why a storage operation failed. */
sealed interface StorageFailure {
    /** No setup is saved with this ID. */
    data object NotFound : StorageFailure

    /** A setup is stored with this ID, but cannot be read. */
    data class Unreadable(val failure: SetupDecodeFailure) : StorageFailure

    /** The platform storage itself failed; [detail] is a technical description. */
    data class StorageError(val detail: String) : StorageFailure
}

sealed interface StorageResult<out T> {
    data class Success<out T>(val value: T) : StorageResult<T>

    data class Failure(val failure: StorageFailure) : StorageResult<Nothing>
}

/**
 * Saved tactical setups, plus which setup was opened last.
 *
 * Operations never throw for storage or file problems; they return a [StorageResult.Failure].
 */
interface SetupRepository {
    /** All saved setups, sorted by name, and the stored entries that could not be read. */
    suspend fun list(): StorageResult<SetupLibrary>

    suspend fun load(id: SetupId): StorageResult<TacticalSetup>

    /** Saves [setup] under its ID, replacing any setup saved with the same ID. */
    suspend fun save(setup: TacticalSetup): StorageResult<Unit>

    /** Deletes the setup with this ID. Succeeds if there is none. */
    suspend fun delete(id: SetupId): StorageResult<Unit>

    /**
     * Deletes a stored entry that could not be read (see [SetupLibrary.unreadable]), by its
     * [UnreadableEntry.entryKey]. Only setup entries can be deleted this way.
     */
    suspend fun deleteEntry(entryKey: String): StorageResult<Unit>

    /** The setup opened last, or `null` if none is recorded or it cannot be read. */
    suspend fun lastOpenedSetupId(): SetupId?

    /** Records [id] as the setup opened last; `null` clears it. */
    suspend fun setLastOpenedSetupId(id: SetupId?): StorageResult<Unit>
}
