# HARDIK BUDS PRO ULTRA MAX

Nothing OS-inspired Android earbuds companion engineered with AMOLED pure black UI, `#E0002B` crimson accents, real Android `DynamicsProcessing` / `AudioEffect` DSP, Android 10–16 Bluetooth & BLE telemetry, and a SHA-256 verified GitHub Releases OTA updater. **Zero Google Account / Zero Login required.**

---

## 1. Project Structure

```text
.
├── .github/workflows/
│   └── android-build.yml          # CI/CD: unit tests, Debug/Release APK, SHA-256, GitHub Releases
├── app/
│   ├── build.gradle.kts           # App dependencies & signing config
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── HardikBudsApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── audio/UltraSoundEngine.kt
│       │   │   ├── bluetooth/BluetoothEarbudsManager.kt
│       │   │   ├── data/PreferencesRepository.kt
│       │   │   ├── model/AppModels.kt
│       │   │   ├── update/
│       │   │   │   ├── AppUpdateManager.kt
│       │   │   │   ├── GitHubApiService.kt
│       │   │   │   └── UpdateCheckWorker.kt
│       │   │   ├── viewmodel/MainViewModel.kt
│       │   │   └── ui/
│       │   │       ├── components/NothingComponents.kt
│       │   │       ├── screens/
│       │   │       │   ├── AncControlScreen.kt
│       │   │       │   ├── DevicesScreen.kt
│       │   │       │   ├── HomeDashboardScreen.kt
│       │   │       │   ├── SettingsAndOnboardingSheet.kt
│       │   │       │   ├── SoundStudioScreen.kt
│       │   │       │   └── SystemUpdateScreen.kt
│       │   │       └── theme/ (Color.kt, Theme.kt, Type.kt)
│       │   └── res/ (font/, drawable/, mipmap-*/, values/, xml/)
│       └── test/java/com/example/ # Unit & Robolectric tests
└── gradle/libs.versions.toml
```

---

## 2. APK Build Instructions (Local)

1. Open the repository in **Android Studio** (or use CLI Gradle with JDK 17 & Android SDK 36).
2. Build a debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`
3. Build a signed release APK:
   ```bash
   KEYSTORE_PATH=/path/to/upload.jks \
   STORE_PASSWORD=your_store_pass \
   KEY_PASSWORD=your_key_pass \
   gradle :app:assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`

---

## 3. GitHub Publishing & Automatic Release Updates

1. Create a GitHub repository and push this project:
   ```bash
   git init
   git add .
   git commit -m "Initial release: Hardik Buds Pro Ultra Max"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
2. (Optional for signed Release APKs in CI) In **GitHub Repo → Settings → Secrets and variables → Actions**, add:
   - `RELEASE_KEYSTORE_BASE64`: Base64-encoded `.jks` keystore (`base64 -w 0 my-upload-key.jks`)
   - `STORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Key password for alias `upload`
3. Tag a release to trigger automatic APK building & GitHub Release publishing:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
   GitHub Actions automatically builds the APK, generates `.sha256` checksums, and attaches both to the GitHub Release.
4. In the app's **SYSTEM** tab, set the **GitHub Repository URL** to `https://github.com/<your-username>/<your-repo>` and tap **CHECK NOW**.

---

## 4. Testing Instructions

Run local JVM unit and Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```

### Hardware Honesty Note (OnePlus Nord Buds 3 Pro)
- **Ultra Sound DSP** (`DynamicsProcessing` 10-band EQ, Soft Limiter, `LoudnessEnhancer`, `BassBoost`, `Virtualizer`, Stereo Balance, Acoustic Test Tone, and Find-My-Buds Chime) executes natively on Android audio hardware.
- **Hardware ANC / Touch Controls / Wear Detection**: OnePlus Nord Buds 3 Pro uses a proprietary encrypted HeyMelody/OPlus protocol over RFCOMM/GATT. The app performs real BLE GATT discovery (`0x180F` Battery, `0x180A` Device Info, `0x1802` Immediate Alert) and honestly flags vendor-locked ANC controls as requiring the official HeyMelody companion app rather than simulating fake ANC toggles.
