package com.notificationlistenerandroid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import org.json.JSONArray

class NotificationListenerAndroidModule(reactContext: ReactApplicationContext) :
  NativeNotificationListenerAndroidSpec(reactContext) {

  override fun requestPermission() {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    reactApplicationContext.startActivity(intent)
  }

  override fun getPermissionStatus(promise: Promise) {
    val packageName = reactApplicationContext.packageName
    val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(reactApplicationContext)
    promise.resolve(if (enabledPackages.contains(packageName)) "authorized" else "denied")
  }

  // Requests the OS rebind the listener without sending the user to
  // Settings. Only has an effect while permission is still granted but the
  // binding was dropped (service crashed, or killed by the OS/OEM battery
  // manager) — a no-op if the user actually revoked permission.
  override fun requestRebind() {
    try {
      val componentName =
        ComponentName(reactApplicationContext, NotificationListenerAndroidService::class.java)
      NotificationListenerService.requestRebind(componentName)
    } catch (e: Exception) {
      Log.d(TAG, "requestRebind error: ${e.message}")
    }
  }

  override fun getConnectionLogs(promise: Promise) {
    try {
      val prefs = reactApplicationContext.getSharedPreferences(
        NotificationListenerAndroidService.CONNECTION_LOG_PREFS,
        Context.MODE_PRIVATE
      )
      val logs = JSONArray(
        prefs.getString(NotificationListenerAndroidService.CONNECTION_LOG_KEY, "[]")
      )

      val result: WritableArray = Arguments.createArray()
      for (i in 0 until logs.length()) {
        val entry = logs.getJSONObject(i)
        val map: WritableMap = Arguments.createMap()
        map.putString("type", entry.getString("type"))
        map.putDouble("time", entry.getLong("time").toDouble())
        result.pushMap(map)
      }
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("ERROR", e.message)
    }
  }

  override fun clearConnectionLogs() {
    val prefs = reactApplicationContext.getSharedPreferences(
      NotificationListenerAndroidService.CONNECTION_LOG_PREFS,
      Context.MODE_PRIVATE
    )
    prefs.edit().clear().apply()
  }

  companion object {
    const val NAME = NativeNotificationListenerAndroidSpec.NAME
    private const val TAG = "NotificationListenerAndroid"
  }
}
