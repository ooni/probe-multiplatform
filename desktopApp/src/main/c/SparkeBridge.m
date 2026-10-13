#import <Foundation/Foundation.h>
#import <Sparkle/Sparkle.h>
#include "SparkeBridge.h"

static SPUUpdater* sparkleUpdater = nil;
static SparkleLogCallback logCallback = NULL;
static SparkleShutdownCallback shutdownCallback = NULL;
static SparkleUpdateCallback updateCallback = NULL;
static SparkleNoUpdateCallback noUpdateCallback = NULL;
static SparkleErrorCallback errorCallback = NULL;

// Internal logging function that handles both NSLog and callback
static void sparkle_log(SparkleLogLevel level, const char* operation, const char* format, ...) {
    va_list args;
    va_start(args, format);

    // Create message string
    char message[1024];
    vsnprintf(message, sizeof(message), format, args);
    va_end(args);

    // Always log to NSLog with level prefix
    const char* levelStr = "";
    switch (level) {
        case SPARKLE_LOG_DEBUG: levelStr = "DEBUG"; break;
        case SPARKLE_LOG_INFO:  levelStr = "INFO";  break;
        case SPARKLE_LOG_WARN:  levelStr = "WARN";  break;
        case SPARKLE_LOG_ERROR: levelStr = "ERROR"; break;
    }

    NSLog(@"SparkleHelper [%s] %s: %s", levelStr, operation, message);

    // Call callback if set
    if (logCallback != NULL) {
        logCallback(level, operation, message);
    }
}

@interface OONIUpdaterDelegate : NSObject <SPUUpdaterDelegate>
@property (nonatomic, strong) NSString *feedURLString;
@end

@implementation OONIUpdaterDelegate

- (nullable NSString *)feedURLStringForUpdater:(SPUUpdater *)updater {
    sparkle_log(SPARKLE_LOG_DEBUG, "delegate", "Providing feed URL: %s", [self.feedURLString UTF8String]);
    return self.feedURLString;
}

- (void)updater:(SPUUpdater *)updater willInstallUpdate:(SUAppcastItem *)item {
    sparkle_log(SPARKLE_LOG_INFO, "update_lifecycle", "Will install update version %s", [item.displayVersionString UTF8String]);

    // Trigger application shutdown for update installation
    if (shutdownCallback != NULL) {
        sparkle_log(SPARKLE_LOG_INFO, "update_lifecycle", "Requesting application shutdown for update installation");
        shutdownCallback();
    }
}

- (void)updater:(SPUUpdater *)updater didFinishLoadingAppcast:(SUAppcast *)appcast {
    sparkle_log(SPARKLE_LOG_INFO, "update_lifecycle", "Finished loading appcast with %lu items", (unsigned long)appcast.items.count);
}

