# Changelog

All notable changes to Open Twenty Forty-Eight are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and version numbers follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Each released version matches a Play Store release; the Play release notes are
a condensed form of the entry here.

## [1.0.1] - Unreleased

### Added

- Press back twice to exit: a first back press on the game screen shows a
  "Press back again to exit" message in the centre of the screen that fades
  after 3 seconds; a second press within 5 seconds closes the app

## [1.0.0] - 2026-07-04

Initial release.

### Added

- Classic 2048 gameplay with slide, merge, and spawn animations
- Grid sizes 4×4, 5×5, and 6×6, with a best score tracked per size
- Undo and redo with a six-move history
- Automatic saving of the game in progress, restored on relaunch
- Light theme and true-black OLED dark theme, with a System option that
  follows the device setting; dark mode uses a dimmed tile colour ramp
- Adaptive layouts for foldables: stacked layout on the cover screen,
  board-and-rail layout on the inner screen, switching live on fold/unfold
- Swipe input registered across the whole play area surrounding the board
- Confirmation dialog before New Game or a grid-size change discards a
  game in progress
- Settings screen with theme, grid size, and about/source link
