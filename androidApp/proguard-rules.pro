-dontwarn io.sentry.android.replay.SessionReplayOptionsKt

# JNA (used at runtime by org.ooni:passport-android UniFFI bindings, via jna aar)
# R8 must not strip/rename JNA classes or their fields. In particular the native
# libjnidispatch.so looks up com.sun.jna.Pointer.peer by JNI; if it is removed or
# renamed the loader fails with:
#   "Can't obtain peer field ID for class com.sun.jna.Pointer"
-dontwarn java.awt.*
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-keep class * implements com.sun.jna.Library { *; }
-keep class * implements com.sun.jna.Callback { *; }

# UniFFI-generated bindings (passport-android) layered on JNA
# Method names map to native symbol names and Structure fields are marshalled by name,
# so keep the generated FFI types intact under minification.
-keep class uniffi.** { *; }
