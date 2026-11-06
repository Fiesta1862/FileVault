# FileVault

A native Android private-file vault with encrypted local storage and optional encrypted cloud backup. The interface uses a dark, iOS-inspired card layout.

## Included in this first build
- Kotlin + Jetpack Compose Android application
- Import files using Android's system file picker
- AES-256-GCM encryption before writing imported files to app-private storage
- AES key generated and held by Android Keystore
- Create folders, browse folders, and search file names
- Text/JSON/XML preview; export other file types through Android's system save picker
- Supabase email/password sign-in and encrypted cloud upload
- Private Supabase Storage bucket with per-user folder policies
- GitHub Actions workflow to build a debug APK

## Requirements
- Android Studio (current stable)
- JDK 17
- Android SDK 35
- A Supabase project for cloud backup (local vault works without it)

## Configure Supabase
1. Create a Supabase project.
2. Create a **private** Storage bucket named `filevault`.
3. Copy `local.properties.example` to `local.properties` in the project root.
4. Set `SUPABASE_URL` and `SUPABASE_ANON_KEY` in `local.properties`.
5. Apply the SQL in `supabase/storage-policies.sql`.
6. Never put a Supabase service-role key in the Android app.

Cloud uploads contain encrypted file bytes only. Cloud upload is implemented; cloud browsing, restore, and cross-device key recovery are not included yet. The encryption key is device-local and is not uploaded.

## Build locally
Open the repository in Android Studio, allow Gradle sync, then run the `app` configuration on an Android device or emulator (Android 8.0+).

To build an APK in Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

A GitHub Actions workflow is included at `.github/workflows/android.yml`. After it completes, download the `FileVault-debug-apk` artifact from the workflow run.

## Important security limitations
- Original source files remain where they were; importing creates an encrypted copy in FileVault.
- File names and basic index metadata are stored in the app-private sandbox; file contents are encrypted.
- The app uses a per-install Android Keystore key. Clearing app data, uninstalling, or losing the device can make local files and uploaded cloud copies unrecoverable.
- There is no recovery-key export or cloud restore in this initial build. Do not use it as the only copy of important files.
- This is an initial implementation, not a security-audited product. Test it with non-sensitive sample files first.
