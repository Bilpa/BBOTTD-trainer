package org.example.bbottd_trainer

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.example.bbottd_trainer.data.BrowserSetupFiles
import org.example.bbottd_trainer.data.LocalStorageTextStore
import org.example.bbottd_trainer.presentation.BoardStateHolder
import web.events.addEventListener
import web.window.BEFORE_UNLOAD
import web.window.BeforeUnloadEvent
import web.window.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val store = LocalStorageTextStore()
    val files = BrowserSetupFiles()

    // With unsaved changes, the browser asks "Leave site?" when the page is closed or reloaded.
    // Browsers only allow their own generic prompt, not the app's Save/Discard dialog. The
    // listener lives as long as the page, so it is registered once and reads the current holder.
    var stateHolder: BoardStateHolder? = null
    window.addEventListener(BeforeUnloadEvent.BEFORE_UNLOAD, { event: BeforeUnloadEvent ->
        if (stateHolder?.state?.value?.isDirty == true) event.preventDefault()
    })

    ComposeViewport {
        val holder = rememberBoardStateHolder(store, files)
        SideEffect { stateHolder = holder }
        App(holder)
    }
}
