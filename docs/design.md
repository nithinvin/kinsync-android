# KinSync Android — Design Notes (Phase-1)

**Companion documents:** project-wide requirements, architecture, API contract and roadmap live
in [kinsync-docs](https://github.com/nithinvin/kinsync-docs) —
[requirements](https://github.com/nithinvin/kinsync-docs/blob/main/specs/requirements.md),
[architecture](https://github.com/nithinvin/kinsync-docs/blob/main/design/architecture.md),
[API contract](https://github.com/nithinvin/kinsync-docs/blob/main/design/api-contract.md),
[roadmap](https://github.com/nithinvin/kinsync-docs/blob/main/plan/roadmap.md). This document only covers what's specific to the Android client
and what was actually built in Phase-1.

## Scope of this document

Phase-1's job (per [phase-1](https://github.com/nithinvin/kinsync-docs/blob/main/plan/phase-1.md)) is to retire two technical risks early:

1. Do the Android background-collection APIs (unlock/screen broadcasts, special permissions,
   battery-optimization allowlisting) behave as expected on a real device?
2. Can this client already reach the deployed backend over HTTPS?

No baseline computation, deviation detection, pairing, or escalation logic exists yet — that's
Phase-2 onward.

## Package layout

```
com.kinsync.android
├── KinSyncApplication.kt      Application subclass, owns the manual DI container
├── AppContainer.kt            Manual dependency container (no DI framework yet — see CONSTITUTION.md §VII)
├── MainActivity.kt            Single Activity hosting all Compose content
├── consent/                   Local consent + onboarding-completion state (FR-7.1, FR-7.3)
├── collector/                 Unlock/screen event capture: Room entity, DAO, BroadcastReceiver,
│                              foreground Service, boot and app-update receivers (FR-2.1, FR-2.4)
├── usage/                     App-usage intervals: UsageStatsManager reader, interval builder,
│                              collector, WorkManager job, Room entity + DAO (FR-2.2)
├── movement/                  "Last moved": significant-motion trigger, Room entity + DAO (FR-2.7)
├── data/                      Room database, migrations + type converters (schemas in app/schemas/)
├── permissions/                Usage-access + battery-optimization permission helpers (FR-2.2)
├── network/                   HealthApiClient — the Phase-1 stretch-goal `/health` check only
└── ui/                        Compose screens (onboarding flow + debug event list), theme, nav
```

## Key design decisions

- **No DI framework yet.** A single `AppContainer` interface + `DefaultAppContainer`
  implementation wires up the database, consent manager, and network client. Hilt/Dagger can be
  introduced in a later phase if the object graph grows enough to justify it (constitution §VII).
- **Foreground `Service` for collection, not just a manifest receiver.** `ACTION_SCREEN_ON` /
  `ACTION_SCREEN_OFF` / `ACTION_USER_PRESENT` are implicit system broadcasts that Android has
  blocked from manifest registration since API 26. `MonitoringService` registers
  `UnlockEventReceiver` dynamically and keeps the process alive (foreground service, `specialUse`
  type) so collection survives normal OEM background-process culling — a prerequisite for the
  dead-man's-switch design in later phases (NFR-2).
- **`BootCompletedReceiver`** restarts the service after a reboot, and
  **`PackageReplacedReceiver`** (`MY_PACKAGE_REPLACED`) restarts it after an app update, but
  only when `ConsentState.canCollect` is true (onboarding finished and the current consent text
  agreed) — collection never starts without consent (FR-2.4, FR-7.1).
- **Room stores only local, on-device signals.** `UnlockEvent(eventType, timestampEpochMillis)`
  `AppUsageInterval(packageName, startEpochMillis, endEpochMillis)` and
  `MovementEvent(timestampEpochMillis)`. No content, no location. App names stay on the phone
  (NFR-1).
- **"Last moved" uses the significant-motion sensor only.** `SignificantMotionDetector` arms
  Android's one-shot `TYPE_SIGNIFICANT_MOTION` trigger inside `MonitoringService` and re-arms it
  after each trigger; only the time is stored. There is no accelerometer sampling and no step
  counting (Phase-2 decision 1). Phones without the sensor show "last moved is not available".
  Movement is recorded only while `ConsentState.canCollect` is true, because Android can
  restart the service (`START_STICKY`) without a consent check.
- **App usage is collected in the background with WorkManager**, once when monitoring starts
  (including every app open) and then every 15 minutes. `MonitoringService.start` schedules the
  job and `MonitoringService.stop` cancels it; the job itself also checks consent and usage
  access before reading anything. Each run reads `UsageStatsManager.queryEvents()` from a stored
  cursor to now and turns resume/pause events into foreground intervals
  (`AppUsageIntervalBuilder`, plain Kotlin, JVM-tested). An app still in the foreground holds
  the cursor at its start so the next run finishes that interval; rows are unique on
  (package, start), so re-reading never duplicates. Reads never go back more than 24 hours.
- **Every schema change has a real Room migration** (`data/Migrations.kt`), never a
  destructive fallback, so a phone updated in place keeps its history. Exported schemas in
  `app/schemas/` are packaged into the instrumented tests, and `MigrationTest` checks each step.
- **`HealthApiClient`** is the only network code in Phase-1, and it only ever calls
  `GET /health` — no elder activity data is sent. The backend base URL is validated as `https://`
  at startup (`BackendConfig.requireHttps`, in `DefaultAppContainer`) rather than inside the
  reusable client, so the client itself stays trivially unit-testable against a plain-HTTP
  `MockWebServer`.
- **Large-text Material 3 theme.** `KinSyncTypography` bumps default type sizes to satisfy the
  elder-facing usability requirement (NFR-6) ahead of any dedicated accessibility pass.
- **Onboarding is a 3-step linear flow**: consent → usage-access rationale (Settings deep-link) →
  battery-optimization allowlist (+ `POST_NOTIFICATIONS` request on API 33+) → debug/home screen.
  Each permission screen re-checks its permission on `ON_RESUME` so the "Continue" button unlocks
  automatically after the user returns from Settings.
- **Consent is versioned** (`CURRENT_CONSENT_VERSION` in `consent/ConsentManager.kt`). The
  consent screen lists every collected signal; whenever that list changes, the version is bumped.
  An install that agreed to an older text (Phase-1 installs count as version 1) is shown the
  consent screen again with a "KinSync has changed" title. Agreeing returns straight to the main
  screen; declining stops `MonitoringService` and clears consent. Collectors added from Phase-2
  onwards must check `ConsentState.canCollect` before recording anything.

## What's explicitly out of scope for Phase-1

Per [phase-1](https://github.com/nithinvin/kinsync-docs/blob/main/plan/phase-1.md): baseline computation, deviation detection, escalation logic, pairing, FCM push,
caregiver app/UI, SMS fallback, and Activity Recognition motion capture. `UsageStatsManager`
querying itself is also deferred to Phase-2 — Phase-1 only requests the permission and shows the
rationale screen, per the plan's explicit Phase-1 deliverable list.

## Backend base URL

The app reads the backend origin from `BuildConfig.KINSYNC_BASE_URL`, sourced from the
`KINSYNC_BASE_URL` Gradle property (set in [gradle.properties](../gradle.properties), currently
pointing at the Hetzner-deployed instance). Override it per-build with
`-PKINSYNC_BASE_URL=https://your-host` without editing tracked files.

## Threat-model notes specific to this client

- `PACKAGE_USAGE_STATS` and battery-optimization exemption are both sensitive, user-visible
  permissions; both onboarding screens explain, in plain language, exactly why they're requested
  before the Settings deep-link is shown (FR-2.2).
- `UnlockEventReceiver` is registered with `RECEIVER_NOT_EXPORTED` — no other app on the device
  can trigger it directly.
- The monitoring foreground-service notification (`IMPORTANCE_MIN`) is intentionally low-priority
  but never hidden — the elder must always be able to see that monitoring is active (transparency,
  FR-2.5's spirit, even though the full transparency log screen is a later phase).
