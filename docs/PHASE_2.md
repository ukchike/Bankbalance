# Phase 2 - Local Account Management and Dashboard

## Delivered
- Room database with bank account and balance observation tables.
- Stable UUID account identity; institution name is not used as the account key.
- Add-account form with currency code and optional manually entered latest balance.
- Multiple accounts can be added for the same institution, including NGN and USD accounts.
- Currency-separated totals calculated with BigDecimal.
- Independent account controls:
  - Include in totals / exclude from totals.
  - Show on dashboard / hide from dashboard.
- Hiding an account does not exclude it from totals. Exclusion and visibility are independent.
- Account records and observation history are retained when excluded or hidden.

## Not yet delivered
- Automated SMS, app notification or Gmail ingestion.
- Bank-specific parser execution.
- Automatic observation chronology and cross-source deduplication.
- Cloud synchronisation and authentication.
- Local database encryption and secure account-identifier tokenisation.
- Automated Android build and device testing.

## Testing required
Open the project in Android Studio and run the app on an Android device/emulator. Add two accounts with the same bank name (one NGN and one USD), then confirm separate currency totals. Exclude one account and confirm only its currency total changes. Hide another account and confirm it disappears from the dashboard but remains included in the total unless separately excluded.

Until local encryption is implemented, use masked account identifiers or last-four-digit labels in test data rather than entering full account numbers.
