# Proguard Rules for SalatSchedule (Android TV — Home Prayer Times & Adhan)

# Keep Data Models & Preferences Serializations
-keep class com.salatsign.tv.data.model.** { *; }
-keepclassmembers class com.salatsign.tv.data.model.** { *; }

# Keep Adhan & Alarm Services / Receivers
-keep class com.salatschedule.tv.service.** { *; }
-keepclassmembers class com.salatschedule.tv.service.** { *; }

# Keep Adhan Offline Astronomical Engine
-keep class com.batoulapps.adhan.** { *; }
-keepclassmembers class com.batoulapps.adhan.** { *; }

# Keep DataStore Preferences
-keep class androidx.datastore.preferences.** { *; }

# Keep Coil 3 Image Loading & Image Decoders
-keep class coil3.** { *; }

# Keep WorkManager Workers
-keep class * extends androidx.work.Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# General Keep Attributes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
