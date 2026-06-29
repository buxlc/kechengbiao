package com.bu.kebiao.widget

import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLaunchClickBindingTest {
    private val projectRoot: Path = Path.of(System.getProperty("user.dir")).parent

    @Test
    fun rendererBindsLaunchPendingIntentToSmallAndLargeWidgetRoots() {
        val source = readProjectFile("app/src/main/java/com/bu/kebiao/widget/WidgetRemoteViewsRenderer.kt")
        val smallLayout = readProjectFile("app/src/main/res/layout/widget_course_small.xml")
        val largeLayout = readProjectFile("app/src/main/res/layout/widget_course_large.xml")

        assertTrue(smallLayout.contains("@+id/widget_small_root"))
        assertTrue(largeLayout.contains("@+id/widget_large_root"))
        assertTrue(source.contains("setOnClickPendingIntent("))
        assertTrue(source.contains("R.id.widget_small_root"))
        assertTrue(source.contains("R.id.widget_large_root"))
    }

    @Test
    fun smallWidgetCourseRowsUseLaunchFillInIntent() {
        val source = readProjectFile("app/src/main/java/com/bu/kebiao/widget/WidgetSmallCourseListService.kt")

        assertTrue(source.contains("setOnClickFillInIntent(R.id.widget_small_item_root"))
        assertTrue(source.contains("WidgetRemoteViewsRenderer.launchFillInIntent()"))
    }

    private fun readProjectFile(path: String): String =
        projectRoot.resolve(path).toFile().readText()
}
