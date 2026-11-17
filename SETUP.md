# FindIt setup in Android Studio

Open this project folder in Android Studio, let Gradle sync, then select the `app` run configuration and an Android device (API 26 or newer). The project uses JDK 17 language features; use Android Studio's bundled Gradle JDK. Do not replace your existing `sdk.dir` in `local.properties`.

## What works before service setup

Accounts, reports, search, private conversations between accounts on the same installation, ownership checks, camera capture, gallery import, GPS tagging, and bundled on-device OCR/image labeling work locally. Data stays in Room and survives restarts. Signup includes 100 editable Indian college/campus suggestions. Android notifications can be enabled from Profile; replies also remain available in Messages. Students on **different phones** need the Firebase setup below. A live Google map needs the Maps key; GPS coordinates, accuracy and private walking-trail recording work without it. Walking directions open Google Maps externally and do not need this app's SDK key. Trails remain on the device and record only while the map is open.

## Firebase: shared campus accounts, reports, photos, and messages

1. Create a project at [Firebase Console](https://console.firebase.google.com/). Add an **Android** app with package name `com.findit.campus`.
2. Enable **Authentication → Sign-in method → Email/Password**. Do not enable anonymous sign-in for this app.
3. Create **Cloud Firestore** in production mode. Create **Storage**. Firebase may require the Blaze billing plan for Storage; review its current pricing and set a budget alert before enabling it.
4. Download the Android app configuration (`google-services.json`). This app initializes Firebase explicitly, so the Google Services Gradle plugin is not required. Copy the following values into your ignored `local.properties`:

   ```properties
   FIREBASE_APPLICATION_ID=your mobilesdk_app_id
   FIREBASE_API_KEY=your current_key
   FIREBASE_PROJECT_ID=your project_id
   FIREBASE_STORAGE_BUCKET=your storage_bucket
   ```

   In the JSON, `mobilesdk_app_id` is under `client → client_info`; `current_key` is under `client → api_key`; `project_id` and `storage_bucket` are under `project_info`. Keep the literal values, without quotes. You may keep `app/google-services.json` locally; it is ignored by Git. Never paste a service account private key into an Android app.

5. Publish [Firestore rules](firebase/firestore.rules) in **Firestore → Rules**, and [Storage rules](firebase/storage.rules) in **Storage → Rules**. Storage rules use Firestore lookups; accept the console prompt to enable the required cross-service permissions. Alternatively, with the Firebase CLI authenticated, run `firebase deploy --only firestore:rules,storage --project YOUR_PROJECT_ID` from this folder.
6. Sync and rebuild in Android Studio. Create **new accounts** after connecting Firebase. Existing local-only accounts remain local; they are not silently uploaded or merged with a cloud identity. Signup takes you to Login, then the dashboard.
7. Give both test accounts the same campus name. Matching ignores case and extra spaces. Use two phones/emulators: one posts a lost item; the second taps **Found it**; the first opens the named conversation in Messages. Share proof/answers, confirm both ownership checks, then confirm the physical handover on each phone.

Public campus reports exclude private answers, contact details, and password hashes. Proof photos and messages are restricted to the two conversation participants. The supplied rules prevent a student from confirming the other person's side. Image evidence cannot prove ownership; participants must compare private details and inspect the physical item.

Room saves changes before uploading. Pending writes retry after connectivity returns; each account's writes remain associated with their original author. Unread alerts arrive while campus services are connected and are available on the next sync. Enable Android notifications from Profile to receive private reply and return notifications while the app process is running; tapping an alert opens its conversation or report for the correct signed-in account. This project does not send background push notifications to a terminated app. Campus membership is self-declared at signup; an institution-wide deployment needs an administrator-controlled student membership process.

## Google sign-in (OAuth)

Email/password remains available. Google sign-in uses Android Credential Manager and Firebase Authentication; there is no client secret or AI API required.

1. Complete Firebase setup above. In Firebase **Authentication → Sign-in method**, enable **Google** and choose a support email.
2. In **Project settings → Your apps**, add the debug signing SHA-1 from `./gradlew signingReport`. Enable Google before downloading an updated Android configuration.
3. Copy the **Web client ID** from the Google provider settings, or `client → oauth_client` with `client_type: 3` in the updated `google-services.json`. Use the web client ID, not the Android client ID.
4. Add it to ignored `local.properties` and rebuild:

   ```properties
   GOOGLE_WEB_CLIENT_ID=your web OAuth client ID
   ```

5. Use a device/emulator with Google Play services and a Google account. Open Create account or Login and tap **Continue with Google**. A new Google user chooses their name and college before joining; an existing user opens their saved campus dashboard. OAuth tokens are not stored as Room passwords. Canceling the Google chooser leaves the current screen open.

All six service values (the four Firebase values, Maps key, and web OAuth client ID) are optional for the local demo. They are configuration identifiers supplied by your own projects; do not invent them. No `.env` file is used. Live OAuth, Maps, and cross-device sync cannot be verified until these are configured.

## Google Maps SDK key

1. Open [Google Cloud Console](https://console.cloud.google.com/) for your Firebase/Google Cloud project. Enable **Maps SDK for Android**, and attach billing if Google requires it. Set usage quotas and a budget alert.
2. Create an API key in **APIs & Services → Credentials**. Restrict the key to **Android apps** and to **Maps SDK for Android**.
3. Add package name `com.findit.campus` and your debug signing certificate's SHA-1. Get the SHA-1 using Android Studio's Gradle signing report, or run:

   ```sh
   ./gradlew signingReport
   ```

4. Add this line to ignored `local.properties`, then sync and rebuild:

   ```properties
   MAPS_API_KEY=your restricted Android Maps key
   ```

5. On the device, enable Location, grant FindIt location permission, and choose **Precise** when offered. The app shows coordinates with measured accuracy; buildings, weak GPS signals, and approximate permission affect that accuracy. No app can guarantee an exact physical position.

An emulator displays its simulated position. In the emulator's **Extended controls → Location**, set a point near your campus. For camera testing, use an emulator camera scene/webcam or a physical phone. The app requests camera permission when taking a photo and uses Android's photo picker for existing images.

## Build and checks

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
```

The connected tests require an unlocked emulator/device with both cameras available, Location enabled, and a supplied emulator location. The retention flag leaves the app installed afterward. For a same-device college walkthrough, use [DEMO.md](DEMO.md). The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Service rules and cross-device Firebase behavior must be tested against your configured project before distributing the app.
