package com.notificationlistenerandroid

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Builds the JSON payload sent to JS for every posted status-bar notification.
 * Field names/shape intentionally match the payload the app already reads
 * (see checkout_app_up's index.js `sendNotification`): `app`, `title`, `text`,
 * `time` are load-bearing; the rest are extras kept for parity/future use.
 */
object NotificationPayloadBuilder {
  private const val TAG = "NotificationListenerAndroid"

  // Icons/large icons from some apps can be full-resolution bitmaps (several
  // MB) — encoding those as base64 into the headless-task Intent risks
  // android.os.TransactionTooLargeException, which would crash/unbind the
  // whole NotificationListenerService. Resize before encoding.
  private const val MAX_ICON_DIMENSION = 128

  fun build(context: Context, sbn: StatusBarNotification): JSONObject {
    val notification = sbn.getNotification()
    val json = JSONObject()

    val packageName = sbn.getPackageName()
    json.put("app", if (packageName.isNullOrEmpty()) "Unknown App" else packageName)
    json.put("time", sbn.getPostTime().toString())

    if (notification?.extras == null) {
      Log.d(TAG, "Notification has no extras, returning minimal payload")
      return json
    }

    val extras = notification.extras
    json.put("title", extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: "")
    json.put(
      "titleBig",
      extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim() ?: ""
    )
    json.put("text", extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: "")
    json.put(
      "subText",
      extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""
    )
    json.put(
      "summaryText",
      extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim() ?: ""
    )
    json.put(
      "bigText",
      extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""
    )
    json.put(
      "extraInfoText",
      extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.trim() ?: ""
    )
    json.put("icon", safeIconDataUri(context, notification, isLarge = false))
    json.put("iconLarge", safeIconDataUri(context, notification, isLarge = true))
    json.put("image", safeImageDataUri(notification))

    return json
  }

  private fun resizeIfNeeded(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= MAX_ICON_DIMENSION && height <= MAX_ICON_DIMENSION) {
      return bitmap
    }
    val ratio = minOf(
      MAX_ICON_DIMENSION.toFloat() / width,
      MAX_ICON_DIMENSION.toFloat() / height
    )
    val newWidth = maxOf(1, Math.round(width * ratio))
    val newHeight = maxOf(1, Math.round(height * ratio))
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
  }

  private fun safeIconDataUri(context: Context, notification: Notification, isLarge: Boolean): String {
    return try {
      val icon = if (isLarge) notification.getLargeIcon() else notification.smallIcon
      val drawable = icon?.loadDrawable(context) ?: return ""
      val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return ""
      val resized = resizeIfNeeded(bitmap)

      val outputStream = ByteArrayOutputStream()
      resized.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
      val encoded = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
      if (encoded.isEmpty()) "" else "data:image/png;base64,$encoded"
    } catch (e: Exception) {
      Log.d(TAG, "Failed to read notification icon: ${e.message}")
      ""
    }
  }

  private fun safeImageDataUri(notification: Notification): String {
    return try {
      if (!notification.extras.containsKey(Notification.EXTRA_PICTURE)) return ""
      val bitmap = notification.extras.get(Notification.EXTRA_PICTURE) as? Bitmap ?: return ""

      val outputStream = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)
      val encoded = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
      if (encoded.isEmpty()) "" else "data:image/jpeg;base64,$encoded"
    } catch (e: Exception) {
      Log.d(TAG, "Failed to read notification image: ${e.message}")
      ""
    }
  }
}
