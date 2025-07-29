#include <jni.h>
#include <string.h>

#ifdef __APPLE__
#include "SparkeBridge.h"
#else
#include "WinSparkleBridge.h"
#endif

// Helper function to convert jstring to C string
static const char* jstring_to_cstring(JNIEnv* env, jstring jstr) {
    if (jstr == NULL) return NULL;
    return (*env)->GetStringUTFChars(env, jstr, NULL);
}

// Helper function to release C string
static void release_cstring(JNIEnv* env, jstring jstr, const char* cstr) {
    if (cstr != NULL) {
        (*env)->ReleaseStringUTFChars(env, jstr, cstr);
    }
}

// Sparkle JNI implementations (macOS)
#ifdef __APPLE__

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_SparkleUpdateManager_nativeInit(JNIEnv* env, jobject obj, jstring appcastUrl, jstring publicKey) {
    const char* url = jstring_to_cstring(env, appcastUrl);
    const char* key = jstring_to_cstring(env, publicKey);
    int result = sparkle_init(url, key);
    release_cstring(env, appcastUrl, url);
    release_cstring(env, publicKey, key);
    return result;
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_SparkleUpdateManager_nativeCheckForUpdates(JNIEnv* env, jobject obj, jboolean showUI) {
    return sparkle_check_for_updates(showUI ? 1 : 0);
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_SparkleUpdateManager_nativeSetAutomaticCheckEnabled(JNIEnv* env, jobject obj, jboolean enabled) {
    return sparkle_set_automatic_check_enabled(enabled ? 1 : 0);
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_SparkleUpdateManager_nativeSetUpdateCheckInterval(JNIEnv* env, jobject obj, jint hours) {
    return sparkle_set_update_check_interval(hours);
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_SparkleUpdateManager_nativeCleanup(JNIEnv* env, jobject obj) {
    return sparkle_cleanup();
}

#endif

// WinSparkle JNI implementations (Windows)
#ifdef _WIN32

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeInit(JNIEnv* env, jobject obj, jstring appcastUrl) {
    const char* url = jstring_to_cstring(env, appcastUrl);
    int result = winsparkle_init(url);
    release_cstring(env, appcastUrl, url);
    return result;
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeCheckForUpdates(JNIEnv* env, jobject obj, jboolean showUI) {
    return winsparkle_check_for_updates(showUI ? 1 : 0);
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeSetAutomaticCheckEnabled(JNIEnv* env, jobject obj, jboolean enabled) {
    return winsparkle_set_automatic_check_enabled(enabled ? 1 : 0);
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeSetUpdateCheckInterval(JNIEnv* env, jobject obj, jint hours) {
    return winsparkle_set_update_check_interval(hours);
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeSetAppDetails(JNIEnv* env, jobject obj, 
                                                                         jstring companyName, 
                                                                         jstring appName, 
                                                                         jstring appVersion) {
    const char* company = jstring_to_cstring(env, companyName);
    const char* app = jstring_to_cstring(env, appName);
    const char* version = jstring_to_cstring(env, appVersion);
    
    int result = winsparkle_set_app_details(company, app, version);
    
    release_cstring(env, companyName, company);
    release_cstring(env, appName, app);
    release_cstring(env, appVersion, version);
    
    return result;
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeCleanup(JNIEnv* env, jobject obj) {
    return winsparkle_cleanup();
}

#endif