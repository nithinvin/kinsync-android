# kinsync-android

Native Android (Kotlin) client for **KinSync** — a passive daily-pattern monitor for elderly
people living alone. This repo is the mobile half of the project; the backend lives in the
sibling [`kinsync-api`](../kinsync-api) repo.

Full requirements and architecture are documented there:
- `../kinsync-api/specs/spec.md` — requirements & acceptance criteria
- `../kinsync-api/specs/plan.md` — architecture, tech stack, and phased execution plan
- `../kinsync-api/specs/deployment.md` — backend deployment guide

This repo's own docs cover what's specific to the Android client:
- [`docs/design.md`](docs/design.md) — Phase-1 architecture, package layout, key decisions
- [`docs/setup.md`](docs/setup.md) — dev-machine and phone setup/tooling
- [`docs/deployment.md`](docs/deployment.md) — building and installing on a device
- [`CONSTITUTION.md`](CONSTITUTION.md) — non-negotiable coding/quality/security standards for
  this repo

## Current status: Phase-1

Per `plan.md`, Phase-1 proves the two biggest technical risks early, with **no business logic
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
