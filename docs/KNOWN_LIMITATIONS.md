# Known limitations

- Local mode shares data between accounts on the same installation. Different phones require configured Firebase; existing local accounts are not automatically converted to cloud accounts.
- Firebase integration and service rules need validation against your configured project. No live cloud project was available for this pass.
- Alerts are stored in-app and synchronize on connection. Android notifications are available after permission is enabled while the app process is running; a terminated app does not receive Firebase background push notifications.
- Google OAuth needs Firebase Google sign-in, a configured web client ID, and Google Play services. It has not been tested against a live configured project.
- Campus membership is self-declared; email/password or Google authentication does not verify institutional membership. College suggestions are a bundled convenience list, not an exhaustive or verified directory.
- Live Google Maps needs a restricted SDK key. The unconfigured map is explicitly labelled as a pin preview.
- GPS accuracy depends on hardware, permission and surroundings. Emulator coordinates are simulated. Physical-phone camera/GPS behavior remains to be checked.
- OCR/image comparison gives clues, never ownership proof. Students must compare private details and inspect the physical item.
- Automated checks used one Android 17 emulator. TalkBack, older devices and all tablet sizes have not been fully audited.
- Local password recovery cannot send email. Configured Firebase provides email recovery.

These are acceptable demonstration boundaries, not a claim of a production deployment. See [the demo guide](../DEMO.md).
