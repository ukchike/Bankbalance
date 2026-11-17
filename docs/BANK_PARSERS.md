# Nigerian Bank Parser Catalogue

Parser inputs are untrusted notification text. Parsers must be deterministic, versioned, tested with redacted fixtures and return structured observations or a rejection reason.

## Initial fixture families from user-provided screenshots
- Ecobank: `Credit:NGN...`, `A/C:...`, `Bal:NGN...`; interest-credit alerts.
- FirstBank: `Debit/Credit`, account identifier, `Amt`, `Date`, `Bal` (sometimes CR suffix).
- GTBank: `Amt`, DR/CR marker, `Avail Bal`, `Date`.
- Keystone: `Credit!`, `Acct`, `Amt`, `Date`, `Bal`.
- ALAT by Wema: `DR/CR`, `Acct No`, `Desc`, `Bal`, event timestamp.
- Access Bank: `Debit/Credit`, `Acc`, `Date`, `Avail Bal`, sometimes `Total`; preserve available vs total distinction.
- Providus: `Debit Alert`, amount, masked card/account context, event timestamp, `Bal`.
- Zenith: `Acct`, `DT`, `CR Amt` or card authorisation, `Bal` and sometimes `Avail Bal`.

These fixtures are based on screenshots and are not a complete certification of every bank template. Account suffixes and amounts in tests must be synthetic/redacted.

## Parser output
institution, account_hint, transaction_direction, transaction_amount, currency, reported_balance, balance_type, event_at, source_received_at, description_hint, confidence, warnings.

## Rules
- Support multiple accounts under one institution.
- Support NGN and USD accounts at the same institution.
- Never use institution name alone as account identity.
- Handle variable whitespace, punctuation, thousands separators and optional currency prefixes.
- Distinguish transaction amount from reported balance.
- Distinguish available balance from ledger/total balance.
- Ignore promotional messages.
- Treat date-only, malformed or contradictory timestamps as uncertain.
- Do not infer missing account digits.
- Match cross-source observations cautiously using account hint, currency, amount, event time and transaction attributes.
- Add tests for every new observed message format before enabling that parser in production.
