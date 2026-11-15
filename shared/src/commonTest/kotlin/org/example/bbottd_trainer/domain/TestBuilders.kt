package org.example.bbottd_trainer.domain

// Small builders shared by the domain tests.

fun onFieldPlayer(id: String, x: Int, y: Int): Player =
    Player(
        id = PlayerId(id),
        name = id,
        number = null,
        status = PlayerStatus.ON_FIELD,
        position = BoardPosition(x, y),
    )

fun reservePlayer(id: String): Player =
    Player(
        id = PlayerId(id),
        name = id,
        number = null,
        status = PlayerStatus.RESERVE,
        position = null,
    )

fun team(id: TeamId, vararg players: Player): Team =
    Team(
        id = id,
        name = id.name,
        color = if (id == TeamId.HOME) TeamColor.BLUE else TeamColor.RED,
        players = players.toList(),
    )

fun setup(
    home: Team = team(TeamId.HOME),
    away: Team = team(TeamId.AWAY),
    ball: BallState = BallState(position = null),
): TacticalSetup =
    TacticalSetup(
        id = SetupId("test-setup"),
        name = "Test setup",
        home = home,
        away = away,
        ball = ball,
    )
