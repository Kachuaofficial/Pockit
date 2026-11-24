# Pockit 2.2 — official brand, offline mode and monthly Pro

## Implemented

- Uses the supplied official logo for the dashboard, login, splash, normal launcher,
  round launcher and Play Store image. The themed launcher layer preserves the rupee,
  stitching and card separation as transparent cutouts. Android applies wallpaper colours.
- A persistent offline card explains which features work. Online-only routes show a
  dedicated offline page. Saved activity, expense/income drafts, cached reports and the
  planner calculator remain usable. Ads, purchases, shared spaces and friend actions
  require internet. Edits to saved cloud records and balance corrections require sync.
- Offline expense/income entries live in an account-scoped Room outbox. They contribute
  to the displayed recorded balance and activity but are not described as cloud saved.
  A cloud refresh cannot erase them. Maximum: 100 pending entries.
- After reconnecting, one server-verified reward video permits one batch of up to 100
  pending entries to sync to Firebase. The same video also earns the existing 10 coins.
  Active, server-verified Pro syncs pending entries without an ad while the app is running.
  The app retries when reopened or reconnected; it does not promise background delivery
  while Android has stopped the app. Keep the app open until the pending card disappears.
- Batch request IDs and row IDs survive retries. The server commits wallet balance,
  records, reward claim and sync receipt atomically. A timeout cannot create duplicates.
  Local entries are removed from the outbox only after cloud acceptance. Failed syncs
  keep local entries. Account switching does not move one account's queue to another.

Uninstalling Pockit or clearing its app data removes entries that have not synced.

## Subscription and ad unlocks

India monthly Pro target: **₹49 per month**. The purchase button uses Google Play's
returned plan and checkout. An INR monthly plan at any amount other than ₹49 is not
offered. Only the monthly base plan is offered for new purchases; previous supported
yearly subscriptions can still be verified/restored.

Pro includes receipt scanning, advanced reports/CSV, ad-free everyday browsing and
automatic sync for offline entries. Google Play bills and renews the monthly plan until
cancelled. Cancellation stops future renewals; verified access lasts through the paid
expiry. Pending payments do not grant Pro. Restore purchases verifies against Google Play
and Firebase for the same Pockit account. The client does not grant paid access itself.

| Option | Unlock |
| --- | --- |
| One completed, server-verified reward video | 10 coins and one offline sync batch |
| 30 coins (3 videos from zero) | 24-hour ad-free browsing pass |
| 40 coins (4 videos from zero) | 7-day receipt scanning pass |
| 40 coins (4 videos from zero) | 7-day advanced reports and CSV pass |
| ₹49 monthly Pro | All included premium features and automatic offline-entry sync |

The existing cap is five verified reward videos per UTC day. Passes are separate from
paid subscriptions. Spending coins on a feature pass does not start a paid subscription.
Coin passes do not include Pro's automatic offline-entry sync. Coins have no cash value.
Reward offers disclose completion, verification, coins and pending-entry sync before
the user chooses to watch. Skipping/no-fill/offline never grants an unverified reward.

Offline premium checks retain the existing short, server-anchored trust window: up to
six hours in the running process. A fresh process requires an online entitlement check.
Core cached viewing, local drafts and the planner calculator do not require Pro.

## Live backend work completed on 23 November 2026

Project: `pockit-c70e9`; region: `asia-south2`.

The original store failure was confirmed: the commerce function URLs returned **404**
and only the two notification handlers had been deployed. Deployed:

- `syncCommerce`, `beginCoinReward`, `redeemCoinPass`, `verifyPlaySubscription`
- `syncOfflineTransactions`, `admobRewardCallback`
- `playSubscriptionNotifications`, `reconcilePlaySubscriptions`
- Firestore rules protecting commerce and offline-sync receipts from client writes

The runtime service account now has `roles/firebaseappcheck.tokenVerifier`, required
for consuming limited-use App Check tokens. The Google Play notification service account
has publisher access on the specific `pockit-play-billing` topic. Other IAM bindings were
preserved. The existing Play Integrity configuration was read and left as configured.

Post-deployment probes: commerce callables return 401 UNAUTHENTICATED to requests without
Auth/App Check, and the SSV endpoint returns 400 to an unsigned callback. The 404 issue is
resolved. These probes validate deployment/security boundaries, not a real paid purchase
or completed production reward.

## Console activation still required

The existing Firebase CLI login cannot call the Google Play Publisher API: Google returns
403 insufficient authentication scopes. Browser automation also stopped because it could
not verify the current browser URL. Consequently, Play/AdMob console changes were not made.

1. Google Play Console, app `com.invatech.pockit`: create/activate subscription `pockit_pro`,
   auto-renewing base plan `monthly`, period one month, India price **₹49**. Confirm the
   tax-inclusive checkout price and country availability. There is no new yearly offer.
2. Grant the deployed runtime service account app-scoped subscription read/manage and
   acknowledgement permissions in Play Console. Enable the Android Developer API if needed.
3. Set Play real-time developer notifications to
   `projects/pockit-c70e9/topics/pockit-play-billing` and send a Play test notification.
4. AdMob rewarded interstitial unit `ca-app-pub-5858375161867194/6728943227`: reward amount
   **10**, item exactly **Pockit coins**; SSV callback:
   `https://asia-south2-pockit-c70e9.cloudfunctions.net/admobRewardCallback`.
5. Confirm Firebase contains the Play app-signing SHA-256 and that Play Integrity is linked
   to this Cloud project. Use a Play internal-track install and license tester for checkout.
6. Test a real controlled SSV callback, one monthly test purchase, cancellation, restore,
   refund/expiry and offline-entry sync before a public rollout. Debug/demo ads cannot
   grant production coins or authorize production sync.

The debug APK previews the app with test ads. The release AAB is signed with the existing
configured upload key; its signing certificate matches upload_certificate.pem. Check the
highest Play version code before uploading version 2.2 (10).

## References

- [Android themed/adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
- [Firebase callable functions](https://firebase.google.com/docs/functions/callable)
- [Firebase App Check enforcement and replay protection](https://firebase.google.com/docs/app-check/cloud-functions)
- [Google Play subscription base plans](https://support.google.com/googleplay/android-developer/answer/12154973)
- [AdMob server-side verification](https://developers.google.com/admob/android/ssv)
