package org.example.bbottd_trainer.data

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Runs [block] for code that never really suspends, such as the in-memory store. Avoids a
 * dependency on kotlinx-coroutines-test, whose `runTest` would otherwise be needed in common tests.
 */
fun runSuspend(block: suspend () -> Unit) {
    var result: Result<Unit>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    (result ?: error("The test suspended; runSuspend only supports code that completes at once.")).getOrThrow()
}

/** Behaviour every [TextStore] must have. Each platform store has a subclass in its test source set. */
abstract class TextStoreContractTest {
    /** Runs [block] with a new, empty store. */
    abstract fun withStore(block: suspend (TextStore) -> Unit)

    @Test
    fun newStoreIsEmpty() = withStore { store ->
        assertEquals(emptySet(), store.keys())
        assertNull(store.read("missing"))
    }

    @Test
    fun writtenTextCanBeRead() = withStore { store ->
        store.write("setup-a", "text a")
        assertEquals("text a", store.read("setup-a"))
        assertEquals(setOf("setup-a"), store.keys())
    }

    @Test
    fun writeReplacesEarlierText() = withStore { store ->
        store.write("k", "first")
        store.write("k", "second")
        assertEquals("second", store.read("k"))
        assertEquals(setOf("k"), store.keys())
    }

    @Test
    fun keysListsEveryWrittenKey() = withStore { store ->
        store.write("a", "1")
        store.write("b-2", "2")
        store.write("c_3", "3")
        assertEquals(setOf("a", "b-2", "c_3"), store.keys())
    }

    @Test
    fun deleteRemovesText() = withStore { store ->
        store.write("a", "1")
        store.write("b", "2")
        store.delete("a")
        assertNull(store.read("a"))
        assertEquals(setOf("b"), store.keys())
    }

    @Test
    fun deletingMissingKeyDoesNothing() = withStore { store ->
        store.delete("missing")
        assertEquals(emptySet(), store.keys())
    }

    @Test
    fun textIsStoredExactly() = withStore { store ->
        val text = "{\n  \"name\": \"Ünïcode – ✓ 🏈\",\r\n\t\"empty\": \"\"\n}\n"
        store.write("k", text)
        assertEquals(text, store.read("k"))
        store.write("empty", "")
        assertEquals("", store.read("empty"))
    }

    @Test
    fun largeTextIsStored() = withStore { store ->
        val text = "x".repeat(200_000)
        store.write("big", text)
        assertEquals(text, store.read("big"))
    }

    @Test
    fun invalidKeyIsRejectedOnWrite() = withStore { store ->
        for (key in listOf("", "Upper", "a/b", "..", "a.json", "a b", "x".repeat(201))) {
            assertFailsWith<IllegalArgumentException>("Key \"$key\"") { store.write(key, "t") }
        }
        assertEquals(emptySet(), store.keys())
    }
}

class InMemoryTextStoreTest : TextStoreContractTest() {
    override fun withStore(block: suspend (TextStore) -> Unit) = runSuspend { block(InMemoryTextStore()) }
}

class TextStoreKeyTest {
    @Test
    fun validKeys() {
        for (key in listOf("a", "setup-0f3a", "last-opened", "a_b", "x".repeat(200))) {
            assertTrue(TextStore.isValidKey(key), key)
        }
    }
}
