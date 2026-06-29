package com.bu.kebiao.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CourseEntity::class, ClassTimeEntity::class, CourseColorEntity::class, SemesterEntity::class],
    version = 6,
    exportSchema = false
)
abstract class BuDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun classTimeDao(): ClassTimeDao
    abstract fun courseColorDao(): CourseColorDao
    abstract fun semesterDao(): SemesterDao

    companion object {
        @Volatile
        private var INSTANCE: BuDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN weeks TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS course_name_colors (courseName TEXT NOT NULL PRIMARY KEY, colorIndex INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plans ADD COLUMN time TEXT")
                db.execSQL("ALTER TABLE plans ADD COLUMN repeatType INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE plans ADD COLUMN dayOfWeek INTEGER")
                db.execSQL("ALTER TABLE plans ADD COLUMN dayOfMonth INTEGER")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plans ADD COLUMN endTime TEXT")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS semesters (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        startDate INTEGER NOT NULL DEFAULT 0,
                        totalWeeks INTEGER NOT NULL DEFAULT 20,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE courses ADD COLUMN semesterId TEXT NOT NULL DEFAULT 'default'")
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO semesters (id, name, startDate, totalWeeks, createdAt)
                    VALUES ('default', '默认学期', 0, 20, strftime('%s','now') * 1000)
                    """.trimIndent()
                )
            }
        }
        fun getInstance(context: Context): BuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BuDatabase::class.java,
                    "bu_kebiao.db"
                )
                    .addCallback(DefaultDataCallback())
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DefaultDataCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val defaultTimes = listOf(
                ClassTimeEntity(1, "08:00", "08:45"),
                ClassTimeEntity(2, "08:55", "09:40"),
                ClassTimeEntity(3, "10:00", "10:45"),
                ClassTimeEntity(4, "10:55", "11:40"),
                ClassTimeEntity(5, "14:00", "14:45"),
                ClassTimeEntity(6, "14:55", "15:40"),
                ClassTimeEntity(7, "16:00", "16:45"),
                ClassTimeEntity(8, "16:55", "17:40"),
                ClassTimeEntity(9, "19:00", "19:45"),
                ClassTimeEntity(10, "19:55", "20:40"),
                ClassTimeEntity(11, "20:50", "21:35"),
                ClassTimeEntity(12, "21:45", "22:30")
            )
            defaultTimes.forEach { time ->
                db.execSQL(
                    "INSERT OR REPLACE INTO class_times (sectionNumber, startTime, endTime) VALUES (?, ?, ?)",
                    arrayOf(time.sectionNumber, time.startTime, time.endTime)
                )
            }
            db.execSQL(
                "INSERT OR IGNORE INTO semesters (id, name, startDate, totalWeeks, createdAt) VALUES ('default', '默认学期', 0, 20, strftime('%s','now') * 1000)"
            )
        }
    }
}
