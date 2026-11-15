package org.example.bbottd_trainer.domain

/**
 * Built-in sample setups. They use fixed, readable IDs so that reset is reproducible.
 */
object SampleSetups {
    const val PLAYERS_PER_TEAM = 16

    /**
     * The initial setup, and the one reset returns to: Home (blue) and Away (red), 16 players each
     * numbered 1–16 with generic names, all in their team's dugout, and the ball off the pitch.
     */
    val initial: TacticalSetup =
        TacticalSetup(
            id = SetupId("sample-initial"),
            name = "Initial setup",
            home = dugoutTeam(TeamId.HOME, name = "Home", color = TeamColor.BLUE, idPrefix = "home"),
            away = dugoutTeam(TeamId.AWAY, name = "Away", color = TeamColor.RED, idPrefix = "away"),
            ball = BallState(position = null),
        )

    private fun dugoutTeam(
        id: TeamId,
        name: String,
        color: TeamColor,
        idPrefix: String,
    ): Team =
        Team(
            id = id,
            name = name,
            color = color,
            players = (1..PLAYERS_PER_TEAM).map { number ->
                Player(
                    id = PlayerId("$idPrefix-$number"),
                    name = "$name Player $number",
                    number = number,
                    status = PlayerStatus.RESERVE,
                    position = null,
                )
            },
        )
}
