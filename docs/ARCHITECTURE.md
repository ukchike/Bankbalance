# Architecture

## Android stack
- Kotlin, Jetpack Compose, MVVM and repository pattern.
- Room/SQLite for offline-first persistence.
- WorkManager for deferred sync and retry.
- Android Keystore-backed encryption for secrets.
- NotificationListenerService for user-enabled app notifications.
- Gmail API with OAuth 2.0 read-only authorisation.
- Cloud sync adapter is provider-agnostic; Supabase PostgreSQL is the planned backend.

## Modules
- app: application shell, navigation and dependency wiring.
- core/model: account, balance observation, source and currency models.
- data/local: Room entities, DAOs and migrations.
- data/sync: encrypted/authenticated sync queue and conflict handling.
- ingestion/notifications: notification listener and source allowlist.
- ingestion/email: Gmail integration and incremental history processing.
- ingestion/sms: isolated adapter, disabled unless an approved access route is available.
- parser: institution-specific parsers and shared normalisation.
- domain: account resolution, balance selection, deduplication and totals.
- feature/dashboard: balance dashboard.
- feature/accounts: account management, include/exclude and hide controls.
- feature/settings: source permissions, privacy and sync status.

## Data model invariants
- A bank/institution is not an account. One institution can own many accounts.
- Account identity uses a stable internal ID; masked account identifiers are matching hints, not globally unique keys.
- Currency belongs to the account and each observation.
- One observation is immutable; current balance is a derived projection.
- Inclusion/visibility flags belong to the account and do not delete history.
- Totals are grouped by currency and include only active, included accounts.

## Sync
Local database is authoritative for immediate UI. A durable outbox queues changes for cloud sync. Sync is idempotent, retries safely and never replaces a newer local observation with an older cloud record.
