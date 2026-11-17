# Pockit v2.0 Release Notes

**Version Name:** `2.0`  
**Version Code:** `8`  
**Release Date:** November 2026  
**Target SDK:** Android 17 / API 37 (Min SDK 24)

---

## 🌟 What's New in Version 2.0

### 1. Android 17 (API 37) Support & Next-Gen Modernization
- **Full Android 17 Targeting:** Built and targeted for **Android 17 / API 37** (`compileSdk = 37`, `targetSdk = 37`).
- **Edge-to-Edge & Modern Inset System:** Fully compliant with Android 17 platform window inset dynamics, predictive gestures, and predictive back animations.

### 2. Material 3 Expressive Back Handling & Fluid Motion
- **Predictive Back Navigation:** Seamless system back gesture handling with expressive spring curves and tactile scale/alpha transitions.
- **Shared Element Transitions:** Liquid transitions connecting transaction items, Udhar ledgers, and friend items directly to their detailed views without visual stutter.

### 3. Expressive Transaction Cards & Visual Polish
- **Color-Coded Financial Direction:**
  - **Expense (Money Deducted):** Soft blush container (`#FFF2F2`) with refined border (`#FFDADA`) and bold `ExpenseRed` (`− ₹X.XX`).
  - **Income (Money Added):** Soft mint container (`#EAF8F3`) with refined border (`#C7EBD7`) and vibrant `IncomeGreen` (`+ ₹X.XX`).
- **Dynamic Category Avatars:** Thematic pastel squircle icons across Food, Groceries, Travel, Utilities, Shopping, Entertainment, Health, and more.
- **Micro Direction Indicators:** Crisp ↗ (Expense) and ↙ (Income) indicators attached to the category avatar.
- **Overflow Options Menu:** Replaced clunky raw text buttons with a sleek 3-dot popup menu for "Edit entry" and "Delete entry", with instant whole-card tap-to-edit.

### 4. "Your Week in Money" Bento Overhaul
- Transformed the weekly summary from a flat prototype card into an elevated Material 3 bento card with `WarmBorder`, high-contrast typography, and live trend indicator pills (e.g. *₹X less than previous 7 days*).
- Category-wise proportional progress indicators styled to the brand palette.

### 5. Friends Hub & Segmented Navigation
- Custom Expressive TabLayout cleanly separating **Personal Udhar** and **Connected Friends**.
- Instant QR code sharing and camera scanning for zero-friction peer connection.

### 6. Streamlined Profile & Settings
- Removed deprecated clutter: removed redundant individual app review checklists and onboarding replay loops.
- Instant, organized access to Budgets, UPI Payment Detection, Biometric App Lock, and Ad Consent controls.
- Quick navigation link for Monthly Budgets directly from the Home bento grid.

### 7. Universal Native & Banner Ad Placements Across All Devices
- Native Ad Cards and Banner Ads active across all devices without ad-free suppression conditions:
  - Recent Activities & All Transactions stream
  - Personal Udhar ledger & individual transaction timelines
  - Monthly Budgets dashboard
  - Friends & Spaces hubs
  - Notification stream
  - Profile settings
- Removed all legacy bypasses and ad-free database rules to ensure consistent ad delivery across all user devices.

---

## 🛠️ Developer & Build Highlights
- Upgraded `versionCode` to `8` and `versionName` to `"2.0"`.
- Cleaned up obsolete subscription routes and unused legacy components.
- Maintained 100% test coverage with 31/31 unit tests passing.
- Full R8 ProGuard compatibility and dead-code shrinking enabled for production release.
