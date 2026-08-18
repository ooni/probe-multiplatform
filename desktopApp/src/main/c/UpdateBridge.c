#include <jni.h>
#include <string.h>

#ifdef __APPLE__
#include "SparkeBridge.h"
#else
#include "WinSparkleBridge.h"
#include <windows.h>
#endif

#ifdef _WIN32
// DllMain to set DLL search directory when the DLL is loaded
// This ensures that libwinpthread-1.dll and other dependencies can be found
BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
    if (fdwReason == DLL_PROCESS_ATTACH) {
        // Get the directory where this DLL is located
        char dllPath[MAX_PATH];
        if (GetModuleFileNameA(hinstDLL, dllPath, MAX_PATH) > 0) {
            // Extract directory path by removing the filename
            char* lastSlash = strrchr(dllPath, '\\');
            if (lastSlash != NULL) {
                *lastSlash = '\0'; // Null-terminate at the last backslash
                // Set this directory as the DLL search directory
                // This allows Windows to find libwinpthread-1.dll and other dependencies
                SetDllDirectoryA(dllPath);
            }
        }
    }
    return TRUE;
}
#endif

static const char* jstring_to_cstring(JNIEnv* env, jstring jstr);
static void release_cstring(JNIEnv* env, jstring jstr, const char* cstr);
static JNIEnv* get_callback_env(int* detach_when_done);

// Global JVM and callback references
static JavaVM* g_jvm = NULL;
static jobject g_logCallbackObject = NULL;
static jmethodID g_logCallbackMethod = NULL;
static jmethodID g_updateFoundCallbackMethod = NULL;
static jmethodID g_noUpdateCallbackMethod = NULL;
static jobject g_shutdownCallbackObject = NULL;
static jmethodID g_shutdownCallbackMethod = NULL;

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

static JNIEnv* get_callback_env(int* detach_when_done) {
    JNIEnv* env = NULL;
    *detach_when_done = 0;

    jint result = (*g_jvm)->GetEnv(g_jvm, (void**)&env, JNI_VERSION_1_6);
    if (result == JNI_OK) {
        return env;
    }
    if (result != JNI_EDETACHED ||
        (*g_jvm)->AttachCurrentThread(g_jvm, (void**)&env, NULL) != JNI_OK) {
        return NULL;
    }

    *detach_when_done = 1;
    return env;
}

