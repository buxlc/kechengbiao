package com.bu.kebiao

import android.app.Application
import com.bu.kebiao.data.local.BuDatabase
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.data.repository.ClassTimeRepositoryImpl
import com.bu.kebiao.data.repository.CourseColorRepositoryImpl
import com.bu.kebiao.data.repository.CourseRepositoryImpl
import com.bu.kebiao.domain.repository.ClassTimeRepository
import com.bu.kebiao.domain.repository.CourseColorRepository
import com.bu.kebiao.domain.repository.CourseRepository
import com.bu.kebiao.liveupdate.CourseLiveUpdateScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class BuApp : Application() {
    @Inject
    lateinit var liveUpdateScheduler: CourseLiveUpdateScheduler

    override fun onCreate() {
        super.onCreate()
        liveUpdateScheduler.refreshNow()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): BuDatabase = BuDatabase.getInstance(app)

    @Provides
    fun provideCourseDao(db: BuDatabase) = db.courseDao()

    @Provides
    fun provideClassTimeDao(db: BuDatabase) = db.classTimeDao()

    @Provides
    fun provideCourseColorDao(db: BuDatabase) = db.courseColorDao()

    @Provides
    @Singleton
    fun provideUserPreferences(app: Application): UserPreferences = UserPreferences(app)

    @Provides
    @Singleton
    fun provideCourseRepository(dao: com.bu.kebiao.data.local.CourseDao): CourseRepository =
        CourseRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideClassTimeRepository(dao: com.bu.kebiao.data.local.ClassTimeDao): ClassTimeRepository =
        ClassTimeRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCourseColorRepository(dao: com.bu.kebiao.data.local.CourseColorDao): CourseColorRepository =
        CourseColorRepositoryImpl(dao)
}
