#include <jni.h>
#include <string.h>

#include "WinSparkleBridge.h"

static const char* jstring_to_cstring(JNIEnv* env, jstring jstr);
static void release_cstring(JNIEnv* env, jstring jstr, const char* cstr);

// Global JVM and callback references
static JavaVM* g_jvm = NULL;
static jobject g_logCallbackObject = NULL;
static jmethodID g_logCallbackMethod = NULL;
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

// Log callback function that forwards to Java (WinSparkle only retained)
static void native_log_callback(WinSparkleLogLevel level, const char* operation, const char* message) {
    if (g_jvm == NULL || g_logCallbackObject == NULL || g_logCallbackMethod == NULL) {
        return;
    }

    JNIEnv* env = NULL;
    if ((*g_jvm)->AttachCurrentThread(g_jvm, (void**)&env, NULL) != JNI_OK) {
        return;
    }

    jstring jOperation = (*env)->NewStringUTF(env, operation);
    jstring jMessage = (*env)->NewStringUTF(env, message);

    if (jOperation != NULL && jMessage != NULL) {
        (*env)->CallVoidMethod(env, g_logCallbackObject, g_logCallbackMethod,
                               (jint)level, jOperation, jMessage);
    }

    if (jOperation != NULL) (*env)->DeleteLocalRef(env, jOperation);
    if (jMessage != NULL) (*env)->DeleteLocalRef(env, jMessage);

    (*g_jvm)->DetachCurrentThread(g_jvm);
}

// Shutdown callback function that forwards to Java (Windows only usage)
static void native_shutdown_callback(void) {
    if (g_jvm == NULL || g_shutdownCallbackObject == NULL || g_shutdownCallbackMethod == NULL) {
        return;
    }

    JNIEnv* env = NULL;
    if ((*g_jvm)->AttachCurrentThread(g_jvm, (void**)&env, NULL) != JNI_OK) {
        return;
    }

    (*env)->CallVoidMethod(env, g_shutdownCallbackObject, g_shutdownCallbackMethod);

    (*g_jvm)->DetachCurrentThread(g_jvm);
}

// JNI function to set log callback
JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_UpdateManagerBase_nativeSetLogCallback(JNIEnv* env, jobject obj, jobject callback) {
    if (g_jvm == NULL) {
        if ((*env)->GetJavaVM(env, &g_jvm) != JNI_OK) {
            return -1;
        }
    }

    if (g_logCallbackObject != NULL) {
        (*env)->DeleteGlobalRef(env, g_logCallbackObject);
        g_logCallbackObject = NULL;
        g_logCallbackMethod = NULL;
    }

    if (callback == NULL) {
#ifdef _WIN32
        winsparkle_set_log_callback(NULL);
#endif
        return 0;
    }

    g_logCallbackObject = (*env)->NewGlobalRef(env, callback);
    if (g_logCallbackObject == NULL) {
        return -2;
    }

    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    g_logCallbackMethod = (*env)->GetMethodID(env, callbackClass, "onLog", "(ILjava/lang/String;Ljava/lang/String;)V");
    (*env)->DeleteLocalRef(env, callbackClass);

    if (g_logCallbackMethod == NULL) {
        (*env)->DeleteGlobalRef(env, g_logCallbackObject);
        g_logCallbackObject = NULL;
        return -3;
    }

#ifdef _WIN32
    winsparkle_set_log_callback(native_log_callback);
#endif

    return 0;
}

// JNI function to set shutdown callback (Windows only)
JNIEXPORT jint JNICALL
Java_org_ooni_probe_shared_WinSparkleUpdateManager_nativeSetShutdownCallback(JNIEnv* env, jobject obj, jobject callback) {
    if (g_jvm == NULL) {
        if ((*env)->GetJavaVM(env, &g_jvm) != JNI_OK) {
            return -1;
        }
    }

    if (g_shutdownCallbackObject != NULL) {
        (*env)->DeleteGlobalRef(env, g_shutdownCallbackObject);
        g_shutdownCallbackObject = NULL;
        g_shutdownCallbackMethod = NULL;
    }

    if (callback == NULL) {
#ifdef _WIN32
        winsparkle_set_shutdown_callback(NULL);
#endif
        return 0;
    }

    g_shutdownCallbackObject = (*env)->NewGlobalRef(env, callback);
    if (g_shutdownCallbackObject == NULL) {
        return -2;
    }

    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    g_shutdownCallbackMethod = (*env)->GetMethodID(env, callbackClass, "onShutdownRequested", "()V");
    (*env)->DeleteLocalRef(env, callbackClass);

    if (g_shutdownCallbackMethod == NULL) {
        (*env)->DeleteGlobalRef(env, g_shutdownCallbackObject);
        g_shutdownCallbackObject = NULL;
        return -3;
    }

#ifdef _WIN32
    winsparkle_set_shutdown_callback(native_shutdown_callback);
#endif

    return 0;
}

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