# Closed-testing finance upgrade

All features remain available without a subscription or rewarded-ad gate.

## Delivered behaviour

- Home / Spaces / Friends / Profile navigation in a custom charcoal capsule. The reference-inspired interface uses a warm cream canvas, coral balance panel, lavender and yellow action cards, teal budgeting accents, bold typography and rounded panels. Activity and Budgets remain reachable from Home; all features remain unlocked. Four-screen liquid onboarding uses the same palette, draw-only organic motion, swipe transitions and a static fallback when animations are disabled.
- Private name-only udhar entries, borrowing/lending, partial repayments, settlement validation and deletion. No friend account or phone number is required. Udhar does not silently change wallet income or spending.
- Overall and category recurring monthly budgets, historical spending comparison, live transaction-driven progress and once-per-threshold monthly alerts. Historical views explicitly use current recurring limits; historical limit snapshots and rollover are not implemented.
- UPI-only allowlist, per-app controls, rejection of non-payment/ambiguous messages, exact currency parsing, durable event identity and an approve/edit/dismiss inbox. Detection never directly writes a wallet entry. Original payment time is retained on approval. Raw notification text is not persisted or sent to a model.
- Shared-element transitions in Activity and Udhar. A persisted animation preference feeds the Compose recomposer's motion-duration scale and honours the Android system scale. Decorative continuous motion and shimmer stop when disabled.
- Consent-gated AdMob initialization/requests, debug test ad units, release ad units, native-ad disposal, banner disposal, rewarded cache expiry and frequency checks. Reward callbacks are never fabricated after an ad error. Core features do not depend on ads.
- Usage-aware contextual reminders, selectable frequency, quiet hours, inactivity backoff and battery-aware WorkManager scheduling. No shop or character reminders.

## Money and data integrity

INR inputs parse to integer paise without floating-point arithmetic. Checked arithmetic detects overflow. Legacy group/friend interfaces still expose rupees for compatibility, but balance/split/settlement arithmetic converts at the boundary to paise; new cloud writes include minor-unit fields. The split engine preserves every paisa, including one-paisa settlements.

Wallet edits preserve transaction dates; deleting income can correctly produce a negative recorded balance. Restore and imported payment requests are idempotent. Local cache updates follow successful cloud commits. Wallet snapshots and mutations are serialized per account, and full paginated history replaces the cache atomically after a successful fetch. Wallet balance updates are observable across entry points.

The previous destructive Room fallback was removed. Schema exports and a 2→3 migration test preserve a seeded wallet entry and validate the new tables. Per-account database files isolate the legacy cache; legacy data is adopted only for the account already signed in during the upgrade. Unassigned legacy databases are preserved rather than assigned to a later user.

## Current boundaries

- Personal udhar, savings goals, structured pending UPI reviews, planner details, notifications/read state and user preferences now have Firestore-backed paths. Existing account-local records are imported create-only; Room remains an offline cache. Unsynced drafts can still be lost if app data is cleared. See firestore-sync-audit.md.
- Wallet mutations and approving a UPI payment still require the existing Firebase transaction connection. Failed approval remains in the review inbox; failed manual entry remains editable. A durable offline wallet outbox is not implemented.
- Notification recognition deliberately uses conservative on-device rules, not a trained ML model. Unknown formats and multiple distinct currency amounts are rejected. Provider-specific real-world notification fixtures should expand the test corpus before public release.
- Owner-only rules for the new private subcollections were deployed to pockit-c70e9. No Play Store submission was performed.
- Real low-end-device frame timing, live AdMob consent/no-fill behaviour, and signed-in multi-device Firebase integration require release validation. Emulator checks do not establish glitch-free production behaviour.

## Validation

Unit tests cover exact decimal parsing, invalid input, overflow, split conservation, one-paisa settlement, partial repayments and notification rejection cases. Instrumented tests exercise onboarding, planner validation, durable inbox deduplication/account isolation, and the Room 2→3 migration. See the build/test reports for the final execution results.

Sources used for platform integrations: [UMP](https://developers.google.com/admob/android/privacy), [test ad units](https://developers.google.com/admob/android/test-ads), [lifecycle-aware Compose recomposer](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/package-summary).

## Reference UI verification

Debug APK assembled successfully. 26 unit tests and 7 emulator instrumented tests passed. The three onboarding/navigation checks also passed at 360 × 640 dp. Lint reports 0 errors and 94 warnings. Screenshots in artifacts/qa/reference-*.png were captured from the actual composables in an unauthenticated instrumentation host; Home therefore shows the sign-in error and Spaces may show loading placeholders. These captures verify layout, not authenticated cloud integration.

## Expressive interaction update

The custom brand now uses MaterialExpressiveTheme and MotionScheme.expressive with Material 3 1.5.0-alpha23. This is a prerelease dependency pinned explicitly for Expressive APIs. Page navigation combines spring scale/slide with fades; finance cards reveal with spring motion. Typography has stronger emphasis. The existing motion preference disables these effects. Personal name-only udhar is now a mode inside the existing FriendsScreen alongside connected friends, rather than a separate destination.
