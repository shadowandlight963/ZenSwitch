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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.api.** { *; }
-keep class com.google.protobuf.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class * extends com.google.protobuf.GeneratedMessageV3 { *; }

# Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.cloud.firestore.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep model classes
-keep class com.example.ourmajor.data.** { *; }
-keep class com.example.ourmajor.ui.** { *; }

# ZenSwitch Focus Engine: services and receivers must not be obfuscated so the system can find them.
-keep class com.example.ourmajor.engine.** { *; }
-keep class com.example.ourmajor.engine.FocusMonitorService { *; }
-keep class com.example.ourmajor.engine.RestartReceiver { *; }
-keep class com.example.ourmajor.engine.FocusRepository { *; }
-keep class com.example.ourmajor.engine.PermissionManager { *; }
-keep class com.example.ourmajor.engine.ActivityRegistry { *; }
-keep class com.example.ourmajor.engine.UsageCalculator { *; }
-keep class com.example.ourmajor.engine.BatteryOptimizationHelper { *; }