# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/stas/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontobfuscate
-dontshrink
-dontoptimize

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}

-keep class com.amazonaws.** { *; }
-keepnames class com.amazonaws.** { *; }

-dontnote android.net.http.*
-dontnote org.apache.commons.codec.**
-dontnote org.apache.http.**
-dontnote android.support.**

#-dontskipnonpubliclibraryclasses
#-dontobfuscate
#-forceprocessing
#-optimizationpasses 5
#
#-assumenosideeffects class android.util.Log {
#    public static boolean isLoggable(java.lang.String, int);
#    public static int v(...);
#    public static int i(...);
#    public static int w(...);
#    public static int d(...);
#}
#
#-keep class com.amazonaws.** { *; }
#-keepnames class com.amazonaws.** { *; }
#-dontwarn com.amazonaws.**
#-dontwarn com.fasterxml.**
#
#-dontnote android.net.http.*
#-dontnote org.apache.commons.codec.**
#-dontnote org.apache.http.**
#-dontnote com.google.vending.licensing.ILicensingService
#-keepattributes Signature
#-keep class com.google.android.gms.internal.** { *; }
#-dontnote android.support.**
#
## Allow obfuscation of android.support.v7.internal.view.menu.**
## to avoid problem on Samsung 4.2.2 devices with appcompat v21
## see https://code.google.com/p/android/issues/detail?id=78377
#-keep class !android.support.v7.internal.view.menu.**,android.support.** {*;}