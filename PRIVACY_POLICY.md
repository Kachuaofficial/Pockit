# Privacy Policy for Pockit

**Effective Date:** October 31, 2026  
**Last Updated:** November 24, 2026  

Welcome to **Pockit** ("we", "our", or "us"), an Android application developed by **InvaTech**. This Privacy Policy explains how we collect, use, disclose, and safeguard your personal information when you use our mobile application **Pockit** (Package Name: `com.invatech.pockit`).

By installing or using Pockit, you agree to the collection and use of information in accordance with this Privacy Policy. If you do not agree with the terms of this policy, please do not use the application.

---

## Advertising, rewarded access and app verification

Pockit is supported by banner, native and optional rewarded ads. It does not sell Pro subscriptions or virtual coins. Reward videos are shown only after you select a clearly labelled offer. Completing one stores the named feature's 24-hour expiry locally on your device.

Google AdMob may process ad and device identifiers needed to deliver, measure and protect advertising. Firebase App Check and Google Play Integrity verify authentic app requests. Receipt images continue to be processed on the device by ML Kit. Rewarded feature access does not change the handling of your financial wallet records.

## 1. Information We Collect

### A. Personal & Account Information
- **Authentication Data:** When you sign up or log in using Google Sign-In or email authentication via Google Firebase Authentication, we collect your name, email address, and unique user identifier (UID).
- **Profile Information:** Display name and avatar image URL (if provided by your Google account).

### B. Financial & Transactional Data (User-Generated)
- **Expenses & Budgets:** Expense descriptions, amounts, timestamps, payment methods, transaction categories, and budget targets that you manually create or manage within the app.
- **Spaces & Group Settlements:** Information regarding shared expense groups ("Spaces"), group member IDs, split calculations, and settlement status.
- *Important note:* Pockit is a manual and collaborative expense tracking utility. We do **not** read your personal SMS messages, access your bank accounts, or store credit/debit card numbers or bank credentials.

### C. Device & Technical Information
- **Push Notification Tokens:** Firebase Cloud Messaging (FCM) registration tokens to deliver timely expense notifications and space activity alerts (requires `android.permission.POST_NOTIFICATIONS`).
- **Device & Log Data:** Operating system version, device model, app version, language preference, and anonymous crash/diagnostic logs for app stability.
- **Internet Access:** Network state and internet access (`android.permission.INTERNET`) used to synchronize your transactions with secure cloud storage and deliver ads.

### D. Device Sensors & Hardware Features
- **Camera / QR Code Scanning:** Pockit integrates Google Play Services Code Scanner and ZXing to scan UPI QR codes or Space invite codes. All scanning occurs locally or via official Google Play system interfaces. **We do not record, store, or upload photos or videos to any remote server.**
- **Biometric Data:** Pockit provides an optional biometric lock (fingerprint/face unlock) for your privacy. Authentication is executed strictly on your local device via Android's secure `BiometricPrompt` framework. **Pockit never accesses, collects, stores, or transmits your biometric data.**

---

## 2. Third-Party Services & SDKs

Pockit integrates trusted third-party SDKs that may collect information used to identify you or provide core app features. These services have their own privacy policies:

