package org.example.bbottd_trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Dugouts start hidden on Android because of limited screen space.
            App(dugoutsVisibleByDefault = false)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(dugoutsVisibleByDefault = false)
}