# Bugs

## BUG-001 — Preset Mountain View campus

- Screen: home, map, new report
- Description: The app used a fixed Riverside label and the coordinates 37.422094, -122.083922.
- Expected: The campus is the one the student enters. Location permission is optional.
- Actual before fix: Hardcoded campus and automatic location snap.
- Severity: P1
- Root cause: `Campus` constants and `seedIfEmpty()`, plus a `LaunchedEffect` that called location on open.
- Fix: Removed the launch seed and the automatic location call. Home shows the account campus name. Map pins exist only when the user adds one.
- Test: Unit test that a place name is enough. Emulator account "North Campus" on the home screen.
- Verification: Passed on Pixel 10a, 17 November 2026.

## BUG-002 — No real account

- Screen: launch
- Expected: Registration creates an account and login restores it.
- Actual before fix: The board opened with no identity.
- Severity: P1
- Fix: Room accounts, salted PBKDF2 passwords, device session.
- Test: Unit tests for rules and hashing. Emulator register, logout, login.
- Verification: Passed. Home showed "Hello, Ada Lovelace" after login.

## BUG-003 — Claim control missing on the detail screen

- Screen: item detail
- Expected: A finder-set private check can be answered from the report.
- Actual: The hash is stored and compared in `ItemRepository.claim`, but the screen has no claim field yet.
- Severity: P2
- Fix: Not applied on the screen.
- Verification: Open.
