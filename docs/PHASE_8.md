# Phase 8 - Categorisation, budgeting and spending analytics

## Implemented foundation
- Rule-based transaction categorisation for transfers, airtime/data, utilities, food/dining, transport, subscriptions, cash withdrawals, bank charges, income, shopping and other.
- Transfer classification runs before income/expense defaults to reduce false expense reporting for own-account movements.
- Transactions retain a category and a transfer flag for reporting.
- Added monthly category budgets by category and currency.
- Spending and budget calculations are grouped by currency; no implicit FX conversion.
- Category analytics exclude transfers and non-debit transactions from expense totals.
- User can review and override suggested categories in the transaction screen.

## Controls
- Keyword categorisation is a suggestion, not a guaranteed classification.
- Ambiguous transactions default to OTHER and remain editable.
- Transfers between own accounts should be excluded from spending. External transfers may require user recategorisation.
- Budget periods use YYYY-MM and are currency-specific.
- No bank credentials or payment actions are involved.
