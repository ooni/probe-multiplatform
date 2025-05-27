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

#include <string.h>
#include <unistd.h>
#include <stdio.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <net/if.h>
#include <arpa/inet.h>
#include <stdlib.h>

// Helper to check if a given interface is a VPN
static int is_vpn_iface(const char *iface) {
    return strncmp(iface, "tun", 3) == 0 || strncmp(iface, "tap", 3) == 0 ||
           strncmp(iface, "ppp", 3) == 0 || strncmp(iface, "wg", 2) == 0 ||
           strncmp(iface, "ipsec", 5) == 0 || strncmp(iface, "utun", 4) == 0;
}

// Helper to check if interface is up and has an IP address
static int iface_is_up_and_has_ip(const char *iface) {
    char path[256];
    snprintf(path, sizeof(path), "/sys/class/net/%s/operstate", iface);
    FILE *fp = fopen(path, "r");
    if (!fp) return 0;
    char state[32] = "";
    fgets(state, sizeof(state), fp);
    fclose(fp);
    if (strncmp(state, "up", 2) != 0) return 0;
    // Check if has IP address
    char cmd[256];
    snprintf(cmd, sizeof(cmd), "ip addr show %s | grep 'inet ' > /dev/null", iface);
    int ret = system(cmd);
    return ret == 0;
}

const char* getNetworkTypeImpl() {
    // 1. Check for VPN by looking for default route via VPN interface
    FILE *route_fp = fopen("/proc/net/route", "r");
    char line[512];
    char vpn_iface[64] = "";
    if (route_fp) {
        // Skip header
        fgets(line, sizeof(line), route_fp);
        while (fgets(line, sizeof(line), route_fp)) {
            char iface[64];
            unsigned long dest, gw;
            unsigned int flags, refcnt, use, metric, mask, mtu, win, irtt;
            int n = sscanf(line, "%63s %lx %lx %X %u %u %u %x %u %u %u", iface, &dest, &gw, &flags, &refcnt, &use, &metric, &mask, &mtu, &win, &irtt);
            if (n >= 11 && dest == 0) { // default route
                if (is_vpn_iface(iface) && iface_is_up_and_has_ip(iface)) {
                    strcpy(vpn_iface, iface);
                    break;
                }
            }
        }
        fclose(route_fp);
    }
    if (vpn_iface[0]) return "vpn";

    // 2. Check for active WireGuard tunnels
    struct stat st;
    if (stat("/proc/net/wireguard", &st) == 0) {
        FILE *wg_fp = fopen("/proc/net/wireguard", "r");
        if (wg_fp) {
            char buf[256];
            while (fgets(buf, sizeof(buf), wg_fp)) {
                char *iface = strtok(buf, " ");
                if (iface && iface_is_up_and_has_ip(iface)) {
                    fclose(wg_fp);
                    return "vpn";
                }
            }
            fclose(wg_fp);
        }
    }

    // 3. Check for active PPP connections
    if (stat("/proc/net/ppp", &st) == 0) {
        FILE *ppp_fp = fopen("/proc/net/ppp", "r");
        if (ppp_fp) {
            char buf[256];
            // skip header
            fgets(buf, sizeof(buf), ppp_fp);
            while (fgets(buf, sizeof(buf), ppp_fp)) {
                char *iface = strtok(buf, ":");
                if (iface && iface_is_up_and_has_ip(iface)) {
                    fclose(ppp_fp);
                    return "vpn";
                }
            }
            fclose(ppp_fp);
        }
    }

    // 4. Fallback: check for VPN interfaces present and up
    DIR *d = opendir("/sys/class/net/");
    if (!d) return "unknown";
    struct dirent *dir;
    int found_vpn = 0, found_wifi = 0, found_wired = 0, found_mobile = 0;
    while ((dir = readdir(d)) != NULL) {
        if (dir->d_name[0] == '.') continue;
        if (is_vpn_iface(dir->d_name) && iface_is_up_and_has_ip(dir->d_name)) {
            found_vpn = 1;
        }
        // Mobile: wwan, ppp
        if ((strncmp(dir->d_name, "wwan", 4) == 0 || strncmp(dir->d_name, "ppp", 3) == 0) && iface_is_up_and_has_ip(dir->d_name)) {
            found_mobile = 1;
        }
        // Wi-Fi: has /sys/class/net/<iface>/wireless
        char wireless_path[256];
        snprintf(wireless_path, sizeof(wireless_path), "/sys/class/net/%s/wireless", dir->d_name);
        if (access(wireless_path, F_OK) == 0 && iface_is_up_and_has_ip(dir->d_name)) {
            found_wifi = 1;
        }
        // Wired: eth*, en*
        if ((strncmp(dir->d_name, "eth", 3) == 0 || strncmp(dir->d_name, "en", 2) == 0) && iface_is_up_and_has_ip(dir->d_name)) {
            found_wired = 1;
        }
    }
    closedir(d);
    if (found_vpn) return "vpn";
    if (found_wifi) return "wifi";
    if (found_mobile) return "mobile";
    if (found_wired) return "wired_ethernet";
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