1. **Google Firebase (Google LLC)**
   - *Services:* Firebase Authentication, Cloud Firestore, Firebase Cloud Messaging.
   - *Purpose:* Secure user login, real-time database synchronization across devices, and push notifications.
   - *Privacy Policy:* [https://firebase.google.com/support/privacy](https://firebase.google.com/support/privacy)

2. **Google AdMob & User Messaging Platform (Google LLC)**
   - *Purpose:* To display contextual or personalized advertisements and manage GDPR/CCPA user consent. AdMob may collect and process device identifiers, advertising ID (GAID), and approximate location data in accordance with Google's Advertising Policies.
   - *Privacy Policy:* [https://policies.google.com/technologies/ads](https://policies.google.com/technologies/ads)

3. **Google Play Services**
   - *Purpose:* Authentication, Google Code Scanner, and system APIs.
   - *Privacy Policy:* [https://policies.google.com/privacy](https://policies.google.com/privacy)

---

## 3. How We Use Your Information

We use the information we collect strictly to:
- Provide, operate, and maintain the core features of Pockit (personal wallets, multi-party ledger, group expense splitting).
- Synchronize your financial data securely across your devices.
- Send important transactional notifications, such as when an expense is added to a shared Space or a settlement is recorded.
- Serve advertisements to support the free features of the app (in compliance with Google Play Developer Policies and user consent preferences).
- Detect, prevent, and troubleshoot technical glitches, crashes, or unauthorized access.

---

## 4. How Your Information Is Shared

We **do not sell, rent, trade, or monetize** your personal or financial data to any third party. Your information is shared only under the following limited circumstances:
- **Collaborative Spaces:** When you join or create a shared Space with other users, participants in that Space can view the expenses, amounts, and settlement balances associated with that group.
- **Service Providers:** Trusted cloud and infrastructure vendors (such as Google Firebase) solely for hosting and data processing on our behalf.
- **Legal Compliance:** If required by applicable law, regulation, legal process, or governmental request.

---

## 5. Data Security

We implement industry-standard technical and organizational security measures to protect your data:
- All data transmission between Pockit and our backend servers is encrypted in transit using Transport Layer Security (HTTPS/TLS).
- Data stored in Google Cloud Firestore is safeguarded by strict database security rules ensuring that only authorized users can read or modify their own data or shared space records.

While we strive to use commercially acceptable means to protect your personal information, please be aware that no method of transmission over the internet or electronic storage is 100% secure.

---

## 6. Data Retention and Account / Data Deletion Policy

In compliance with Google Play Data Safety requirements:
- **Retention:** We retain your account and financial ledger data for as long as your account is active and needed to provide you with the application's services.
- **Account & Data Deletion:** You have the right to delete your account and all associated personal data at any time.
  - You can request complete deletion of your account and data directly from within the app settings or by sending an email request to **[YOUR_SUPPORT_EMAIL_HERE]** with the subject line *"Account Deletion Request"*.
  - Upon receiving your request, your account, personal details, and associated cloud ledger entries will be permanently deleted from our active database within 30 days.

---

## 7. Children's Privacy

Pockit is not intended for use by children under the age of 13 (or under 16 in the European Economic Area). We do not knowingly collect personally identifiable information from children. If you become aware that a child has provided us with personal information without parental consent, please contact us immediately, and we will delete such information promptly.

---

## 8. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Any changes will be posted on this page with an updated "Effective Date". We encourage you to review this Privacy Policy periodically for any changes.

---

## 9. Contact Us

If you have questions, suggestions, or concerns regarding this Privacy Policy or your data, please contact us at:

- **Application:** Pockit
- **Developer / Organization:** InvaTech
- **Email:** [YOUR_SUPPORT_EMAIL_HERE]
- **Country:** India


## Optional notification review and private planning

When you enable notification access, Android makes notifications available to Pockit. Pockit checks the originating app against its supported UPI-app list before extracting payment details. You can disable each supported app or turn tracking off in Settings. Parsing happens on the device; raw notification text is not retained. Structured detected payment details sync to your Firebase account in a private review inbox and do not affect your wallet until approved. Approved wallet entries use the existing Firebase account sync.

Name-only udhar records, savings goals, pocket-money plans, notification history/read state and user preferences sync to Firestore under your signed-in account. Local databases retain cached data and may hold payment drafts waiting for a connection. Unsynced changes can be lost if app data is cleared. Name-only records are not shared with the named friend. Device permissions and biometric enrollment stay device-specific. Reminder timing uses activity within Pockit, not activity in other apps. Financial notification text is not sent to advertising providers.


## Offline entries and synchronization

New expense and income entries recorded offline remain in an account-scoped local queue on the device. When the user reconnects, pending entries sync automatically to their Firebase account. The server stores an account-linked sync receipt to prevent duplicate writes. Entries remain local until the server accepts the batch. Clearing app data or uninstalling before sync removes unsynced entries.
