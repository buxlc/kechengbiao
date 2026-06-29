package com.bu.kebiao.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import com.bu.kebiao.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleWidgetSmallReceiver : AppWidgetProvider() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var widgetDataLoader: WidgetDataLoader

    companion object {
        private const val TAG = "ScheduleWidgetSmall"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                val schedule = widgetDataLoader.loadSchedule()
                appWidgetIds.forEach { appWidgetId ->
                    val views = WidgetRemoteViewsRenderer.buildSmall(context, schedule.small, appWidgetId)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_small_course_list)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "update widget failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
