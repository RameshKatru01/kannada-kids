# Fun Learn – ಕನ್ನಡ (Kannada Kids) — Android + iOS

This is your Kannada Kids app converted to **Compose Multiplatform**. The same Kotlin screens, games
and data code now run on **Android and iPhone/iPad**. Only a small platform layer is written
separately for each system.

```
KannadaKids/
├── composeApp/
│   ├── src/commonMain/      ← all screens, games, data, navigation (shared by Android + iOS)
│   │   ├── kotlin/com/example/platform/   ← small cross-platform layer (storage, speech, sound, video, sign-in, toasts)
│   │   └── composeResources/              ← shared files (applause sound, Google logo)
│   ├── src/androidMain/     ← MainActivity, Android versions of the platform layer, icons, manifest
│   └── src/iosMain/         ← iOS versions of the platform layer + MainViewController
└── iosApp/                  ← Xcode project (SwiftUI wrapper that shows the shared app)
```

---

## 1. Run on Android

1. Install the latest **Android Studio** and the **Kotlin Multiplatform** plugin
   (Settings → Plugins → search for "Kotlin Multiplatform").
2. **File → Open** and choose the `KannadaKids` folder. Wait for Gradle sync to finish.
3. (Optional) Copy `.env.example` to `.env` and set `WEB_CLIENT_ID` for Google sign-in, same as before.
4. Pick the **composeApp** run configuration and press ▶.

The Android app id is `com.funlearn.studio` (was `com.aistudio.kannadakids.ycpceg` before Play publishing), Firebase setup,
Google sign-in and saved progress as before, so it updates over the existing Play Store app.
For a signed release, set `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` like before.

> If Android Studio offers to upgrade the Android Gradle Plugin to 9.x, choose **Don't remind me**
> for now. This project uses AGP 8.13 on purpose.

## 2. Run on iPhone / iPad (needs a Mac)

Requirements: macOS with **Xcode 16 or newer** (Xcode 26 works), and the steps in part 1 done at least once
so Gradle has downloaded everything.

1. Open `iosApp/Configuration/Config.xcconfig` and set:
   - `TEAM_ID` — your Apple Developer Team ID (Xcode → Settings → Accounts → your team).
   - `PRODUCT_BUNDLE_IDENTIFIER` — e.g. `com.yourname.kannadakids` (must be unique on the App Store).
2. Open `iosApp/iosApp.xcodeproj` in Xcode.
3. Choose an iPhone simulator or your connected iPhone and press ▶ (Run).
   - The first build takes several minutes: Xcode runs Gradle to compile the shared Kotlin code
     (`./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`).
   - Alternatively, in Android Studio choose the **iosApp** run configuration (it appears once the
     Kotlin Multiplatform plugin is installed).
4. To publish: in Xcode, **Product → Archive**, then **Distribute App → App Store Connect**
   (needs a paid Apple Developer account).

### Optional: Firebase e-mail sign-in on iPhone
Gmail-address sign-in works on iPhone without any setup (it signs the child in locally, exactly like
the Android app does when Firebase isn't set up). To use your Firebase project on iPhone too,
put your Firebase **Web API key** (Firebase console → Project settings → General) in
`FIREBASE_API_KEY` in `Config.xcconfig`.

### Optional: "Continue with Google" on iPhone
1. In the Firebase console, add an **iOS app** with your bundle id and download `GoogleService-Info.plist`.
   Copy `CLIENT_ID` into `GOOGLE_IOS_CLIENT_ID` and `REVERSED_CLIENT_ID` into
   `GOOGLE_REVERSED_CLIENT_ID` in `Config.xcconfig`.
2. In Xcode: **File → Add Package Dependencies…** → `https://github.com/google/GoogleSignIn-iOS`
   → add the **GoogleSignIn** library to the `iosApp` target.
3. Build again. `GoogleSignInBridge.swift` switches itself on automatically when the package is present.

Until this is done, the Google button on iPhone shows a friendly message and children can use the
Gmail address box or "guest" sign-in.

---

## What changed to make it cross-platform

| Area | Before (Android only) | Now |
|---|---|---|
| UI | Jetpack Compose | Compose Multiplatform (same code, now shared) |
| Saved progress | SharedPreferences | Same API; Android keeps using SharedPreferences (existing progress is kept), iOS uses NSUserDefaults |
| Text-to-speech | Android TextToSpeech | Android TextToSpeech / iOS AVSpeechSynthesizer. iPhones have no Kannada voice, so iOS automatically uses the same online Kannada voice the app already used for phonics |
| Sound effects | AudioTrack / ToneGenerator / MediaPlayer | Same sounds, generated in shared code and played by each platform |
| Videos | ExoPlayer | ExoPlayer on Android, AVPlayer on iOS |
| Images | Coil 2 | Coil 3 (multiplatform) |
| Navigation / ViewModel | AndroidX | JetBrains multiplatform versions of the same libraries |
| Sign-in | Firebase + Credential Manager | Unchanged on Android; Firebase REST + optional GoogleSignIn on iOS |
| Reward animation | Lottie (Android only) | A built-in star-burst animation drawn in Compose (works on both) |
| Letters drawn on game canvases | Android `Paint` | Compose text drawing (works on both) |
| Toast messages | Android Toast | A small shared toast drawn by the app |

Small differences on iOS:
- The pause-menu volume slider controls the app's own sound-effect volume (iOS doesn't let apps change the system volume).
- Full-screen videos keep the iOS status bar visible.
- Background music in the Letter Runner game is the same tune, now played as a loop.

Removed leftovers from AI Studio: the many `patch_*.py` / `fix_*.sh` scripts, the old APK, unused
dependencies (Retrofit, Vico charts, Firebase AI, App Check) and the Robolectric test setup.

## Library versions
Kotlin 2.2.21 · Compose Multiplatform 1.9.3 · Android Gradle Plugin 8.13.0 · Gradle 8.14.3 ·
Navigation 2.9.1 · Lifecycle 2.9.6 · Material 3 1.9.0 · Coil 3.3.0 · Ktor 3.3.0 (see `gradle/libs.versions.toml`).
