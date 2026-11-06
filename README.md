# FileVault

A native Android private-file vault with encrypted local storage and optional encrypted cloud backup.

## Stack
- Kotlin + Jetpack Compose
- Android Storage Access Framework for importing files
- AES-256-GCM file encryption with keys protected by Android Keystore
- Supabase Auth + private Storage bucket for encrypted cloud backups

## Requirements
- Android Studio (current stable)
- JDK 17
- Android SDK 35
- A Supabase project for cloud backup (local vault works without it)

## Configure Supabase
1. Create a Supabase project.
2. Create a **private** Storage bucket named `filevault`.
3. In Android Studio, copy `local.properties.example` to `local.properties` and set:
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
4. Never put a service-role key in the Android app.
5. Apply the Storage policies in `supabase/storage-policies.sql`.

Cloud backups are encrypted on the device before upload. The encryption key is held in that device's Android Keystore and is not uploaded. This first version therefore supports restoring backups on the same device; cross-device recovery/key transfer is not implemented.

## Build
Open this repository in Android Studio, allow Gradle sync, then run the `app` configuration on an Android device or emulator (Android 8.0+).

For an APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Security notes
- Imported files are encrypted before being written to app-private storage.
- Original files remain in their original location unless you delete them yourself.
- Cloud uploads contain ciphertext only.
- The app uses a per-install Android Keystore key. Clearing app data or uninstalling may make local encrypted files and cloud backups unrecoverable. Export/recovery-key support should be added before relying on this for irreplaceable data.
- This is an initial project; review and test security and backup recovery before storing sensitive or important files.
