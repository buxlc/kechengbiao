# Course Fluid Cloud Live Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight course live update that appears 20 minutes before class, shows a countdown, switches to in-class text, and disappears after class.

**Architecture:** Keep the feature in `com.bu.kebiao.liveupdate`. Pure Kotlin calculator and formatter are tested first. Android-specific notification and scheduling code wraps those pure units and is called from app startup and course changes.

**Tech Stack:** Kotlin, Android notifications, AlarmManager, Hilt, JUnit 4, existing Room repository interfaces.

---

## File Structure

- Create `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateModels.kt`: sealed state and display text models.
- Create `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateCalculator.kt`: pure time-window logic.
- Create `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateFormatter.kt`: Chinese notification titles and text.
- Create `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateNotifier.kt`: Android notification creation, ColorOS extras, cancel behavior.
- Create `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateScheduler.kt`: reads repositories and schedules refreshes.
- Create `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateReceiver.kt`: receives alarm broadcasts.
- Modify `app/src/main/java/com/bu/kebiao/BuApp.kt`: provide live update classes with Hilt.
- Modify `app/src/main/java/com/bu/kebiao/MainActivity.kt`: request notification permission and kick off scheduler.
- Modify `app/src/main/java/com/bu/kebiao/ui/home/HomeViewModel.kt`: request a schedule refresh after course edits/deletes.
- Modify `app/src/main/AndroidManifest.xml`: add notification/alarm permissions and receiver.
- Create `app/src/test/java/com/bu/kebiao/liveupdate/CourseLiveUpdateCalculatorTest.kt`.
- Create `app/src/test/java/com/bu/kebiao/liveupdate/CourseLiveUpdateFormatterTest.kt`.

### Task 1: Calculator Models And Tests

**Files:**
- Create: `app/src/test/java/com/bu/kebiao/liveupdate/CourseLiveUpdateCalculatorTest.kt`
- Create: `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateModels.kt`
- Create: `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateCalculator.kt`

- [ ] **Step 1: Write the failing calculator test**

```kotlin
package com.bu.kebiao.liveupdate

import com.bu.kebiao.domain.model.ClassTime
import com.bu.kebiao.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CourseLiveUpdateCalculatorTest {
    private val monday = LocalDate.of(2026, 6, 29)
    private val course = Course(
        name = "高等数学",
        location = "教学楼 301",
        teacher = "张三",
        dayOfWeek = 1,
        startSection = 1,
        endSection = 2,
        weeks = listOf(1)
    )
    private val classTimes = listOf(
        ClassTime(1, "08:00", "08:45"),
        ClassTime(2, "08:55", "09:40")
    )

    @Test
    fun courseStartingInTwentyOneMinutesIsHidden() {
        val state = CourseLiveUpdateCalculator.calculate(
            courses = listOf(course),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, java.time.LocalTime.of(7, 39))
        )
        assertTrue(state is CourseLiveUpdateState.Hidden)
    }

    @Test
    fun courseStartingInTwentyMinutesIsUpcoming() {
        val state = CourseLiveUpdateCalculator.calculate(
            courses = listOf(course),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, java.time.LocalTime.of(7, 40))
        )
        state as CourseLiveUpdateState.Upcoming
        assertEquals("高等数学", state.course.name)
        assertEquals(20, state.minutesUntilStart)
    }

    @Test
    fun startedCourseIsInClass() {
        val state = CourseLiveUpdateCalculator.calculate(
            courses = listOf(course),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, java.time.LocalTime.of(8, 10))
        )
        state as CourseLiveUpdateState.InClass
        assertEquals("高等数学", state.course.name)
        assertEquals("09:40", state.endTimeText)
    }

    @Test
    fun endedCourseIsHidden() {
        val state = CourseLiveUpdateCalculator.calculate(
            courses = listOf(course),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, java.time.LocalTime.of(9, 41))
        )
        assertTrue(state is CourseLiveUpdateState.Hidden)
    }

    @Test
    fun missingClassTimeIsSkipped() {
        val state = CourseLiveUpdateCalculator.calculate(
            courses = listOf(course.copy(startSection = 11, endSection = 12)),
            classTimes = classTimes,
            currentWeek = 1,
            now = LocalDateTime.of(monday, java.time.LocalTime.of(7, 50))
        )
        assertTrue(state is CourseLiveUpdateState.Hidden)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.bu.kebiao.liveupdate.CourseLiveUpdateCalculatorTest`

Expected: FAIL because `CourseLiveUpdateCalculator` and `CourseLiveUpdateState` do not exist.

- [ ] **Step 3: Implement models and calculator**

Create `CourseLiveUpdateModels.kt` with `Hidden`, `Upcoming`, and `InClass` states. Create `CourseLiveUpdateCalculator.kt` that parses `HH:mm`, maps course start/end sections to `ClassTime`, filters inactive weeks, chooses active course first, then the earliest upcoming course within 20 minutes.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests com.bu.kebiao.liveupdate.CourseLiveUpdateCalculatorTest`

Expected: PASS.

### Task 2: Formatter Tests And Implementation

**Files:**
- Create: `app/src/test/java/com/bu/kebiao/liveupdate/CourseLiveUpdateFormatterTest.kt`
- Create: `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateFormatter.kt`

- [ ] **Step 1: Write the failing formatter test**

```kotlin
package com.bu.kebiao.liveupdate

