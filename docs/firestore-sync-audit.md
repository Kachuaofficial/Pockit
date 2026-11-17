# Account data sync audit

## Storage and behaviour

| Feature | Firestore path | Behaviour |
|---|---|---|
| Profile | users/{uid} | Missing name is requested; names observed by Home/Profile |
| Wallet | users/{uid}/wallets/main | Atomic monetary writes; wallet updates observed live |
| Activity | users/{uid}/transactions/{id} | Cloud history refreshes Room; Home supports edit/delete |
| Balance corrections | users/{uid}/balanceAdjustments/{id} | Recorded separately from income/expenses; compare-and-set prevents stale overwrite |
| Personal udhar/goals/UPI inbox | users/{uid}/financeRecords/{id} | Create-only migration of existing records, live reads, acknowledged writes, deletion tombstones |
| Udhar outstanding balances | users/{uid}/ledgerBalances/{personId} | Transactions serialize repayments; aggregate bootstraps from legacy cloud records |
| Budgets | users/{uid}/budgets/{category} | Legacy import once per installation/account; cloud snapshots replace cache; acknowledged writes |
| Recurring bills | users/{uid}/subscriptions/{id} | Legacy import, cloud snapshots, acknowledged edits/deletes; save errors shown |
| Planner | users/{uid}/settings/planner | Loads cloud values; imports existing local plan; save requires an account |
| User preferences | users/{uid}/settings/preferences | Motion, reminders, frequency and UPI choices synchronized |
| Notification history | users/{uid}/notifications/{id} | Private history/read state; delete tombstones; create-only legacy import |
| Connected friends/Spaces | Existing friendships, friend_transactions, spaces paths | Existing Firestore repositories retained |

Local files are retained for cache/offline reading, notification drafts, migration bookkeeping and device-specific runtime state. Android permissions, device authentication enrollment, onboarding seen state and reminder scheduling metrics remain device-specific. No bank credentials or raw notification text are uploaded by the new structured review sync.

## Required fields

Home checks the signed-in profile name and wallet balance against the server. Missing values prompt the user. Explicit zero and negative recorded balances are supported. Setup uses a transaction and fills missing fields only, avoiding overwriting another device's completed setup. Optional fields and runtime metadata do not generate intrusive setup questions.

## Validation and deployment

Owner-only rules for financeRecords, ledgerBalances, balanceAdjustments, notifications and settings have been compiled and deployed to pockit-c70e9. Local build/test reports and the rule deployment log are in artifacts. The emulator regression suite does not authenticate a production user, so these checks do not establish signed-in cross-device or real-network conflict behaviour. Those scenarios require a signed-in release validation pass.

Cloud financial mutations require connectivity/acknowledgement. Room is not a durable wallet mutation outbox. Firestore preference/FCM writes use the SDK's pending write queue; background UPI drafts remain locally recoverable and are imported on the next account sync if their initial transaction cannot run offline. Clearing app data before sync can lose pending data. Existing shared-space/friend authorization rules have not been independently security-audited by this change.

Final verification: debug APK assembled; 28 unit tests passed; 7 emulator instrumentation tests passed. Lint completed without errors before the final cache-only refinement. The production rules deployment completed successfully. No production user was signed in or modified during testing.
