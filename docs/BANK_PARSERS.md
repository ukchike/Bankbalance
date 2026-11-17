# Nigerian Bank Parser Catalogue

Parser inputs are untrusted notification text. Parsers must be deterministic, versioned, tested with redacted fixtures and return structured observations or a rejection reason.

## Supported first-pass sender aliases
- Ecobank
- FirstBank / FirstOnline
- GTBank / GTB
- Keystone
- ALAT by Wema / Wema
- Access Bank
- Providus
- Zenith Bank
- OPay, PalmPay and Moniepoint sender identification (notification-template parsing remains unverified)

## Initial fixture families from user-provided screenshots
- Ecobank: `Credit:NGN...`, `A/C:...`, `Bal:NGN...`; interest-credit alerts.
- FirstBank: `Debit/Credit`, account identifier, `Amt`, `Date`, `Bal` (sometimes CR suffix).
- GTBank: `Amt`, DR/CR marker, `Avail Bal`, `Date`.
- Keystone: `Credit!`, `Acct`, `Amt`, `Date`, `Bal`.
- ALAT by Wema: `DR/CR`, `Acct No`, `Desc`, `Bal`, event timestamp.
- Access Bank: `Debit/Credit`, `Acc`, `Date`, `Avail Bal`, sometimes `Total`; parser selects available balance.
- Providus: `Debit Alert`, amount, masked card/account context, event timestamp, `Bal`; card-like reference is not assumed to be an account number.
- Zenith: `Acct`, `DT`, `CR Amt` or card authorisation, `Bal` and sometimes `Avail Bal`.

These fixtures are based on screenshots and are not a complete certification of every bank template. Account suffixes and amounts in tests are synthetic/redacted.

## Parser output
Institution, account hint/type, transaction direction/amount, currency, reported balance, balance type, event timestamp, date-only flag, description hint, confidence, warnings and parser version.

## Rules
- Support multiple accounts under one institution.
- Support NGN and USD accounts at the same institution.
- Never use institution name alone as account identity.
- Handle variable whitespace, punctuation, thousands separators and optional currency prefixes.
- Distinguish transaction amount from reported balance.
- Distinguish available balance from ledger/total balance.
- Ignore promotional messages and messages without an explicit balance.
- Treat date-only, malformed or contradictory timestamps as uncertain.
- Do not infer missing account digits.
- Match cross-source observations cautiously using account hint, currency, amount, event time and transaction attributes.
- Add tests for every new observed message format before enabling that parser in production.

## Implementation
The first-pass parser is in `app/src/main/java/com/bankscope/parser/`. JUnit fixtures are in `app/src/test/java/com/bankscope/parser/`. It is currently a parsing library only; live message collection, account matching and balance-history updates are separate future steps.
