# Quick Contact Sheet

Android app + Glance home-screen widget for one-tap contact actions and reusable text snippets.

## Highlights

- Welcome screen with in-app widget pinning
- Per-widget contact configuration from the device contacts list
- Local per-widget persistence with Jetpack DataStore
- Glance widget layouts for narrow, wide, and large size buckets
- Bottom-sheet quick actions for call, text, contact details, and preset messages
- Message add/edit/delete/reorder UI

## Build

1. Open in Android Studio Ladybug+ (or any recent version with AGP 8.8 support), or run:
   ```bash
   ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:assembleDebug
   ```
2. Install the debug APK from `app/build/outputs/apk/debug/`.

## Notes

- The widget uses `ACTION_DIAL` and `ACTION_SENDTO` instead of direct calling/SMS permissions.
- Contacts permission is only required for browsing contacts and loading contact photos in-app.

## Legal & Policies

- [Privacy Policy](PRIVACY_POLICY.md)
- [Terms of Use](TERMS_OF_USE.md)
