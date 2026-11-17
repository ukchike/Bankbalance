# Architecture

## Android stack
- Kotlin, Jetpack Compose, MVVM/repository direction.
- Room/SQLite for offline-first persistence.
- WorkManager planned for deferred sync and retry.
- Android Keystore-backed encryption planned for secrets.
- NotificationListenerService planned for user-enabled app notifications.
- Gmail API with OAuth 2.0 read-only authorisation planned.
- Cloud sync adapter is provider-agnostic; Supabase PostgreSQL is the planned backend.

## Implemented modules
- app: application shell, navigation, dashboard, account management and add-account dialog.
- data/local: Room account and balance-observation entities, DAO, database singleton and repository.

## Planned modules
- ingestion/notifications: notification listener and source allowlist.
- ingestion/email: Gmail integration and incremental history processing.
- ingestion/sms: isolated adapter, disabled unless an approved access route is available.
- parser: institution-specific parsers and shared normalisation.
- domain: account resolution, balance selection, deduplication and totals.
- data/sync: durable outbox, encrypted/authenticated sync and conflict handling.
- feature/settings: source permissions, privacy and sync status.

## Data model invariants
- A bank/institution is not an account. One institution can own many accounts.
- Account identity uses a stable internal ID; masked account identifiers are matching hints, not globally unique keys.
- Currency belongs to the account and each observation.
- Observations are retained as history; account latest balance fields are a dashboard projection.
- Inclusion/visibility flags belong to the account and do not delete history.
- Totals are grouped by currency and include all included accounts, even hidden accounts.
- Excluded accounts do not contribute to totals.

## Sync
Local database is authoritative for immediate UI. A durable outbox queues changes for cloud sync. Sync is idempotent, retries safely and never replaces a newer local observation with an older cloud record.
