# Phase 3 - Bank Notification Intelligence

## Delivered in this phase
- Deterministic sender identification for Ecobank, FirstBank, GTBank, Keystone, Wema/ALAT, Access Bank, Providus, Zenith, OPay, PalmPay and Moniepoint.
- First-pass parser for explicit balance fields, account hints, transaction amount/direction, currency, event date/time, description hint and confidence.
- Available balance is distinguished from current/unknown balance.
- Access Bank "Avail Bal" is selected rather than an unrelated "Total" amount.
- Exact decimal text is used for money values.
- Date-only observations are marked as date-only and receive a warning.
- Unknown senders, marketing messages and messages without an explicit balance are rejected.
- Unit tests added for redacted examples from the user's screenshots.

## Known limitations
- Sender identification does not mean that every OPay/PalmPay/Moniepoint app notification template is parseable.
- Providus sample exposes a masked card-like reference, not a proven deposit account number; parser intentionally leaves accountHint empty.
- The Ecobank screenshot's date is an interest-period date ("on 01-MAR-26"), not necessarily the notification delivery time. It is parsed as a date-only event date and flagged.
- Some screenshot messages contain dates outside the current chronology or incomplete timestamps. Parser does not use device receipt time as a substitute for bank event time.
- Currency defaults to NGN when no explicit currency marker exists. USD/GBP require an explicit marker; this is a conservative initial policy.
- Parser output is not yet wired to live SMS, notification or Gmail ingestion.
- No account auto-linking is performed. Parsed account hints must be matched against user-created accounts using a separate cautious resolver.

## Test execution
JUnit tests are included in the Android module. A successful CI build/test run is still required before treating the parser as verified on Android.
