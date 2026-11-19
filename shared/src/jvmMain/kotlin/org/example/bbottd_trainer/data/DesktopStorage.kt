package org.example.bbottd_trainer.data

import java.nio.file.Path
import kotlin.io.path.Path

private const val APP_DIRECTORY_NAME = "BBOTTD-trainer"

/**
 * The directory where the desktop app keeps saved setups, following each operating system's
 * convention for application data:
 * - Windows: `%APPDATA%\BBOTTD-trainer\setups`
 * - macOS: `~/Library/Application Support/BBOTTD-trainer/setups`
 * - Linux and others: `$XDG_DATA_HOME/BBOTTD-trainer/setups`, by default `~/.local/share/...`
 *
 * The parameters exist for tests; the defaults read the running system.
 */
fun desktopSetupDirectory(
    osName: String = System.getProperty("os.name").orEmpty(),
    environment: Map<String, String> = System.getenv(),
    userHome: String = System.getProperty("user.home").orEmpty(),
): Path {
    val os = osName.lowercase()
    val dataDirectory = when {
        os.startsWith("windows") ->
            environment["APPDATA"]?.takeIf { it.isNotBlank() }?.let { Path(it) }
                ?: Path(userHome, "AppData", "Roaming")
        os.startsWith("mac") -> Path(userHome, "Library", "Application Support")
        else ->
            environment["XDG_DATA_HOME"]?.takeIf { it.isNotBlank() }?.let { Path(it) }
                ?: Path(userHome, ".local", "share")
    }
    return dataDirectory.resolve(APP_DIRECTORY_NAME).resolve("setups")
}

/** The setup repository of the desktop app. */
fun desktopSetupRepository(): SetupRepository =
    StoredSetupRepository(DirectoryTextStore(desktopSetupDirectory()))
