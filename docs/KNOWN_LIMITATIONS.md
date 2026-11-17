# Known limitations

- Local mode shares data between accounts on the same installation. Different phones require configured Firebase; existing local accounts are not automatically converted to cloud accounts.
- Firebase integration and service rules need validation against your configured project. No live cloud project was available for this pass.
- Alerts are stored in-app and synchronize on connection. Android notifications are available after permission is enabled while the app process is running; a terminated app does not receive Firebase background push notifications.
- Google OAuth needs Firebase Google sign-in, a configured web client ID, and Google Play services. It has not been tested against a live configured project.
- Campus membership is self-declared; email/password or Google authentication does not verify institutional membership. College suggestions are a bundled convenience list, not an exhaustive or verified directory.
- Live Google Maps was verified with the locally configured SDK key. Other checkouts need their own restricted SDK key; the unconfigured map is explicitly labelled as a pin preview.
- Walking trails record your own device only while the map is open. There is no background or other-student tracking. Up to 5,000 points are saved per account; fixes worse than 60 m accuracy are excluded. Walking directions open Google Maps externally; the app does not provide native turn-by-turn navigation.
- GPS accuracy depends on hardware, permission and surroundings. Emulator coordinates are simulated. Physical-phone camera/GPS behavior remains to be checked.
- OCR/image comparison gives clues, never ownership proof. Students must compare private details and inspect the physical item.
- Automated checks used one Android 17 emulator. TalkBack, older devices and all tablet sizes have not been fully audited.
- Local password recovery cannot send email. Configured Firebase provides email recovery.

These are acceptable demonstration boundaries, not a claim of a production deployment. See [the demo guide](../DEMO.md).
