# Today's college demonstration

This checkout has Firebase and Google Maps configured locally. Open this folder in Android Studio, select **app**, choose your device and click **Run**. Allow camera, notifications and precise location when requested. Do not uninstall or clear the app between demonstrations: that removes its local accounts and reports.

If Home shows **Connect account**, this is an older account saved only on that phone. Tap it and enter that account's password. Its saved reports are preserved and queued for your campus board. Both phones must use the latest APK and the same college. Wait for the upload notice to clear before checking the second phone. No `.env` file is needed.

1. Create your first account with your college name. Signup opens **Login**; sign in to reach Home.
2. Tap **Report** and post a lost bottle or notebook. Add a photo, description, private identifying detail and location. The public Lost board shows it. Search its title and view its detail page.
3. Log out from Profile. Create and log in to a second account with the **same college name**. Tap the green **Found it** on that report and send a message.
4. Open **Ownership checks** in the conversation. Share a proof photo. The on-device AI compares photo labels and readable text with the report photo and posts advisory clues. Share private color, mark and last-location answers.
5. Switch back to the first account. Messages shows the request and exact conversation. Share your own evidence or identifying answers and review the second student's evidence. Each account confirms its own item check.
6. Only after both checks, each account confirms the actual handover. The report moves to Reunited in Profile. The Found board supports the reverse workflow through **This is mine**.
7. Show Scan using a clear photographed label or `demo/ocr-label.png` from this repository. Choose the image, review the recognized text, and use it in a lost report. On the emulator, drag the PNG onto the emulator to import it before opening the photo picker.
8. Show Map and report location tagging. Without a Maps key the app shows a labelled pin preview and real GPS coordinates; with a key it uses Google Maps. Set a simulated position through emulator **Extended controls → Location**. A physical phone gives device GPS and camera capture.
9. On Map, tap **Start trail**, walk with the map open, then **Pause trail**. Resume creates a separate segment, avoiding a false line across the paused period. The saved trail survives reopening. Tap a report pin to open its draggable card, then **Walking directions** to open Google Maps. Use the clear button only when you want to remove your own saved trail.
10. In a private conversation, tap the location icon. Share a current GPS pin or choose **Share live while chatting · 15 min**. The other student opens **View map & directions** to see your latest shared pin and walking directions. Live sharing ends on Stop, when leaving/backgrounding the chat, or after 15 minutes. A bubble marked **Waiting to upload** is still on the sender's phone.

Camera/OCR cannot read lettering that is absent from the emulator camera scene. Use the supplied image, a printed label, a webcam, or a physical phone. No paid AI API is required.

Different phones use Firebase with Room as the local cache. Other checkouts need their own ignored Firebase configuration and Google Maps key; follow [SETUP.md](SETUP.md).
