package com.bu.kebiao.liveupdate

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.domain.repository.ClassTimeRepository
import com.bu.kebiao.domain.repository.CourseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseLiveUpdateScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val courseRepository: CourseRepository,
    private val classTimeRepository: ClassTimeRepository,
    private val userPreferences: UserPreferences,
    private val notifier: CourseLiveUpdateNotifier
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun refreshNow(onComplete: (() -> Unit)? = null) {
        scope.launch {
            try {
                refresh()
            } finally {
                onComplete?.invoke()
            }
        }
    }

    suspend fun refresh() {
        val prefs = userPreferences.preferencesFlow.first()
        val courses = courseRepository.getCoursesByWeek(prefs.currentWeek).first()
        val classTimes = classTimeRepository.getAllClassTimes().first()
        val now = LocalDateTime.now()

        val state = CourseLiveUpdateCalculator.calculate(
            courses = courses,
            classTimes = classTimes,
            currentWeek = prefs.currentWeek,
            now = now
        )

        notifier.show(state)
        scheduleNext(
            CourseLiveUpdateCalculator.nextCheckTime(
                courses = courses,
                classTimes = classTimes,
                currentWeek = prefs.currentWeek,
                now = now
            )
        )
    }

    private fun scheduleNext(nextCheckTime: LocalDateTime?) {
        val pendingIntent = buildPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)
        if (nextCheckTime == null) return

        val triggerAtMillis = nextCheckTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAtMillis <= System.currentTimeMillis()) return

        try {
            val showIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                Intent(context, com.bu.kebiao.MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                pendingIntent
            )
        } catch (_: SecurityException) {
            try {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SCHEDULE_EXACT_ALARM
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return alarmManager.canScheduleExactAlarms()
    }

    private fun buildPendingIntent(flags: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, CourseLiveUpdateReceiver::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or flags
        )

    companion object {
        const val ACTION_REFRESH = "com.bu.kebiao.liveupdate.REFRESH"
        private const val REQUEST_CODE = 2001
    }
}
