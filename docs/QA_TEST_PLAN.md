# QA test plan

## Executed this pass

- `./gradlew :app:testDebugUnitTest` — 14 tests, 0 failures. Covers match scoring, feed filters, place-name validation without coordinates, account rules, password hashing, and private-check comparison.
- `./gradlew :app:compileDebugAndroidTestKotlin` — device tests still compile. They were not re-run after the auth gate, because they set their own Compose content and do not launch the activity.
- Emulator Pixel 10a: install, launch, welcome screen, create account for Ada Lovelace / ada@north.test / North Campus, home greeting and campus name, empty board, log out, log in, home restored. No crash in `AndroidRuntime`.

## Still to run

- Full report, camera, pin, claim, and notification path on the emulator.
- Re-run `connectedDebugAndroidTest` after the auth gate (the previous 3 tests passed before this pass).
- TalkBack, rotation, and a second screen size.
