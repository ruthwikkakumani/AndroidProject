# Verification

Final run passed: **21 unit tests, 27 connected emulator tests, 5 server notification policy tests, one comprehensive Firestore emulator flow test, and lintDebug (0 errors, 40 warnings)**. The debug app and instrumentation APK built successfully. Automated checks used an Android 17 Pixel 10a emulator, including simulated GPS and camera scenes. Lint/build deprecation warnings do not prevent running the project.

```sh
./gradlew testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
npm test --prefix firebase/functions
npx firebase-tools emulators:exec --only firestore --project demo-findit --config firebase-tests.json 'node --test firebase/rules/flow.test.cjs'
```

The installation-retention flag prevents the connected test runner from removing the app afterward. Use dedicated test devices and keep your presentation device's data intact. Tests use isolated Room databases or in-memory screen fixtures and do not seed student records into the main app.

Coverage: account rules/password hashes, filters/matching, explicit database migration, saved session after database reopen, signup-before-login, exact private thread recipients/unread alerts, evidence and both ownership/handover confirmations, queued-write coalescing, bundled OCR/image labeling, both camera directions, fresh Fused Location, board/report/detail/scan/profile/map screens and private conversation UI. New checks cover college dropdown filtering/selection/custom names, signup at 320 dp with 1.3 font scale, Google setup feedback and prevention of an unverified local OAuth session, and actual Android notification delivery/read cancellation plus correct account/thread/report intent routing.

Additional trail checks cover filtering invalid/stale GPS fixes and jumps, segmented distance after pause, account-isolated Room persistence after reopening, migrations through database version 7, actual Fused GPS-to-Room saving, and trail controls/draggable report navigation. Captured report and trail screens use isolated test fixtures, not student data or proof of live Google map tiles.

The supplied Maps key successfully loaded live Google tiles and the device location marker. A cold-start regression check waits for the native map-loaded callback and captures the rendered map. It skips when no Maps key is configured. Camera updates now wait for map readiness to avoid the CameraUpdateFactory initialization crash. The key remains in ignored local configuration.

A regression test also verifies that connecting Firebase preserves login to an existing local account without converting its identity.

The refreshed design was captured on the installed app with the existing student session intact. The suite also checks signup at 320 dp with enlarged text. New scanner checks run bundled recognition on a real photograph, read text, compare identical evidence and verify that tapping a suggested object updates the search clue. The photograph is test-only, with attribution in the test assets folder. The server notification tests verify exact recipients, invalid destinations, private routing-only payloads, handover transitions and invalid-token cleanup.

The emulator rule test covers missing own-profile and new-thread lookups, private conversation/message access, prevention of confirming the other participant's side, required checks before handover and final reunited status. Its runner rejects nonlocal hosts and non-demo projects.

A separate live service check used two temporary, isolated email/password accounts in the configured Firebase project. It passed account creation, first-profile setup, report/photo upload and second-user reads, exact-recipient private replies, anonymous-read denial and both ownership/handover confirmations. Only that check's accounts, documents and photo were removed afterward. This verifies deployed service access; it does not replace two physical phones or Google chooser/dashboard verification.

Firestore and Storage rules, cross-service Storage permissions, and all three notification functions were deployed. Function listing confirmed their presence. The deployment CLI returned a cleanup-policy notice after successful function creation. Background FCM delivery has not yet been observed on two devices.

Manual classroom flow: [DEMO.md](../DEMO.md). Remaining checks: retry Google dashboard entry after rule deployment, Firebase/push with two phones, physical camera/GPS, TalkBack and broader screen sizes.
