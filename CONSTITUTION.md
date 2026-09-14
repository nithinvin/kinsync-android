<!--
SYNC IMPACT REPORT
==================
Version change  : 1.0.0 → 2.0.0
Modified sections: Core Principles (I–VII) — retargeted from Python to Kotlin/Android,
                   Code Quality, Security, Non-functional Requirements, Governance
Rationale        : kinsync-android is a native Android/Kotlin client, not a Python service.
                   The original ratification copied the kinsync-api (Python/FastAPI) template
                   verbatim; this amendment replaces all language- and tooling-specific
                   content with Kotlin/Android/Gradle equivalents while preserving intent.
TODOs            : none — all placeholders resolved
-->

# kinsync-android Constitution

## Core Principles

### I. Coding Standards (NON-NEGOTIABLE)

All Kotlin source MUST conform to the
[official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and be
formatted with `ktlint` (or Android Studio's built-in Kotlin formatter set to the official style),
max line length 120 characters.

Naming conventions:
- `camelCase` for functions, properties, and local variables.
- `PascalCase` for classes, interfaces, objects, and Composable functions.
- `UPPER_SNAKE_CASE` for top-level and companion-object constants.
- Prefix backing properties / private helpers with `_` only where idiomatic (e.g. `_uiState`).
- Composable functions are the one exception to "functions are verbs" — they are named as
  `PascalCase` nouns describing the UI they emit.

Package structure MUST follow feature-first organization under `com.kinsync.android`
(e.g. `collector`, `onboarding`, `data`, `network`, `ui`), not layer-first (`models`, `utils`).

Imports MUST NOT use wildcard (`import foo.bar.*`); rely on Android Studio's per-symbol imports.

Use descriptive names. Avoid single-letter variables except simple loop counters (`i`, `j`) and
conventional Compose scope receivers (`it`).

### II. Quality Gates (NON-NEGOTIABLE)

Every implementation MUST satisfy all of the following before a task is considered complete:

- Zero `ktlintCheck` violations.
- Zero `detekt` findings (configuration in `config/detekt/detekt.yml`).
- Zero warnings from `./gradlew lint` (Android Lint) at the `error`-severity threshold.
- All unit tests passing (`./gradlew test`).
- Build succeeds via `./gradlew assembleDebug`.

No merge is permitted while any gate is red.

### III. Testing Principles

Every code change MUST include tests covering:

- Happy path — expected successful behaviour.
- Error path — failure and exception handling (e.g. no network, permission denied, DB failure).
- Edge cases — boundary values and unusual inputs (e.g. clock changes, empty event log).
- Empty / malformed input — robustness under bad data.

Additional requirements:
- New ViewModels/UseCases require local unit tests (JVM, no device needed) using JUnit +
  kotlinx-coroutines-test.
- Room DAOs require instrumented tests against an in-memory database.
- New UI flows require at least one Compose UI test for the primary interaction.
- Every new code path MUST be exercised by at least one test.
- Branch coverage is preferred over simple line coverage.

### IV. Refactoring Principles

Modified code MUST be continuously evaluated for:

- Small, focused functions and Composables (single responsibility).
- Duplicate code elimination (DRY).
- Long method decomposition.
- Long file splitting (files ~400+ lines SHOULD be considered for splitting).
- Better naming.
- Helper/extension-function extraction.

Refactoring opportunities identified during implementation MUST be reported,
even when not immediately addressed.

### V. Assertive Programming

- Validate all inputs at system boundaries, immediately and explicitly (Intent extras,
  Bundle args, network responses, broadcast extras).
- Validate configuration (e.g. base URL, feature flags) on app startup before any processing
  begins.
- Validate HTTP/external responses before consuming them; never assume a 2xx body is well-formed.
- Raise specific, descriptive exceptions — never catch or throw a bare `Exception`/`Throwable`.
- Never silently swallow exceptions; log with context or surface to the UI/caller.
- Use `require`/`check`/`assert` only for internal invariants that MUST never be false in
  correct code; use sealed `Result`-style types for expected, recoverable failure paths.

### VI. SOLID

- **Single Responsibility**: Every class, ViewModel, and use case has exactly one reason to
  change.
- **Open/Closed**: Extend behaviour via new code (new use cases, new Composables); avoid
  modifying stable, tested code.
- **Liskov Substitution**: Subtypes/implementations MUST be substitutable for their base
  types/interfaces without altering correctness.
- **Interface Segregation**: Clients MUST NOT be forced to depend on interfaces they do not use;
  prefer narrow, focused abstractions (e.g. separate `EventCollector` from `EventReader`).
- **Dependency Inversion**: High-level modules (ViewModels, use cases) MUST NOT depend directly
  on low-level modules (Room, platform APIs); depend on interfaces, injected via constructor.

### VII. Design Principles

- Prefer unidirectional data flow (UDF): state flows down (`StateFlow`/Compose state), events
  flow up (lambdas/callbacks).
- Use the Repository pattern to isolate ViewModels/use cases from data-source details (Room,
  network, platform APIs).
- Favor Kotlin `sealed class`/`sealed interface` for representing UI/result state over booleans
  or nullable flags.
- Avoid God classes/Activities — no class should own too many responsibilities; keep Activities
  thin (host Compose content + platform glue only).
- Do not introduce architectural patterns or dependencies (DI frameworks, extra libraries)
  unless they demonstrably simplify maintenance for the current phase.

## Code Quality

### Code Smells Policy

The following are prohibited and MUST be corrected before completion:

- Duplicate code (DRY violation).
- Dead code (unreachable or unused).
- Magic numbers or magic strings (extract to named constants).
- Long functions/Composables exceeding a single screen of logic.
- Happy-path-only testing.
- Business logic embedded directly in Activities/Composables instead of ViewModels/use cases.

### Logging Policy

- Use `android.util.Log` (or a thin wrapper) with a consistent tag per class/feature; NEVER use
  `println`/`print` for diagnostics.
- Apply appropriate log levels: `Log.d` for trace detail, `Log.i` for lifecycle events,
  `Log.w` for recoverable anomalies, `Log.e` for failures.
- Never log raw activity content, credentials, tokens, or other sensitive values — this is
  doubly critical given the privacy-by-construction goal of this app (see `docs/design.md`).
- Include sufficient diagnostic context (e.g. event type, operation name) so failures are
  actionable without a debugger.

## Security

- Treat ALL external inputs (Intents from other apps, deep links, network responses,
  broadcast extras) as untrusted; validate before use.
- Never expose secrets, keys, or credentials in source code, logs, or error messages; no
  credentials are expected in this client app, but tokens issued by the backend (from Phase-2
  onward) MUST be stored using `EncryptedSharedPreferences` or the Android Keystore, never plain
  `SharedPreferences`.
- Use HTTPS exclusively for any network communication; cleartext traffic MUST remain disabled
  (default Android network security policy) with no exceptions added.
- Raw on-device activity data (unlock events, app usage, motion) MUST NEVER be transmitted
  off-device — only derived/minimal signals leave the device, per `spec.md` NFR-1.
- Follow OWASP Mobile Top 10 / OWASP Top 10 guidance; review every new code path for insecure
  data storage, insecure communication, and improper platform-permission usage.
- Dependency updates MUST be evaluated for known CVEs before adoption.

## Non-functional Requirements

Every implementation MUST consider the following dimensions:

- **Reliability**: Failures MUST be handled gracefully with clear error reporting; background
  collection MUST survive app-process death and device reboot where the design calls for it
  (see `spec.md` NFR-2).
- **Performance**: UI must remain responsive (no blocking I/O on the main thread); DB and
  network operations MUST run on background dispatchers/WorkManager.
- **Battery**: Background collection MUST be evaluated for battery impact against the
  <5% additional daily drain target (`spec.md` NFR-3) before being considered complete.
- **Scalability**: Design choices MUST not prevent future support for additional collectors,
  baseline computation, or multi-elder pairing scenarios.

## Governance

This constitution supersedes all other practices and guidelines within the `kinsync-android` project.

Amendment procedure:
1. Propose a change with a written rationale.
2. Update this file, increment `CONSTITUTION_VERSION` per semantic versioning rules
   (MAJOR: incompatible principle removal/redefinition; MINOR: new principle/section;
   PATCH: clarification/wording fix).
3. Update `LAST_AMENDED_DATE` to today's date in ISO 8601 format (YYYY-MM-DD).
4. Commit with message: `docs: amend constitution to vX.Y.Z (<summary>)`.

All pull requests MUST verify compliance with every principle herein before merging.
Complexity or deviation from these principles MUST be explicitly justified in the PR description.

**Version**: 2.0.0 | **Ratified**: 2026-09-12 | **Last Amended**: 2026-09-14
