package com.bu.kebiao.liveupdate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CourseLiveUpdateReceiver : BroadcastReceiver() {
    @Inject
    lateinit var scheduler: CourseLiveUpdateScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CourseLiveUpdateScheduler.ACTION_REFRESH) return
        val pendingResult = goAsync()
        scheduler.refreshNow {
            pendingResult.finish()
        }
    }
}
