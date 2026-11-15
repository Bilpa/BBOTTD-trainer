package org.example.bbottd_trainer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SampleSetupsTest {

    private val setup = SampleSetups.initial

    @Test
    fun hasFixedSetupId() {
        assertEquals(SetupId("sample-initial"), setup.id)
    }

    @Test
    fun homeIsBlueAndAwayIsRed() {
        assertEquals("Home", setup.home.name)
        assertEquals(TeamColor.BLUE, setup.home.color)
        assertEquals("Away", setup.away.name)
        assertEquals(TeamColor.RED, setup.away.color)
    }

    @Test
    fun eachTeamHas16PlayersNumbered1To16() {
        for (team in listOf(setup.home, setup.away)) {
            assertEquals(16, team.players.size, team.name)
            assertEquals((1..16).toList(), team.players.map { it.number }, team.name)
        }
    }

    @Test
    fun playersHaveFixedIdsAndGenericNames() {
        assertEquals((1..16).map { PlayerId("home-$it") }, setup.home.players.map { it.id })
        assertEquals((1..16).map { PlayerId("away-$it") }, setup.away.players.map { it.id })
        assertEquals((1..16).map { "Home Player $it" }, setup.home.players.map { it.name })
        assertEquals((1..16).map { "Away Player $it" }, setup.away.players.map { it.name })
    }

    @Test
    fun allPlayersStartInTheirDugoutAsReserves() {
        for (player in setup.players) {
            assertEquals(PlayerStatus.RESERVE, player.status, player.id.value)
            assertNull(player.position, player.id.value)
        }
    }

    @Test
    fun allPlayerIdsAreUnique() {
        assertEquals(32, setup.players.map { it.id }.toSet().size)
    }

    @Test
    fun ballStartsOffThePitch() {
        assertNull(setup.ball.position)
    }
}
