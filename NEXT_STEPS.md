# Next Steps & Production Checklist

## 1. Real-Device Verification (Pending Hardware Access)
Because execution in this environment has no physical Android device or ADB emulator attached, verify these items on a real phone:
- **Google Sign-In Sheet:** Test Credential Manager Google account selection on a physical device with Google Play Services and verify initial Firestore sync.
- **Floating Capture Overlay (`SYSTEM_ALERT_WINDOW`):** Launch the bubble over YouTube/Chrome/Netflix (DRM players and non-DRM apps), test dragging, edge snapping, rotation bounds, and Android 14+ foreground notification controls.
- **Voice Capture (`RECORD_AUDIO`):** Test push-to-talk English (`en-US`) recognition both inside the app and from the floating panel to observe OEM restriction behavior and automatic fallback routing.
- **TTS & Media3 Audio:** Verify English (`en-US`) TTS voice quality and speed rates on target devices; upload hosted `.mp3`/`.m4a` tracks and switch lessons from `audioMode = "TTS"` to `"MEDIA3"` + `audioUri` as recordings are produced.
- **OEM Battery Optimization:** Verify background retention of `FloatingCaptureService` on Xiaomi/Samsung/OnePlus devices.

## 2. Optional Backend Enhancements
- **Email/Password & Password Reset:** If email sign-in is desired alongside Google Sign-In, enable Email/Password provider in Firebase Authentication Console and add corresponding Auth UI screens.
- **Remote Curriculum Sync:** Sync published `contentGrammar`, `contentReading`, and `contentListening` updates from Firestore into local cache using `tools/seed_firestore_content.mjs --apply` for live content updates without APK releases.
- **Server-Side Account Deletion Cleanup:** Add a Firebase Cloud Function triggered on `auth.user().onDelete()` so deleting an account also cleans up any residual documents if client deletion is interrupted mid-batch.
