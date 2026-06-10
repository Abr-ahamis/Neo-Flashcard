# Neo-Flashcard

A modern Android flashcard application built with Jetpack Compose and Room.

## Prerequisites

To compile and run this project, you need:
* **JDK 17** (Ensure `JAVA_HOME` is set to JDK 17)
* **Android SDK** (API 34)
* **Android Debug Bridge (adb)** (For installation on a device)

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone <your-repo-url>
   cd flashneo
   ```

2. **Navigate to the project directory:**
   ```bash
   cd Neo-Flashcard
   ```

## Compilation and Build Commands

Run these commands from within the `Neo-Flashcard` directory:

### 1. Clean the project
Removes previous build artifacts to ensure a fresh start:
```bash
./gradlew clean
```

### 2. Compile and Build APK
Builds the debug APK (located in `build/outputs/apk/debug/` after completion):
```bash
./gradlew assembleDebug
```

### 3. Build Release Bundle (AAB)
If you are preparing for the Play Store:
```bash
./gradlew bundleRelease
```

## Installation Commands

### 1. Install Debug APK on a connected device
Make sure your device has "USB Debugging" enabled:
```bash
./gradlew installDebug
```

### 2. Launch the app via Command Line
After installation, you can launch the app directly:
```bash
adb shell am start -n com.neo.flashcard/com.neo.flashcard.MainActivity
```

## Testing and Verification

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests (on device)
```bash
./gradlew connectedAndroidTest
```

## Project Structure

The project uses a flattened structure for simplicity. All core source code and configurations are located within the `Neo-Flashcard` directory.
