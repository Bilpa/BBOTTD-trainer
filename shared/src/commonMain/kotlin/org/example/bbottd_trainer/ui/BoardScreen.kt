package org.example.bbottd_trainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.domain.Team
import org.example.bbottd_trainer.presentation.BoardAction
import org.example.bbottd_trainer.presentation.BoardUiState
import org.example.bbottd_trainer.presentation.dugoutSections
import org.example.bbottd_trainer.presentation.pitchTokens
import org.example.bbottd_trainer.ui.ball.OffPitchBallArea
import org.example.bbottd_trainer.ui.dugout.Dugout
import org.example.bbottd_trainer.ui.pitch.Pitch

private val DugoutWidth = 168.dp

/**
 * The board editor screen: the home dugout left of the pitch and the away dugout right of it.
 * Renders [state] and reports user actions through [onAction]; it never changes state itself.
 */
@Composable
fun BoardScreen(
    state: BoardUiState,
    onAction: (BoardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val setup = state.setup

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = setup.name,
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(
                onClick = { onAction(BoardAction.SetDugoutsVisible(!state.dugoutsVisible)) },
            ) {
                Text(if (state.dugoutsVisible) "Hide dugouts" else "Show dugouts")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.dugoutsVisible) {
                TeamDugout(state, setup.home)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Pitch(
                    tokens = setup.pitchTokens(),
                    ballPosition = setup.ball.position,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                OffPitchBallArea(ballIsOffPitch = !setup.ball.isOnPitch)
            }
            if (state.dugoutsVisible) {
                TeamDugout(state, setup.away)
            }
        }
    }
}

@Composable
private fun TeamDugout(
    state: BoardUiState,
    team: Team,
) {
    Dugout(
        teamName = team.name,
        teamColor = team.color,
        sections = state.setup.dugoutSections(team.id),
        modifier = Modifier
            .width(DugoutWidth)
            .fillMaxHeight(),
    )
}
