package org.example.bbottd_trainer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IdsTest {
    @Test
    fun generatedSetupIdsAreUnique() {
        val ids = List(1000) { SetupId.generate() }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun generatedSetupIdIsAUuid() {
        val uuidPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        val id = SetupId.generate().value
        assertTrue(uuidPattern.matches(id), "Not a UUID: $id")
    }
}
