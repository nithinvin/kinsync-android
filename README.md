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

## Current status: Phase-1

Per [phase-1](https://github.com/nithinvin/kinsync-docs/blob/main/plan/phase-1.md), Phase-1 proves the two biggest technical risks early, with **no business logic
yet** (no baseline, deviation detection, pairing, or escalation):

- ✅ Onboarding screen requesting the `PACKAGE_USAGE_STATS` special permission and the
  battery-optimization allowlist, each with a plain-language rationale.
- ✅ A `BroadcastReceiver` (kept alive via a foreground `Service`) capturing
  `ACTION_USER_PRESENT` / screen on-off events into a local Room database.
- ✅ A debug/list screen showing captured events live.
- ✅ Stretch goal: a `/health` check against the deployed `kinsync-api` backend on app launch.

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
