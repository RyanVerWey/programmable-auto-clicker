# Changelog

All notable changes are documented here. This project follows
[Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-07-23

### Added

- Configurable keyboard, mouse, and combined automation
- Native Windows `SendInput` backend for mouse actions
- Native keyboard scan-code input for improved game compatibility
- Java Robot desktop fallback
- Tap, hold, double, and burst action modes
- Current, fixed, and random-rectangle mouse targeting
- CPS and exact interval timing with live rate updates
- Timing jitter, position jitter, start delay, and random breaks
- Manual, duration, and action-count stop modes
- Global `F6`-`F12` start/stop hotkeys
- Top-left-corner fail-safe
- Local profile save and load
- Portable Windows x64 package with bundled Java runtime
- SHA-256 checksum generation for release archives

### Fixed

- Included key hold time inside the configured action cadence
- Applied CPS spinner edits immediately while automation is running
- Froze elapsed time when a run stops and reset it on the next run

[1.0.0]: https://github.com/RyanVerWey/programmable-auto-clicker/releases/tag/v1.0.0
