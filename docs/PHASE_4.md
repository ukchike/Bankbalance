# Phase 4 - Android Notification Collection

## Implemented foundation
- NotificationListenerService receives Android-posted notifications after the user grants notification access in Android Settings.
- Collection is restricted to an explicit package allowlist and per-source opt-in flags.
- Notification title and expanded/body text are passed to the Nigerian bank parser.
- Parsed observations are matched to an existing account using currency and account suffix.
- If no account hint exists, automatic matching is permitted only when exactly one account exists for that institution/currency.
- Ambiguous or unmatched notifications are not applied to an account.
- Parsed balance observations are stored with source, event time, receipt time, parser version and a content fingerprint.
- Duplicate fingerprints are ignored.
- Latest balance update uses event chronology when available; receipt time is used only when event time is absent.
- Notification access status helper and Android Settings launch helper are included.

## User permission flow
The user must manually enable BankBalance under Android Settings > Notification access. Android does not permit the app to silently grant this access.

## Important limitations
- Android package names for financial apps can vary by region, app variant and version. The initial package allowlist is a starting point and must be verified on the user's device. The settings screen should expose source selection and allow package discovery in a later refinement.
- Notification listeners only see notifications posted after access is granted; they cannot read historical app notification history.
- Notifications may be redacted, grouped, truncated or hidden by the source application.
- Some app notifications do not include the available balance or account identifier. These cannot safely update a balance automatically.
- Sender labels from app notifications may not match SMS sender IDs; the parser catalogue needs device-captured fixtures before claiming support for each app.
- SMS inbox permissions are intentionally not requested in this phase. Direct SMS access is subject to Google Play restricted-permission policy. A compliant default SMS-handler/approved exception or user-mediated import route must be evaluated separately.
- This phase does not include Gmail; Gmail OAuth and message processing are a separate integration.
- Verify package IDs, run Android unit tests and install/test on a physical device before production use.
