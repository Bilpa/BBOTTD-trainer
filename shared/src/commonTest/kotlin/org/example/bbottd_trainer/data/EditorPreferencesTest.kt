package org.example.bbottd_trainer.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class EditorPreferencesTest {
    private val store = InMemoryTextStore()
    private val preferences = StoredEditorPreferences(store)

    @Test
    fun themeFollowsTheSystemUntilSet() = runSuspend {
        assertNull(preferences.darkTheme())
    }

    @Test
    fun themeChoiceIsRemembered() = runSuspend {
        preferences.setDarkTheme(true)
        assertEquals(true, StoredEditorPreferences(store).darkTheme())
        preferences.setDarkTheme(false)
        assertEquals(false, StoredEditorPreferences(store).darkTheme())
    }

    @Test
    fun themeCanGoBackToFollowingTheSystem() = runSuspend {
        preferences.setDarkTheme(true)
        preferences.setDarkTheme(null)
        assertNull(preferences.darkTheme())
    }

    @Test
    fun anUnknownStoredValueFollowsTheSystem() = runSuspend {
        store.write("pref-dark-theme", "maybe")
        assertNull(preferences.darkTheme())
    }

    @Test
    fun preferencesDoNotAppearInTheLibrary() = runSuspend {
        preferences.setDarkTheme(true)
        val library = (StoredSetupRepository(store).list() as StorageResult.Success).value
        assertEquals(SetupLibrary(emptyList(), emptyList()), library)
    }

    @Test
    fun storageFailuresAreReportedNotThrown() = runSuspend {
        val broken = object : TextStore by InMemoryTextStore() {
            override suspend fun read(key: String): String = throw StorageException("Disk error")
            override suspend fun write(key: String, text: String) = throw StorageException("Disk error")
        }
        val failing = StoredEditorPreferences(broken)
        assertNull(failing.darkTheme())
        assertIs<StorageResult.Failure>(failing.setDarkTheme(true))
    }
}
