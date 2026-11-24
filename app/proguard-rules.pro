# =========================================================================
# TechEnglish v3.1 R8 / ProGuard Rules (SP1-08 / BUG-11)
# Covers: Kotlinx Serialization, Room, Hilt/WorkManager, Ktor/OkHttp, Glance
# =========================================================================

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.techenglish.app.core.network.**$$serializer { *; }
-keepclassmembers class com.techenglish.app.core.network.** {
    *** Companion;
}
-keepclasseswithmembers class com.techenglish.app.core.network.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.techenglish.app.core.database.**$$serializer { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class **
-keep class <1>$$serializer { *; }

# --- Room Database & Entities ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

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

# --- Jetpack Glance AppWidget ---
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class com.techenglish.app.feature.widget.** { *; }

# --- Keep clean stack traces in release crashes ---
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
