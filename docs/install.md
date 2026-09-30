# Installation & Build Guide

This guide covers building, testing, and running **Murmr** on Android, Desktop (JVM), and iOS.

---

## 1. Prerequisites

| Component | Required Version | Notes |
|---|---|---|
| **JDK** | **Java 21 LTS** (Temurin recommended) | **Crucial:** Do not use Java 25 or higher due to netty/ktor compatibility issues. |
| **Android SDK** | API Level 34+ | Platform Tools, Build-Tools 34.0.0, NDK r26 |
| **Gradle** | 8.14+ (included via wrapper) | `./gradlew` |
| **macOS & Xcode** | macOS 14+, Xcode 15+ | **Only required for building the iOS target** |

---

## 2. Environment Setup

### Configure `JAVA_HOME`
Ensure your terminal session points to Java 21:

**Linux / macOS:**
```sh
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
```

Verify your active Java version:
```sh
java -version
# Expected: openjdk version "21.x.x"
```

### Configure Android SDK
Ensure `ANDROID_HOME` or `ANDROID_SDK_ROOT` is set in your environment:
```sh
export ANDROID_HOME=$HOME/Android/Sdk
```
Or create a local file `local.properties` in the root of `murmr-app/`:
```properties
sdk.dir=/path/to/your/android/sdk
```
*(Note: `local.properties` is intentionally ignored by git to keep your paths private).*

---

## 3. Building for Android

### Debug APK
Compile the debug build:
```sh
./gradlew :composeApp:assembleDebug
```
Output location:
`composeApp/build/outputs/apk/debug/composeApp-debug.apk`

### Direct Install on Connected Device
With developer options and USB debugging enabled on your Android phone:
```sh
./gradlew :composeApp:installDebug
```

### Release APK
Compile an optimized release build:
```sh
./gradlew :composeApp:assembleRelease
```
Output location:
`composeApp/build/outputs/apk/release/composeApp-release.apk`

---

## 4. Running the Desktop Application (JVM)

You can launch and test Murmr directly on your desktop machine (Windows, macOS, or Linux) with full audio input/output and SQLite storage:

```sh
./gradlew :composeApp:run
```

Desktop configuration and databases are persisted locally in `~/.murmr/`.

---

## 5. Building for iOS (macOS required)

1. Open `iosApp/Murmr.xcodeproj` in Xcode 15+:
   ```sh
   open iosApp/Murmr.xcodeproj
   ```
2. Or generate the shared multiplatform framework from the command line:
   ```sh
   ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```
3. Select your iOS simulator or physical connected device and click **Run** (Cmd + R) in Xcode.

---

## 6. Running Automated Tests

To run the unit test suite across common and platform targets:
```sh
# Run common and JVM unit tests
./gradlew test

# Run Android unit tests
./gradlew :composeApp:testDebugUnitTest
```
