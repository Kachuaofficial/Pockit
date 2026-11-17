# Release / R8 validation

## Fix

The previous release mapping renamed or removed getters on four objects passed directly to Firestore: FinanceRecord, CategoryBudgetEntity, SubscriptionEntity and NotificationEntity. App-specific keep rules now preserve those models and their reflective properties. Broad keep/dontwarn rules covering entire SDKs/repositories were removed; dependency consumer rules preserve their own reflection contracts. Optimization and resource shrinking remain enabled. Release lint checks now run and fail on errors.

Run `python scripts/verify_release_mapping.py` after a release build. It checks four model names and 33 required getters against the actual R8 mapping. Before the fix, the check failed against the old signed-export mapping. It now passes against the new release mapping.

## Build

`gradlew.bat :app:assembleRelease :app:bundleRelease :app:lintRelease`

Outputs are in app/build/outputs/apk/release, app/build/outputs/bundle/release and app/build/outputs/mapping/release. Keep mapping.txt for retracing release crashes. The app/release directory is a manually signed Android Studio export and is not automatically replaced by this command. A new signed APK was exported there during this validation; see final results below.

No production signing passwords are configured in this shell. The newly generated APK is explicitly named app-release-unsigned.apk and must be signed before installation/distribution. Use Android Studio Build > Generate Signed Bundle / APK with the existing upload-keystore.jks, or configure KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD securely in the environment/user Gradle properties. Do not commit signing passwords. For Play Console upload use a signed AAB; increase versionCode if the current code has already been uploaded.

## Smoke test

A copy of the minified APK is signed with the local debug key solely for QA, at artifacts/qa/app-release-r8-qa.apk. It is not a production release. It is installed on a separate fresh PockitReleaseQA emulator (emulator-5556), leaving the original emulator/device data intact. The QA APK launches successfully. The framework-only ReleaseSmokeInstrumentation runner can inspect the real minified target without requiring debug Kotlin/AndroidX symbols that R8 may rename.

Build the framework-only runner with `gradlew.bat :app:assembleDebugAndroidTest -PreleaseSmoke=true`. After installing that test APK signed by the same QA key, run:
`adb -s emulator-5556 shell am instrument -w com.invatech.pockit.test/com.invatech.pockit.ReleaseSmokeInstrumentation`

The runner checks preserved model getter values, generated Room implementation constructors and persisted WorkManager worker constructor signatures. It does not authenticate a production user or validate live Google sign-in, Firebase permissions or live ads. Those need a signed-in release-device check.

## Installation diagnosis

The existing signed export verifies correctly. Installing it over the debug installation reproduces INSTALL_FAILED_UPDATE_INCOMPATIBLE because the signing certificates differ. R8 rules cannot fix a certificate mismatch. Update using the same signing identity as the installed app, or use a fresh installation after ensuring all data is synced. A Play App Signing build may use a different certificate from a locally upload-key-signed APK; distribute Play updates through the test/release track.

References: [Android keep-rule best practices](https://developer.android.com/topic/performance/app-optimization/keep-rules-best-practices), [library consumer rules](https://developer.android.com/topic/performance/app-optimization/library-optimization).

Final results: assembleRelease, bundleRelease and lintRelease passed (0 lint errors). Mapping validation passed for all four models and 33 getters. The framework-only runtime probe passed on the installed minified QA APK for model values, Room implementations and worker constructors. A newer manually signed APK appeared in app/release during verification; its DEX matches the tested QA release, and apksigner verifies the same original release certificate. The current Gradle AAB is unsigned and needs a signed bundle export. Signed-in Google/Firebase/ads flows were not exercised.

The isolated QA emulator was stopped after testing. Automatic approval review rejected the temporary emulator-folder cleanup command as blocked by policy, so those files remain under artifacts/qa.
