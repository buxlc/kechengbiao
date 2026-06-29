package com.bu.kebiao.ui.courseimport

import com.bu.kebiao.domain.model.WeekType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsCourseParserTest {

    @Test
    fun parsesWakeUpScheduleWeeklyEvents() {
        val input = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//YZune//WakeUpSchedule//EN
            BEGIN:VEVENT
            UID:course-1
            SUMMARY:${"\u751f\u7269\u5316\u5b66"}
            DTSTART;TZID=Asia/Shanghai:20260302T085500
            DTEND;TZID=Asia/Shanghai:20260302T104500
            RRULE:FREQ=WEEKLY;UNTIL=20260315T160000Z;INTERVAL=1
            LOCATION:A3${"\u6821\u533a"}27-${"\u5206\u5b50\u533b\u5b66"}
            DESCRIPTION:${"\u7b2c"}1 - 2${"\u8282"}\nA3${"\u6821\u533a"}27-${"\u5206\u5b50\u533b\u5b66"}\n${"\u738b\u5cf0"},${"\u8d75\u745e\u5f3a"}
            BEGIN:VALARM
            ACTION:DISPLAY
            TRIGGER;RELATED=START:-PT20M
            DESCRIPTION:${"\u751f\u7269\u5316\u5b66"}@A3${"\u6821\u533a"}27-${"\u5206\u5b50\u533b\u5b66"}\n
            END:VALARM
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = IcsCourseParser.parse(input, source = "ics_import")

        assertEquals(emptyList<String>(), result.errors)
        assertEquals(1, result.courses.size)
        val course = result.courses.single()
        assertEquals("\u751f\u7269\u5316\u5b66", course.name)
        assertEquals("A3\u6821\u533a27-\u5206\u5b50\u533b\u5b66", course.location)
        assertEquals("\u738b\u5cf0,\u8d75\u745e\u5f3a", course.teacher)
        assertEquals(1, course.dayOfWeek)
        assertEquals(1, course.startSection)
        assertEquals(2, course.endSection)
        assertEquals(listOf(1, 2), course.weeks)
        assertEquals(WeekType.ALL, course.weekType)
        assertEquals("ics_import", course.source)
    }

    @Test
    fun mergesSplitEventsIntoExplicitWeeks() {
        val input = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:course-a
            SUMMARY:${"\u75c5\u7406\u5b66"}
            DTSTART;TZID=Asia/Shanghai:20260302T152000
            DTEND;TZID=Asia/Shanghai:20260302T171000
            RRULE:FREQ=WEEKLY;UNTIL=20260308T160000Z;INTERVAL=1
            DESCRIPTION:${"\u7b2c"}6 - 7${"\u8282"}\nA1${"\u6821\u533a"}14${"\u6559\u5ba4"}\n${"\u52b3\u5b8f\u4f1f"}
            END:VEVENT
            BEGIN:VEVENT
            UID:course-b
            SUMMARY:${"\u75c5\u7406\u5b66"}
            DTSTART;TZID=Asia/Shanghai:20260316T152000
            DTEND;TZID=Asia/Shanghai:20260316T171000
            RRULE:FREQ=WEEKLY;UNTIL=20260322T160000Z;INTERVAL=1
            DESCRIPTION:${"\u7b2c"}6 - 7${"\u8282"}\nA1${"\u6821\u533a"}14${"\u6559\u5ba4"}\n${"\u52b3\u5b8f\u4f1f"}
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = IcsCourseParser.parse(input, source = "ics_import")

        assertEquals(1, result.courses.size)
        val course = result.courses.single()
        assertEquals(listOf(1, 3), course.weeks)
        assertEquals(WeekType.ODD, course.weekType)
    }

    @Test
    fun unfoldsEscapedLongLines() {
        val input = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:course-1
            SUMMARY:${"\u5927\u5b66\u751f\u5b89\u5168\u6559\u80b2"}
            DTSTART;TZID=Asia/Shanghai:20260303T171500
            DTEND;TZID=Asia/Shanghai:20260303T190000
            DESCRIPTION:${"\u7b2c"}8 - 9${"\u8282"}\nA1${"\u6821\u533a"}10${"\u6559\u5ba4"}\n${"\u675c\u654f"}\,
             ${"\u9ec4\u5c1a\u4e3d"}
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = IcsCourseParser.parse(input, source = "ics_import")

        assertEquals(1, result.courses.size)
        assertEquals("\u675c\u654f,\u9ec4\u5c1a\u4e3d", result.courses.single().teacher)
    }

    @Test
    fun skipsEventsWithoutSectionRange() {
        val input = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:course-1
            SUMMARY:${"\u65e0\u8282\u6b21\u8bfe\u7a0b"}
            DTSTART;TZID=Asia/Shanghai:20260303T171500
            DTEND;TZID=Asia/Shanghai:20260303T190000
            DESCRIPTION:A1${"\u6821\u533a"}10${"\u6559\u5ba4"}
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val result = IcsCourseParser.parse(input, source = "ics_import")

        assertTrue(result.courses.isEmpty())
        assertEquals(1, result.errors.size)
    }
}
