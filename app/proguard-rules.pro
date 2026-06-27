# ProGuard rules for BuKeBiao
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# pdfbox
-dontwarn org.apache.pdfbox.**
-dontwarn org.spongycastle.**
