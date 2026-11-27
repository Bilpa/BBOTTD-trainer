package org.example.bbottd_trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.example.bbottd_trainer.data.InMemoryTextStore
import org.example.bbottd_trainer.data.StoredSetupRepository
import org.example.bbottd_trainer.data.androidSetupRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repository = androidSetupRepository(applicationContext)
        setContent {
            // Dugouts start hidden on Android because of limited screen space.
            App(rememberBoardStateHolder(repository, dugoutsVisibleByDefault = false))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    val repository = remember { StoredSetupRepository(InMemoryTextStore()) }
    App(rememberBoardStateHolder(repository, dugoutsVisibleByDefault = false))
}
