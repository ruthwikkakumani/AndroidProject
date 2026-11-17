# Verification

Final run passed: **15 unit tests, 17 connected emulator tests, and lintDebug**. The debug app and instrumentation APK built successfully. Automated checks used an Android 17 Pixel 10a emulator, including simulated GPS and camera scenes. Lint/build deprecation warnings do not prevent running the project.

```sh
./gradlew testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
```

The installation-retention flag prevents the connected test runner from removing the app afterward. Use dedicated test devices and keep your presentation device's data intact. Tests use isolated Room databases or in-memory screen fixtures and do not seed student records into the main app.

Coverage: account rules/password hashes, filters/matching, explicit database migration, saved session after database reopen, signup-before-login, exact private thread recipients/unread alerts, evidence and both ownership/handover confirmations, queued-write coalescing, bundled OCR/image labeling, both camera directions, fresh Fused Location, board/report/detail/scan/profile/map screens and private conversation UI.

Manual classroom flow: [DEMO.md](../DEMO.md). Remaining checks: live Firebase with two phones, restricted Google Maps key, physical camera/GPS, TalkBack and broader screen sizes.
