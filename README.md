# Blood Bowl One Turn Touchdown Trainer (BBOTTD Trainer)

A local-first tactical editor and training sandbox for Blood Bowl, built with Kotlin Multiplatform and
Compose Multiplatform.

The goal is to set up and study tactical positions on a Blood Bowl pitch, especially one-turn touchdown
(OTT) scenarios. The editor is a **free sandbox**: it does not enforce Blood Bowl rules such as movement,
tackle zones, blocks or dice. Optional rule-aware analysis may be added later, separately from the editor.

## Status

Early development; editing interactions (milestone 2) are in progress.

What works now:

- A 15 × 26 pitch with end zones, wide zones and the line of scrimmage.
- Home (blue) and away (red) teams with 16 players each, numbered 1–16. Every player token shows its team
  colour.
- A dugout for each team (home on the left of the pitch, away on the right), with sections for reserves,
  knocked out, casualties and sent off. The dugouts can be hidden; on Android they start hidden.
- A ball marker that can be placed on any cell, including a player's. While the ball is off the pitch it
  sits in an area below the pitch.
- Moving players and the ball by dragging and dropping them, or by selecting them and then clicking a
  destination (see below). Both work with a mouse and with touch.
- Player statuses: on the pitch a player is standing, prone or stunned; in the dugout they are a reserve,
  knocked out, a casualty or sent off. Prone players are shown darker; stunned players darker with a yellow
  border.

The app starts with the initial sample setup: all 32 players in their dugouts and the ball off the pitch.

## Using the editor

Moving players and the ball by dragging:

- Drag a player (on the pitch or in a dugout) or the ball onto a pitch cell, a section of the player's own
  dugout, or (for the ball) the area below the pitch. While dragging, valid places are highlighted and the
  place under the pointer shows a cyan border if the drop is allowed, or a red one if not. Dropping
  anywhere else cancels the drag.

Moving players and the ball by selecting and clicking:

1. Click a player (on the pitch or in a dugout) or the ball to select it. It gets a cyan ring, and the
   places it can go are highlighted.
2. Click where it should go:
   - a player: an empty pitch cell, or a section of their own team's dugout (which sets their status);
   - the ball: any pitch cell, including one with a player, or the area below the pitch to take it off.
3. A player moved to a pitch cell stays selected, so you can keep moving them. Clicking another player
   selects that player instead.
4. To deselect, click the selected player again, press Escape, right-click an empty pitch cell, or click
   outside the pitch.

A player coming from the dugout arrives on the pitch standing; a player moved within the pitch keeps their
status. A player standing on the same cell as the ball carries it with them when moved to another pitch
cell. If a move is not possible, for example onto another player or into the other team's dugout, a message
explains why and nothing changes. Both ways of moving follow the same rules.

Setting a player's status:

- Right-click a player (long-press on touch) to open their status menu. On the pitch it offers standing,
  prone and stunned; in the dugout it offers the four dugout sections.
- Double-click a player on the pitch to toggle between standing and prone; a stunned player stands up.
  Double-clicking a player in the dugout does nothing.

## Planned features

- Resetting to the sample setup.
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
| Android | Android | Built and host tests run after every change; touch input checked by hand on an emulator |

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
