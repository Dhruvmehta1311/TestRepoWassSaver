# WASSaver Android

Feature-rich WhatsApp Status Manager with video splitting, deleted message recovery, and more.

---

## 📁 Project Structure

```
WASSaver/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/pbhadoo/wassaver/
│       │   ├── MainActivity.kt
│       │   ├── data/model/Models.kt
│       │   ├── service/NotificationListenerService.kt
│       │   ├── ui/
│       │   │   ├── navigation/Navigation.kt
│       │   │   ├── screens/  (all screens)
│       │   │   └── theme/Theme.kt
│       │   ├── utils/
│       │   │   ├── MessageStore.kt
│       │   │   └── PrefsManager.kt
│       │   └── viewmodel/  (all viewmodels)
│       └── res/
│           ├── values/strings.xml, themes.xml
│           └── xml/  (network config, file provider, etc.)
├── build.gradle
├── settings.gradle
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
└── .github/workflows/build.yml
```

---

## 🔨 How to Build the APK

### Option A — Android Studio (Easiest)

1. **Install Android Studio** (Hedgehog or newer): https://developer.android.com/studio
2. **Open the project**: File → Open → select the `WASSaver` folder
3. Let Gradle sync complete (~2-5 minutes first time)
4. **Build debug APK** (no signing needed, installs directly):
   - Menu: Build → Build Bundle(s) / APK(s) → Build APK(s)
   - APK will be at: `app/build/outputs/apk/debug/app-debug.apk`
5. **Transfer to phone**:
   - USB cable → copy APK → open it on phone (enable "Install from unknown sources")
   - OR: `adb install app/build/outputs/apk/debug/app-debug.apk`

### Option B — Command Line (Gradle)

```bash
# 1. Make sure Java 17+ is installed
java -version

# 2. In the WASSaver folder:
chmod +x gradlew      # Mac/Linux only

# Build debug APK:
./gradlew assembleDebug       # Mac/Linux
gradlew.bat assembleDebug     # Windows

# APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Option C — GitHub Actions (Automated CI/CD)

1. Push this project to a GitHub repo
2. Go to Settings → Secrets → add these secrets (for signed release builds):
   - `KEYSTORE_BASE64`: Base64 of your keystore file (`base64 -i keystore.jks`)
   - `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
3. Push a tag like `v1.0.0`:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
4. GitHub Actions automatically builds and attaches the APK to the release

---

## 🔑 Generate a Signing Keystore (for release builds)

```bash
keytool -genkey -v \
  -keystore wassaver-release.jks \
  -alias wassaver \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Keep this file safe — you need it for all future updates.

---

## 📱 First-Time App Setup

After installing:

1. **Status Viewer**: Open the app → Status Viewer → grant folder access when prompted
   - On Android 11+, navigate to the `.Statuses` folder manually in the picker
2. **Deleted Messages**: Enable the toggle → tap it → grant Notification Access in system settings → come back and re-enable
3. **Media Browser**: Grant storage permissions when prompted

---

## ⚙️ Requirements

- Android 8.0 (API 26) or higher
- WhatsApp or WhatsApp Business installed
- Java 17+ for building
- Android Studio Hedgehog+ or Gradle 8.7+
