package com.bu.kebiao.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.bu.kebiao.MainActivity
import com.bu.kebiao.R

object WidgetRemoteViewsRenderer {
    private const val REQUEST_OPEN_SMALL_WIDGET = 2101
    private const val REQUEST_OPEN_LARGE_WIDGET = 2102
    private const val REQUEST_OPEN_SMALL_WIDGET_ITEM = 2103

    private data class CourseRowIds(
        val rowId: Int,
        val accentId: Int,
        val titleId: Int,
        val detailId: Int
    )

    fun buildSmall(context: Context, snapshot: WidgetSmallScheduleSnapshot, appWidgetId: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_course_small).apply {
            setTextViewText(R.id.widget_state_label, snapshot.label)
            setTextViewText(R.id.widget_small_count, snapshot.countText)
            setOnClickPendingIntent(
                R.id.widget_small_root,
                launchPendingIntent(context, REQUEST_OPEN_SMALL_WIDGET, mutable = false)
            )

            val adapterIntent = Intent(context, WidgetSmallCourseListService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            setRemoteAdapter(R.id.widget_small_course_list, adapterIntent)
            setPendingIntentTemplate(
                R.id.widget_small_course_list,
                launchPendingIntent(context, REQUEST_OPEN_SMALL_WIDGET_ITEM, mutable = true)
            )
            setEmptyView(R.id.widget_small_course_list, R.id.widget_small_empty)
        }

    fun buildLarge(context: Context, schedule: WidgetScheduleSnapshot): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_course_large).apply {
            setOnClickPendingIntent(
                R.id.widget_large_root,
                launchPendingIntent(context, REQUEST_OPEN_LARGE_WIDGET, mutable = false)
            )
            setTextViewText(R.id.widget_large_header_left, schedule.headerLeft)
            setTextViewText(R.id.widget_large_header_center, schedule.headerCenter)
            setTextViewText(R.id.widget_large_header_right, schedule.headerRight)
            bindDayPanel(
                labelId = R.id.widget_today_title,
                card1Id = R.id.widget_today_card_1,
                card1AccentId = R.id.widget_today_card_1_accent,
                card1TitleId = R.id.widget_today_card_1_title,
                card1DetailId = R.id.widget_today_card_1_detail,
                card2Id = R.id.widget_today_card_2,
                card2AccentId = R.id.widget_today_card_2_accent,
                card2TitleId = R.id.widget_today_card_2_title,
                card2DetailId = R.id.widget_today_card_2_detail,
                column = schedule.today
            )
            bindDayPanel(
                labelId = R.id.widget_tomorrow_title,
                card1Id = R.id.widget_tomorrow_card_1,
                card1AccentId = R.id.widget_tomorrow_card_1_accent,
                card1TitleId = R.id.widget_tomorrow_card_1_title,
                card1DetailId = R.id.widget_tomorrow_card_1_detail,
                card2Id = R.id.widget_tomorrow_card_2,
                card2AccentId = R.id.widget_tomorrow_card_2_accent,
                card2TitleId = R.id.widget_tomorrow_card_2_title,
                card2DetailId = R.id.widget_tomorrow_card_2_detail,
                column = schedule.tomorrow
            )
        }

    private fun RemoteViews.bindDayPanel(
        labelId: Int,
        card1Id: Int,
        card1AccentId: Int,
        card1TitleId: Int,
        card1DetailId: Int,
        card2Id: Int,
        card2AccentId: Int,
        card2TitleId: Int,
        card2DetailId: Int,
        column: WidgetDayColumn
    ) {
        setTextViewText(labelId, column.label)
        val rows = listOf(
            CourseRowIds(card1Id, card1AccentId, card1TitleId, card1DetailId),
            CourseRowIds(card2Id, card2AccentId, card2TitleId, card2DetailId)
        )
        rows.forEachIndexed { index, row ->
            bindCourseRow(row, column.tiles.getOrNull(index))
        }
    }

    private fun RemoteViews.bindCourseRow(
        row: CourseRowIds,
        tile: WidgetCourseTile?
    ) {
        if (tile == null) {
            setViewVisibility(row.rowId, View.INVISIBLE)
            setTextViewText(row.titleId, "")
            setTextViewText(row.detailId, "")
            return
        }

        setViewVisibility(row.rowId, View.VISIBLE)
        setInt(row.accentId, "setBackgroundColor", tile.accentColor)
        setTextViewText(row.titleId, tile.title)
        setTextViewText(row.detailId, tile.detailLine())
    }

    private fun WidgetCourseTile.detailLine(): String =
        listOf(timeLine, locationLine)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" · ")

    fun launchFillInIntent(): Intent = Intent()

    private fun launchPendingIntent(context: Context, requestCode: Int, mutable: Boolean): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
