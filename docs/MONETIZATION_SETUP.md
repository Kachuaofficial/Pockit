# Pockit coins and Google Play Pro — activation guide

> Version 2.2 status (23 November 2026): the commerce backend and rules have now been deployed.
> The India monthly target is ₹49. Read [BRAND_OFFLINE_V2.2.md](BRAND_OFFLINE_V2.2.md) for current behaviour, completed cloud setup and the remaining Play/AdMob console steps.
> The original 2.1 preparation notes below are retained for reference.

The Android app and backend implementation are prepared locally. No Firebase deployment,
AdMob console settings, Play products, prices, IAM permissions or store rollout have been
changed by this implementation. Live activation needs the steps below.

## Product contract

Basic expense/income tracking, budgets, goals, shared expenses and basic insights stay free.
Advanced reports, CSV export, receipt scanning and ad-free everyday use are premium.

| Unlock | Current server price | Duration |
| --- | ---: | --- |
| Reward ad completion | Earn 10 coins | Maximum 5 verified rewards per UTC day |
| Ad-free pass | 30 coins | 24 hours |
| Receipt scanning | 40 coins | 7 days |
| Advanced reports + CSV | 40 coins | 7 days |
| Google Play Pro | Price returned by Google Play | Monthly or yearly, auto-renewing |

Coin prices and limits live in `functions/monetization/policy.js`. The Android store fetches
this catalog from the server. Coin passes start on redemption and repeat purchases extend
the remaining period. Pro includes all premium features while active. Coin passes and
coins survive Pro expiry. Pro users are prevented from spending coins on included features.
Coins have no cash value and cannot be transferred or withdrawn.

## 1. Google Play Console

For package `com.invatech.pockit`, create and activate:

* Subscription product: **`pockit_pro`**.
* Auto-renewing base plan: **`monthly`**, billing period **1 month**.
* Auto-renewing base plan: **`yearly`**, billing period **1 year**.

Choose the actual regional prices, tax and availability in Play Console. No real price is
hardcoded in the app. The app displays Google Play's formatted price and billing period.
Version 1 of this integration presents standard base plans; trial/promotional and prepaid
offers are intentionally not offered by this client. A plan with the wrong billing period
is not displayed. Existing subscriptions can be managed through Google Play.

Use the project's configured upload signing key for the release AAB. Check the highest
version code already in Play before upload; the prepared project is version **2.1 (9)**.
Distribute through an internal/closed test track and add license testers. A sideloaded debug
APK does not establish that a real Play purchase or price listing works.

Enable Google Play Android Developer API in the linked Cloud project. Grant the Cloud
Functions runtime service account app-scoped permissions to read subscription purchases,
manage subscriptions and acknowledge purchases. The code uses Application Default
Credentials; do not package service-account JSON or OAuth credentials in the APK.

## 2. Firebase App Check

Project: **`pockit-c70e9`**, Functions region: **`asia-south2`**.

Register the Play app-signing SHA-256 for this Android app and configure Play Integrity
in Firebase App Check. Link the correct Cloud project in Play Console's Integrity setup.
The release variant installs the Play Integrity provider. The debug variant installs the
App Check debug provider; register its debug token privately when testing connected calls.

The new callable functions enforce Auth and App Check. Reward-session creation, pass
redemption and purchase verification also consume limited-use App Check tokens. Grant
the runtime service account **Firebase App Check Token Verifier** permissions. Do not
disable enforcement to make a production integration appear to work.

## 3. Pub/Sub and subscription lifecycle

Create topic **`pockit-play-billing`** in the Firebase project and allow
`google-play-developer-notifications@system.gserviceaccount.com` to publish to that topic.
Configure this topic under the app's Google Play real-time developer notifications setting
and send a Play test notification after deployment.

```powershell
gcloud pubsub topics create pockit-play-billing --project pockit-c70e9
gcloud pubsub topics add-iam-policy-binding pockit-play-billing --project pockit-c70e9 --member="serviceAccount:google-play-developer-notifications@system.gserviceaccount.com" --role="roles/pubsub.publisher"
```

RTDN never directly grants entitlement from a notification's type. The backend re-queries
the Play Developer API, checks package/product/base plan and account ownership, and updates
the entitlement. Valid paid purchases are acknowledged on the backend. Active and grace
states retain access through the verified expiry; cancelled subscriptions retain the paid
period. Pending, held, paused and expired purchases do not grant access. A 30-minute
reconciliation job backs up RTDN and retries acknowledgement. Monitor function failures,
Pub/Sub delivery and acknowledgement errors before enabling public paid sales.

