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

## Current implementation
- Android Compose application shell and navigation.
- Room/SQLite local persistence for bank accounts and balance observations.
- Add-account workflow supporting NGN, USD and other three-letter currency codes.
- Multiple accounts under the same institution.
- Currency-separated totals using decimal arithmetic.
- Independent controls to exclude an account from totals or hide it from the dashboard.
- Manual opening/latest balance entry for initial testing.

## Important distinction
Balances are the latest validated reported observations, not live bank API balances. Each observation retains its source, balance type, event timestamp and receipt timestamp.

## Development status
Phase 1 foundation and Phase 2 local account management/dashboard are committed. SMS/app notification ingestion, parsers, Gmail, cloud sync and automated tests remain future phases.

See [requirements](docs/REQUIREMENTS.md), [architecture](docs/ARCHITECTURE.md) and [parser design](docs/BANK_PARSERS.md).
