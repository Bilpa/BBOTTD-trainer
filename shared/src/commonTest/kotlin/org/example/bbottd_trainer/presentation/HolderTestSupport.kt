package org.example.bbottd_trainer.presentation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.example.bbottd_trainer.data.EditorPreferences
import org.example.bbottd_trainer.data.InMemoryTextStore
import org.example.bbottd_trainer.data.StoredEditorPreferences
import org.example.bbottd_trainer.data.SetupRepository
import org.example.bbottd_trainer.data.StorageResult
import org.example.bbottd_trainer.data.StoredSetupRepository
import org.example.bbottd_trainer.domain.SetupId
import org.example.bbottd_trainer.domain.TacticalSetup

// Helpers shared by the state holder tests.

/**
 * A state holder whose storage work finishes before each call returns: the in-memory store never
 * suspends, and the unconfined dispatcher runs launched coroutines at once.
 */
fun testHolder(
    repository: SetupRepository = StoredSetupRepository(InMemoryTextStore()),
    initialSetup: TacticalSetup? = null,
    dugoutsVisible: Boolean = true,
    preferences: EditorPreferences = StoredEditorPreferences(InMemoryTextStore()),
): BoardStateHolder =
    BoardStateHolder(
        repository = repository,
        preferences = preferences,
        scope = CoroutineScope(Dispatchers.Unconfined),
        dugoutsVisible = dugoutsVisible,
        initialSetup = initialSetup,
    )

fun BoardStateHolder.click(vararg actions: BoardAction) = actions.forEach(::onAction)

val BoardStateHolder.current: BoardUiState get() = state.value

/**
 * Wraps a repository so that saves (and, with [gateLoads], reopening the last setup) wait until
 * [open] is called, to test what happens while storage is busy. The gate starts closed.
 */
class GatedRepository(
    private val delegate: SetupRepository,
    private val gateLoads: Boolean = true,
) : SetupRepository by delegate {
    private var gate = CompletableDeferred<Unit>()

    fun open() {
        gate.complete(Unit)
    }

    fun closeGate() {
        gate = CompletableDeferred()
    }

    override suspend fun lastOpenedSetupId(): SetupId? {
        if (gateLoads) gate.await()
        return delegate.lastOpenedSetupId()
    }

    override suspend fun save(setup: TacticalSetup): StorageResult<Unit> {
        gate.await()
        return delegate.save(setup)
    }
}
