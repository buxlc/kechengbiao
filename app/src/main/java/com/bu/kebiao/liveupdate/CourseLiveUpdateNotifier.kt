package com.bu.kebiao.liveupdate

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bu.kebiao.MainActivity
import com.bu.kebiao.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseLiveUpdateNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun show(state: CourseLiveUpdateState) {
        if (state is CourseLiveUpdateState.Hidden) {
            cancel()
            return
        }
        if (!canPostNotifications()) return

        val text = CourseLiveUpdateFormatter.format(state)
        ensureChannel()

        val openIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_course)
            .setContentTitle(text.title)
            .setContentText(text.content)
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    text.expandedLines.ifEmpty { listOf(text.expandedText) }.forEach(style::addLine)
                }
            )
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(ContextCompat.getColor(context, R.color.bu_primary))
            .apply {
                if (text.progressMax > 0) {
                    setProgress(text.progressMax, text.progress, false)
                }
            }
            .build()
            .also { notification ->
                notification.extras.putBoolean(REQUEST_PROMOTED_ONGOING_EXTRA, true)
                notification.extras.putBoolean("oplus_smallicon_use_app_icon", false)
            }

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel() {
        manager.cancel(NOTIFICATION_ID)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "课程流体云提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "上课前20分钟倒计时和上课中状态"
            setShowBadge(false)
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "course_live_update"
        private const val NOTIFICATION_ID = 2001
        private const val REQUEST_PROMOTED_ONGOING_EXTRA = "android.requestPromotedOngoing"
    }
}
