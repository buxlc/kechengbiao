package com.bu.kebiao.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bu_preferences")

class UserPreferences(private val context: Context) {

    private object Keys {
        val VIEWING_WEEK = intPreferencesKey("current_week")
        val TOTAL_WEEKS = intPreferencesKey("total_weeks")
        val SEMESTER_NAME = stringPreferencesKey("semester_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EDU_SCHOOL = stringPreferencesKey("edu_school")
        val EDU_ACCOUNT = stringPreferencesKey("edu_account")
        val HAS_IMPORTED = booleanPreferencesKey("has_imported")
        val SEMESTER_START_DATE = longPreferencesKey("semester_start_date")
        val COURSE_TEXT_SIZE = stringPreferencesKey("course_text_size")
        val CURRENT_SEMESTER_ID = stringPreferencesKey("current_semester_id")
    }

    data class Preferences(
        val viewingWeek: Int = 1,
        val totalWeeks: Int = 20,
        val semesterName: String = "",
        val themeMode: String = "system",
        val eduSchool: String = "",
        val eduAccount: String = "",
        val hasImported: Boolean = false,
        val semesterStartDate: Long = 0L,
        val courseTextSize: String = "medium",
        val currentSemesterId: String = "default"
    ) {
        val currentWeek: Int
            get() = viewingWeek
    }

    val preferencesFlow: Flow<Preferences> = context.dataStore.data.map { prefs ->
        Preferences(
            viewingWeek = prefs[Keys.VIEWING_WEEK] ?: 1,
            totalWeeks = prefs[Keys.TOTAL_WEEKS] ?: 20,
            semesterName = prefs[Keys.SEMESTER_NAME] ?: "",
            themeMode = prefs[Keys.THEME_MODE] ?: "system",
            eduSchool = prefs[Keys.EDU_SCHOOL] ?: "",
            eduAccount = prefs[Keys.EDU_ACCOUNT] ?: "",
            hasImported = prefs[Keys.HAS_IMPORTED] ?: false,
            semesterStartDate = prefs[Keys.SEMESTER_START_DATE] ?: 0L,
            courseTextSize = prefs[Keys.COURSE_TEXT_SIZE] ?: "medium",
            currentSemesterId = prefs[Keys.CURRENT_SEMESTER_ID] ?: "default"
        )
    }

    suspend fun updateViewingWeek(week: Int) {
        context.dataStore.edit { it[Keys.VIEWING_WEEK] = week }
    }

    suspend fun updateCurrentWeek(week: Int) = updateViewingWeek(week)

    suspend fun updateTotalWeeks(weeks: Int) {
        context.dataStore.edit { it[Keys.TOTAL_WEEKS] = weeks }
    }

    suspend fun updateSemesterInfo(name: String, startDate: Long, totalWeeks: Int) {
        context.dataStore.edit {
            it[Keys.SEMESTER_NAME] = name
            it[Keys.SEMESTER_START_DATE] = startDate
            it[Keys.TOTAL_WEEKS] = totalWeeks
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun updateEduInfo(school: String, account: String) {
        context.dataStore.edit {
            it[Keys.EDU_SCHOOL] = school
            it[Keys.EDU_ACCOUNT] = account
        }
    }

    suspend fun setHasImported(imported: Boolean) {
        context.dataStore.edit { it[Keys.HAS_IMPORTED] = imported }
    }

    suspend fun updateCourseTextSize(size: String) {
        context.dataStore.edit { it[Keys.COURSE_TEXT_SIZE] = size }
    }

    suspend fun updateCurrentSemesterId(semesterId: String) {
        context.dataStore.edit { it[Keys.CURRENT_SEMESTER_ID] = semesterId }
    }
}
