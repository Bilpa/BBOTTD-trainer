package org.example.bbottd_trainer.data

import org.example.bbottd_trainer.domain.BallState
import org.example.bbottd_trainer.domain.BoardPosition
import org.example.bbottd_trainer.domain.SampleSetups
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup
import org.example.bbottd_trainer.domain.TeamId
import org.example.bbottd_trainer.domain.onFieldPlayer
import org.example.bbottd_trainer.domain.setup
import org.example.bbottd_trainer.domain.team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoredSetupRepositoryTest {
    private val store = InMemoryTextStore()
    private val repository = StoredSetupRepository(store)

    private fun named(id: String, name: String): TacticalSetup =
        setup().copy(id = SetupId(id), name = name)

    private fun <T> StorageResult<T>.value(): T = assertIs<StorageResult.Success<T>>(this).value

    private fun StorageResult<*>.failure(): StorageFailure = assertIs<StorageResult.Failure>(this).failure

    // Save and load.

    @Test
    fun savedSetupCanBeLoaded() = runSuspend {
        val setup = setup(
            home = team(TeamId.HOME, onFieldPlayer("h", x = 3, y = 4)),
            ball = BallState(BoardPosition(3, 4)),
        ).copy(id = SetupId("one"), notes = "Notes")
        repository.save(setup).value()
        assertEquals(setup, repository.load(SetupId("one")).value())
    }

    @Test
    fun saveReplacesSetupWithSameId() = runSuspend {
        repository.save(named("one", "First")).value()
        repository.save(named("one", "Renamed")).value()
        assertEquals("Renamed", repository.load(SetupId("one")).value().name)
        assertEquals(1, repository.list().value().setups.size)
    }

    @Test
    fun loadingMissingSetupIsNotFound() = runSuspend {
        assertEquals(StorageFailure.NotFound, repository.load(SetupId("missing")).failure())
    }

    @Test
    fun sampleSetupCanBeSavedAndLoaded() = runSuspend {
        repository.save(SampleSetups.initial).value()
        assertEquals(SampleSetups.initial, repository.load(SampleSetups.initial.id).value())
    }

    // Listing.

    @Test
    fun emptyRepositoryListsNothing() = runSuspend {
        assertEquals(SetupLibrary(emptyList(), emptyList()), repository.list().value())
    }

    @Test
    fun listIsSortedByNameIgnoringCase() = runSuspend {
        repository.save(named("1", "charlie")).value()
        repository.save(named("2", "Alpha")).value()
        repository.save(named("3", "bravo")).value()
        assertEquals(listOf("Alpha", "bravo", "charlie"), repository.list().value().setups.map { it.name })
    }

    @Test
    fun setupsWithSameNameAreOrderedById() = runSuspend {
        repository.save(named("b", "Same")).value()
        repository.save(named("a", "Same")).value()
        assertEquals(listOf(SetupId("a"), SetupId("b")), repository.list().value().setups.map { it.id })
    }

    @Test
    fun listIgnoresOtherEntries() = runSuspend {
        repository.save(named("one", "One")).value()
        repository.setLastOpenedSetupId(SetupId("one")).value()
        store.write("something-else", "not a setup")
        assertEquals(listOf(SetupSummary(SetupId("one"), "One")), repository.list().value().setups)
        assertEquals(emptyList(), repository.list().value().unreadable)
    }

    // Delete.

    @Test
    fun deletedSetupIsGone() = runSuspend {
        repository.save(named("one", "One")).value()
        repository.save(named("two", "Two")).value()
        repository.delete(SetupId("one")).value()
        assertEquals(StorageFailure.NotFound, repository.load(SetupId("one")).failure())
        assertEquals(listOf("Two"), repository.list().value().setups.map { it.name })
    }

    @Test
    fun deletingMissingSetupSucceeds() = runSuspend {
        repository.delete(SetupId("missing")).value()
    }

    // Unreadable entries.

    @Test
    fun damagedEntryIsListedAsUnreadable() = runSuspend {
        repository.save(named("good", "Good")).value()
        store.write("setup-bad", "{ this is not json")
        val library = repository.list().value()
        assertEquals(listOf("Good"), library.setups.map { it.name })
        assertEquals(listOf(UnreadableEntry("setup-bad", SetupDecodeFailure.NotJson)), library.unreadable)
    }

    @Test
    fun loadingDamagedEntryIsUnreadable() = runSuspend {
        store.write("setup-bad", "{ this is not json")
        assertEquals(
            StorageFailure.Unreadable(SetupDecodeFailure.NotJson),
            repository.load(SetupId("bad")).failure(),
        )
    }

    @Test
    fun newerVersionEntryIsUnreadableWithItsVersion() = runSuspend {
        store.write("setup-future", """{"format": "bbottd-setup", "formatVersion": 9}""")
        assertEquals(
            StorageFailure.Unreadable(SetupDecodeFailure.UnsupportedVersion(9)),
            repository.load(SetupId("future")).failure(),
        )
    }

    @Test
    fun entryWithAnotherSetupsIdIsUnreadable() = runSuspend {
        store.write("setup-x", SetupCodec.encode(named("y", "Y")))
        assertIs<SetupDecodeFailure.InvalidContent>(
            assertIs<StorageFailure.Unreadable>(repository.load(SetupId("x")).failure()).failure,
        )
        assertEquals(listOf("setup-x"), repository.list().value().unreadable.map { it.entryKey })
    }

    // IDs that are not plain lowercase.

    @Test
    fun idsWithAnyCharactersRoundTrip() = runSuspend {
        val ids = listOf("UPPER", "upper", "a/b", "../x", "a b", "ünï", "🏈", "a_2f", "con", "")
        for (id in ids) repository.save(named(id, "Name $id")).value()
        for (id in ids) assertEquals("Name $id", repository.load(SetupId(id)).value().name, "ID \"$id\"")
        assertEquals(ids.toSet(), repository.list().value().setups.map { it.id.value }.toSet())
    }

    @Test
    fun entryKeysAreValidAndDistinct() {
        val ids = listOf("UPPER", "upper", "a/b", "a_2fb", "a_2f", "ünï", "🏈", "😀", "", "-", "_")
        val keys = ids.map { StoredSetupRepository.entryKeyOf(SetupId(it)) }
        assertEquals(keys.size, keys.toSet().size, "Keys collide: $keys")
        keys.forEach { assertTrue(TextStore.isValidKey(it), it) }
    }

    @Test
    fun generatedIdKeysAreTheIdItself() {
        val id = SetupId.generate()
        assertEquals("setup-${id.value}", StoredSetupRepository.entryKeyOf(id))
    }

    @Test
    fun idTooLongForAKeyFailsWithStorageError() = runSuspend {
        val result = repository.save(named("x".repeat(300), "Long"))
        assertIs<StorageFailure.StorageError>(result.failure())
    }

    // Last opened.

    @Test
    fun lastOpenedIsNullAtFirst() = runSuspend {
        assertNull(repository.lastOpenedSetupId())
    }

    @Test
    fun lastOpenedIsRemembered() = runSuspend {
        repository.setLastOpenedSetupId(SetupId("Some ID/1")).value()
        assertEquals(SetupId("Some ID/1"), repository.lastOpenedSetupId())
        repository.setLastOpenedSetupId(SetupId("other")).value()
        assertEquals(SetupId("other"), repository.lastOpenedSetupId())
    }

    @Test
    fun lastOpenedCanBeCleared() = runSuspend {
        repository.setLastOpenedSetupId(SetupId("one")).value()
        repository.setLastOpenedSetupId(null).value()
        assertNull(repository.lastOpenedSetupId())
    }

    @Test
    fun lastOpenedIsNotAffectedByDeletingTheSetup() = runSuspend {
        // The editor decides what to do with a last-opened setup that is gone; the repository
        // just reports NotFound when it is loaded.
        repository.save(named("one", "One")).value()
        repository.setLastOpenedSetupId(SetupId("one")).value()
        repository.delete(SetupId("one")).value()
        assertEquals(SetupId("one"), repository.lastOpenedSetupId())
    }

    // Storage failures.

    @Test
    fun storageFailuresAreReportedNotThrown() = runSuspend {
        val failing = StoredSetupRepository(FailingTextStore())
        assertIs<StorageFailure.StorageError>(failing.list().failure())
        assertIs<StorageFailure.StorageError>(failing.load(SetupId("a")).failure())
        assertIs<StorageFailure.StorageError>(failing.save(named("a", "A")).failure())
        assertIs<StorageFailure.StorageError>(failing.delete(SetupId("a")).failure())
        assertIs<StorageFailure.StorageError>(failing.setLastOpenedSetupId(SetupId("a")).failure())
        assertNull(failing.lastOpenedSetupId())
    }

    @Test
    fun storageErrorDetailComesFromThePlatformError() = runSuspend {
        val failure = StoredSetupRepository(FailingTextStore()).save(named("a", "A")).failure()
        assertEquals(StorageFailure.StorageError("Disk full"), failure)
    }

    @Test
    fun failedSaveKeepsEarlierVersion() = runSuspend {
        val flaky = FlakyTextStore()
        val repository = StoredSetupRepository(flaky)
        repository.save(named("one", "Original")).value()
        flaky.failWrites = true
        assertIs<StorageFailure.StorageError>(repository.save(named("one", "Changed")).failure())
        assertEquals("Original", repository.load(SetupId("one")).value().name)
        assertNotEquals(0, flaky.keys().size)
    }
}

