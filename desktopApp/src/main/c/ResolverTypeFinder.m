#import <jni.h>
#if defined(__APPLE__)
#import <Foundation/Foundation.h>
#import <Network/Network.h>

// Resolves a hostname through the system resolver (same as getaddrinfo, honouring DoH/DoT
// profiles, NEDNSSettingsManager apps and DDR) and reports observed DNS transports.

static const int64_t REPORT_TIMEOUT_MILLIS = 1000;

// Kotlin `(String) -> Unit` callback context, seen from JNI as `Function1`.
typedef struct {
    JavaVM *jvm;
    jobject logFn;    // global ref
    jmethodID invoke; // Function1.invoke(Object):Object
} ResolverLogContext;

// Network.framework runs on a private queue, so attach the thread before JNI callbacks
// (mirrors UpdateBridge.c).
static void resolver_log(ResolverLogContext *ctx, const char *message) {
    if (ctx == NULL || ctx->jvm == NULL || ctx->logFn == NULL || ctx->invoke == NULL || message == NULL) {
        return;
    }

    JNIEnv *env = NULL;
    int detach = 0;
    jint envResult = (*ctx->jvm)->GetEnv(ctx->jvm, (void **)&env, JNI_VERSION_1_6);
    if (envResult == JNI_EDETACHED) {
        if ((*ctx->jvm)->AttachCurrentThread(ctx->jvm, (void **)&env, NULL) != JNI_OK) {
            return;
        }
        detach = 1;
    } else if (envResult != JNI_OK) {
        return;
    }

    jstring jMessage = (*env)->NewStringUTF(env, message);
    if (jMessage != NULL) {
        jobject ignored = (*env)->CallObjectMethod(env, ctx->logFn, ctx->invoke, jMessage);
        if (ignored != NULL) {
            (*env)->DeleteLocalRef(env, ignored);
        }
        // Never propagate a callback exception back across the boundary
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
        (*env)->DeleteLocalRef(env, jMessage);
    }

    if (detach) {
        (*ctx->jvm)->DetachCurrentThread(ctx->jvm);
    }
}

static NSString *protocolName(nw_report_resolution_protocol_t protocol) {
    switch (protocol) {
        case nw_report_resolution_protocol_udp: return @"udp";
        case nw_report_resolution_protocol_tcp: return @"tcp";
        case nw_report_resolution_protocol_tls: return @"tls";
        case nw_report_resolution_protocol_https: return @"https";
        default: return @"unknown";
    }
}

