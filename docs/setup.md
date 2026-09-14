# Development Environment Setup

This covers what to install on a **development machine** to build and run kinsync-android, and
what's needed on the **physical Android phone** used for testing. Phase-1 is meant to be proven
on a real device (per `plan.md`) — an emulator can be used for UI iteration, but the unlock-event
collection and permission flows should ultimately be verified on real hardware.

## 1. Development machine

### Required

| Tool | Why | Notes |
|---|---|---|
| **JDK 17 or newer** | Compiles Kotlin/AGP | JDK 21 works fine (used to build this project). |
| **Android SDK** (platform-tools, platform 35, build-tools 35.0.0) | Compiles/packages the app | Installed via Android Studio's SDK Manager, or the standalone `cmdline-tools` (see below). |
| **Gradle wrapper (`./gradlew`)** | Builds the project | Already checked into the repo — no separate Gradle install needed *if* you have network access on first run (it downloads Gradle 8.9 once). |

### Recommended

- **Android Studio** (latest stable, "Ladybug" or newer) — easiest way to get the SDK, an
  emulator, Kotlin/Compose tooling, and a debugger, all in one install. Not strictly required —
  the CLI-only path below works too and is what was used to validate this project.

### CLI-only setup (no Android Studio)

If you'd rather not install the full IDE:

```bash
# 1. Install a JDK (17+) via your OS package manager, e.g.:
#    Debian/Ubuntu: sudo apt install openjdk-21-jdk
#    openSUSE:      sudo zypper install java-21-openjdk-devel

# 2. Download the Android command-line tools
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
curl -sL -o cmdline-tools.zip \
  https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q cmdline-tools.zip && rm cmdline-tools.zip && mv cmdline-tools latest

# 3. Accept licenses and install exactly what this project needs
export ANDROID_HOME=~/android-sdk
yes | ~/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses
~/android-sdk/cmdline-tools/latest/bin/sdkmanager \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# 4. Point the project at your SDK (this file is gitignored — create it yourself)
echo "sdk.dir=$HOME/android-sdk" > local.properties

# 5. Build
./gradlew assembleDebug
```

> If `./gradlew` can't download its Gradle distribution due to a restrictive network, install
> Gradle 8.9 yourself (e.g. from https://gradle.org/releases/ or your package manager) and run
> `gradle assembleDebug` directly instead — the project doesn't require the wrapper specifically.

### Verify the toolchain

```bash
java -version                 # 17+
./gradlew -v                  # Gradle 8.9, Kotlin 1.9.x (wrapper's own bundled Kotlin, unrelated to app's 2.0.21)
./gradlew tasks                # confirms the project configures correctly
```

## 2. Physical Android phone (for real-device testing)

### Required on the phone

- **Android 8.0 (API 26) or newer** — this is the project's `minSdk`.
- **Google Play Services** — required later for Activity Recognition and FCM (Phase-2/3), so
  worth confirming now; most consumer Android phones have it pre-installed.
- **Developer options + USB debugging enabled**, so `adb`/Android Studio can install the app:
  1. Settings → About phone → tap "Build number" 7 times.
  2. Settings → System → Developer options → enable "USB debugging".
- **A USB cable** (or Wi-Fi debugging, API 30+) to connect the phone to the dev machine.

### Nothing else needs to be pre-installed on the phone

The app itself requests everything else it needs at first launch:
- The `PACKAGE_USAGE_STATS` special permission (via a Settings deep-link, with an in-app
  rationale screen first).
- The battery-optimization allowlist prompt (also via a Settings deep-link).
- `POST_NOTIFICATIONS` (API 33+ runtime permission, requested via the standard system dialog).

### Installing the debug build on the phone

```bash
# with the phone connected and USB debugging authorized
adb devices                      # confirm the phone is listed
./gradlew installDebug           # builds + installs in one step
```

Or manually:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

See [deployment.md](deployment.md) for release-build signing and the on-device onboarding
checklist.
