# Blood Bowl One Turn Touchdown Trainer (BBOTTD Trainer)

A local-first tactical editor and training sandbox for Blood Bowl, built with Kotlin Multiplatform and
Compose Multiplatform.

The goal is to set up and study tactical positions on a Blood Bowl pitch, especially one-turn touchdown
(OTT) scenarios. The editor is a **free sandbox**: it does not enforce Blood Bowl rules such as movement,
tackle zones, blocks or dice. Optional rule-aware analysis may be added later, separately from the editor.

## Status

Early development. The project foundation is in place, and the app currently shows a placeholder screen.
No editor functionality or tests are implemented yet.

## Planned features

- A 15 × 26 pitch with end zones, wide zones and the line of scrimmage.
- Home and away teams with 16 players each; every player token shows which team it belongs to.
- A dugout for each team (home on the left of the pitch, away on the right) for players who are not on
  the pitch: reserves, knocked out, casualties and sent off.
- Moving players by drag-and-drop or by selecting a player and then clicking/tapping a destination.
- A ball marker that moves independently of players.
- Saving and reopening setups locally. There is no backend, account or cloud sync.
- Later: undo/redo, training scenarios with instructions and annotations, and optional rule-aware analysis.

## Pitch conventions

- The pitch is 15 columns × 26 rows (390 cells), oriented north–south.
- Coordinates are zero-based: `x` is the column (`0..14`) and `y` is the row (`0..25`).
  `y = 0` is north (top of the screen) and `y = 25` is south.
- End zones are rows `y = 0` and `y = 25`. The home team defends the south end zone (`y = 25`).
- Wide zones are columns `x = 0..3` and `x = 11..14`.
- The line of scrimmage lies between rows `y = 12` and `y = 13`.

## Platforms

| Platform | Target | Verification |
|---|---|---|
| Desktop | JVM | Built, tested and run |
| Web (primary) | Kotlin/Wasm (`wasmJs`) | Built, tested and run in a browser |
| Web (fallback) | Kotlin/JS (`js`) | Compiled after every change; tested and run in a browser at each milestone |
| Android | Android | Build only: the app and host tests are built, but it is not run on a device or emulator yet |

## Project structure

| Module | Purpose |
|---|---|
| `shared` | Shared Kotlin code and Compose UI: domain models, application logic and tests |
| `desktopApp` | Desktop (JVM) entry point |
| `webApp` | Web entry point for both the Wasm and JS targets |
| `androidApp` | Android entry point |

Source sets in `shared`:

- `commonMain` and `commonTest`: code and tests for all targets.
- `jvmMain` and `jvmTest`: desktop.
- `wasmJsMain` and `jsMain`: web (Wasm and JS). `webTest` holds tests shared by both.
- `androidMain` and `androidHostTest`: Android.

`webApp` uses a single `webMain` source set for both web targets.

All code uses the package `org.example.bbottd_trainer`.

## Requirements

- JDK 21. The Gradle daemon toolchain (Azul Zulu 21) is downloaded automatically if it is missing.
- The Android SDK, only for building the Android app. Set `sdk.dir` in `local.properties`.
- Gradle comes with the wrapper; Node.js for the web targets is downloaded by the build.

## Running the apps

On Windows, use `gradlew.bat` instead of `./gradlew`.

- Desktop: `./gradlew :desktopApp:run`
- Web (Wasm, primary): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`, then open http://localhost:8080/
- Web (JS, fallback): `./gradlew :webApp:jsBrowserDevelopmentRun`
- Android (build only): `./gradlew :androidApp:assembleDebug`

## Running tests

- Shared and desktop: `./gradlew :shared:jvmTest`
- Web (Wasm): `./gradlew :shared:wasmJsTest`
- Web (JS): `./gradlew :shared:jsTest`
- Android host tests: `./gradlew :shared:testAndroidHostTest`

The web tests run in a headless browser, so Chrome must be installed.

## Built with

Kotlin 2.4.21, Compose Multiplatform 1.12.2, Gradle 9.3.1 and Android Gradle Plugin 9.1.1.
Exact versions are in `gradle/libs.versions.toml`.
