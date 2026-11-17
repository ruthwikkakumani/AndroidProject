# Verification

Final run passed: **19 unit tests, 25 connected emulator tests, and lintDebug (0 errors, 33 warnings)**. The debug app and instrumentation APK built successfully. Automated checks used an Android 17 Pixel 10a emulator, including simulated GPS and camera scenes. Lint/build deprecation warnings do not prevent running the project.

```sh
./gradlew testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
```

The installation-retention flag prevents the connected test runner from removing the app afterward. Use dedicated test devices and keep your presentation device's data intact. Tests use isolated Room databases or in-memory screen fixtures and do not seed student records into the main app.

Coverage: account rules/password hashes, filters/matching, explicit database migration, saved session after database reopen, signup-before-login, exact private thread recipients/unread alerts, evidence and both ownership/handover confirmations, queued-write coalescing, bundled OCR/image labeling, both camera directions, fresh Fused Location, board/report/detail/scan/profile/map screens and private conversation UI. New checks cover college dropdown filtering/selection/custom names, signup at 320 dp with 1.3 font scale, Google setup feedback and prevention of an unverified local OAuth session, and actual Android notification delivery/read cancellation plus correct account/thread/report intent routing.

Additional trail checks cover filtering invalid/stale GPS fixes and jumps, segmented distance after pause, account-isolated Room persistence after reopening, migrations through database version 7, actual Fused GPS-to-Room saving, and trail controls/draggable report navigation. Captured report and trail screens use isolated test fixtures, not student data or proof of live Google map tiles.

The supplied Maps key successfully loaded live Google tiles and the device location marker. A cold-start regression check waits for the native map-loaded callback and captures the rendered map. It skips when no Maps key is configured. Camera updates now wait for map readiness to avoid the CameraUpdateFactory initialization crash. The key remains in ignored local configuration.

Manual classroom flow: [DEMO.md](../DEMO.md). Remaining checks: live Google OAuth, Firebase with two phones, physical camera/GPS, TalkBack and broader screen sizes.
