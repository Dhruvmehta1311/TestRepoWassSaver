# Keep app classes
-keep class com.pbhadoo.wassaver.** { *; }

# Coil
-dontwarn coil.**

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# org.json (used for GitHub API)
-keep class org.json.** { *; }

# Keep service
-keep class com.pbhadoo.wassaver.service.** { *; }
