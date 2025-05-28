#ifndef NetworkTypeFinder_h
#define NetworkTypeFinder_h

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL Java_org_ooni_engine_MacOsNetworkTypeFinder_getNetworkType(JNIEnv *env, jobject obj);

#ifdef __cplusplus
}
#endif
#endif
