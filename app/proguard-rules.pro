# App-specific reflection contracts. SDKs supply their own consumer R8 rules.
# Keep optimization and resource shrinking enabled in the release build.

-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Firestore serializes these Kotlin objects via reflection. Preserve their
# property/getter names, including boolean isRead -> read, across release builds.
-keep class com.invatech.pockit.data.local.database.FinanceRecord { *; }
-keep class com.invatech.pockit.data.local.entity.CategoryBudgetEntity { *; }
-keep class com.invatech.pockit.data.local.entity.SubscriptionEntity { *; }
-keep class com.invatech.pockit.data.local.entity.NotificationEntity { *; }

# Glance can instantiate action callbacks by their class name.
-keep class com.invatech.pockit.** implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}

# Room, WorkManager, Firebase, Play services, Coil, coroutines and biometric
# libraries contribute consumer rules. Do not blanket-keep those SDK packages or
# suppress all their missing-class warnings: release builds must report problems.
