#import <jni.h>

#if defined(__APPLE__)
#import <Foundation/Foundation.h>
#import <SystemConfiguration/SystemConfiguration.h>
#import <NetworkExtension/NetworkExtension.h>
#import <Network/Network.h>

@interface NetworkTypeFinder : NSObject
- (NSString *)getNetworkType;
- (BOOL)isVpnActive;
@end

@implementation NetworkTypeFinder {
    nw_path_monitor_t _pathMonitor;
    dispatch_queue_t _queue;
    nw_path_t _currentPath;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _queue = dispatch_get_global_queue(QOS_CLASS_UTILITY, 0);
        _pathMonitor = nw_path_monitor_create();
        nw_path_monitor_set_queue(_pathMonitor, _queue);

        nw_path_monitor_set_update_handler(_pathMonitor, ^(nw_path_t path) {
            if (self->_currentPath != NULL) {
                nw_release(self->_currentPath);
            }
            self->_currentPath = nw_retain(path);
        });

        nw_path_monitor_start(_pathMonitor);
    }
    return self;
}

- (void)dealloc {
    if (_currentPath) {
        nw_release(_currentPath);
    }
    if (_pathMonitor) {
        nw_path_monitor_cancel(_pathMonitor);
        nw_release(_pathMonitor);
    }
    [super dealloc];
}

- (NSString *)getNetworkType {
    if ([self isVpnActive]) {
        return @"vpn";
    }

    NSString *result = @"unknown";

    nw_path_t currentPath = _currentPath;
    if (currentPath && nw_path_get_status(currentPath) == nw_path_status_satisfied) {
        if (nw_path_uses_interface_type(currentPath, nw_interface_type_wifi)) {
            result = @"wifi";
        } else if (nw_path_uses_interface_type(currentPath, nw_interface_type_cellular)) {
            result = @"mobile";
        } else if (nw_path_uses_interface_type(currentPath, nw_interface_type_wired)) {
            result = @"wired_ethernet";
        } else {
            result = @"";
        }
    }

    return result;
}

- (BOOL)isVpnActive {
    NSDictionary *proxySettings = CFBridgingRelease(CFNetworkCopySystemProxySettings());
    NSDictionary *scoped = proxySettings[@"__SCOPED__"];
    NSArray *keys = scoped.allKeys;

    NSArray *vpnProtocols = @[@"tap", @"tun", @"ppp", @"ipsec", @"utun"];
    for (NSString *key in keys) {
        for (NSString *protocol in vpnProtocols) {
            if ([key hasPrefix:protocol]) {
                return YES;
            }
        }
    }

    return NO;
}
@end
#endif

#if defined(__APPLE__)
const char* getNetworkTypeImpl() {
    @autoreleasepool {
        NetworkTypeFinder *finder = [[NetworkTypeFinder alloc] init];
        NSString *networkType = [finder getNetworkType];
        // Return pointer valid for JNI lifetime, no need to free
        return [networkType UTF8String];
    }
}
#elif defined(_WIN32)
const char* getNetworkTypeImpl() {
    // TODO: Implement actual Windows network type detection
    return "unknown";
}
#elif defined(__linux__)
const char* getNetworkTypeImpl() {
    // TODO: Implement actual Linux network type detection
    return "unknown";
}
#else
const char* getNetworkTypeImpl() {
    return "unknown";
}
#endif

const char* getNetworkType() {
    return getNetworkTypeImpl();
}

JNIEXPORT jstring JNICALL Java_org_ooni_engine_DesktopNetworkTypeFinder_getNetworkType(JNIEnv *env, jobject obj)
{
    const char* networkType = getNetworkType();
    return (*env)->NewStringUTF(env, networkType);
}
