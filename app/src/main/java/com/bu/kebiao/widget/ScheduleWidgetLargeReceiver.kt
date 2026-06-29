package com.bu.kebiao.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleWidgetLargeReceiver : AppWidgetProvider() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var widgetDataLoader: WidgetDataLoader

    companion object {
        private const val TAG = "ScheduleWidgetLarge"
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
                val views = WidgetRemoteViewsRenderer.buildLarge(context, schedule)
                appWidgetIds.forEach { appWidgetId ->
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "update widget failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