import com.bu.kebiao.domain.model.Course
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class CourseLiveUpdateFormatterTest {
    private val course = Course(
        name = "大学英语",
        location = "语音室 402",
        dayOfWeek = 2,
        startSection = 3,
        endSection = 4
    )

    @Test
    fun formatsUpcomingCountdown() {
        val text = CourseLiveUpdateFormatter.format(
            CourseLiveUpdateState.Upcoming(
                course = course,
                startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
                endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
                minutesUntilStart = 12,
                startTimeText = "10:00"
            )
        )
        assertEquals("12分钟后上课", text.title)
        assertEquals("大学英语 · 语音室 402 · 10:00开始", text.content)
    }

    @Test
    fun formatsInClassState() {
        val text = CourseLiveUpdateFormatter.format(
            CourseLiveUpdateState.InClass(
                course = course,
                startsAt = LocalDateTime.of(2026, 6, 30, 10, 0),
                endsAt = LocalDateTime.of(2026, 6, 30, 11, 40),
                endTimeText = "11:40"
            )
        )
        assertEquals("正在上课", text.title)
        assertEquals("大学英语 · 语音室 402 · 11:40下课", text.content)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.bu.kebiao.liveupdate.CourseLiveUpdateFormatterTest`

Expected: FAIL because `CourseLiveUpdateFormatter` does not exist.

- [ ] **Step 3: Implement formatter**

Create `CourseLiveUpdateFormatter.kt` returning `CourseLiveUpdateText(title, content)`. Empty locations are omitted from the middle text.

- [ ] **Step 4: Run formatter tests**

Run: `.\gradlew.bat testDebugUnitTest --tests com.bu.kebiao.liveupdate.CourseLiveUpdateFormatterTest`

Expected: PASS.

### Task 3: Android Notification Wrapper

**Files:**
- Create: `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateNotifier.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add manifest permissions**

Add `POST_NOTIFICATIONS`, `POST_PROMOTED_NOTIFICATIONS`, and `SCHEDULE_EXACT_ALARM`.

- [ ] **Step 2: Implement notifier**

Create a notifier that checks notification permission, builds a high-importance channel, builds a standard notification with `setOngoing(true)`, `setRequestPromotedOngoing(true)`, `Notification.EXTRA_REQUEST_PROMOTED_ONGOING`, and `oplus_smallicon_use_app_icon=false`, then sends it through `NotificationManager.notify()`.

- [ ] **Step 3: Build check**

Run: `.\gradlew.bat compileDebugKotlin`

Expected: SUCCESS.

### Task 4: Scheduler And Receiver

**Files:**
- Create: `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateScheduler.kt`
- Create: `app/src/main/java/com/bu/kebiao/liveupdate/CourseLiveUpdateReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/bu/kebiao/BuApp.kt`

- [ ] **Step 1: Implement receiver**

Create a `BroadcastReceiver` that starts a coroutine through an injected or entry-point scheduler trigger.

- [ ] **Step 2: Implement scheduler**

Read `CourseRepository.getCoursesByDayAndWeek()`, `ClassTimeRepository.getAllClassTimes()`, and `UserPreferences.preferencesFlow`. Calculate the current state, call notifier show/cancel, and schedule the next alarm at the next minute while upcoming, at class start, at class end, or at the next 20-minute pre-class boundary.

- [ ] **Step 3: Wire Hilt providers**

Add singleton providers for notifier and scheduler in `BuApp.kt`.

- [ ] **Step 4: Build check**

Run: `.\gradlew.bat compileDebugKotlin`

Expected: SUCCESS.

### Task 5: App Startup And Permission Request

**Files:**
- Modify: `app/src/main/java/com/bu/kebiao/MainActivity.kt`
- Modify: `app/src/main/java/com/bu/kebiao/ui/home/HomeViewModel.kt`

- [ ] **Step 1: Request notification permission**

In `MainActivity`, request `POST_NOTIFICATIONS` on Android 13+ if missing.

- [ ] **Step 2: Start scheduler on app launch**

Inject `CourseLiveUpdateScheduler` into `MainActivity` and call `refreshNow()` after content setup.

- [ ] **Step 3: Refresh schedule after course edit/delete**

Inject scheduler into `HomeViewModel` and call `refreshNow()` after saving or deleting a course.

- [ ] **Step 4: Full verification**

Run: `.\gradlew.bat testDebugUnitTest compileDebugKotlin`

Expected: SUCCESS.

## Self-Review

- Spec coverage: 20-minute countdown, in-class state, cancellation, fallback notification, permissions, and tests are covered.
- Placeholder scan: no task uses TODO, TBD, or incomplete placeholders.
- Type consistency: state names, formatter names, and scheduler names match across tasks.
