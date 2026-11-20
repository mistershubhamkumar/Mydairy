# My Diary – Secret Feelings

**Write • Feel • Heal • Remember Forever**

A Cute Aesthetic Personal Diary Android application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Firebase Authentication (Google Sign-In via Credential Manager)**, **Cloud Firestore**, and **Firebase AI Logic (Gemini)**.

## Features Implemented
- **Animated Splash & 3-Step Onboarding** with anime girl & white cat illustrations, floating hearts, and glowing night-sky particles.
- **Private Authenticated Diary** using Google Sign-In via Jetpack Credential Manager (`GetSignInWithGoogleOption`) and per-user isolated Cloud Firestore collections with zero-trust security rules.
- **Home Dashboard** with Princess avatar, personalized greeting, Today's Mood quick check-in, weekly calendar strip, 6 quick actions, recent entries, writing streak & stats, Daily Motivation (with opt-in AI quote refresh), and interactive Mochi Kitty mascot widget.
- **Advanced Rich Diary Editor** supporting unlimited text, Bold/Italic/Underline/Highlight, alignment, font size, text color, custom fonts, bullet & numbered lists, stickers, entry paper backgrounds, custom date/time, categories, tags, location, multiple photos via the zero-permission Android Photo Picker, private entry lock, reminder toggle, auto-save, word/character count, and opt-in Gemini AI Diary Assistant.
- **Full Calendar Diary** with Monthly, Weekly, and Daily views, mood indicators, Jump to Today, and date-specific entry creation.
- **Mood Tracker & Analytics** with This Week / This Month / This Year glowing charts, average happiness score, mood distribution, multiple mood check-ins per day, and opt-in AI mood insights.
- **Dream Journal** with starry castle artwork, 6 dream categories, search, image attachments, favorites, and CRUD.
- **Voice Diary** with real `MediaRecorder` audio capture (pause/resume/stop), runtime `RECORD_AUDIO` permission handling, `MediaPlayer` playback with 0.75x–2.0x speed control, rename, description, and soft delete.
- **Photo & Video Memory Gallery** using zero-permission `PickMultipleVisualMedia`, custom albums, grid gallery, fullscreen caption/tag editor, and slideshow mode.
- **Categories & Global Search** with 11 built-in diary books + unlimited custom categories and multi-filter search.
- **Privacy & Security** with hashed 4-digit PIN or Password App Lock, native Android BiometricPrompt unlock, background Auto-Lock timer, Secret Mode, and Clear All Data.
- **Reminders & Notifications** with runtime `POST_NOTIFICATIONS` support (Android 13+), `AlarmManager` daily scheduling, weekday repeat filters, and instant test notifications.
- **Recycle Bin** with 30-day recovery across Entries, Dreams, Photos, and Voice Notes, plus auto-cleanup toggle.
- **Backup & Export** with Cloud Firestore sync snapshots, Storage Access Framework export to **PDF**, **TXT**, and **JSON** (with category filtering), and JSON backup import.
- **Theme Store** featuring 20 switchable aesthetic themes, 4 bundled fonts (`Quicksand`, `Nunito`, `Playfair Display`, `Pacifico`), mascot selector, and particle animation toggle.

## Building Locally
1. Open the project in Android Studio (JDK 11+, Android SDK 36).
2. Ensure `app/google-services.json` is present for your Firebase project.
3. Build a debug APK:
   ```bash
   ./gradlew :app:assembleDebug
   ```
4. Build a release APK:
   ```bash
   ./gradlew :app:assembleRelease
   ```
