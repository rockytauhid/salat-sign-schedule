# Proguard Rules for SalatSign (Android TV — Strictly Silent Mosque Signage)

# Keep Data Models & Preferences Serializations
-keep class com.salatsign.tv.data.model.** { *; }
-keepclassmembers class com.salatsign.tv.data.model.** { *; }

# Keep Local Phone Controller API & Models
-keep class com.salatsign.tv.remote.** { *; }
-keepclassmembers class com.salatsign.tv.remote.** { *; }

# Keep Adhan Offline Astronomical Engine
-keep class com.batoulapps.adhan.** { *; }
-keepclassmembers class com.batoulapps.adhan.** { *; }

# Keep DataStore Preferences
-keep class androidx.datastore.preferences.** { *; }

# Keep Coil 3 Image Loading & Image Decoders
-keep class io.coilkt.coil3.** { *; }

# General Keep Attributes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
