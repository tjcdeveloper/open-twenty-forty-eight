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
- TalkBack support: tiles announce their value and position, the four moves
  are available as accessibility actions on the board, game over / win and
  the exit prompt are announced, and controls expose proper roles and
  selection states

### Changed

- Saved data (game in progress, best scores, settings) is excluded from
  Android cloud backup and device transfer, matching the privacy policy's
  promise that no data ever leaves the device
- After the system splash screen, the app now starts directly in the saved
  theme instead of flashing the light palette before dark mode loads

### Fixed

- A corrupted or unreadable data file no longer crashes the app on launch;
  it recovers with a fresh game and default settings
- Moves made in the final moments before exiting could be lost; saves now
  always complete
- Navigating to and from Settings no longer counts toward the double back
  press that exits the app
- Tile numbers no longer clip at large system font sizes
- Opening the GitHub link no longer crashes on devices without a browser
- The Settings screen showed a hardcoded version number

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
