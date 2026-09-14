# KinSync Android — Design Notes (Phase-1)

**Companion documents:** the authoring specs live in the sibling `kinsync-api` repo at
`../kinsync-api/specs/spec.md` (requirements) and `../kinsync-api/specs/plan.md` (full
architecture, all phases). This document only covers what's specific to the Android client and
what was actually built in Phase-1.

## Scope of this document

Phase-1's job (per `plan.md`) is to retire two technical risks early:

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
│                              foreground Service, boot receiver (FR-2.1, FR-2.4)
├── data/                      Room database + type converters
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
- **`BootCompletedReceiver`** restarts the service after a reboot, but only if the elder has
  already completed onboarding — collection never starts without consent (FR-2.4, FR-7.1).
- **Room stores only local, on-device signals.** `UnlockEvent(eventType, timestampEpochMillis)` —
  nothing else. No raw content, no app names yet (that's Phase-2's `UsageStatsManager` work), no
  location. This is the schema-level enforcement of NFR-1.
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

## What's explicitly out of scope for Phase-1

Per `plan.md`: baseline computation, deviation detection, escalation logic, pairing, FCM push,
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