## 4. AdMob configuration

The supplied production identifiers are wired into the release variant:

* App: `ca-app-pub-5858375161867194~8387330202`
* Banner: `ca-app-pub-5858375161867194/7308523574`
* Rewarded interstitial: `ca-app-pub-5858375161867194/6728943227`
* Native advanced: `ca-app-pub-5858375161867194/8837227666`

Ensure this AdMob app is linked to the intended Android package. For the rewarded unit,
set the reward amount to **`10`** and reward item to exactly **`Pockit coins`**.

After deploying the callback, configure AdMob SSV to target:

`https://asia-south2-pockit-c70e9.cloudfunctions.net/admobRewardCallback`

This is the intended deployment URL, not a claim that the endpoint is already live.
AdMob's signed URL-validation probes can receive HTTP 200 without granting coins. A real
credit additionally needs an authenticated, server-issued, unexpired reward session bound
to the same account. Invalid signatures receive an error. Unknown sessions, duplicate
events and permanent ineligible signed events are acknowledged without minting coins.

Debug builds use Google's demo inventory. Demo inventory cannot mint coins in the
production wallet. Test the live SSV configuration using a controlled, registered test
device and validate the backend ledger. Never click live publisher ads during testing.
Native ads are placed across Home, Transactions, Insights, advanced reports, budgets,
goals, personal udhar, connected friends, friend history, Spaces, space history,
notifications and profile/preferences. Long feeds insert after five records, then at
eight-record intervals, with content after the last ad. Forms, confirmation dialogs,
login, privacy pages and subscription checkout do not contain native ads. Pro and active
ad-free passes suppress all native/banner slots; signed-in accounts wait for their first
entitlement check before requesting inventory. Reward ads remain explicitly optional.

## 5. Deploy the reviewed backend and rules

The new collections must be protected before shipping the monetized client. This command
is provided for an authorized deployment; it has not been executed here.

```powershell
firebase deploy --project pockit-c70e9 --only "functions:syncCommerce,functions:beginCoinReward,functions:redeemCoinPass,functions:verifyPlaySubscription,functions:admobRewardCallback,functions:playSubscriptionNotifications,functions:reconcilePlaySubscriptions,firestore:rules"
```

The source also migrates the two existing notification handlers to the Firebase Admin 14
modular API. Existing deployed handlers continue running their previous deployed version
until separately redeployed. A full Functions deployment includes those compatible source
updates. The new functions use the project's configured region and Node 22 runtime.

## Validation before public rollout

1. Confirm signed-in account synchronization and App Check on an internal-track release.
2. Complete one controlled reward and confirm exactly one +10 ledger entry. Replaying
   its callback must not change the balance. Check no-fill, skipped ads and delayed SSV.
3. Redeem a pass, restart the app, check expiry/extension and verify banner removal.
4. Buy both test plans, restore on a second install with the same Pockit account, cancel,
   test grace/hold/recovery, and verify expiry/refund/revocation removes Pro access.
5. Switch Pockit accounts during a pending action. The other account must not be charged
   coins or receive an entitlement. Backend requests bind the expected account to Auth.
6. Confirm Google Play acknowledgement, RTDN and scheduled reconciliation succeed.
7. Review current in-app terms/privacy disclosures and Play Data safety answers for the
   purchase history, reward validation and app-attestation fields actually used.

Local checks use a demo Firestore emulator and fake Google API responses for lifecycle
tests. They verify signature handling, concurrency, rules and fulfillment logic without
making a real payment. They are not a substitute for the live checks above.

```powershell
cd functions
npm ci
npm test
npm run test:emulator
npm audit
```

The ledger and entitlement documents are server-write-only. Account, pass and ledger
reads are owner scoped. Purchase tokens and account-link records are not readable by
clients. Local tools use verified entitlements with a server-anchored monotonic clock;
offline trust expires after six hours or process restart. Rewards never increment a
SharedPreferences balance, and Play client callbacks never grant Pro themselves.

Reference documentation:

* [AdMob rewarded interstitial](https://developers.google.com/admob/android/next-gen/rewarded-interstitial/single-load)
* [AdMob server-side verification](https://developers.google.com/admob/android/ssv)
* [Google Play Billing integration](https://developer.android.com/google/play/billing/integrate)
* [Play purchase verification security](https://developer.android.com/google/play/billing/security)
* [Firebase App Check callable enforcement](https://firebase.google.com/docs/app-check/cloud-functions)
