package com.bu.kebiao.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.bu.kebiao.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class WidgetSmallCourseListService : RemoteViewsService() {
    @Inject
    lateinit var widgetDataLoader: WidgetDataLoader

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        SmallCourseFactory(applicationContext, widgetDataLoader)
}

private class SmallCourseFactory(
    private val context: Context,
    private val widgetDataLoader: WidgetDataLoader
) : RemoteViewsService.RemoteViewsFactory {
    private var courses: List<WidgetCourseCard> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        courses = runCatching {
            runBlocking { widgetDataLoader.loadSchedule().small.courses }
        }.getOrDefault(emptyList())
    }

    override fun onDestroy() {
        courses = emptyList()
    }

    override fun getCount(): Int = courses.size

    override fun getViewAt(position: Int): RemoteViews? {
        val course = courses.getOrNull(position) ?: return null
        return RemoteViews(context.packageName, R.layout.widget_small_course_item).apply {
            setTextViewText(R.id.widget_small_item_title, course.courseName)
            setTextViewText(R.id.widget_small_item_detail, course.smallDetailLine())
            setInt(R.id.widget_small_item_accent, "setBackgroundColor", course.accentColor)
            setOnClickFillInIntent(
                R.id.widget_small_item_root,
                WidgetRemoteViewsRenderer.launchFillInIntent()
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun WidgetCourseCard.smallDetailLine(): String =
        listOf(detailLine, extraLine)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" · ")
}
