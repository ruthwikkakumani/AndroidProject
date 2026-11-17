# FindIt Campus

A Kotlin Android college project for finding and returning campus belongings. The app uses an ivory, navy and emerald interface with original illustrations, animated navigation and draggable return-check sheets.

- Create an account, then log in. Your saved session opens the dashboard on later launches until you log out.
- Search separate Lost and Found boards. **Report** posts a lost item; the Found board has its own posting action.
- **Found it** and **This is mine** open a private conversation with the exact reporter and create unread alerts.
- Capture photos with either camera, choose gallery images, and tag measured GPS coordinates with accuracy.
- Record a private walking trail with pause/resume and distance. Tap map pins for a draggable report card and walking directions.
- Bundled ML Kit reads text in photos and compares image labels/readable text for ownership clues. Both students review evidence and confirm the actual handover.
- Room stores accounts, reports, messages and return progress offline. Optional Firebase synchronizes accounts, reports, photos and conversations across phones.

Start with [the college demo guide](DEMO.md). [SETUP.md](SETUP.md) explains Android Studio, Firebase and Google Maps configuration. The project is a classroom demonstration; live Firebase and Google Maps require your own service configuration.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Package: `com.findit.campus`. Minimum Android: API 26.

See [architecture](docs/ARCHITECTURE.md), [checks](docs/QA_TEST_PLAN.md), and [limitations](docs/KNOWN_LIMITATIONS.md).
