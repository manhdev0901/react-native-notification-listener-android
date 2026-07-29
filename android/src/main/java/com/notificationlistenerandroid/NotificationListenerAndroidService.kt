package com.notificationlistenerandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads posted status-bar notifications and forwards each one to JS via a
 * headless task. Deliberately does NOT rely on any BroadcastReceiver
 * starting this service on boot — Android automatically rebinds
 * NotificationListenerServices after reboot as long as permission is still
 * granted (see BootReceiver for the one safe thing a boot hook does here).
 */
class NotificationListenerAndroidService : NotificationListenerService() {

  override fun onListenerConnected() {
    super.onListenerConnected()
    appendConnectionLog(applicationContext, "connected")
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    appendConnectionLog(applicationContext, "disconnected")

    // Ask the OS to rebind immediately on disconnect — recovers from a
    // service crash/OEM kill without waiting for the app to come to the
    // foreground and call requestRebind() itself. No-op if permission was
    // actually revoked.
    try {
      val componentName = ComponentName(applicationContext, NotificationListenerAndroidService::class.java)
      requestRebind(componentName)
    } catch (e: Exception) {
      Log.d(TAG, "requestRebind on disconnect failed: ${e.message}")
    }
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    val notification = sbn.notification
    if (notification == null || notification.extras == null) {
      Log.d(TAG, "The notification received has no data")
      return
    }

    val context = applicationContext
    val payload = NotificationPayloadBuilder.build(context, sbn)

    val serviceIntent = Intent(context, NotificationListenerAndroidHeadlessTaskService::class.java)
    serviceIntent.putExtra(EXTRA_NOTIFICATION, payload.toString())

    try {
      HeadlessJsTaskService.acquireWakeLockNow(context)
      context.startService(serviceIntent)
    } catch (e: Exception) {
      // Never let a bad payload (e.g. android.os.TransactionTooLargeException
      // from an oversized icon/image) crash this service — a crash here
      // unbinds the whole NotificationListenerService, which is worse than
      // dropping a single event.
      Log.d(TAG, "Failed to start headless task for notification: ${e.message}")
    }
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification) {}

  companion object {
    private const val TAG = "NotificationListenerAndroid"
    const val EXTRA_NOTIFICATION = "notification"
    const val CONNECTION_LOG_PREFS = "notification_listener_android_logs"
    const val CONNECTION_LOG_KEY = "logs"
    private const val MAX_CONNECTION_LOGS = 50

    fun appendConnectionLog(context: Context, type: String) {
      try {
        val prefs: SharedPreferences =
          context.getSharedPreferences(CONNECTION_LOG_PREFS, Context.MODE_PRIVATE)
        val logs = JSONArray(prefs.getString(CONNECTION_LOG_KEY, "[]"))

        val entry = JSONObject()
        entry.put("type", type)
        entry.put("time", System.currentTimeMillis())
        logs.put(entry)

        while (logs.length() > MAX_CONNECTION_LOGS) {
          logs.remove(0)
        }

        prefs.edit().putString(CONNECTION_LOG_KEY, logs.toString()).apply()
      } catch (e: Exception) {
        Log.d(TAG, "appendConnectionLog error: ${e.message}")
      }
    }
  }
}
