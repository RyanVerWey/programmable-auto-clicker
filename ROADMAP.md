# Roadmap

The roadmap describes intended direction, not guaranteed delivery dates.
Priorities may change based on reliability, accessibility, and user feedback.

## v1.0 - Portable Foundation

Status: released

- Native Windows mouse input
- Native keyboard scan-code input
- Java Robot desktop fallback
- Keyboard, mouse, and combined actions
- Fixed, current, and randomized mouse targeting
- CPS and interval timing with live updates
- Start delay and multiple stop conditions
- Random timing, position jitter, and breaks
- Global hotkeys and top-left-corner fail-safe
- Local profile save/load
- Portable Windows package with bundled runtime
- Automated build and elapsed-timer regression test

## v1.1 - Distribution And Diagnostics

- Code-signed Windows releases
- Standard Windows installer alongside the portable ZIP
- In-app diagnostics for input backend, hotkey registration, and privilege level
- Copyable runtime status and error report
- Clearer first-run guidance and SmartScreen documentation
- Additional automated tests for timing, profiles, and validation

## v1.2 - Workflow Builder

- Ordered multi-action keyboard and mouse sequences
- Per-step delay, hold, repeat, and enable controls
- Drag-to-reorder action editor
- Named profile library with recent profiles
- Pause/resume separate from stop/reset
- Import/export validation with profile versioning

## v1.3 - Desktop Integration

- Optional system-tray mode
- Launch minimized and remember window position
- Configurable emergency-stop hotkey
- High-DPI and multi-monitor coordinate improvements
- Improved keyboard navigation and screen-reader labels
- Portable settings directory option

## Future Investigation

- Linux desktop support using a platform-specific input backend
- macOS support with explicit Accessibility permission handling
- Signed update manifests with user-controlled update checks
- A plugin boundary for new input backends

## Non-Goals

The project will not add:

- anti-cheat or security-control bypasses
- hidden process injection
- credential collection
- cloud tracking or mandatory accounts
- features designed to evade application policies

## Suggesting Work

Open a GitHub issue with:

- the workflow you are trying to automate
- Windows version and input mode
- expected and actual behavior
- a minimal profile or reproduction when safe to share
