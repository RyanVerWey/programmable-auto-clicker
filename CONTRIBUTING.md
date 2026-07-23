# Contributing

Bug reports, focused improvements, documentation fixes, and tests are welcome.

## Before Opening An Issue

1. Check existing issues and the roadmap.
2. Reproduce the problem with the latest release.
3. Record the Windows version, input mode, action type, and relevant settings.
4. Remove personal information from screenshots and profile files.

## Development Setup

Requirements:

- Windows 10 or 11
- JDK 21 or newer
- PowerShell 5.1 or PowerShell 7

```powershell
.\test.ps1
.\package.ps1
```

The first build downloads JNA 5.19.1 from Maven Central. Generated files under
`lib`, `out`, and `dist` are intentionally ignored.

## Pull Requests

- Keep each pull request focused on one behavior or closely related group.
- Explain user-visible behavior and the reason for the change.
- Add or update tests for timing, validation, and profile behavior.
- Run `.\test.ps1` before submitting.
- Do not include generated binaries, local profiles, or machine-specific files.

## Coding Notes

- Use Java 21-compatible language and library features.
- Keep Swing updates on the Event Dispatch Thread.
- Keep automation work off the Event Dispatch Thread.
- Treat native input, hotkeys, and timer state as cross-thread boundaries.
- Validate settings before starting an automation worker.
