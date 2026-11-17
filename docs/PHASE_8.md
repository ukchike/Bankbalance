# Phase 8 - Categorisation, budgeting and spending analytics

## Implemented
- Rule-based suggested categories: transfers, airtime/data, utilities, food/dining, transport, subscriptions, cash withdrawals, bank charges, income, shopping and other.
- Transaction category override in the Transactions screen.
- Explicit own-account transfer flag; flagged transfers are excluded from expense analytics and budget actuals.
- Monthly category budgets stored by category, currency and YYYY-MM period.
- Budget creation/update and deletion from the Transactions screen.
- Current-month budget-versus-actual progress, remaining budget and over-budget amount.
- Current-month category spending summary grouped by category and currency.
- CSV exports include category and internal-transfer status.
- Room migration from database version 2 to version 3.

## Calculation rules and limitations
- Spending analytics include matched debit transactions with an event timestamp in the current month.
- Unmatched transactions are not included in budget actuals until reconciled.
- Transactions flagged as internal transfers are excluded. Keyword detection is only a suggestion; users should review and flag own-account transfers manually.
- External transfers are not automatically treated as internal transfers.
- Budgets and spending remain currency-specific; no FX conversion is performed.
- Categorisation is rule-based and can be overridden. Ambiguous descriptions may remain OTHER.

## Validation still required
- GitHub Actions must complete successfully before treating the build as verified.
- Install and exercise the app on a physical Android device, including category editing, budget creation/update/deletion, and migration from an existing v2 database.
