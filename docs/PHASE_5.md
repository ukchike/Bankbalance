# Phase 5 - Gmail ingestion and cross-source reconciliation

## Implemented in this phase
- Added a Gmail message adapter that accepts message sender, subject, body and received timestamp.
- Reuses the existing Nigerian bank parser; Gmail messages do not get a separate, inconsistent bank-balance parser.
- Produces a canonical cross-source fingerprint from institution, account suffix, currency, reported balance, balance type, event timestamp and transaction details.
- Canonical fingerprints are deliberately unavailable when a reliable event timestamp is missing. This avoids silently merging unrelated messages received at different times.
- Added unit tests for supported bank email content and rejection of unrelated messages.

## OAuth/API integration boundary
Live Gmail access is not enabled yet. The adapter is ready to process message content, but secure Gmail retrieval requires a Google Cloud project, Android OAuth client configuration, Gmail API enablement, consent-screen configuration and the restricted read-only Gmail scope. These account-specific credentials and consent settings cannot be safely invented or embedded in source control.

The intended permission is Gmail read-only access. BankBalance must never request Gmail send, delete, modify or mailbox-wide write permissions. The user must sign in through Google's supported authorisation flow and explicitly consent.

## Reconciliation rules
1. A parsed email is not automatically applied just because its sender is a bank.
2. Match institution, currency and account suffix; require a unique account match.
3. Prefer the event timestamp from the bank message, not the email receipt timestamp.
4. Use canonical fingerprints to detect the same reported balance arriving through app notification and email.
5. Do not merge observations without a reliable event timestamp.
6. Preserve source provenance for every observation.
7. An older event must not overwrite a newer account balance.

## Remaining activation steps
- Create/configure the Google Cloud OAuth client for Android using the app's final signing certificate fingerprints.
- Enable the Gmail API and configure the consent screen for the Gmail read-only scope.
- Add the OAuth client configuration through secure build configuration; never commit private secrets.
- Implement and test paginated Gmail history/message retrieval, token refresh, revoked-consent handling and incremental sync.
- Supply redacted sample bank email formats so sender aliases and HTML/text body variants can be validated.

## Privacy
Only bank messages that parse as supported financial alerts should be processed. Do not persist unrelated email bodies. Store only the structured parsed observation and source metadata. Do not request Gmail write permissions.
