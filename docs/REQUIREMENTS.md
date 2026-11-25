# Product Requirements

## 1. Core dashboard
- Show one row per bank account, not one row per bank.
- Show institution, masked account identifier, currency, latest accepted balance and event/receipt time.
- Show totals separately by currency. Never add unlike currencies without an explicit FX rate.
- Dashboard visibility and inclusion in totals are independent settings.
- Clearly mark stale, uncertain or manually entered balances.

## 2. Account management
Each account has a stable internal UUID and fields for institution, account identifier, currency, optional account type/nickname, source associations, inclusion and visibility.

Users may own multiple accounts at one institution and accounts in different currencies (e.g. Access Bank NGN and USD accounts).

### Exclusion controls
- Exclude from totals: keep the account visible and continue collecting notifications, but omit its balance from consolidated totals.
- Hide from dashboard: hide the account from the main account list without stopping collection. Unless separately excluded, a hidden account remains included in its currency total.
- These controls are independent and reversible.
- Excluded/hidden accounts retain history and remain available in account management.

## 3. Ingestion sources
- SMS: implement only through a policy-compliant route. Do not assume READ_SMS/RECEIVE_SMS is available to a normal Play-distributed app; assess eligibility and Play declaration before enabling.
- Android app notifications: opt-in NotificationListenerService; user selects allowed source apps such as OPay, PalmPay and Moniepoint. Respect redacted/hidden notification content.
- Gmail: OAuth 2.0 read-only access, least privilege, user-authorised mailbox and bank-message filtering.
- Manual balance entry/import is a fallback for sources that do not expose usable notifications.

## 4. Balance semantics
Preserve balance type: AVAILABLE, LEDGER, CURRENT, MANUAL, or UNKNOWN. Do not silently treat these as equivalent. Preferred dashboard metric is latest AVAILABLE balance; if unavailable, show the best available reported type explicitly.

Each observation stores account_id, amount as exact decimal text, currency ISO code, balance_type, event_at, received_at, source_type, parser identifier/version, confidence and deduplication fingerprint.

## 5. Validation and ordering
- Use bank event time when reliably parsed; retain receipt time separately.
- A late-arriving older message must not overwrite a newer observation.
- If event time is missing or ambiguous, mark chronology uncertain and do not silently supersede a trusted observation.
- Deduplicate across SMS, email and app notification sources.
- Conflicting observations at the same event time are flagged for review.
- Ignore marketing messages and messages without a parseable balance.
- Do not infer balance by adding/subtracting transaction amounts when a bank-reported balance is available.

## 6. Security and privacy
- Encrypt sensitive local data and cloud transport/storage. **Implemented for local data**: the Room database is encrypted at rest with SQLCipher, keyed by a random passphrase held only in Keystore-backed `EncryptedSharedPreferences`. Cloud transport/storage is not applicable yet — no cloud sync exists.
- Store OAuth tokens using Android Keystore-backed secure storage. Not yet applicable: no integration (Gmail) that acquires OAuth tokens is live.
- Do not upload raw message bodies by default; process locally and sync only minimum structured observations. Satisfied trivially: there is no upload path of any kind yet.
- Provide source disconnect, data export and account/data deletion. **Implemented**: "Export all data" (JSON) and a confirmed "Delete account" action (removes the account and its balance/transaction history) are both available from the app.
- Never request or store bank passwords, PINs or OTPs. Satisfied: no such field or flow exists anywhere in the app.

## 7. Current milestone
Implemented: Room entities for accounts, observations, transactions and budgets; account repository; local encrypted database; add-account UI; dashboard; currency-grouped totals; include/exclude and show/hide controls; live Android-notification and SMS-import ingestion through a shared, content-based deduplication path; category suggestion and monthly budgets; full data export and account deletion.

Not implemented: Gmail ingestion (parsing-only adapter exists with no OAuth, no API call and no caller — entirely disconnected from the app), cloud sync of any kind, and physical-device validation of the notification-source package/template catalogue.
