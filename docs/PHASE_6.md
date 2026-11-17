# Phase 6 - Transaction intelligence and reconciliation

## Implemented foundation
- Added a Room transaction entity separate from balance observations.
- Transaction rows retain institution, account hint, debit/credit direction, amount, currency, description, event time, receipt time, source, confidence and deduplication fingerprint.
- Transactions are linked to an account only when a unique account suffix match is available.
- Unmatched or ambiguous records remain unassigned and enter the account-review queue.
- Low-confidence matched transactions are flagged for confirmation.
- Transactions without a parseable amount or explicit debit/credit direction are not created as transaction records. Balance-only alerts remain balance observations.

## Reconciliation statuses
- MATCHED: unique account suffix match and parser confidence at or above the initial threshold.
- NEEDS_CONFIRMATION: unique account match but lower parser confidence.
- NEEDS_ACCOUNT_REVIEW: no unique account match, including missing or ambiguous account hints.

## Controls and limitations
- Cross-source duplicate detection depends on a stable event timestamp and comparable parsed fields. Provider-specific email and app notification IDs should be incorporated when live connectors are added.
- Do not infer a transaction direction from the sign of a balance.
- Do not infer a missing transaction amount from a balance movement.
- Do not treat a reported available balance as a ledger balance.
- This phase adds the persistence and reconciliation foundation; a user-facing review screen and live connector integration remain follow-up work.
