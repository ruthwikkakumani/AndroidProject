# FindIt Play Store preparation

Prepared artwork: `findit-icon-512.png` (512 × 512). The app also has an adaptive launcher icon and Android launch screen. This folder is preparation, not a published listing.

Suggested app name: **FindIt Campus**

Suggested short description: **Find lost campus belongings, connect privately, and return them with care.**

Before uploading:

- Verify the deployed Firebase rules, Storage and server functions with Google sign-in and reports/photos/chat across two physical phones. The workspace's isolated live service check passed; physical two-device and background-push checks remain.
- Add student-content reporting, user blocking and a moderation process.
- Add in-app account deletion and the required public deletion-request page; current rules intentionally prevent profile deletion.
- Supply a real privacy-policy URL, support contact and Play Console data-safety answers reflecting Firebase and camera/location use.
- Create and retain your own release/upload keystore. Keep it and passwords outside Git; do not use the debug key for release. Register the Play App Signing SHA-1 with Firebase and the restricted Maps key.
- Build a signed release Android App Bundle in Android Studio, complete content rating and permission declarations, add authentic device screenshots and a feature graphic, then use an internal testing track before release.

Background notifications require an eligible Firebase billing plan and deployed functions. Google Maps may require billing. No billing plan or Play Store publication has been changed automatically.
