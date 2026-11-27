# Blood Bowl One Turn Touchdown Trainer (BBOTTD Trainer)

A local-first tactical editor and training sandbox for Blood Bowl, built with Kotlin Multiplatform and
Compose Multiplatform.

The goal is to set up and study tactical positions on a Blood Bowl pitch, especially one-turn touchdown
(OTT) scenarios. The editor is a **free sandbox**: it does not enforce Blood Bowl rules such as movement,
tackle zones, blocks or dice. Optional rule-aware analysis may be added later, separately from the editor.

## Status

Early development. The tactical editor and saving setups (milestones 0–3) are complete; editing quality
(milestone 4: undo/redo, player details, keyboard and layout improvements) is next.

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
- Saving setups locally, a library to open, rename, duplicate and delete them, and exporting and importing
  setups as `.json` files. Everything stays on your device; there is no account or cloud sync.
- A light and a dark theme.

The first time, the app starts with the initial sample setup: all 32 players in their dugouts and the ball
off the pitch. After that it reopens the setup you saved or opened last.

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

## Saving and the library

The bar above the pitch shows the setup's name and whether it is saved. Saving and files are in the
**File** menu:

- **New** starts a new setup from the initial sample setup.
- **Save** saves the setup. A setup that has never been saved asks for a name first.
- **Save as…** saves a copy under a new name; you continue with the copy.
- **Library…** lists your saved setups. Click one to open it. Its **More** menu renames, duplicates,
  exports or deletes it (deleting asks first). Entries that cannot be read, such as damaged files, are
  listed with the reason and can be deleted.
- **Import…** adds a setup file to the library. It is always added as a new setup, so nothing in the
  library is replaced. Files that are not setup files, are damaged, or come from a newer version of the app
  are refused with the reason.
- **Export…** saves the setup on screen, including unsaved changes, as a `.json` file.

Nothing is saved automatically. Before something would lose unsaved changes (New, opening another setup,
or closing the desktop app) you are asked whether to save them. The web app shows the browser's own
"Leave site?" warning instead.

Where setups are kept:

| Platform | Location |
|---|---|
| Windows | `%APPDATA%\BBOTTD-trainer\saved_data` |
| macOS | `~/Library/Application Support/BBOTTD-trainer/saved_data` |
| Linux | `~/.local/share/BBOTTD-trainer/saved_data` (or under `$XDG_DATA_HOME`) |
| Android | The app's private storage |
| Web | The browser's local storage for the site |

The browser may clear its storage (for example when you clear site data), so export setups you want to
keep from the web app.

**Dark mode / Light mode** in the bar switches the theme. The app follows your system's setting until you
use it, and then remembers your choice.

## Planned features

- Undo/redo, player details, keyboard shortcuts and a better layout on small screens.
- Later: training scenarios with instructions and annotations, and optional rule-aware analysis.

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
| Android | Android | Built and host tests run after every change; touch input checked by hand on an emulator. Saving and the file picker are built but not run. |

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

Kotlin 2.4.21, Compose Multiplatform 1.12.2, kotlinx.serialization 1.13.0 (setup files), Gradle 9.3.1 and
Android Gradle Plugin 9.1.1. Exact versions are in `gradle/libs.versions.toml`.
