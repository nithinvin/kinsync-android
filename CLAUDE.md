# CLAUDE.md — kinsync-android

Native Android (Kotlin) client for **KinSync** (passive daily-pattern monitor for elderly people
living alone; VIT Chennai BACSE291 Innovative Design Project, AY 2026–27). One app, two roles:
Elder (passive collection) and Caregiver (alerts) — Caregiver mode arrives in later phases.

## Sibling repos (cloned side by side)

| Repo | Local path | Use for |
|---|---|---|
| kinsync-docs | `../kinsync-docs` | Requirements, design, **API contract**, roadmap/status, runbooks |
| kinsync-api | `../kinsync-api` | Backend this app talks to (`https://kinsync.ddns.net`) |

Before feature work, read `../kinsync-docs/plan/roadmap.md` (current phase), the matching
`../kinsync-docs/plan/phase-N.md`, and `../kinsync-docs/design/api-contract.md`.
Code comments that cite "`spec.md` NFR-x / FR-x" or "`plan.md` Phase-1" refer to
`../kinsync-docs/specs/requirements.md` and `../kinsync-docs/plan/phase-1.md` (moved 2026-10-04;
IDs unchanged).

## Hard rules

- **Review before commit:** always make the changes in the working tree, summarise them and stop.
  The user reviews (and, for app changes, tests on the phone). Only after the user explicitly
  approves, commit and push. Never commit or push without that go-ahead.
- **No `Co-Authored-By` / AI attribution trailer** in commit messages.
- **Repo is public:** never commit keystores, tokens, `local.properties`, or the VM's IP.
- **Privacy (NON-NEGOTIABLE):** raw activity data (unlock events, app usage, motion) stays in the
  on-device Room DB. Only derived signals may be sent to the backend. Never log raw activity.
- Follow [CONSTITUTION.md](CONSTITUTION.md) and `../kinsync-docs/engineering/common-principles.md`.
- Network contract changes go into `../kinsync-docs/design/api-contract.md` **first**.
- Never change the production VM; ask the user.

## Commands

```bash
./gradlew assembleDebug                 # build
./gradlew test                          # JVM unit tests (app/src/test)
./gradlew test --tests 'com.kinsync.android.network.BackendConfigTest'   # single class
./gradlew connectedAndroidTest          # instrumented tests — EMULATOR ONLY, see warning below
./gradlew lint                          # Android Lint
./gradlew installDebug                  # install on connected device
./gradlew installDebug -PKINSYNC_BASE_URL=https://other-host   # point at another backend
adb logcat --pid=$(adb shell pidof -s com.kinsync.android)
```

**Demo phone safety:** the demo phone holds weeks of real collected data that the reviews rely
on. Never run `connectedAndroidTest` (or anything that uninstalls the app) while the demo phone
is connected — Gradle uninstalls the app after instrumented tests, deleting its data. Run
instrumented tests on an emulator by serial instead:

```bash
adb -s emulator-5554 install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w com.kinsync.android.test/androidx.test.runner.AndroidJUnitRunner
```

Update the phone only with `adb -s <phone-serial> install -r` (keeps data), after copying its
database off with `adb exec-out run-as com.kinsync.android cat databases/kinsync.db` (also the
`-wal` and `-shm` files) as a backup. Debug builds must be signed with the team's shared debug
keystore, or the phone refuses the update — see [docs/build-and-install.md](docs/build-and-install.md).

Needs JDK 17+ and Android SDK 35 (`local.properties` with `sdk.dir`, gitignored) — see
[docs/setup.md](docs/setup.md).

Constitution §II names `ktlintCheck` and `detekt` as gates, but both are **deferred by team
decision** (2026-10-04) — not configured in Gradle yet. Don't add them unless asked; current gates
are `./gradlew lint`, `./gradlew test`, `./gradlew assembleDebug`.

## Layout (`app/src/main/java/com/kinsync/android/`)

- `KinSyncApplication.kt`, `AppContainer.kt` — manual DI (`DefaultAppContainer`); no Hilt (ADR-0004)
- `MainActivity.kt` — single Activity hosting Compose
- `collector/` — `UnlockEventReceiver` (dynamic, `RECEIVER_NOT_EXPORTED`), `MonitoringService`
  (foreground, `specialUse`), `BootCompletedReceiver`, Room entity/DAO
- `consent/` — consent + onboarding state
- `data/` — Room `AppDatabase`, converters
- `permissions/` — usage-access and battery-optimization helpers
- `network/` — `BackendConfig` (HTTPS-only validation), `HealthApiClient` (OkHttp)
- `ui/` — Compose: onboarding flow, debug event list, theme (large-text M3), navigation

`minSdk` 26, `targetSdk`/`compileSdk` 35. Details: [docs/design.md](docs/design.md).

## Code style (from CONSTITUTION.md)

Official Kotlin conventions, 120-char lines, no wildcard imports, feature-first packages,
UDF with `StateFlow`, sealed types for UI/result state, Repository pattern, thin Activities,
`android.util.Log` with per-class tags (never `println`). Tests: happy, error, edge, malformed;
ViewModels/use cases → JVM tests; DAOs → instrumented tests with in-memory Room.

## After finishing work

Update in `../kinsync-docs`: `plan/phase-N.md` (deliverable status + SHA) and
`specs/traceability.md`. Update `docs/design.md` here if the package layout or key decisions
changed.