// Log callback function that forwards to Java
#ifdef __APPLE__
static void native_log_callback(SparkleLogLevel level, const char* operation, const char* message) {
#else
static void native_log_callback(WinSparkleLogLevel level, const char* operation, const char* message) {
#endif
    if (g_jvm == NULL || g_logCallbackObject == NULL || g_logCallbackMethod == NULL) {
        return;
    }

    int detach_when_done;
    JNIEnv* env = get_callback_env(&detach_when_done);
    if (env == NULL) return;

    // Create Java strings
    jstring jOperation = (*env)->NewStringUTF(env, operation);
    jstring jMessage = (*env)->NewStringUTF(env, message);

    if (jOperation != NULL && jMessage != NULL) {
        // Call Java callback method
        (*env)->CallVoidMethod(env, g_logCallbackObject, g_logCallbackMethod,
                (jint)level, jOperation, jMessage);
    }

    // Clean up local references
    if (jOperation != NULL) (*env)->DeleteLocalRef(env, jOperation);
    if (jMessage != NULL) (*env)->DeleteLocalRef(env, jMessage);

    if (detach_when_done) (*g_jvm)->DetachCurrentThread(g_jvm);
}

// Shutdown callback function that forwards to Java
static void native_shutdown_callback(void) {
    if (g_jvm == NULL || g_shutdownCallbackObject == NULL || g_shutdownCallbackMethod == NULL) {
        return;
    }

    int detach_when_done;
    JNIEnv* env = get_callback_env(&detach_when_done);
    if (env == NULL) return;

    // Call Java shutdown callback method
    (*env)->CallVoidMethod(env, g_shutdownCallbackObject, g_shutdownCallbackMethod);

    if (detach_when_done) (*g_jvm)->DetachCurrentThread(g_jvm);
}

static void native_update_found_callback(const char* version, const char* description) {
    if (g_jvm == NULL || g_logCallbackObject == NULL || g_updateFoundCallbackMethod == NULL) {
        return;
    }

    int detach_when_done;
    JNIEnv* env = get_callback_env(&detach_when_done);
    if (env == NULL) return;

    jstring jVersion = (*env)->NewStringUTF(env, version);
    jstring jDescription = (*env)->NewStringUTF(env, description);
    if (jVersion != NULL && jDescription != NULL) {
        (*env)->CallVoidMethod(env, g_logCallbackObject, g_updateFoundCallbackMethod, jVersion, jDescription);
    }
    if (jVersion != NULL) (*env)->DeleteLocalRef(env, jVersion);
    if (jDescription != NULL) (*env)->DeleteLocalRef(env, jDescription);
    if (detach_when_done) (*g_jvm)->DetachCurrentThread(g_jvm);
}

static void native_no_update_callback(void) {
    if (g_jvm == NULL || g_logCallbackObject == NULL || g_noUpdateCallbackMethod == NULL) {
        return;
    }

    int detach_when_done;
    JNIEnv* env = get_callback_env(&detach_when_done);
    if (env == NULL) return;
    (*env)->CallVoidMethod(env, g_logCallbackObject, g_noUpdateCallbackMethod);
    if (detach_when_done) (*g_jvm)->DetachCurrentThread(g_jvm);
}

static jint set_log_callback(JNIEnv* env, jobject callback) {
    if (g_jvm == NULL && (*env)->GetJavaVM(env, &g_jvm) != JNI_OK) {
        return -1;
    }

    if (g_logCallbackObject != NULL) {
        (*env)->DeleteGlobalRef(env, g_logCallbackObject);
        g_logCallbackObject = NULL;
        g_logCallbackMethod = NULL;
        g_updateFoundCallbackMethod = NULL;
        g_noUpdateCallbackMethod = NULL;
    }

    if (callback == NULL) {
#ifdef __APPLE__
        sparkle_set_log_callback(NULL);
        sparkle_set_update_callback(NULL);
        sparkle_set_no_update_callback(NULL);
#else
        winsparkle_set_log_callback(NULL);
        winsparkle_set_update_callback(NULL);
        winsparkle_set_no_update_callback(NULL);
#endif
        return 0;
    }

    g_logCallbackObject = (*env)->NewGlobalRef(env, callback);
    if (g_logCallbackObject == NULL) return -2;

    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    g_logCallbackMethod = (*env)->GetMethodID(env, callbackClass, "onLog", "(ILjava/lang/String;Ljava/lang/String;)V");
    g_updateFoundCallbackMethod = (*env)->GetMethodID(env, callbackClass, "onUpdateFound", "(Ljava/lang/String;Ljava/lang/String;)V");
    g_noUpdateCallbackMethod = (*env)->GetMethodID(env, callbackClass, "onUpdateNotFound", "()V");
    (*env)->DeleteLocalRef(env, callbackClass);

    if (g_logCallbackMethod == NULL || g_updateFoundCallbackMethod == NULL || g_noUpdateCallbackMethod == NULL) {
        (*env)->DeleteGlobalRef(env, g_logCallbackObject);
        g_logCallbackObject = NULL;
        g_logCallbackMethod = NULL;
        g_updateFoundCallbackMethod = NULL;
        g_noUpdateCallbackMethod = NULL;
        return -3;
    }

#ifdef __APPLE__
    sparkle_set_log_callback(native_log_callback);
    sparkle_set_update_callback(native_update_found_callback);
    sparkle_set_no_update_callback(native_no_update_callback);
#else
    winsparkle_set_log_callback(native_log_callback);
    winsparkle_set_update_callback(native_update_found_callback);
    winsparkle_set_no_update_callback(native_no_update_callback);
#endif
    return 0;
}

// JNI function to set log callback
JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_UpdateManagerBase_nativeSetLogCallback(JNIEnv* env, jobject obj, jobject callback) {
    return set_log_callback(env, callback);
}

// JNI function to set shutdown callback
JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeSetShutdownCallback(JNIEnv* env, jobject obj, jobject callback) {
    // Get JavaVM for later use
    if (g_jvm == NULL) {
        if ((*env)->GetJavaVM(env, &g_jvm) != JNI_OK) {
            return -1;
        }
    }

    // Clear existing callback
    if (g_shutdownCallbackObject != NULL) {
        (*env)->DeleteGlobalRef(env, g_shutdownCallbackObject);
        g_shutdownCallbackObject = NULL;
        g_shutdownCallbackMethod = NULL;
    }

    if (callback == NULL) {
        // Disable callback
#ifdef _WIN32
        winsparkle_set_shutdown_callback(NULL);
#endif
        return 0;
    }

    // Create global reference to callback object
    g_shutdownCallbackObject = (*env)->NewGlobalRef(env, callback);
    if (g_shutdownCallbackObject == NULL) {
        return -2;
    }

    // Get the callback method
    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    g_shutdownCallbackMethod = (*env)->GetMethodID(env, callbackClass, "onShutdownRequested", "()V");
    (*env)->DeleteLocalRef(env, callbackClass);

    if (g_shutdownCallbackMethod == NULL) {
        (*env)->DeleteGlobalRef(env, g_shutdownCallbackObject);
        g_shutdownCallbackObject = NULL;
        return -3;
    }

    // Set native callback
#ifdef _WIN32
    winsparkle_set_shutdown_callback(native_shutdown_callback);
#endif

    return 0;
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
Java_org_ooni_probe_shared_SparkleUpdateManager_nativeSetLogCallback(JNIEnv* env, jobject obj, jobject callback) {
    return set_log_callback(env, callback);
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

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_SparkleUpdateManager_nativeSetShutdownCallback(JNIEnv* env, jobject obj, jobject callback) {
    // Get JavaVM for later use
    if (g_jvm == NULL) {
        if ((*env)->GetJavaVM(env, &g_jvm) != JNI_OK) {
            return -1;
        }
    }

    // Clear existing shutdown callback
    if (g_shutdownCallbackObject != NULL) {
        (*env)->DeleteGlobalRef(env, g_shutdownCallbackObject);
        g_shutdownCallbackObject = NULL;
        g_shutdownCallbackMethod = NULL;
    }

    if (callback == NULL) {
        // Disable callback
        sparkle_set_shutdown_callback(NULL);
        return 0;
    }

    // Create global reference to callback object
    g_shutdownCallbackObject = (*env)->NewGlobalRef(env, callback);
    if (g_shutdownCallbackObject == NULL) {
        return -2;
    }

    // Get the callback method
    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    g_shutdownCallbackMethod = (*env)->GetMethodID(env, callbackClass, "onShutdownRequested", "()V");
    (*env)->DeleteLocalRef(env, callbackClass);

    if (g_shutdownCallbackMethod == NULL) {
        (*env)->DeleteGlobalRef(env, g_shutdownCallbackObject);
        g_shutdownCallbackObject = NULL;
        return -3;
    }

    // Set native callback
    sparkle_set_shutdown_callback(native_shutdown_callback);

    return 0;
}

#endif

// WinSparkle JNI implementations (Windows)
#ifdef _WIN32

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeInit(JNIEnv* env, jobject obj, jstring appcastUrl, jstring publicKey, jstring appVersion, jint checkIntervalHours) {
    const char* url = jstring_to_cstring(env, appcastUrl);
    const char* key = jstring_to_cstring(env, publicKey);
    const char* version = jstring_to_cstring(env, appVersion);
    int result = winsparkle_init(url, key, "OONI", "OONI Probe", version, checkIntervalHours);
    release_cstring(env, appcastUrl, url);
    release_cstring(env, publicKey, key);
    release_cstring(env, appVersion, version);
    return result;
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeSetLogCallback(JNIEnv* env, jobject obj, jobject callback) {
    return set_log_callback(env, callback);
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

JNIEXPORT void JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeSetDllRoot(JNIEnv* env, jobject obj, jstring rootPath) {
    const char* root_utf8 = jstring_to_cstring(env, rootPath);
    winsparkle_set_dll_root(root_utf8);
    release_cstring(env, rootPath, root_utf8);
}

JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeCleanup(JNIEnv* env, jobject obj) {
    return winsparkle_cleanup();
}

#endif
