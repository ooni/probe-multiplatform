#include <jni.h>
#import <Cocoa/Cocoa.h>

// JNI function to show the app in dock (NSApp.setActivationPolicy(.regular))
JNIEXPORT void JNICALL Java_org_ooni_probe_shared_MacDockVisibility_showInDockNative
  (JNIEnv *env, jclass cls) {
    @autoreleasepool {
        dispatch_async(dispatch_get_main_queue(), ^{
            [NSApp setActivationPolicy:NSApplicationActivationPolicyRegular];
            NSLog(@"MacDockVisibility: Set activation policy to Regular (show in dock)");
        });
    }
}

// JNI function to remove the app from dock (NSApp.setActivationPolicy(.accessory))
JNIEXPORT void JNICALL Java_org_ooni_probe_shared_MacDockVisibility_removeFromDockNative
  (JNIEnv *env, jclass cls) {
    @autoreleasepool {
        dispatch_async(dispatch_get_main_queue(), ^{
            [NSApp setActivationPolicy:NSApplicationActivationPolicyAccessory];
            NSLog(@"MacDockVisibility: Set activation policy to Accessory (remove from dock)");
        });
    }
}