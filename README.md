# FindIt Campus

A Kotlin Android college project for finding and returning campus belongings. The app uses an ivory, navy and emerald interface with original illustrations, animated navigation, a branded launch screen, floating navigation and draggable return-check sheets.

- Create an account, then log in. Your saved session opens the dashboard on later launches until you log out.
- Search separate Lost and Found boards. **Report** posts a lost item; the Found board has its own posting action.
- **Found it** and **This is mine** open a private conversation with the exact reporter and create unread alerts.
- Capture photos with either camera, choose gallery images, and tag measured GPS coordinates with accuracy.
- Record a private walking trail with pause/resume and distance. Tap map pins for a draggable report card and walking directions.
- Bundled ML Kit recognizes objects and reads text in photos, with tappable search clues. It also compares image labels/readable text for ownership clues. Both students review evidence and confirm the actual handover.
- Room stores accounts, reports, messages and return progress offline. Firebase can synchronize accounts, reports, photos and conversations across phones; server functions provide private background alerts after deployment.

Start with [the college demo guide](DEMO.md). [SETUP.md](SETUP.md) explains Android Studio, Firebase and Google Maps configuration. The project is a classroom demonstration; live Firebase and Google Maps require your own service configuration.

This workspace has Firebase configuration, a live Maps key, deployed Firestore/Storage rules and three notification functions. A live service check passed account creation, campus setup, photo sharing, private messaging and both-user handover using temporary isolated accounts, which were removed afterward. Google dashboard entry and background push on two physical devices still need a manual check. No `.env` or paid AI API is needed.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Package: `com.findit.campus`. Minimum Android: API 26.

See [architecture](docs/ARCHITECTURE.md), [checks](docs/QA_TEST_PLAN.md), and [limitations](docs/KNOWN_LIMITATIONS.md).
