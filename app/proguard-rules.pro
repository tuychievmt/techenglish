# =========================================================================
# TechEnglish v3.1.0 R8 / ProGuard Rules (Production Release)
# Covers: Kotlinx Serialization + Ktor DTOs, Room, AndroidKeystore, Hilt,
#         WorkManager, OkHttp/WebSockets, Glance, and Debug Log Stripping
# =========================================================================

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# --- Strip verbose/debug/info logs from release bytecode ---
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# --- Kotlinx Serialization (Official R8 full-mode + Ktor TypeInfo reflection) ---
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable classes, Companion serializer() lookups, synthetic constructors, and $$serializer across TechEnglish
-keep,includedescriptorclasses @kotlinx.serialization.Serializable class com.techenglish.** { *; }
-keep,includedescriptorclasses class com.techenglish.**$$serializer { *; }
-keepclassmembers class com.techenglish.** {
    *** Companion;
}
-keepclasseswithmembers class com.techenglish.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Generic rules for any library @Serializable classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    <init>(...);
}
-if @kotlinx.serialization.Serializable class **
-keep class <1>$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep domain enums & models used across Ktor / Room / ViewModels
-keep class com.techenglish.domain.model.** { *; }

# --- Room Database, Entities, DAOs & Query Projections ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.techenglish.app.core.database.entity.** { *; }
-keep class com.techenglish.app.core.database.dao.** { *; }
-dontwarn androidx.room.paging.**
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# --- AndroidKeystore & Token Security ---
-keep class com.techenglish.app.core.security.** { *; }
-keep class android.security.keystore.** { *; }
-keep class javax.crypto.** { *; }

# --- Hilt / Dagger / WorkManager ---
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(...);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# --- Ktor Client + OkHttp + Coroutines ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.Unsafe
-dontwarn kotlinx.coroutines.debug.**

# --- Jetpack Glance AppWidget ---
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class com.techenglish.app.feature.widget.** { *; }

# --- Keep clean stack traces in release crashes ---
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
