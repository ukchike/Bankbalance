# Phase 7 - Transaction history, review and analytics

## Implemented
- Added a Transactions tab to the main navigation.
- Displays transaction amount, direction, currency, institution, source, event time, parser confidence and review status.
- Search across institution, account hint, description, currency and source.
- Filter all, matched or transactions requiring review.
- Manually assign an unassigned transaction to an account after user confirmation.
- Monthly matched transaction summaries grouped by month, currency and debit/credit direction.
- Monthly totals use BigDecimal arithmetic in the application layer rather than SQLite floating-point aggregation.
- Export transaction history to CSV using Android's document-creation picker.
- CSV export includes provenance, event/receipt timestamps, confidence and review status; only the last four account-hint digits are exported.

## Current scope and limitations
- Transaction history only contains records produced by currently connected parsers. Gmail live retrieval is not yet active.
- The current app notification listener is the only live source feeding the transaction repository.
- The review screen permits account assignment, but does not yet support editing transaction amount, direction or description.
- Monthly summaries include matched transactions with event timestamps. Transactions without event time remain visible in history but are omitted from monthly summaries.
- CSV export is a user-initiated local file action. Cloud export and XLSX are not included.
- Parser accuracy and package identifiers still require physical-device validation.

## CI
The workflow now uses android-actions/setup-android v4 instead of v3 following repeated failures at the Android SDK setup step. Build and test success must be confirmed by the new workflow run.
