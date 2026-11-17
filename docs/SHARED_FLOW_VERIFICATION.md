# Shared campus flow verification

Verified on 17 November 2026 using the locally configured Firebase project and an Android 17 emulator.

- The complete debug build, 23 JVM tests, emulator suite, and Android lint passed. The opt-in live backend test was skipped during the ordinary suite and then run separately, successfully. Lint has existing warnings and no errors.
- The live Android SDK test used two independent Firebase authentication instances and Room databases. An older offline account connected without losing its report; another account at the same college received that report. Private messages worked in both directions, a shared GPS pin arrived, both students' checks synchronized, and both return confirmations marked the item reunited. Pending uploads drained. Temporary test accounts and documents were removed.
- Home navigation was checked from every campus tab and by tapping Home repeatedly. Connection notices and pending-message labels were checked in Compose. The empty board provides an inline report action so the floating button does not cover its text.
- Emulator camera tests captured photos with front and rear cameras. Fused location returned fresh coordinates and updates; Google Maps loaded live tiles. OCR read rendered text and bundled recognition identified a real object photo. These checks do not prove physical-phone camera accuracy or item ownership.
- The app was installed and launched without clearing the student's saved session. Home, chat, and meeting-location screens were inspected. The design closeout retained the approved Home illustration and branding, and kept genuine messages, location controls, and upload feedback in the app's existing theme.

To demonstrate between phones, install the latest APK on both and choose the same college. On an older local account, tap **Connect account** on Home and enter its saved password. Wait for queued reports or messages to upload before checking the other phone. No `.env` file is required for this locally configured checkout.

Physical two-phone interaction, background push receipt, and a fresh Google OAuth flow were not completed in this verification. See [known limitations](KNOWN_LIMITATIONS.md) for those checks and release requirements.
