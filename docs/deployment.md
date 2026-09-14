# Building & Deploying to a Device (Phase-1)

This is the Android counterpart to `kinsync-api`'s `specs/deployment.md`. There is no server-side
deployment for this repo — "deployment" here means getting a build onto the elder's (or a test)
phone.

## 1. Debug build → real device (day-to-day development)

```bash
./gradlew installDebug     # builds app-debug.apk and installs it over adb
adb shell am start -n com.kinsync.android/.MainActivity
```

Logs while developing:

```bash
adb logcat --pid=$(adb shell pidof -s com.kinsync.android)
```

## 2. Release build (unsigned, Phase-1)

Phase-1 does not yet define a signing/release-distribution pipeline (no Play Store listing, no
internal app-sharing service configured). Debug builds are sufficient for the Review 2 demo. To
produce a release-shaped build for local testing only:

```bash
./gradlew assembleRelease
```

The resulting `app/build/outputs/apk/release/app-release-unsigned.apk` is **unsigned** and cannot
be installed as-is; `adb install` will reject it. Building a real release pipeline (keystore
generation, `signingConfig`, Play App Signing or an internal distribution channel) is deferred to
a later phase once the app has real functionality worth distributing beyond the dev team.

## 3. First-run checklist on the phone

After installing, walk through onboarding once to confirm the Phase-1 slice works end-to-end:

1. **Consent screen** — read the plain-language explanation, tap "I understand, continue".
2. **Usage access screen** — tap "Open settings", grant "Permit usage access" for KinSync in the
   Settings screen that opens, then return to the app (the "Continue" button enables
   automatically once granted).
3. **Battery optimization screen** — tap "Allow background activity" and confirm the system
   dialog; a `POST_NOTIFICATIONS` prompt will also appear on Android 13+. Tap "Continue".
4. You should land on the **debug screen**:
   - A "Backend reachable: …" or "Backend unreachable: …" banner (the Phase-1 stretch-goal
     `/health` check).
   - A live-updating list of unlock/screen events — lock and unlock the phone a few times and
     confirm new rows appear within a second or two.
   - A persistent low-priority notification ("KinSync is watching over you") confirming the
     foreground service is running.

## 4. Verifying the backend connection

The app reads its backend URL from `BuildConfig.KINSYNC_BASE_URL` (see
[design.md](design.md#backend-base-url)). To point a build at a different backend without
editing tracked files:

```bash
./gradlew installDebug -PKINSYNC_BASE_URL=https://your-alternate-host
```

To confirm independently of the app that the backend is reachable (matches what the debug screen
shows):

```bash
curl -s https://kinsync.ddns.net/health
curl -s https://kinsync.ddns.net/health/db
```

## 5. Uninstalling / resetting local state

Both the on-device Room database (unlock events) and the consent/onboarding flags live in normal
app storage, so a plain uninstall clears everything (FR-7.3-equivalent local reset):

```bash
adb uninstall com.kinsync.android
```

The in-app "Stop monitoring" button on the debug screen does the same thing without uninstalling:
it revokes local consent, stops the foreground service, and returns to the consent screen.
