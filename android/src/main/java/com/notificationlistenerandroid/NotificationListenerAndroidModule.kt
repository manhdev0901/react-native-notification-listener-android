package com.notificationlistenerandroid

import com.facebook.react.bridge.ReactApplicationContext

class NotificationListenerAndroidModule(reactContext: ReactApplicationContext) :
  NativeNotificationListenerAndroidSpec(reactContext) {

  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = NativeNotificationListenerAndroidSpec.NAME
  }
}
