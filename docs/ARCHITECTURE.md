# Architecture

One Kotlin Android application module uses Compose Material 3 screens, ViewModels, coroutines and lifecycle-aware StateFlow collection. Forms and drafts use SavedStateHandle/state hoisting; reusable cards, photo actions and stateless chat/inbox content keep layouts consistent.

Room database `campus-board.db` is version 6. Tables hold accounts, a device session, items, categories, private threads/messages, alerts and pending cloud writes. Explicit migrations preserve earlier records. Room Flow is the UI source of truth. Logout clears the device session; startup waits for its first resolved value before choosing authentication or the dashboard.

Repositories enforce campus filtering, exact conversation participants, evidence requirements and two-sided verification/handover. Photo checks are advisory; only both physical-return confirmations change a report to Reunited. Passwords for local/offline login use salted PBKDF2.

Bundled ML Kit Latin text recognition and image labeling provide genuine on-device AI. Text scanning searches reports; proof comparison combines labels/readable text and image clues. Deterministic search/ranking is not presented as a trained recommendation model.

Optional programmatically configured Firebase supplies Auth, Firestore and Storage. Local writes enter an author-bound coalescing queue and retry after connection returns. Participant listeners feed remote changes back into Room. Configuration/rules are documented in [SETUP.md](../SETUP.md); live cloud behavior remains unverified without project credentials.

CameraX binds camera use cases to lifecycle and releases them on disposal. Fused Location supplies fresh coordinates with measured accuracy; permission denial has an explicit UI state. Maps Compose renders live maps when configured; otherwise a labelled local pin preview is available.
