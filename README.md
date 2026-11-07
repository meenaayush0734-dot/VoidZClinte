# VoidX Client

A Kotlin + Jetpack Compose (Material 3) companion launcher for Minecraft Bedrock on Android.
Minimum Android 8.0 (API 26).

## What it does

| Feature | Status |
|---|---|
| Detects whether the official Minecraft Bedrock app (`com.mojang.minecraftpe`) is installed and shows its version | Implemented |
| PLAY button launches the installed Minecraft app; shows an error if it cannot | Implemented |
| "Get Minecraft on Google Play" shortcut when not installed | Implemented |
| Import `.mcpack`, `.mcaddon`, `.mcworld` via Android's document picker | Implemented; each archive is parsed (`manifest.json` / `levelname.txt`) and rejected with a message if invalid |
| Library view with name, version, type, size, modules, UUID | Implemented |
| Remove a pack from the VoidX library (optional confirmation) | Implemented |
| "Open in Minecraft" hands the stored file to any app that accepts it via a FileProvider content URI | Best effort: works only if Minecraft (or another app) registers for the file |
| Download `.mcpack` / `.mcaddon` / `.mcworld` from a direct `https://` link via `DownloadManager`, then import | Implemented |
| Theme (system / dark / light) and confirm-delete preferences, saved with DataStore | Implemented |
| Loading indicators, snackbar error messages | Implemented |

### Limits (by design — not faked)

- Android Bedrock does not allow arbitrary client injection. VoidX does not modify Minecraft's
  files, data folders, or memory, and does not install packs into Minecraft's worlds or profiles.
- VoidX cannot switch the installed Minecraft Bedrock version. The version card shows only what is
  installed. There is no "install older version" feature.
- Imported packs are kept in VoidX's private storage. Whether Minecraft applies a pack after it is
  opened depends on Minecraft; VoidX does not claim it has been applied.

## Project structure

```
app/src/main/java/com/voidx/client/
  MainActivity.kt, VoidXApp.kt       entry points
  domain/model/Models.kt             pure models (no Android types)
  data/AppContainer.kt               manual dependency container
  data/minecraft/MinecraftDetector   package detection + launch intent
  data/packs/PackRepository.kt       import, validation, library index
  data/download/DownloadRepository   DownloadManager wrapper
  data/prefs/PreferencesRepository   DataStore preferences
  ui/AppViewModel.kt                 state + actions for all screens
  ui/VoidXNavHost.kt                 bottom navigation
  ui/screens/                        Home, Packs, Downloads, Settings
  ui/theme/, ui/components/          Material 3 theme, original VoidX logo
```

Stack: Kotlin 2.0.20, AGP 8.5.2, Compose BOM 2024.09.02, Navigation Compose, DataStore,
`compileSdk 34`, `minSdk 26`, `targetSdk 34`.

## Build instructions

### Option A: Android Studio

1. Open the `VoidXClient` folder in Android Studio (Ladybug or newer).
2. Let Gradle sync. Android Studio creates the Gradle wrapper automatically.
3. Run **Build > Build APK(s)** or **Run** on a device.
   The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

### Option B: command line

Requires JDK 17 and the Android SDK (`ANDROID_HOME` set), and Gradle 8.9 on PATH:

```bash
gradle wrapper --gradle-version 8.9   # creates ./gradlew once
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease        # signed only if signing is configured (below)
```

### Option C: GitHub Actions (no local setup)

The workflow `.github/workflows/build-apk.yml` builds on every push, pull request, or manual run.

1. Push this project to a GitHub repository.
2. Open the **Actions** tab, select **Build VoidX APK**, and wait for the run to finish.
3. Open the run and download **VoidX-debug-apk** (and **VoidX-release-apk**) from the
   **Artifacts** section at the bottom of the page.

## Release signing

Release APKs are signed only when a keystore is provided. Without one, the release build is
produced unsigned and Android will not install it.

Create a keystore once:

```bash
keytool -genkeypair -v -keystore voidx-release.jks -alias voidx \
  -keyalg RSA -keysize 2048 -validity 10000
```

Local builds, using `keystore.properties` in the project root (git-ignored):

```properties
storeFile=/absolute/path/to/voidx-release.jks
storePassword=...
keyAlias=voidx
keyPassword=...
```

GitHub Actions: add repository secrets `VOIDX_KEYSTORE_BASE64`
(`base64 -w0 voidx-release.jks`), `VOIDX_KEYSTORE_PASSWORD`, `VOIDX_KEY_ALIAS`, and
`VOIDX_KEY_PASSWORD`. The workflow then signs the release APK.

## Installing the APK on a phone

1. Copy the APK to the device or download it from the Actions artifact.
2. Allow installs from that source when Android asks.
3. Open the file to install.

## Brand notice

VoidX Client is an independent project. It is not affiliated with, endorsed by, or sponsored by
Mojang or Microsoft. Minecraft is a trademark of Mojang Synergies AB. All VoidX branding and
artwork in this repository is original.
