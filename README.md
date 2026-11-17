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

## Important distinction
Balances are the latest validated reported observations, not live bank API balances. Each observation retains its source, balance type, event timestamp and receipt timestamp.

## Development status
Phases 1-3 source foundations are committed. Automated build/test verification and live notification ingestion remain outstanding.

See [requirements](docs/REQUIREMENTS.md), [architecture](docs/ARCHITECTURE.md), [parser design](docs/BANK_PARSERS.md) and [Phase 3 notes](docs/PHASE_3.md).
