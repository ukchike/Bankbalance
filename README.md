# BankBalance

Android-first personal bank balance monitor.

## Purpose
BankBalance extracts the latest reported balances from user-authorised bank notifications and email, then presents a per-account dashboard and currency-separated totals.

## Product decisions
- Standalone native Android application (Kotlin + Jetpack Compose).
- Multiple accounts per institution, including foreign-currency accounts.
- Sources: SMS where permitted, Android app notifications, and Gmail read-only integration.
- Local-first storage with optional encrypted cloud synchronisation.
- Account-level inclusion and visibility controls.
- No bank credentials, PINs or OTPs are collected.

## Important distinction
Balances are the latest validated balances reported by a notification, not live bank API balances. Each balance must retain its source, balance type, event timestamp and receipt timestamp.

## Repository status
Initial product specification and Android foundation. Notification ingestion, parsers, Gmail and cloud sync are staged for subsequent implementation.

See [requirements](docs/REQUIREMENTS.md), [architecture](docs/ARCHITECTURE.md) and [parser design](docs/BANK_PARSERS.md).
