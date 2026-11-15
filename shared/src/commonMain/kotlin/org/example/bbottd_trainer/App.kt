package org.example.bbottd_trainer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Shared root composable used by the Android, desktop and web entry points.
 *
 * Placeholder until the tactical board is implemented in Milestone 1.
 */
@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "BBOTTD Trainer",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}
