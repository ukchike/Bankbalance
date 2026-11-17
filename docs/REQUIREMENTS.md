# Product Requirements

## 1. Core dashboard
- Show one row per bank account, not one row per bank.
- Show institution, masked account identifier, currency, latest accepted balance and event/receipt time.
- Show totals separately by currency. Never add unlike currencies without an explicit FX rate.
- Clearly mark stale, uncertain or manually entered balances.

## 2. Account management
Each account has a stable internal UUID and fields for institution, masked account identifier, currency, optional account type/nickname, source associations, inclusion and visibility.

Users may own multiple accounts at one institution and accounts in different currencies (e.g. Access Bank NGN and USD accounts).

### Exclusion controls
- Exclude from totals: keep collecting and displaying the account, but omit it from consolidated totals.
- Hide from dashboard: hide the account from the main account list without stopping collection.
- These controls are independent and reversible.
- Excluded accounts retain history and remain searchable in account management.

## 3. Ingestion sources
- SMS: implement only through a policy-compliant route. Do not assume READ_SMS/RECEIVE_SMS is available to a normal Play-distributed app; assess eligibility and Play declaration before enabling.
- Android app notifications: opt-in NotificationListenerService; user selects allowed source apps such as OPay, PalmPay and Moniepoint. Respect redacted/hidden notification content.
- Gmail: OAuth 2.0 read-only access, least privilege, user-authorised mailbox and bank-message filtering.
- Manual balance entry/import is a fallback for sources that do not expose usable notifications.

## 4. Balance semantics
Preserve balance type: AVAILABLE, LEDGER, CURRENT, or UNKNOWN. Do not silently treat these as equivalent. Preferred dashboard metric is latest AVAILABLE balance; if unavailable, show the best available reported type explicitly.

Each observation stores:
- account_id
- amount as exact decimal (never binary floating point)
- currency ISO code
- balance_type
- event_at (bank transaction/event timestamp, nullable)
- received_at (device/email receipt timestamp)
- source_type and source reference
- parser identifier/version
- confidence and validation status
- deduplication fingerprint

## 5. Validation and ordering
- Use bank event time when reliably parsed; retain receipt time separately.
- A late-arriving older message must not overwrite a newer observation.
- If event time is missing or ambiguous, mark chronology uncertain and do not silently supersede a trusted observation.
- Deduplicate across SMS, email and app notification sources.
- Conflicting observations at the same event time are flagged for review.
- Ignore marketing messages and messages without a parseable balance.
- Do not infer balance by adding/subtracting transaction amounts when a bank-reported balance is available.

## 6. Security and privacy
- Encrypt sensitive local data and cloud transport/storage.
- Store OAuth tokens using Android Keystore-backed secure storage.
- Do not upload raw message bodies by default; process locally and sync only minimum structured observations.
- Provide source disconnect, data export and account/data deletion.
- Never request or store bank passwords, PINs or OTPs.
