package com.bu.kebiao.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.bu.kebiao.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdateDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataLoader: WidgetDataLoader
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refresh() {
        scope.launch {
            val manager = AppWidgetManager.getInstance(context)
            val smallIds = manager.getAppWidgetIds(ComponentName(context, ScheduleWidgetSmallReceiver::class.java))
            val largeIds = manager.getAppWidgetIds(ComponentName(context, ScheduleWidgetLargeReceiver::class.java))
            val schedule = dataLoader.loadSchedule()
            val largeViews = WidgetRemoteViewsRenderer.buildLarge(context, schedule)
            smallIds.forEach { appWidgetId ->
                val smallViews = WidgetRemoteViewsRenderer.buildSmall(context, schedule.small, appWidgetId)
                manager.updateAppWidget(appWidgetId, smallViews)
                manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_small_course_list)
            }
            largeIds.forEach { manager.updateAppWidget(it, largeViews) }
        }
    }
}
