#import <Foundation/Foundation.h>
#import <Sparkle/Sparkle.h>
#include "SparkeBridge.h"

static SPUStandardUpdaterController* updaterController = nil;

@interface OONIUpdaterDelegate : NSObject <SPUUpdaterDelegate>
@property (nonatomic, strong) NSString *feedURLString;
@end

@implementation OONIUpdaterDelegate

- (nullable NSString *)feedURLStringForUpdater:(SPUUpdater *)updater {
    return self.feedURLString;
}

@end

static OONIUpdaterDelegate* updaterDelegate = nil;

int sparkle_init(const char* appcast_url, const char* public_key) {
    __block int result = 0;

    dispatch_sync(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (updaterController != nil) {
                result = 0; // Already initialized
                return;
            }

            NSString* urlString = [NSString stringWithUTF8String:appcast_url];
            NSURL* url = [NSURL URLWithString:urlString];

            if (url == nil) {
                NSLog(@"SparkleHelper: Invalid appcast URL: %s", appcast_url);
                result = -1;
                return;
            }

            // Set public key in user defaults if provided and validate it
            if (public_key != NULL && strlen(public_key) > 0) {
                NSString* publicKeyString = [NSString stringWithUTF8String:public_key];

                // Validate base64 encoding
                NSData* keyData = [[NSData alloc] initWithBase64EncodedString:publicKeyString options:0];
                if (keyData == nil) {
                    NSLog(@"SparkleHelper: Invalid base64 encoding for public key: %s", public_key);
                    result = -3;
                    return;
                }

                // Check if the decoded key has the correct length (32 bytes for EdDSA)
                if ([keyData length] != 32) {
                    NSLog(@"SparkleHelper: Invalid key length: %lu bytes (expected 32 bytes for EdDSA)", (unsigned long)[keyData length]);
                    result = -4;
                    return;
                }

                NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
                [defaults setObject:publicKeyString forKey:@"SUPublicEDKey"];
                [defaults synchronize];
                NSLog(@"SparkleHelper: Set public key for signature verification");
            } else {
                // Clear any existing public key if none provided
                NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
                [defaults removeObjectForKey:@"SUPublicEDKey"];
                [defaults synchronize];
                NSLog(@"SparkleHelper: No public key provided - signature verification disabled");
            }

            // Create delegate to provide feed URL
            updaterDelegate = [[OONIUpdaterDelegate alloc] init];
            updaterDelegate.feedURLString = urlString;

            // Create updater controller with delegate
            @try {
                updaterController = [[SPUStandardUpdaterController alloc] initWithStartingUpdater:NO
                                                                                    updaterDelegate:updaterDelegate
                                                                                     userDriverDelegate:nil];

                if (updaterController == nil) {
                    NSLog(@"SparkleHelper: Failed to create updater controller");
                    result = -2;
                    return;
                }

                NSError *updaterError = nil;
                if (![updaterController.updater startUpdater:&updaterError]) {
                    NSLog(@"Fatal updater error (%ld): %@", updaterError.code, updaterError.localizedDescription);
                }

                NSLog(@"SparkleHelper: Initialized with appcast URL: %@", urlString);
                result = 0;
            } @catch (NSException *exception) {
                NSLog(@"SparkleHelper: Exception during initialization: %@", exception.reason);
                result = -5;
                return;
            }
        }
    });

    return result;
}
int sparkle_check_for_updates(int show_ui) {
    __block int result = 0;

    dispatch_sync(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (updaterController == nil) {
                NSLog(@"SparkleHelper: Updater not initialized");
                result = -1;
                return;
            }

            @try {
                if (show_ui) {
                    [updaterController checkForUpdates:nil];
                } else {
                    [updaterController.updater checkForUpdatesInBackground];
                }
                result = 0;
            } @catch (NSException *exception) {
                NSLog(@"SparkleHelper: Error checking for updates: %@", exception.reason);
                result = -2;
            }
        }
    });

    return result;
}

int sparkle_set_automatic_check_enabled(int enabled) {
    __block int result = 0;

    dispatch_sync(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (updaterController == nil) {
                NSLog(@"SparkleHelper: Updater not initialized");
                result = -1;
                return;
            }

            @try {
                [updaterController.updater setAutomaticallyChecksForUpdates:(enabled != 0)];
                result = 0;
            } @catch (NSException *exception) {
                NSLog(@"SparkleHelper: Error setting automatic check enabled: %@", exception.reason);
                result = -2;
            }
        }
    });

    return result;
}

int sparkle_set_update_check_interval(int hours) {
    __block int result = 0;

    dispatch_sync(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (updaterController == nil) {
                NSLog(@"SparkleHelper: Updater not initialized");
                result = -1;
                return;
            }

            @try {
                NSTimeInterval interval = hours * 3600; // Convert hours to seconds
                [updaterController.updater setUpdateCheckInterval:interval];
                result = 0;
            } @catch (NSException *exception) {
                NSLog(@"SparkleHelper: Error setting update check interval: %@", exception.reason);
                result = -2;
            }
        }
    });

    return result;
}

int sparkle_cleanup(void) {
    @autoreleasepool {
        if (updaterController != nil) {
            NSLog(@"SparkleHelper: Cleaning up");
            updaterController = nil;
        }
        if (updaterDelegate != nil) {
            updaterDelegate = nil;
        }
        return 0;
    }
}
