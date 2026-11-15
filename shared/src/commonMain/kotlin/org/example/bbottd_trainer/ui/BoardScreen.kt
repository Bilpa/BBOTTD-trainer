package org.example.bbottd_trainer.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.domain.Team
import org.example.bbottd_trainer.presentation.BoardAction
import org.example.bbottd_trainer.presentation.BoardUiState
import org.example.bbottd_trainer.presentation.Selection
import org.example.bbottd_trainer.presentation.dugoutSections
import org.example.bbottd_trainer.presentation.isCellTarget
import org.example.bbottd_trainer.presentation.isDugoutTarget
import org.example.bbottd_trainer.presentation.isOffPitchBallTarget
import org.example.bbottd_trainer.presentation.pitchTokens
import org.example.bbottd_trainer.ui.ball.OffPitchBallArea
import org.example.bbottd_trainer.ui.dugout.Dugout
import org.example.bbottd_trainer.ui.pitch.Pitch

private val DugoutWidth = 168.dp
private val SnackbarBottomOffset = 72.dp

/**
 * The board editor screen: the home dugout left of the pitch and the away dugout right of it.
 *
 * Renders [state] and reports clicks, taps and keys through [onAction]; it never changes state
 * itself. Messages from the state holder are shown in a snackbar. Escape clears the selection.
 */
@Composable
fun BoardScreen(
    state: BoardUiState,
    onAction: (BoardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val setup = state.setup
    val selectedPlayerId = (state.selection as? Selection.Player)?.playerId
    val ballSelected = state.selection == Selection.Ball

    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message.text)
            onAction(BoardAction.MessageShown(message.id))
        }
    }

    // Focus the screen so Escape works before anything has been clicked.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                    onAction(BoardAction.ClearSelection)
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            modifier = Modifier
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
                    TeamDugout(state, setup.home, onAction)
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
                        selectedPlayerId = selectedPlayerId,
                        ballSelected = ballSelected,
                        isTarget = state::isCellTarget,
                        onCellClick = { onAction(BoardAction.CellClicked(it)) },
                        onBallClick = { onAction(BoardAction.BallClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    OffPitchBallArea(
                        ballIsOffPitch = !setup.ball.isOnPitch,
                        ballSelected = ballSelected,
                        isTarget = state.isOffPitchBallTarget,
                        onClick = { onAction(BoardAction.OffPitchBallAreaClicked) },
                    )
                }
                if (state.dugoutsVisible) {
                    TeamDugout(state, setup.away, onAction)
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            // Raised so it does not cover the off-pitch ball area at the bottom.
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = SnackbarBottomOffset),
        )
    }
}

@Composable
private fun TeamDugout(
    state: BoardUiState,
    team: Team,
    onAction: (BoardAction) -> Unit,
) {
    Dugout(
        teamName = team.name,
        teamColor = team.color,
        sections = state.setup.dugoutSections(team.id),
        selectedPlayerId = (state.selection as? Selection.Player)?.playerId,
        isTarget = state.isDugoutTarget(team.id),
        onSectionClick = { status -> onAction(BoardAction.DugoutSectionClicked(team.id, status)) },
        onPlayerClick = { playerId -> onAction(BoardAction.DugoutPlayerClicked(playerId)) },
        modifier = Modifier
            .width(DugoutWidth)
            .fillMaxHeight(),
    )
}
