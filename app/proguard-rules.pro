# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Gson — preserve the field names R8 would otherwise rename.
#
# Gson maps JSON keys <-> field names by reflection. R8 renames fields by
# default, which silently changes the JSON keys and breaks (a) backup export +
# restore, and (b) legacy JSON import. Keep the fields of every class Gson
# touches. Class names may still be obfuscated — only field names matter here.
# ---------------------------------------------------------------------------

# Keep generic type info for TypeToken (SearchPreferences' List<String>) and any
# Gson @SerializedName annotations.
-keepattributes Signature
-keepattributes *Annotation*

# Backup / restore models, plus the Room entities serialized directly inside a backup.
-keepclassmembers class in.deepak.remindme.data.backup.BackupData { <fields>; }
-keepclassmembers class in.deepak.remindme.data.backup.BackupSettings { <fields>; }
-keepclassmembers class in.deepak.remindme.data.db.ReminderEntity { <fields>; }
-keepclassmembers class in.deepak.remindme.data.db.TemplateEntity { <fields>; }
-keepclassmembers class in.deepak.remindme.data.db.ActivityStatEntity { <fields>; }

# Legacy JSON import models.
-keepclassmembers class in.deepak.remindme.data.dto.ReminderDto { <fields>; }
-keepclassmembers class in.deepak.remindme.data.dto.ReminderFileEnvelope { <fields>; }

# Enums serialized by name across the data layer (Gson uses name()/valueOf()).
-keepclassmembers enum in.deepak.remindme.data.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}