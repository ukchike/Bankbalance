# BankBalance

Android-first personal bank balance monitor.

## Purpose
BankBalance extracts the latest reported balances from user-authorised bank notifications and email, then presents a per-account dashboard and currency-separated totals.

## Product decisions
- Standalone native Android application (Kotlin + Jetpack Compose).
- Multiple accounts per institution, including foreign-currency accounts.
- Sources: SMS where permitted, Android app notifications, and Gmail read-only integration.
- Local-first storage with optional encrypted cloud synchronisation.
- Account-level inclusion and dashboard visibility controls.
- No bank credentials, PINs or OTPs are collected.

## Implemented foundation
- Android Compose application shell and navigation.
- Room/SQLite local persistence for bank accounts and balance observations.
- Add-account workflow supporting NGN, USD and other three-letter currency codes.
- Multiple accounts under the same institution.
- Currency-separated totals using decimal arithmetic.
- Independent controls to exclude an account from totals or hide it from the dashboard.
- Manual balance entry for initial testing.

## Phase 3: Bank notification intelligence
- First-pass deterministic sender identification and parser for the supplied Nigerian bank SMS formats.
- Extracts explicit balance, account hint, transaction amount/direction, currency, event date/time and warnings.
- Unit tests added for redacted examples.
- Parser is not yet connected to live SMS, app notification or Gmail ingestion.

## Phase 4: Android notification collection\n- Added an opt-in Android NotificationListenerService and shortcut to Android notification access settings.\n- Initial supported-app package catalogue and local source preferences added.\n- Parsed notification balances are matched conservatively to accounts and stored with source metadata and duplicate protection.\n- Package identifiers and real-device notification formats still require validation.\n\n## Phase 5: Gmail parsing and reconciliation foundation\n- Added a Gmail message adapter that reuses the bank parser.\n- Added a canonical cross-source fingerprint when a reliable event timestamp is present.\n- Live Gmail OAuth/API retrieval is not yet enabled; it requires Google Cloud OAuth configuration and user consent.\n\n## Important distinction
Balances are the latest validated reported observations, not live bank API balances. Each observation retains its source, balance type, event timestamp and receipt timestamp.

## Development status
Phases 1-5 foundations are committed. Android notification collection requires device validation; live Gmail authorisation/retrieval and successful CI verification remain outstanding.

See [requirements](docs/REQUIREMENTS.md), [architecture](docs/ARCHITECTURE.md), [parser design](docs/BANK_PARSERS.md) and [Phase 3 notes](docs/PHASE_3.md).
