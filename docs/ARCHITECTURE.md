# Architecture

FindIt Campus is one Android application module.

- UI: Jetpack Compose screens and a navigation graph. Signed-out users see welcome, registration, login, and the forgot-password explanation. Signed-in users see the board, map, scan, reports, and posting.
- State: ViewModels. `SessionViewModel` reads the signed-in account. Report screens keep their own form state.
- Accounts: Room table `accounts`. Passwords are PBKDF2-HMAC-SHA256 with a random salt. The signed-in id is one row in `device_session`.
- Reports: Room tables `categories` and `items`. Each report stores `ownerId`. A map pin is stored only when the user asks for one (`hasCoordinates`). A found report may store a sealed private check, not the raw text, in `verificationSecret`.
- Matching: `SmartMatchEngine` scores the other open report. Distance is ignored unless both reports have a real pin.
- Text reading: ML Kit Latin recognizer, on device.
- Location: Play services fused location and Geocoder, only after the user taps the location control.
- Schema: database `campus-board.db`, version 3. Opening a version 2 database (the old sample board) is rebuilt with `fallbackToDestructiveMigration()` because that database was the hardcoded Mountain View sample set.

There is no network API in this repository.
