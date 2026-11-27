package org.example.bbottd_trainer.data

import kotlinx.coroutines.CancellationException

/**
 * App settings that are remembered between runs. They are not part of any setup.
 *
 * Reading never fails: an unreadable or missing value is reported as not set.
 */
interface EditorPreferences {
    /** `true` for dark, `false` for light, `null` to follow the system setting. */
    suspend fun darkTheme(): Boolean?

    /** Remembers the theme choice; `null` goes back to following the system setting. */
    suspend fun setDarkTheme(dark: Boolean?): StorageResult<Unit>
}

/**
 * [EditorPreferences] kept in a [TextStore], next to the saved setups. Each setting has its own
 * key; none starts with `setup-`, so they never appear in the setup library.
 */
class StoredEditorPreferences(private val store: TextStore) : EditorPreferences {
    override suspend fun darkTheme(): Boolean? =
        try {
            when (store.read(DARK_THEME_KEY)) {
                "true" -> true
                "false" -> false
                else -> null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    override suspend fun setDarkTheme(dark: Boolean?): StorageResult<Unit> =
        try {
            if (dark == null) store.delete(DARK_THEME_KEY) else store.write(DARK_THEME_KEY, dark.toString())
            StorageResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            StorageResult.Failure(StorageFailure.StorageError(e.message ?: e::class.simpleName ?: "Unknown error"))
        }

    private companion object {
        const val DARK_THEME_KEY = "pref-dark-theme"
    }
}