- (void)updater:(SPUUpdater *)updater didFindValidUpdate:(SUAppcastItem *)item {
    sparkle_log(SPARKLE_LOG_INFO, "update_lifecycle", "Found valid update: %s (current: %s)",
            [item.displayVersionString UTF8String],
            [[[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleShortVersionString"] UTF8String]);
    if (updateCallback != NULL) {
        updateCallback([item.displayVersionString UTF8String], "");
    }
}

- (void)updaterDidNotFindUpdate:(SPUUpdater *)updater {
    sparkle_log(SPARKLE_LOG_INFO, "update_lifecycle", "No update found - application is up to date");
    if (noUpdateCallback != NULL) {
        noUpdateCallback();
    }
}

- (void)updater:(SPUUpdater *)updater willInstallUpdateOnQuit:(SUAppcastItem *)item immediateInstallationInvoked:(BOOL)immediateInstallation {
    sparkle_log(SPARKLE_LOG_INFO, "update_lifecycle", "Will install update on quit: %s (immediate: %s)",
            [item.displayVersionString UTF8String], immediateInstallation ? "yes" : "no");

    // If immediate installation, trigger shutdown
    if (immediateInstallation && shutdownCallback != NULL) {
        sparkle_log(SPARKLE_LOG_INFO, "update_lifecycle", "Requesting immediate application shutdown for update installation");
        shutdownCallback();
    }
}

- (void)updater:(SPUUpdater *)updater didAbortWithError:(NSError *)error {
    // Sparkle also aborts the update cycle for outcomes that are not failures: no update
    // found (reported through updaterDidNotFindUpdate:) and the user cancelling the install.
    // Forwarding them would make every up-to-date check look like a feed failure.
    if ([error.domain isEqualToString:SUSparkleErrorDomain] &&
            (error.code == SUNoUpdateError || error.code == SUInstallationCanceledError)) {
        sparkle_log(SPARKLE_LOG_INFO, "update_lifecycle", "Update cycle ended without error (%ld): %s",
                (long)error.code, [error.localizedDescription UTF8String]);
        return;
    }

    sparkle_log(SPARKLE_LOG_ERROR, "update_lifecycle", "Update aborted with error (%ld): %s",
            (long)error.code, [error.localizedDescription UTF8String]);
    if (errorCallback != NULL) {
        errorCallback((int)error.code, [error.localizedDescription UTF8String]);
    }
}

@end

static OONIUpdaterDelegate* updaterDelegate = nil;

// Standard Sparkle UI, except that error alerts can be held back while the app fails over to
// another feed, so the user only sees an error once every feed has been tried.
@interface OONIUserDriver : SPUStandardUserDriver
@property (nonatomic) BOOL deferErrors;
@property (nonatomic, strong, nullable) NSError *deferredError;
- (void)showDeferredError;
@end

@implementation OONIUserDriver

- (void)showUpdaterError:(NSError *)error acknowledgement:(void (^)(void))acknowledgement {
    if (!self.deferErrors) {
        [super showUpdaterError:error acknowledgement:acknowledgement];
        return;
    }
    sparkle_log(SPARKLE_LOG_INFO, "user_driver", "Deferring error alert (%ld): %s",
            (long)error.code, [error.localizedDescription UTF8String]);
    self.deferredError = error;
    // Acknowledge right away so Sparkle ends this update session and a new check can start
    acknowledgement();
}

- (void)showDeferredError {
    NSError *error = self.deferredError;
    if (error == nil) {
        sparkle_log(SPARKLE_LOG_WARN, "user_driver", "No deferred error to show");
        return;
    }
    [super showUpdaterError:error acknowledgement:^{}];
    // Cleared only after use: without ARC, clearing the property releases the error
    self.deferredError = nil;
}

@end

static OONIUserDriver* userDriver = nil;

// Run a block synchronously on the main queue. Bridge entry points can be invoked from
// Sparkle delegate callbacks (already on the main thread), where a plain dispatch_sync
// to the main queue would assert and crash.
static void sparkle_dispatch_main_sync(dispatch_block_t block) {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_sync(dispatch_get_main_queue(), block);
    }
}

void sparkle_set_log_callback(SparkleLogCallback callback) {
    logCallback = callback;
    sparkle_log(SPARKLE_LOG_INFO, "callback", "Log callback %s", callback ? "enabled" : "disabled");
}

void sparkle_set_shutdown_callback(SparkleShutdownCallback callback) {
    shutdownCallback = callback;
    sparkle_log(SPARKLE_LOG_INFO, "callback", "Shutdown callback %s", callback ? "enabled" : "disabled");
}

void sparkle_set_update_callback(SparkleUpdateCallback callback) {
    updateCallback = callback;
}

void sparkle_set_no_update_callback(SparkleNoUpdateCallback callback) {
    noUpdateCallback = callback;
}

void sparkle_set_error_callback(SparkleErrorCallback callback) {
    errorCallback = callback;
    sparkle_log(SPARKLE_LOG_INFO, "callback", "Error callback %s", callback ? "enabled" : "disabled");
}

int sparkle_set_defer_errors(int enabled) {
    __block int result = 0;

    sparkle_dispatch_main_sync(^{
        if (userDriver == nil) {
            result = -1;
            return;
        }
        userDriver.deferErrors = enabled ? YES : NO;
        if (!enabled) {
            userDriver.deferredError = nil;
        }
    });

    return result;
}

int sparkle_show_deferred_error(void) {
    __block int result = 0;

    sparkle_dispatch_main_sync(^{
        @autoreleasepool {
            if (userDriver == nil) {
                result = -1;
                return;
            }
            [userDriver showDeferredError];
        }
    });

    return result;
}

int sparkle_set_feed_url(const char* url) {
    __block int result = 0;

    sparkle_dispatch_main_sync(^{
        @autoreleasepool {
            if (updaterDelegate == nil || url == NULL) {
                sparkle_log(SPARKLE_LOG_WARN, "set_feed_url", "Updater not initialized, cannot set feed URL");
                result = -1;
                return;
            }
            updaterDelegate.feedURLString = [NSString stringWithUTF8String:url];
            sparkle_log(SPARKLE_LOG_INFO, "set_feed_url", "Switched appcast feed URL to: %s", url);
            result = 0;
        }
    });

    return result;
}

int sparkle_init(const char* appcast_url, const char* public_key) {
    __block int result = 0;

    sparkle_log(SPARKLE_LOG_INFO, "init", "Starting Sparkle initialization");

    sparkle_dispatch_main_sync(^{
        @autoreleasepool {
            if (sparkleUpdater != nil) {
                sparkle_log(SPARKLE_LOG_WARN, "init", "Sparkle already initialized, skipping");
                result = 0; // Already initialized
                return;
            }

            NSString* urlString = [NSString stringWithUTF8String:appcast_url];
            NSURL* url = [NSURL URLWithString:urlString];

            if (url == nil) {
                sparkle_log(SPARKLE_LOG_ERROR, "init", "Invalid appcast URL: %s", appcast_url);
                result = -1;
                return;
            }

            sparkle_log(SPARKLE_LOG_INFO, "init", "Using appcast URL: %s", appcast_url);

            // Set public key in user defaults if provided and validate it
            if (public_key != NULL && strlen(public_key) > 0) {
                NSString* publicKeyString = [NSString stringWithUTF8String:public_key];

                sparkle_log(SPARKLE_LOG_INFO, "init", "Validating EdDSA public key (length: %lu chars)", strlen(public_key));

                // Validate base64 encoding
                NSData* keyData = [[NSData alloc] initWithBase64EncodedString:publicKeyString options:0];
                if (keyData == nil) {
                    sparkle_log(SPARKLE_LOG_ERROR, "init", "Invalid base64 encoding for public key");
                    result = -3;
                    return;
                }

                // Check if the decoded key has the correct length (32 bytes for EdDSA)
                if ([keyData length] != 32) {
                    sparkle_log(SPARKLE_LOG_ERROR, "init", "Invalid key length: %lu bytes (expected 32 bytes for EdDSA)", (unsigned long)[keyData length]);
                    result = -4;
                    return;
                }

                NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
                [defaults setObject:publicKeyString forKey:@"SUPublicEDKey"];
                [defaults synchronize];
                sparkle_log(SPARKLE_LOG_INFO, "init", "Set public key for signature verification");
            } else {
                // Clear any existing public key if none provided
                NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
                [defaults removeObjectForKey:@"SUPublicEDKey"];
                [defaults synchronize];
                sparkle_log(SPARKLE_LOG_WARN, "init", "No public key provided - signature verification disabled");
            }

            // Create delegate to provide feed URL
            updaterDelegate = [[OONIUpdaterDelegate alloc] init];
            updaterDelegate.feedURLString = urlString;

            sparkle_log(SPARKLE_LOG_DEBUG, "init", "Creating updater with delegate");

            // Create updater with our user driver and delegate
            @try {
                NSBundle *hostBundle = [NSBundle mainBundle];
                userDriver = [[OONIUserDriver alloc] initWithHostBundle:hostBundle delegate:nil];
                sparkleUpdater = [[SPUUpdater alloc] initWithHostBundle:hostBundle
                                               applicationBundle:hostBundle
                                                      userDriver:userDriver
                                                        delegate:updaterDelegate];

                if (sparkleUpdater == nil) {
                    sparkle_log(SPARKLE_LOG_ERROR, "init", "Failed to create updater");
                    result = -2;
                    return;
                }

                NSError *updaterError = nil;
                if (![sparkleUpdater startUpdater:&updaterError]) {
                    sparkle_log(SPARKLE_LOG_ERROR, "init", "Fatal updater error (%ld): %s",
                            updaterError.code, [updaterError.localizedDescription UTF8String]);
                    result = -999;
                    return;
                }

                sparkle_log(SPARKLE_LOG_INFO, "init", "Successfully initialized Sparkle updater");
                result = 0;
            } @catch (NSException *exception) {
                sparkle_log(SPARKLE_LOG_ERROR, "init", "Exception during initialization: %s", [exception.reason UTF8String]);
                result = -5;
                return;
            }
        }
    });

    return result;
}

int sparkle_check_for_updates(int show_ui) {
    __block int result = 0;

    sparkle_log(SPARKLE_LOG_INFO, "check_updates", "Starting update check (show_ui: %s)", show_ui ? "true" : "false");

    sparkle_dispatch_main_sync(^{
        @autoreleasepool {
            if (sparkleUpdater == nil) {
                sparkle_log(SPARKLE_LOG_ERROR, "check_updates", "Updater not initialized");
                result = -1;
                return;
            }

            @try {
                if (show_ui) {
                    sparkle_log(SPARKLE_LOG_DEBUG, "check_updates", "Checking for updates with UI");
                    [sparkleUpdater checkForUpdates];
                } else {
                    sparkle_log(SPARKLE_LOG_DEBUG, "check_updates", "Checking for updates in background");
                    [sparkleUpdater checkForUpdatesInBackground];
                }
                sparkle_log(SPARKLE_LOG_INFO, "check_updates", "Update check initiated successfully");
                result = 0;
            } @catch (NSException *exception) {
                sparkle_log(SPARKLE_LOG_ERROR, "check_updates", "Exception occurred: %s", [exception.reason UTF8String]);
                result = -2;
            }
        }
    });

    return result;
}

int sparkle_set_automatic_check_enabled(int enabled) {
    __block int result = 0;

    sparkle_log(SPARKLE_LOG_INFO, "set_automatic", "Setting automatic checks to: %s", enabled ? "enabled" : "disabled");

    sparkle_dispatch_main_sync(^{
        @autoreleasepool {
            if (sparkleUpdater == nil) {
                sparkle_log(SPARKLE_LOG_ERROR, "set_automatic", "Updater not initialized");
                result = -1;
                return;
            }

            @try {
                [sparkleUpdater setAutomaticallyChecksForUpdates:(enabled != 0)];
                sparkle_log(SPARKLE_LOG_INFO, "set_automatic", "Automatic checks successfully %s", enabled ? "enabled" : "disabled");
                result = 0;
            } @catch (NSException *exception) {
                sparkle_log(SPARKLE_LOG_ERROR, "set_automatic", "Exception occurred: %s", [exception.reason UTF8String]);
                result = -2;
            }
        }
    });

    return result;
}

int sparkle_set_update_check_interval(int hours) {
    __block int result = 0;

    sparkle_log(SPARKLE_LOG_INFO, "set_interval", "Setting update check interval to %d hours", hours);

    sparkle_dispatch_main_sync(^{
        @autoreleasepool {
            if (sparkleUpdater == nil) {
                sparkle_log(SPARKLE_LOG_ERROR, "set_interval", "Updater not initialized");
                result = -1;
                return;
            }

            @try {
                NSTimeInterval interval = hours * 3600; // Convert hours to seconds
                [sparkleUpdater setUpdateCheckInterval:interval];
                sparkle_log(SPARKLE_LOG_INFO, "set_interval", "Update check interval set to %d hours (%d seconds)", hours, (int)interval);
                result = 0;
            } @catch (NSException *exception) {
                sparkle_log(SPARKLE_LOG_ERROR, "set_interval", "Exception occurred: %s", [exception.reason UTF8String]);
                result = -2;
            }
        }
    });

    return result;
}

int sparkle_cleanup(void) {
    sparkle_log(SPARKLE_LOG_INFO, "cleanup", "Starting Sparkle cleanup");

    @autoreleasepool {
        if (sparkleUpdater != nil) {
            sparkle_log(SPARKLE_LOG_DEBUG, "cleanup", "Releasing updater");
            sparkleUpdater = nil;
        }
        userDriver = nil;
        if (updaterDelegate != nil) {
            sparkle_log(SPARKLE_LOG_DEBUG, "cleanup", "Releasing updater delegate");
            updaterDelegate = nil;
        }
        sparkle_log(SPARKLE_LOG_INFO, "cleanup", "Sparkle cleanup completed");
        return 0;
    }
}
