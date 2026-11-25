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

## Phase 4: Android notification collection\n- Added an opt-in Android NotificationListenerService and shortcut to Android notification access settings.\n- Initial supported-app package catalogue and local source preferences added.\n- Parsed notification balances are matched conservatively to accounts and stored with source metadata and duplicate protection.\n- Package identifiers and real-device notification formats still require validation.\n\n## Phase 5: Gmail parsing and reconciliation foundation\n- Added a Gmail message adapter that reuses the bank parser.\n- Added a canonical cross-source fingerprint when a reliable event timestamp is present.\n- Live Gmail OAuth/API retrieval is not yet enabled; it requires Google Cloud OAuth configuration and user consent.\n\n## Phase 6: Transaction intelligence and reconciliation\n- Added a separate transaction table with source provenance, debit/credit, amount, currency, event/receipt timestamps and confidence.\n- Transactions are automatically linked only when account suffix matching is unique; otherwise they remain in the review queue.\n- Added Room database migration from version 1 to version 2.\n- App-notification parsing now feeds the transaction review pipeline as well as balance observations.\n\n## Phase 8: Categorisation and budgeting foundation
- Added keyword-based suggested categories for transfers, airtime/data, utilities, food/dining, transport, subscriptions, cash withdrawals, bank charges, income, shopping and other.
- Own-account transfer keywords are checked before credit/income classification; internal transfers are separately flagged for spending analysis.
- Added category and internal-transfer fields to transactions with a Room v2-to-v3 migration.
- Added transaction category editing and an own-account transfer override in the Transactions screen.
- Added monthly budget creation, replacement and deletion by category, currency and YYYY-MM period.
- Added current-month budget-versus-actual progress and category spending summaries, excluding unmatched transactions and flagged internal transfers.
- CSV exports include category and internal-transfer fields.

## Phase 9: Closing gaps between stated requirements and implementation
- Deduplication fingerprints for both transactions and balance observations are now derived purely from message content (institution, account, currency, direction/amount or balance, event time), shared by every ingestion path. Previously SMS import, live notifications and the (still-disconnected) Gmail adapter each built their own source-specific fingerprint, so the same real-world event reported through two channels could be double-counted; this is now unified in `TransactionReviewRepository`.
- SMS import now records balance observations (and refreshes an account's dashboard balance) the same way the live notification listener does, not just transactions.
- The local database is encrypted at rest with SQLCipher. The encryption passphrase is random and is held only in Keystore-backed `EncryptedSharedPreferences` (`SecureKeyProvider`) — it is never hardcoded. Upgrading from a build that predates this change requires a fresh install or clearing app data, since SQLCipher cannot open a pre-existing plaintext database file.
- Added "Export all data" (accounts, balance history, transactions and budgets as JSON) and a confirmed, destructive "Delete account" action, closing out the data-export/deletion requirement that previously had no implementation at all.
- Added JVM unit tests for the previously-untested matching/dedup/ordering logic in `TransactionReviewRepository` (ambiguous-match-goes-to-review, cross-source dedup, late-arriving-older-observation-does-not-overwrite-newer).

## Important distinction
Balances are the latest validated reported observations, not live bank API balances. Each observation retains its source, balance type, event timestamp and receipt timestamp.

## Development status
Phase 8 category and budgeting UI is committed, and Phase 9 closes the dedup/encryption/data-governance gaps called out in requirements review. Android notification collection still needs physical-device validation of the real package/notification-template catalogue (the Phase 4 allow-list remains a small, unverified set). Gmail integration remains entirely unimplemented beyond a parsing-only adapter with no caller: there is no OAuth flow, no Gmail API dependency, and no live retrieval path — it is not wired into the app at all.

See [requirements](docs/REQUIREMENTS.md), [architecture](docs/ARCHITECTURE.md), [parser design](docs/BANK_PARSERS.md) and [Phase 3 notes](docs/PHASE_3.md).
