package com.bu.kebiao.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseCardLayoutTest {
    @Test
    fun smallModeUsesCompactLayoutForMoreInformation() {
        val layout = courseCardTextLayout("small")

        assertEquals(0.86f, layout.fontScale, 0.001f)
        assertEquals(7, layout.titleMaxLines)
        assertEquals(7, layout.detailMaxLines)
        assertEquals(6, layout.horizontalPaddingDp)
        assertEquals(6, layout.verticalPaddingDp)
        assertTrue(layout.showDetailFallback)
    }

    @Test
    fun largeModeUsesRoomierLayoutToAvoidCrowding() {
        val layout = courseCardTextLayout("large")

        assertEquals(1.14f, layout.fontScale, 0.001f)
        assertEquals(4, layout.titleMaxLines)
        assertEquals(3, layout.detailMaxLines)
        assertEquals(8, layout.horizontalPaddingDp)
        assertEquals(8, layout.verticalPaddingDp)
        assertTrue(!layout.showDetailFallback)
    }

    @Test
    fun unknownModeFallsBackToMediumLayout() {
        val layout = courseCardTextLayout("anything")

        assertEquals(courseCardTextLayout("medium"), layout)
    }
}
