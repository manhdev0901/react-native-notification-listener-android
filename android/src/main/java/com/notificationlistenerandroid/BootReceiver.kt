package com.notificationlistenerandroid

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.util.Log

/**
 * On boot, does NOT start NotificationListenerAndroidService directly.
 * Android automatically re-binds a NotificationListenerService after reboot
 * as long as notification-listener permission is still granted — calling
 * startForegroundService()/startService() here for a service that never
 * calls startForeground() is exactly what crashes apps on Android 12+ with
 * ForegroundServiceDidNotStartInTimeException (the bug that motivated this
 * library). This receiver only nudges an earlier rebind via the plain
 * NotificationListenerService.requestRebind() API, which is a no-op if
 * permission isn't actually granted.
 */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
    try {
      val componentName = ComponentName(context, NotificationListenerAndroidService::class.java)
      NotificationListenerService.requestRebind(componentName)
    } catch (e: Exception) {
      Log.d(TAG, "requestRebind on boot failed: ${e.message}")
    }
  }

  companion object {
    private const val TAG = "NotificationListenerAndroid"
  }
}
