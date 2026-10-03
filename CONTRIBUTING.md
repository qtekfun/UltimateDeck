<!--
SPDX-FileCopyrightText: 2026 UltimateDeck contributors
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Contributing

Thanks for helping! A few rules keep UltimateDeck free, reliable and easy to review.

## Ground rules

- **Free software only.** No Google Play Services, Firebase, analytics, crash reporters or any non-free dependency. Before adding a dependency, open an issue: its license must be compatible with GPL-3.0-or-later.
- **No telemetry**, of any kind.
- **Only what Deck can store.** Features Deck cannot save on the server (epics, sprints, custom fields…) are out of scope; see SPEC.md.
- **Every visible string in `strings.xml`**, in English (`values/`) and Spanish (`values-es/`).
- **SPDX header** in every source file: `SPDX-License-Identifier: GPL-3.0-or-later`.

## Workflow

1. One task or fix per branch (`feat/…`, `fix/…`), started from `master`.
2. Small, atomic commits following [Conventional Commits](https://www.conventionalcommits.org) (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`…).
3. `./gradlew check` must pass before you push: unit tests, detekt, ktlint, Android Lint (warnings are errors) and Kover.
4. Open a pull request against `master`; CI runs the same checks.

## Code

- Kotlin, Jetpack Compose and Material 3; MVVM with `ui` / `domain` / `data` / `sync` layers and unidirectional data flow.
- Room is the single source of truth: the UI reads Room, never the network.
- Network and IO errors are typed results (`ApiResult`, sealed classes), not exceptions reaching the UI.
- Room schema changes need a new version, a migration and a migration test.
- Immutable by default; one public class per file; business logic in `domain`, not in composables.
- Accessibility: content descriptions, 48 dp touch targets, layouts that work at 200% font size.

## Tests

- JUnit 5, MockK, Turbine, MockWebServer and in-memory Room for unit tests; Compose UI tests for the key flows.
- Coverage (Kover): at least 85% over `domain`, `data` and `sync`; **100%** on the sync queue and the conflict resolver.
- A test must be able to fail for a real reason: no empty or tautological tests to raise the numbers.

## Dependencies and versions

Dependabot keeps versions up to date; do not bump them by hand. When dependencies change, regenerate `gradle/verification-metadata.xml` from a clean Gradle home.
