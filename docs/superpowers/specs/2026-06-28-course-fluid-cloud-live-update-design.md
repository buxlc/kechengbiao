# Course Fluid Cloud Live Update Design

## Goal

Bu课表 will show a ColorOS-style Fluid Cloud / Android live update for the next course, starting 20 minutes before class. The live update shows a countdown before class, switches to an in-class state after class begins, and disappears after class unless another course is about to start.

## User Experience

The feature is automatic and lightweight.

- 20 minutes before a course starts, the system shows a course reminder.
- Before class starts, the content shows the course name, location, and remaining time.
- When class starts, the content changes to an in-class state with course name, location, and class end time.
- When class ends, the live update is cancelled unless another course starts within 20 minutes.
- If the phone or system does not support Fluid Cloud, Bu课表 falls back to a normal course notification.

No new complex setup screen is required for the first version. The app should request notification permission when needed and provide a clear fallback if permission is missing.

## Scope

In scope:

- Course live update scheduling based on local timetable data.
- 20-minute pre-class countdown.
- In-class live update state.
- Automatic cancellation after class.
- Android notification permission handling.
- ColorOS compatibility extras based on the referenced FluidCloud project.
- Unit tests for time-window and text-formatting logic.

Out of scope:

- Manual custom Fluid Cloud text.
- Always-on all-day live updates.
- Cloud push service or server-side push.
- Per-course custom reminder offsets.
- Vendor-specific settings automation beyond opening the app notification settings page.

## Architecture

The feature will use the existing local course data. A small live update package will decide what should be displayed now, format the text, and ask Android's notification system to show or hide it.

The design keeps vendor-specific details isolated so the rest of the app only asks for "course live update status".

Main pieces:

- `CourseLiveUpdateState`: a simple model for hidden, upcoming, and in-class states.
- `CourseLiveUpdateCalculator`: pure Kotlin logic that chooses the current live update state from courses, class times, current week, and current time.
- `CourseLiveUpdateFormatter`: pure Kotlin logic for user-facing Chinese notification text.
- `CourseLiveUpdateNotifier`: Android notification wrapper. It creates the notification channel, sends promoted ongoing notifications when supported, adds ColorOS compatibility extras, and cancels notifications.
- `CourseLiveUpdateScheduler`: Android scheduling wrapper. It schedules the next check and updates the live notification around class boundaries.

## Data Flow

1. The scheduler wakes up at app start, after course data changes, and near class boundaries.
2. It reads local courses, class times, and current week.
3. The calculator decides one of these states:
   - hidden: nothing should be shown.
   - upcoming: a course starts within 20 minutes.
   - in-class: a course is currently active.
4. The formatter turns that state into short Chinese text.
5. The notifier sends or cancels the Android notification.
6. The scheduler sets the next wake-up time.

## Notification Behavior

The notification should be built with standard Android notification APIs:

- Use a notification channel for course live updates.
- Set the notification as ongoing while it is active.
- Request promoted ongoing display on Android versions that support it.
- Add the promoted ongoing extra for API compatibility.
- Add `oplus_smallicon_use_app_icon=false` so ColorOS does not replace the small icon in the way observed by the reference project.
- Send the visible live update through `NotificationManager.notify()`.

The notification should not rely on `startForeground()` for the visible Fluid Cloud entry. The reference project found that foreground-service notifications can render differently on ColorOS and may cause a temporary white placeholder.

## Time Rules

The reminder window is 20 minutes before class.

- If the next class starts in more than 20 minutes, show nothing.
- If the next class starts in 1 to 20 minutes, show upcoming countdown.
- If the next class starts now or already started and has not ended, show in-class state.
- If classes are back-to-back, move from the current class to the next class when appropriate.
- If multiple courses overlap, pick the one with the earliest end time for the in-class state. If there are multiple upcoming courses, pick the one with the earliest start time.

The first version uses local class-time definitions. If a course's section time is missing, that course is skipped for live update scheduling instead of guessing.

## Permissions

The app needs notification permission on modern Android versions.

Manifest additions:

- `android.permission.POST_NOTIFICATIONS`
- `android.permission.POST_PROMOTED_NOTIFICATIONS`
- alarm permission if exact scheduling is used

If permission is denied, the app does not crash. It should keep the timetable usable and simply skip live update notification display.

## Error Handling

- Missing class time: skip the affected course for live update calculations.
- Missing notification permission: do not show the notification; allow the app to keep working.
- Unsupported Fluid Cloud system: show a normal notification when notification permission allows it.
- Notification API failure: catch and log the failure; do not block the main timetable UI.

## Testing

Tests focus on pure logic first.

Required cases:

- No course today returns hidden.
- Course starting in 21 minutes returns hidden.
- Course starting in 20 minutes returns upcoming.
- Course starting in 5 minutes returns upcoming with correct countdown.
- Course already started returns in-class.
- Course after end time returns hidden.
- Back-to-back courses transition to the correct next state.
- Missing class time skips the course.

Android notification rendering still needs real-device validation on OPPO / ColorOS. Build success and true Fluid Cloud runtime behavior must be reported separately.

## Acceptance Criteria

- Bu课表 can calculate the correct live update state for today's courses.
- The app can show a notification 20 minutes before class and update it as time passes.
- The notification asks for promoted ongoing display where Android supports it.
- ColorOS-specific notification extras are isolated in the notifier.
- The app still works when notification permission is denied.
- Unit tests cover the scheduling and formatting rules.
