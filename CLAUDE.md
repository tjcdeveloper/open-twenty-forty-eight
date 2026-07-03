# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"Open Twenty Forty-Eight" — a free, open-source, ad-free 2048 clone for Android, built with Jetpack Compose. Primary target is the Google Pixel 10 Pro Fold: the UI adapts between the narrow cover screen and the near-square inner screen. The visual source of truth is `design_handoff_open_2048/README.md` (hi-fi design handoff with exact colors, dimensions, and copy — the directory is gitignored but present locally). Recreate designs pixel-faithfully; all colors are hardcoded design tokens, not Material dynamic color.

## Build & Test

No system Java is installed — use Android Studio's bundled JDK for every Gradle invocation:

```bash
export JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug              # build APK
./gradlew :app:testDebugUnitTest          # run all unit tests
./gradlew :app:testDebugUnitTest --tests "com.tjcdeveloper.open2048.game.GameEngineTest"  # one class
```

`local.properties` (gitignored) must contain `sdk.dir=$HOME/Library/Android/sdk`.

## Testing on the fold emulator

A `Pixel_10_Pro_Fold` AVD exists (`~/Library/Android/sdk/emulator/emulator -avd Pixel_10_Pro_Fold`). Gotchas learned the hard way:

- **Two displays.** Plain `screencap` fails with a "Multiple displays" warning polluting the PNG. Get IDs via `adb shell dumpsys SurfaceFlinger --display-id`, then `adb exec-out screencap -p -d <id>`. The inner display is 2076×2152; the cover is 1080×2364 @ 390dpi.
- **Fold state.** `adb shell cmd device_state state 0` (CLOSED) / `2` (OPENED) forces a persistent override that **pins the posture — the emulator's hinge toggle stops working** until you run `adb shell cmd device_state state reset`. Always reset after scripted fold tests. Check with `cmd device_state print-state`.
- Dark mode: `adb shell cmd uimode night yes|no`. Swipes: `adb shell input swipe x1 y1 x2 y2 120` on board coordinates.

## Architecture

Package root: `app/src/main/java/com/tjcdeveloper/open2048/`.

**`game/` — pure Kotlin, no Android deps (all unit tests live here).**
`GameEngine` operates on immutable `GameState` snapshots. Tiles carry stable `id`s so Compose can animate them (`key(tile.id)` + `animateDpAsState` offsets). Two non-obvious invariants:

- A merge keeps the surviving tile's id and marks it `justMerged`; the absorbed tile stays in `tiles` for one state flagged `consumed = true` so the UI can animate it sliding into the merge target. Consumed tiles are pruned on the next `slide()`. Anything that reads board occupancy must use `state.activeTiles`, not `state.tiles`.
- `MoveHistory` caps undo at 6 moves (a product requirement, tested in `MoveHistoryTest`); redo clears on every new move.

**`data/` — persistence.**
`GameStateCodec` serializes a state as `"size;score;v0,v1,..."` (tile ids are regenerated on decode — callers pass non-overlapping `idStart` ranges to keep Compose keys unique across undo/redo stacks). `GameRepository` stores the codec strings, settings, and per-grid-size best scores (`best_4`/`best_5`/`best_6`) in DataStore Preferences.

**`ui/` — single ViewModel, two adaptive layouts.**
`GameViewModel` (AndroidViewModel) is the only state holder: game state, undo/redo, theme, best scores. Every mutation calls `persist()`; state loads async in `init` and `MainActivity` gates rendering on `isLoaded`. Navigation is a plain `showSettings` boolean in `MainActivity` — no navigation library.

Layout is driven by `WindowWidthSizeClass`, not device type: `Compact` → stacked cover-screen layout (controls above board), anything wider → board-left/rail-right unfolded layout with grid-size chips. `MainActivity` declares `configChanges` for size/uiMode so fold and theme transitions recompose live without recreation.

Theming goes through `LocalOpenColors` (custom `CompositionLocal` in `ui/theme/Theme.kt`) with complete light and true-black dark palettes from the design handoff; MaterialTheme is only a thin wrapper for dialogs. Tile colors come from `tileColors(value, isDark)` — the dark theme uses a fully separate, dimmed ramp (same hues, lower brightness) so tiles aren't glaring on the black background. Theme preference LIGHT/DARK/SYSTEM lives in settings; SYSTEM follows `isSystemInDarkTheme()`.

Icons are hand-built `ImageVector` paths in `ui/theme/Icons.kt` (the redo icon is the undo "reply" icon mirrored via `graphicsLayer.scaleX = -1`). No bitmap assets anywhere.

## Conventions

- Avoid nested if statements — use early returns and `when`.
- Beware JVM signature clashes: a `var foo` property with a private setter already generates `setFoo`; name mutation functions `updateFoo(...)`.
