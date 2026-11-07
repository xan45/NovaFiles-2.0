# ============================================================================
# NovaFiles 2.0 — Production ProGuard & R8 Optimization Rules
# ============================================================================

# ----------------------------------------------------------------------------
# 1. Kotlin & Coroutines
# ----------------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn kotlin.Unit
-dontwarn kotlinx.coroutines.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ----------------------------------------------------------------------------
# 2. Kotlinx Serialization
# ----------------------------------------------------------------------------
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepnames class kotlinx.serialization.protobuf.** { *; }

# ----------------------------------------------------------------------------
# 3. AndroidX Compose & Material 3
# ----------------------------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# ----------------------------------------------------------------------------
# 4. AndroidX Media3 (ExoPlayer & MediaSession)
# ----------------------------------------------------------------------------
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# ----------------------------------------------------------------------------
# 5. Coil Image Loading
# ----------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# ----------------------------------------------------------------------------
# 6. AndroidX Biometric & Security
# ----------------------------------------------------------------------------
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# ----------------------------------------------------------------------------
# 7. NovaFiles Core Domain, Models & Security
# ----------------------------------------------------------------------------
-keep class com.novafiles.core.model.** { *; }
-keep class com.novafiles.core.security.** { *; }
-keep class com.novafiles.core.navigation.** { *; }
-keep class com.novafiles.core.designsystem.theme.** { *; }
