-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.convex.app.data.prefs.** { *; }
-dontwarn com.arthenica.**

# Optimize: Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Optimize: Hilt
-keep class dagger.hilt.** { *; }

# Optimize: Remove log statements in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