// Returns observed protocol/source names (retained), or nil on failure/timeout. Cached
// answers are returned as "cache".
static NSArray<NSString *> *probeDnsProtocols(const char *host, int64_t timeoutMillis, ResolverLogContext *logCtx) {
    dispatch_queue_t queue = dispatch_queue_create("org.ooni.probe.dns-protocol-probe", DISPATCH_QUEUE_SERIAL);
    dispatch_semaphore_t done = dispatch_semaphore_create(0);

    nw_endpoint_t endpoint = nw_endpoint_create_host(host, "443");
    nw_parameters_t parameters = nw_parameters_create_secure_tcp(
        NW_PARAMETERS_DISABLE_PROTOCOL,
        NW_PARAMETERS_DEFAULT_CONFIGURATION
    );
    // Break retain cycle under MRC.
    __block nw_connection_t connection = nw_connection_create(endpoint, parameters);
    nw_release(endpoint);
    nw_release(parameters);

    // Only accessed on `queue`
    __block BOOL finished = NO;
    __block BOOL isReady = NO;
    __block NSArray<NSString *> *result = nil;

    void (^finish)(NSArray<NSString *> *) = ^(NSArray<NSString *> *value) {
        if (finished) return;
        finished = YES;
        result = [value retain];
        nw_connection_cancel(connection);
        dispatch_semaphore_signal(done);
    };

    nw_connection_set_queue(connection, queue);
    nw_connection_set_state_changed_handler(connection, ^(nw_connection_state_t state, nw_error_t error) {
        switch (state) {
            case nw_connection_state_ready:
                resolver_log(logCtx, "[DesktopResolverTypeFinder] Connection ready; requesting establishment report");
                isReady = YES;
                nw_connection_access_establishment_report(connection, queue, ^(nw_establishment_report_t report) {
                    if (report == NULL) {
                        finish(nil);
                        return;
                    }
                    NSMutableArray<NSString *> *names = [[NSMutableArray alloc] init];
                    nw_establishment_report_enumerate_resolution_reports(report, ^bool(nw_resolution_report_t resolution) {
                        nw_report_resolution_source_t source = nw_resolution_report_get_source(resolution);
                        if (source == nw_report_resolution_source_cache || source == nw_report_resolution_source_expired_cache) {
                            // Cached answers report no DNS transport
                            [names addObject:@"cache"];
                        } else {
                            [names addObject:protocolName(nw_resolution_report_get_protocol(resolution))];
                        }
                        return true;
                    });
                    char msg[192];
                    snprintf(msg, sizeof(msg), "[DesktopResolverTypeFinder] Establishment report resolutions: %lu", (unsigned long)[names count]);
                    resolver_log(logCtx, msg);
                    NSString *joined = [names componentsJoinedByString:@","];
                    resolver_log(logCtx, [[@"[DesktopResolverTypeFinder] Observed DNS protocols: " stringByAppendingString:joined] UTF8String]);
                    finish([names count] == 0 ? nil : names);
                    [names release];
                });
                break;
            case nw_connection_state_waiting:
                // A DNS error means the hostname will not resolve and there is no report.
                // Other waiting reasons are transient, so keep waiting for a retry.
                if (error != NULL && nw_error_get_error_domain(error) == nw_error_domain_dns) {
                    resolver_log(logCtx, "[DesktopResolverTypeFinder] DNS resolution failed while waiting for connection");
                    finish(nil);
                }
                break;
            case nw_connection_state_failed: {
                char msg[192];
                snprintf(msg, sizeof(msg), "[DesktopResolverTypeFinder] Connection failed: error code %d", nw_error_get_error_code(error));
                resolver_log(logCtx, msg);
                finish(nil);
                break;
            }
            case nw_connection_state_cancelled:
                // Expected after `finish()`; only log unexpected early cancellation.
                if (!finished) {
                    resolver_log(logCtx, "[DesktopResolverTypeFinder] Connection cancelled before completion");
                }
                finish(nil);
                break;
            default:
                break;
        }
    });
    nw_connection_start(connection);

    if (dispatch_semaphore_wait(done, dispatch_time(DISPATCH_TIME_NOW, timeoutMillis * NSEC_PER_MSEC)) != 0) {
        __block BOOL reportPending = NO;
        dispatch_sync(queue, ^{ reportPending = isReady && !finished; });
        if (reportPending) {
            // Connection is ready; allow extra time for the establishment report.
            char msg[192];
            snprintf(msg, sizeof(msg), "[DesktopResolverTypeFinder] Initial probe timeout elapsed, waiting %lld ms for establishment report", (long long)REPORT_TIMEOUT_MILLIS);
            resolver_log(logCtx, msg);
            dispatch_semaphore_wait(done, dispatch_time(DISPATCH_TIME_NOW, REPORT_TIMEOUT_MILLIS * NSEC_PER_MSEC));
        }
    }
    // Resolve race between timeout and late callback on the queue.
    dispatch_sync(queue, ^{ finish(nil); });

    nw_release(connection);
    dispatch_release(done);
    dispatch_release(queue);
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_org_ooni_engine_DesktopResolverTypeFinder_nativeProbeDnsProtocols(
    JNIEnv *env, jobject obj, jstring host, jlong timeoutMillis, jobject logFn
) {
    @autoreleasepool {
        const char *hostChars = (*env)->GetStringUTFChars(env, host, NULL);
        if (hostChars == NULL) return NULL;

        // Configure logging callback for the synchronous probe.
        ResolverLogContext logCtx = { NULL, NULL, NULL };
        if ((*env)->GetJavaVM(env, &logCtx.jvm) == JNI_OK && logFn != NULL) {
            logCtx.logFn = (*env)->NewGlobalRef(env, logFn);
            jclass fnClass = (*env)->FindClass(env, "kotlin/jvm/functions/Function1");
            if (fnClass != NULL) {
                logCtx.invoke = (*env)->GetMethodID(env, fnClass, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");
                (*env)->DeleteLocalRef(env, fnClass);
            }
            if ((*env)->ExceptionCheck(env)) {
                (*env)->ExceptionClear(env);
                logCtx.invoke = NULL;
            }
        }

        NSArray<NSString *> *protocols = probeDnsProtocols(hostChars, (int64_t) timeoutMillis, &logCtx);

        (*env)->ReleaseStringUTFChars(env, host, hostChars);
        if (logCtx.logFn != NULL) {
            (*env)->DeleteGlobalRef(env, logCtx.logFn);
        }
        if (protocols == nil) return NULL;

        jclass stringClass = (*env)->FindClass(env, "java/lang/String");
        jobjectArray values = (*env)->NewObjectArray(env, (jsize)[protocols count], stringClass, NULL);
        for (NSUInteger i = 0; i < [protocols count]; i++) {
            jstring value = (*env)->NewStringUTF(env, [protocols[i] UTF8String]);
            (*env)->SetObjectArrayElement(env, values, (jsize)i, value);
            (*env)->DeleteLocalRef(env, value);
        }
        [protocols release];
        return values;
    }
}
#endif
