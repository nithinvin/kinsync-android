# Building & Deploying to a Device

Backend operations are in the [kinsync-docs runbooks](https://github.com/nithinvin/kinsync-docs/tree/main/runbooks); preparing a phone
for a review demo is [android-demo-device](https://github.com/nithinvin/kinsync-docs/blob/main/runbooks/android-demo-device.md). There is no server-side
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

### Updating a phone without losing its data

- **Shared debug keystore.** `adb install -r` only updates an app signed with the same key. The
  team uses one debug keystore (Sri Hasini's), copied to each developer machine at the path
  Gradle uses — `~/.android/debug.keystore`, or `~/.config/.android/debug.keystore` on newer
  setups. Its SHA-256 fingerprint starts `EE:BE:D8:96`. Check with
  `keytool -list -v -keystore <path> -storepass android | grep SHA256`. Never commit it.
  A build signed with a different key fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; **do not
  uninstall** to get round that on the demo phone — get the shared keystore instead.
- **Back up first:** copy `databases/kinsync.db`, `kinsync.db-wal` and `kinsync.db-shm` off the
  phone with `adb exec-out run-as com.kinsync.android cat databases/<file> > <file>` (works for
  debug builds). Keep the copy off GitHub — it is real personal data.
- **Install:** `adb -s <phone-serial> install -r app/build/outputs/apk/debug/app-debug.apk`.
- **Open the app afterwards.** Installing an update stops the monitoring service; opening
  KinSync starts it again (since `cf166db`).
- **Instrumented tests on an emulator only.** `./gradlew connectedAndroidTest` uninstalls the app
  from every connected device when it finishes. Use an emulator and run the tests by serial
  (commands in [CLAUDE.md](../CLAUDE.md#commands)).

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

1. **Consent screen** — read the list of collected signals, tap "I agree, continue". (After
   an upgrade that adds signals, the same screen appears titled "KinSync has changed".)
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

The in-app "Stop monitoring" button on the debug screen revokes local consent, stops the foreground
service and returns to the consent screen, without deleting collected data. "No, don't monitor me" on
the consent screen does the same.
