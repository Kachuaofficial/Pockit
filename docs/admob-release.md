# AdMob release setup and troubleshooting

Pockit requests only banner and native ads. Both debug and release use the publisher's real units:

- App: `ca-app-pub-5858375161867194~2066564513`
- Banner: `ca-app-pub-5858375161867194/6572551621`
- Native: `ca-app-pub-5858375161867194/5264880402`

No code forces test devices or bypasses consent. Google automatically treats Android emulators as test devices. Use an emulator, or register development devices in the AdMob console, for QA with these real units. Never click live ads or automate live impressions.

## Confirmed Android 17 blocker (2026-11-13)

The installed release emitted this UMP failure on the Android 17 physical device:

> Publisher misconfiguration: Failed to read publisher's account configuration; no form(s) configured for the input app ID.

After the publisher completed the AdMob privacy setup, the same installed Android 17 release initialized Mobile Ads successfully at 01:43:59 IST without an APK change. This confirms the consent configuration was blocking initialization; actual ad inventory still requires separate load verification.

The reported app ID matched the manifest above. This is a publisher-side consent configuration error; no Android-version-specific SDK crash was observed. The SDK was not initialized because UMP did not authorize requests. Do not work around it by ignoring `canRequestAds()` or resetting consent in production.

In AdMob, open Privacy & messaging, create/publish the applicable consent messages for this exact app, provide the publisher privacy-policy URL, and choose the options appropriate to the publisher's legal requirements. European regulations is the usual initial UMP message; configure US-state/other messages where applicable. Reopen the app after publication. UMP is updated each launch and failed updates are retried on foreground with a 60-second cooldown. A profile entry opens privacy choices whenever UMP requires it. Existing eligible consent can permit requests while a refresh is pending or fails. Opening privacy choices removes mounted ads and suspends new requests until the form closes; subsequent requests use the updated consent.

Also check app readiness/review status, ad-unit ownership/format, account or policy serving limits, app-ads.txt verification and store listing association in AdMob. These cannot be fixed by hardcoding another unit or promising ad fill.

## Second confirmed device blocker

After consent was configured, the phone emitted `Unable to obtain a JavascriptEngine` (code 0), including for Google's test units in its previously installed debug build. Android System WebView was installed/enabled. The device had AdGuard Private DNS enabled; `googleads.g.doubleclick.net` resolved to loopback (`127.0.0.1`), confirming blocked ad traffic. The publisher then disabled the filtering DNS; the same hostname subsequently resolved to a public Google address. Restart the app after a network/DNS change so the SDK does not retain a failed engine session. Legacy SDK 25.5.0 still reported the JavaScript-engine failure after DNS recovery, despite its bootstrap JS loading successfully. The integration was therefore migrated to GMA Next-Gen SDK 1.6.0. On the same Android 17 phone, its SDK initialized successfully, a real banner loaded at 02:03:07 IST, and the publisher confirmed that both the banner and native card were visible. Lifecycle observer replay is guarded to prevent duplicate initial requests. Pockit does not change or bypass a user's DNS/ad-blocking settings. A blocked network cannot be repaired by changing ad-unit IDs.

## Runtime behavior

- GMA Next-Gen SDK 1.6.0 and UMP 4.0.0. Legacy Ads/Ads Lite artifacts are excluded to prevent duplicate SDK runtimes. Initialization runs off the UI thread once per process after UMP permits requests. Existing request privacy settings are preserved.
- Banners in scrollable lists request inline-adaptive ads using the measured available width and a 60dp maximum height. Both the Compose container and Android child use the actual dimensions returned by the SDK; assets are never scaled/cropped to fit an arbitrary box. Automatic refresh callbacks update dimensions too. The physical device returned 320x60dp; the publisher confirmed that the wide banner and scrolling looked correct.
- Banner refresh remains SDK-owned: enable Google-optimized automatic refresh on the banner unit in AdMob. No app timer refreshes a loaded banner. Failed slots collapse, with bounded cooldown before a future foreground/viewport retry. Cached banner views become invisible/detached offscreen and expire after five idle minutes (never retain an unused creative beyond 55 minutes).
- Each navigation destination owns its ad resources above LazyColumn. Stable placement keys retain the loaded ad AND asset view while a list item is recycled. Returning to that item reattaches the same view without a new ad request. Navigation away, privacy changes and screen disposal destroy resources. Memory pressure evicts idle resources. Cache capacity is at most two entries per format per screen, reduced to one on low-RAM devices; attached ads are never evicted for another placement.
- Home preloads its one upcoming native placement only when it is within two content items of the viewport. It shares the same slot/in-flight request with the actual card. Other placements load as LazyColumn composes nearby items. No background service/worker loads ads, and pending retries wait for a started foreground lifecycle. In-flight callbacks are safely discarded if their screen/privacy scope is destroyed.
- Native internal/network/timeout failures get at most two retries with 30/60-second backoff. Terminal/no-fill errors have no retry timer and retain a five-minute cooldown across item recycling. Cached native ads expire at 55 minutes, with replacement requested only if their placement is still needed in the foreground.
- Native cards register headline, advertiser, body, icon, media and CTA with NativeAdView. Missing optional assets are hidden; CTA text is never fabricated. Ad label and top-right AdChoices space remain visible. Clicks/impressions are SDK-owned, with no custom click handlers or manual impression recording.
- At most one native placement per list after eight content rows. Empty friends/activity screens, notification management, profile settings and transaction-detail action areas do not show ads. No fullscreen/rewarded formats or ad-based feature locks remain.

## Scroll/reuse validation

On Android 17, a fresh banner load and a fresh native load succeeded. Scrolling then emitted `banner slot reused without a new request`, with no replacement request for that placement. The publisher confirmed both correct banner sizing and stable scroll-back behavior. Five cache regression tests cover repeated reuse without factory calls, idle LRU eviction, protecting attached ads, scope/privacy cleanup and rejecting closed resources. Retry tests cover exhaustion and terminal failures. Development QA should still use an emulator or an AdMob-registered test device; never click live creatives or automate live impressions.

## Diagnose without exposing financial data

Collect only the app PID and these tags with ADB: `ConsentManager`, `AdManager`, `PockitAds`, `Ads`. Failures report format, error code, message and response ID. Do not upload full device logs, identifiers or financial notifications.

- Consent error: fix the app's Privacy & messaging configuration first.
- Invalid request/account error: inspect unit format, app ID and AdMob status.
- Network error: inspect device connectivity, VPN/private DNS and SDK error details.
- No fill (`NO_FILL`): Google returned no inventory; do not retry aggressively or fabricate an ad.

The application requests production units; personalized versus non-personalized/limited ads and actual fill remain subject to consent, Google serving decisions and account eligibility. Enabling production IDs does not guarantee an impression for every user.

## Primary references

- https://developers.google.com/admob/android/privacy
- https://developers.google.com/admob/android/next-gen/banner
- https://developers.google.com/admob/android/next-gen/native
- https://developers.google.com/admob/android/next-gen/native/advanced
- https://developers.google.com/admob/android/test-ads
- https://developers.google.com/admob/android/next-gen/quick-start
- https://developers.google.com/admob/android/next-gen/migration
