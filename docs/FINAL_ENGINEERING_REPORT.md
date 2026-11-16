# Final engineering report

This is a progress report, not a claim that every product area is finished.

## 1. Initial condition

A local Kotlin Compose app, not under Git. No API client, no `ApiClientTest.kt`, no `http://10.0.2.2:8787` integration, and no Firebase files. The board seeded fictional reports around a Mountain View coordinate and labeled itself Riverside Campus.

## 2. Features that were already real

Local reports, search, filters, camera, on-device text reading, match scoring, map pins, and Room.

## 3. Defects

See `docs/BUGS.md`. The preset campus and the missing accounts were P1.

## 4. Architecture changes

Added `accounts`, `device_session`, and `notifications`. Reports gained `ownerId`, `hasCoordinates`, and `verificationSecret`. Database version is 3. A version 2 file is rebuilt because it only held the sample board.

## 5. Screens

Welcome, create account, log in, and forgot-password explanation are new. Home greeting and campus line now come from the account. The map no longer draws the preset campus.

## 6. Components

Create account, log in, password visibility, forgot password, and log out all have behavior. Forgot password does not fake a reset.

## 7. Backend

No remote backend was in the project, and none was invented. Persistence is Room on the device.

## 8. AI

Match scoring and on-device text reading remain. No new cloud model was added. The private check is a hash comparison, not a generated match percentage.

## 9. Security

Passwords are salted PBKDF2 hashes. The private check is stored sealed. Login failure uses one message for unknown email and bad password. There is no server-side authorization because there is no server.

## 10. New product behavior

A student names their campus at registration. The board starts empty. A place name does not require GPS.

## 11–13. Files

New: auth rules, account store, account repository, session and auth screens, and these docs.
Modified: database, items, navigation, home, map, report form, application startup, tests.
Removed: automatic sample-report insert. The sample data file remains unused.

## 14–15. Tests actually run

- `./gradlew :app:testDebugUnitTest`: 14 tests, 0 failures.
- `./gradlew :app:compileDebugAndroidTestKotlin`: success. `connectedDebugAndroidTest` was not re-run after the auth gate.
- Emulator Pixel 10a: installDebug, launch, create account, empty home for North Campus, log out, log in, home restored. No `AndroidRuntime` crash.

## 16. Emulator checks not done this pass

Posting a report, camera shutter, GPS pin, claim answer, notification list, rotation, TalkBack.

## 17. Build

`:app:installDebug` succeeded and the process stayed running.

## 18. Remaining defects

BUG-003. No notification inbox. No chat. No admin.

## 19–20. Credentials

No API key is required for the current build. `MAPS_API_KEY` in `local.properties` is empty. Mail, Firebase, and a remote API are not configured because they are not in the project.

## 21. Deferred

Multi-device sync, email verification, password email, chat, admin moderation, QR desks, heatmaps, and extra languages.

## 22. Production checklist

Do not ship this build as a multi-student service. It is a single-device campus board with real local accounts.

| Feature | Implementation | Test evidence | Status |
|---|---|---|---|
| Registration | Room account, PBKDF2 | Emulator created Ada Lovelace | Verified |
| Login and logout | Device session | Emulator logout then login restored the home greeting | Verified |
| Campus name | Stored on the account | Home showed North Campus | Verified |
| Forgot password email | Explanation only | Screen has no success path | Blocked |
| Lost and found reporting | Existing form, now owned by the account | Not re-run on the emulator after auth | Partial |
| Search | Existing query | Prior tests; not re-run this pass | Partial |
| AI text reading | ML Kit | Camera preview seen in an earlier pass; OCR not re-run | Partial |
| Match scoring | On-device scorer | 14 unit tests passed | Verified |
| Private claim | Repository and unit test | Detail screen has no claim field | Partial |
| Messaging | None | None | Blocked |
| Notifications | Rows inserted, no inbox | None on device | Partial |
| Admin | None | None | Blocked |
