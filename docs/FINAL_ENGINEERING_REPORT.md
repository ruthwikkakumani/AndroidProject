# FindIt college project report

The app implements both sides of a campus lost-and-found workflow in Kotlin. An illustrated Material interface supports posting, searching, camera capture, GPS tagging, private messages and careful returns. On-device ML Kit reads labels and compares photos as advisory ownership evidence. Each student verifies their own side and confirms physical handover before a report is reunited.

Room provides offline storage and a persistent session. Google OAuth/Firebase/Google Maps integration is configurable and documented, while a single-device classroom walkthrough works without external keys. New improvements include college autocomplete, opt-in Android notifications, unread badges, photo/account feedback, a tablet navigation rail, and readable narrow-screen signup. See [DEMO.md](../DEMO.md), [QA results](QA_TEST_PLAN.md), and [known limits](KNOWN_LIMITATIONS.md).

Development remains on `feat/findit-campus-upgrade`, with reversible commits and no push. Android Studio UI automation was unavailable in this session; Gradle and the Android emulator were used for build/run checks.
