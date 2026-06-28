package com.bu.kebiao.liveupdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CourseBootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var scheduler: CourseLiveUpdateScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pendingResult = goAsync()
        scheduler.refreshNow {
            pendingResult.finish()
        }
    }
}
