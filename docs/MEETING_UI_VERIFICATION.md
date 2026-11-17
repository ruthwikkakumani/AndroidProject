# Private meetings and interface verification

Implemented on `feat/findit-live-meeting-chat` on 17 November 2026. No Git push was performed.

## Behavior

- Contact and meeting-plan cards travel through the same participant-restricted Room/Firebase message path as ordinary chat. Contact numbers are optional, validated, copyable and open the dialer. Inbox and notification previews do not expose phone numbers or coordinates.
- The real Google meeting map shows each participant's latest shared position, accuracy, freshness and direct distance. Sharing is explicit, foreground only, and limited to 15 minutes. Stop publishes an ended pin without inventing a newer GPS timestamp. Older positions remain clearly labelled. Actual walking routes open in Google Maps.
- Chat bubbles animate as messages arrive. Attachments, contacts, meeting plans and AI evidence are accessible from one sliding options panel. Routine feedback uses snackbars/toasts; handover and logout retain confirmation dialogs.
- Profile uses the student's real account information and report counts, with working account, recovery, privacy, notification and campus-tool controls. Home offers search clearing and direct access to AI, maps and messages. New menus share one accessible row component instead of duplicating layouts.
- The AI shortcuts open existing on-device OCR/object recognition and private photo-comparison flows. AI gives advisory clues; both students still review identifying details and confirm the handover.

## Design review

The required 12ui closeout ran against the original approved Home, Profile, conversation and map LayerDocs. Local kits are in `.design/meeting-closeout-{home,profile,chat,map}`. Their README and annotated plans were reviewed. Screenshot inputs produced unanchored plans with zero automatic element matching, so they are reference specifications rather than reliable code diffs.

The existing cream/navy/emerald theme, original backpack and profile illustrations, branding, rounded surfaces and native navigation remain. Screenshot comparisons covered Profile, privacy, tool sheets, contact/plan controls, chat and the two-position map. A 320 dp Home with 1.5× text was exercised for reachable controls and scrolling.

Reference-specific raster layers for fictional portraits, college identity, sample reports and proof documents were not inserted into student data: the requested real accounts, reports and evidence take precedence. The existing profile illustration and Home cutouts are reused. Static reference map artwork was not substituted for Google Maps. Scan and action icons remain accessible Material vectors rather than raster screenshots. New tool/privacy/contact/plan sheets have no original matching target and were reviewed against the established theme. This is a native functional adaptation, not a claimed pixel-identical export.

## Validation

- `assembleDebug`, 29 Kotlin/JUnit tests and Android lint passed. Lint reports 41 warnings and no errors.
- The complete emulator suite recorded 36 tests: 35 passed, with the opt-in live backend test skipped. That live test was then run separately and passed against two independent Firebase authentication instances and separate Room databases: report migration/visibility, bidirectional messages, both shared positions, contact/meeting cards, both checks, both return confirmations and drained upload queues. Its temporary accounts and documents were removed.
- Six Firebase notification-policy tests and the function syntax check passed. The quiet-location update to `privateMessageCreated` deployed successfully; function listing confirmed it. The CLI subsequently exited with an artifact-cleanup-policy notice. No automatic artifact deletion policy was enabled.
- Device checks cover camera capture, fresh fused location, real map tiles, OCR and object recognition, plus chat, Profile actions and narrow-screen tool access. Emulator coordinates and test identities are fixtures, not physical-device observations.
- The latest debug APK was installed with replacement and cold-launched successfully on the emulator. The saved student session was preserved and Home rendered. That particular older account remains local and displays **Connect account**; it needs the student's saved password to enable cross-phone sharing.

Physical two-phone interaction, a fresh Google sign-in flow, and background push receipt remain outside this verification. The APK remains a demonstration build; release requirements are in [known limitations](KNOWN_LIMITATIONS.md).
