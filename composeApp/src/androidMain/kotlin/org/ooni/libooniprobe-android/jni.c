#include <jni.h>
#include <stdlib.h>
#include <string.h>

struct go_string { const char *str; long n; };
extern char *apiCall(struct go_string fname);
extern char *apiCallWithArgs(struct go_string fname, struct go_string args);

JNIEXPORT jstring JNICALL Java_org_ooni_probe_GoOONIProbeClient_apiCall(JNIEnv *env, jclass c, jstring fname)
{
    jstring ret;
	const char *fname_str = (*env)->GetStringUTFChars(env, fname, 0);
	size_t fname_len = (*env)->GetStringUTFLength(env, fname);
	char *d = apiCall((struct go_string){
		.str = fname_str,
		.n = fname_len
	});
	(*env)->ReleaseStringUTFChars(env, fname, fname_str);
	if (!d) {
        return NULL;
    }
	ret = (*env)->NewStringUTF(env, d);
	free(d);
	return ret;
}

JNIEXPORT jstring JNICALL Java_org_ooni_probe_GoOONIProbeClient_apiCallWithArgs(JNIEnv *env, jclass c, jstring fname, jstring args)
{
    jstring ret;
	const char *fname_str = (*env)->GetStringUTFChars(env, fname, 0);
	size_t fname_len = (*env)->GetStringUTFLength(env, fname);

	const char *args_str = (*env)->GetStringUTFChars(env, args, 0);
	size_t args_len = (*env)->GetStringUTFLength(env, args);

	char *d = apiCallWithArgs((struct go_string){
		.str = fname_str,
		.n = fname_len
	}, (struct go_string){
		.str = args_str,
		.n = args_len
	});
	(*env)->ReleaseStringUTFChars(env, fname, fname_str);
	if (!d) {
        return NULL;
    }
	ret = (*env)->NewStringUTF(env, d);
	free(d);
	return ret;
}