private class FailingTextStore : TextStore {
    override suspend fun keys(): Set<String> = throw StorageException("Disk full")

    override suspend fun read(key: String): String = throw StorageException("Disk full")

    override suspend fun write(key: String, text: String) = throw StorageException("Disk full")

    override suspend fun delete(key: String) = throw StorageException("Disk full")
}

private class FlakyTextStore : TextStore {
    private val delegate = InMemoryTextStore()
    var failWrites = false

    override suspend fun keys(): Set<String> = delegate.keys()

    override suspend fun read(key: String): String? = delegate.read(key)

    override suspend fun write(key: String, text: String) {
        if (failWrites) throw StorageException("Quota exceeded")
        delegate.write(key, text)
    }

    override suspend fun delete(key: String) = delegate.delete(key)
}

class DeleteEntryTest {
    private val store = InMemoryTextStore()
    private val repository = StoredSetupRepository(store)

    @Test
    fun unreadableEntryCanBeDeletedByKey() = runSuspend {
        store.write("setup-bad", "{ damaged")
        assertIs<StorageResult.Success<Unit>>(repository.deleteEntry("setup-bad"))
        assertEquals(emptyList(), (repository.list() as StorageResult.Success).value.unreadable)
    }

    @Test
    fun otherEntriesCannotBeDeletedByKey() = runSuspend {
        repository.setLastOpenedSetupId(SetupId("one"))
        assertIs<StorageResult.Failure>(repository.deleteEntry("last-opened"))
        assertEquals(SetupId("one"), repository.lastOpenedSetupId())
    }
}
