# kinsync-android

Native Android (Kotlin) client for **KinSync** — a passive daily-pattern monitor for elderly
people living alone. This repo is the mobile half of the project; the backend is
[nithinvin/kinsync-api](https://github.com/nithinvin/kinsync-api).

## Project docs

Requirements, design, API contract, roadmap and runbooks live in
**[nithinvin/kinsync-docs](https://github.com/nithinvin/kinsync-docs)**:
[specs](https://github.com/nithinvin/kinsync-docs/tree/main/specs) ·
[design](https://github.com/nithinvin/kinsync-docs/tree/main/design) ·
[API contract](https://github.com/nithinvin/kinsync-docs/blob/main/design/api-contract.md) ·
[roadmap & status](https://github.com/nithinvin/kinsync-docs/blob/main/plan/roadmap.md) ·
[demo-phone runbook](https://github.com/nithinvin/kinsync-docs/blob/main/runbooks/android-demo-device.md).

This repo's own docs cover what's specific to the Android client:
- [`docs/design.md`](docs/design.md) — Phase-1 architecture, package layout, key decisions
- [`docs/setup.md`](docs/setup.md) — dev-machine and phone setup/tooling
- [`docs/build-and-install.md`](docs/build-and-install.md) — building and installing on a device
- [`CONSTITUTION.md`](CONSTITUTION.md) — non-negotiable coding/quality/security standards for
  this repo

## Current status: Phase-2 (version `0.2.0-phase2`, for Review III)

Phase-1 (tag `review-2`) proved the two biggest technical risks: a foreground service capturing
unlock / screen events into Room, onboarding with plain-language permission screens, a debug
list, and a `/health` check against `kinsync-api`.

Per [phase-2](https://github.com/nithinvin/kinsync-docs/blob/main/plan/phase-2.md), Phase-2
collects every on-device signal and shows it to the elder. Everything stays on the phone; there
is still no baseline, deviation detection, pairing or escalation.

- ✅ Versioned consent listing every collected signal; re-consent after an upgrade (M1).
- ✅ App-usage intervals from `UsageStatsManager`, collected every 15 minutes (M2).
- ✅ "Last moved" from the significant-motion sensor, time only (M3).
- ✅ Still / walking / in a vehicle from the Activity Recognition Transition API, with its own
  permission screen (M4).
- ✅ "Your day so far" summary as the main screen: first unlock, unlocks, screen time, top
  apps, last moved, time per activity (M5).
- ✅ "My day" timeline: a 24-hour band and a list in time order of phone sessions (with the
  apps used), activity periods and movements, with a day picker (M6).
- 🟡 Wrap-up: version `0.2.0-phase2`, docs, demo script; tag `review-3` at the demoed commit
  (M7).

Live step tracker: kinsync-docs
[`plan/phase-2.md` §10](https://github.com/nithinvin/kinsync-docs/blob/main/plan/phase-2.md).

## Quick start

```bash
./gradlew assembleDebug     # build
./gradlew test              # JVM unit tests
./gradlew installDebug      # install on a connected device/emulator
```

See [docs/setup.md](docs/setup.md) if you don't yet have the Android SDK / Gradle set up.

## Tech stack

Kotlin, Jetpack Compose (Material 3), Navigation-Compose, Room, OkHttp, coroutines. `minSdk` 26,
`compileSdk`/`targetSdk` 35. No DI framework yet (see `docs/design.md`); manual dependency wiring
via `AppContainer`.
