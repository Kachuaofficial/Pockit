# ProGuard & R8 rules for Pockit

# --- WorkManager & Room (Required by AdMob / Google Mobile Ads SDK) ---
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}

-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
    *;
}

# --- Google Play Services, AdMob & UMP ---
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}
-keep class com.google.android.ump.** { *; }
-keep class com.google.android.gms.code.scanner.** { *; }
-dontwarn com.google.android.gms.**

# --- Biometric ---
-keep class androidx.biometric.** { *; }

# --- ZXing Barcode / QR ---
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# --- Preserve Annotations & Reflection ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Pockit Data & UI Models (Preserve from R8 obfuscation for serialization) ---
-keep class com.invatech.pockit.data.**.model.** { *; }
-keep class com.invatech.pockit.ui.**.model.** { *; }
-keep class com.invatech.pockit.data.repository.** { *; }

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
