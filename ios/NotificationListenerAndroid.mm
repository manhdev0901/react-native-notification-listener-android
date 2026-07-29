#import "NotificationListenerAndroid.h"

// NotificationListenerService has no iOS equivalent — this module only does
// anything on Android. Stubs below fail safe (no-op / resolve empty) instead
// of crashing, so an app that's built for both platforms doesn't need to
// platform-branch every call site.
@implementation NotificationListenerAndroid

- (void)requestPermission {
}

- (void)getPermissionStatus:(RCTPromiseResolveBlock)resolve
                      reject:(RCTPromiseRejectBlock)reject {
  resolve(@"denied");
}

- (void)requestRebind {
}

- (void)getConnectionLogs:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject {
  resolve(@[]);
}

- (void)clearConnectionLogs {
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeNotificationListenerAndroidSpecJSI>(params);
}

+ (NSString *)moduleName
{
  return @"NotificationListenerAndroid";
}

@end
