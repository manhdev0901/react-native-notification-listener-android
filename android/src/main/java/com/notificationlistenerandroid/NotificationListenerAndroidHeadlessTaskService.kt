package com.notificationlistenerandroid

import android.content.Intent
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

/** Delivers one posted notification's payload to the JS headless task. */
class NotificationListenerAndroidHeadlessTaskService : HeadlessJsTaskService() {
  override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig? {
    val extras = intent?.extras ?: return null
    return HeadlessJsTaskConfig(
      TASK_NAME,
      Arguments.fromBundle(extras),
      TASK_TIMEOUT_MS,
      true
    )
  }

  companion object {
    // Must match `NotificationListenerAndroidHeadlessJsName` in src/index.tsx.
    const val TASK_NAME = "NotificationListenerAndroidHeadlessTask"
    private const val TASK_TIMEOUT_MS = 30000L
  }
}
