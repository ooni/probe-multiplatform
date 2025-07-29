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
            
            // Set public key in user defaults if provided
            if (public_key != NULL && strlen(public_key) > 0) {
                NSString* publicKeyString = [NSString stringWithUTF8String:public_key];
                NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
                [defaults setObject:publicKeyString forKey:@"SUPublicEDKey"];
                [defaults synchronize];
                NSLog(@"SparkleHelper: Set public key for signature verification");
            }
            
            // Create delegate to provide feed URL
            updaterDelegate = [[OONIUpdaterDelegate alloc] init];
            updaterDelegate.feedURLString = urlString;
            
            // Create updater controller with delegate
            updaterController = [[SPUStandardUpdaterController alloc] initWithStartingUpdater:YES
                                                                               updaterDelegate:updaterDelegate
                                                                                userDriverDelegate:nil];
            
            if (updaterController == nil) {
                NSLog(@"SparkleHelper: Failed to create updater controller");
                result = -2;
                return;
            }
            
            NSLog(@"SparkleHelper: Initialized with appcast URL: %@", urlString);
            result = 0;
        }
    });
    
    return result;
}

void sparkle_check_for_updates(int show_ui) {
    dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (updaterController == nil) {
                NSLog(@"SparkleHelper: Updater not initialized");
                return;
            }
            
            if (show_ui) {
                [updaterController checkForUpdates:nil];
            } else {
                [updaterController.updater checkForUpdatesInBackground];
            }
        }
    });
}

void sparkle_set_automatic_check_enabled(int enabled) {
    dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (updaterController == nil) {
                NSLog(@"SparkleHelper: Updater not initialized");
                return;
            }
            
            [updaterController.updater setAutomaticallyChecksForUpdates:(enabled != 0)];
        }
    });
}

void sparkle_set_update_check_interval(int hours) {
    dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
            if (updaterController == nil) {
                NSLog(@"SparkleHelper: Updater not initialized");
                return;
            }
            
            NSTimeInterval interval = hours * 3600; // Convert hours to seconds
            [updaterController.updater setUpdateCheckInterval:interval];
        }
    });
}

void sparkle_cleanup(void) {
    @autoreleasepool {
        if (updaterController != nil) {
            NSLog(@"SparkleHelper: Cleaning up");
            updaterController = nil;
        }
        if (updaterDelegate != nil) {
            updaterDelegate = nil;
        }
    }
}