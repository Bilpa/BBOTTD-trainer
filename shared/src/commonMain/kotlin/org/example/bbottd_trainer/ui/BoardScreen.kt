package org.example.bbottd_trainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.bbottd_trainer.presentation.BoardUiState
import org.example.bbottd_trainer.ui.pitch.Pitch

/** The board editor screen. Renders [state]; it does not change it. */
@Composable
fun BoardScreen(
    state: BoardUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.setup.name,
            style = MaterialTheme.typography.titleMedium,
        )
        Pitch(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}
