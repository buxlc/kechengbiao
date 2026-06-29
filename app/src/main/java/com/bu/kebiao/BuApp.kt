package com.bu.kebiao

import android.app.Application
import com.bu.kebiao.data.local.BuDatabase
import com.bu.kebiao.data.adapter.cloud.AdapterCloudConfig
import com.bu.kebiao.data.adapter.cloud.AdapterCloudHttpClient
import com.bu.kebiao.data.adapter.cloud.AdapterCloudRepository
import com.bu.kebiao.data.adapter.cloud.OkHttpAdapterCloudHttpClient
import com.bu.kebiao.data.adapter.cloud.SchoolIndexCloudRepository
import com.bu.kebiao.data.preferences.UserPreferences
import com.bu.kebiao.data.repository.ClassTimeRepositoryImpl
import com.bu.kebiao.data.repository.CourseColorRepositoryImpl
import com.bu.kebiao.data.repository.CourseRepositoryImpl
import com.bu.kebiao.data.repository.SemesterRepositoryImpl
import com.bu.kebiao.domain.repository.ClassTimeRepository
import com.bu.kebiao.domain.repository.CourseColorRepository
import com.bu.kebiao.domain.repository.CourseRepository
import com.bu.kebiao.domain.repository.SemesterRepository
import com.bu.kebiao.liveupdate.CourseLiveUpdateScheduler
import com.bu.kebiao.widget.WidgetDataLoader
import com.bu.kebiao.widget.WidgetUpdateDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

@HiltAndroidApp
class BuApp : Application() {
    @Inject
    lateinit var liveUpdateScheduler: CourseLiveUpdateScheduler
    @Inject
    lateinit var widgetUpdateDispatcher: WidgetUpdateDispatcher

    override fun onCreate() {
        super.onCreate()
        liveUpdateScheduler.refreshNow()
        widgetUpdateDispatcher.refresh()
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
    fun provideSemesterDao(db: BuDatabase) = db.semesterDao()

    @Provides
    @Singleton
    fun provideUserPreferences(app: Application): UserPreferences = UserPreferences(app)

    @Provides
    @Singleton
    fun provideCourseRepository(
        dao: com.bu.kebiao.data.local.CourseDao,
        userPreferences: UserPreferences
    ): CourseRepository =
        CourseRepositoryImpl(dao, userPreferences)

    @Provides
    @Singleton
    fun provideSemesterRepository(dao: com.bu.kebiao.data.local.SemesterDao): SemesterRepository =
        SemesterRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideClassTimeRepository(dao: com.bu.kebiao.data.local.ClassTimeDao): ClassTimeRepository =
        ClassTimeRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCourseColorRepository(dao: com.bu.kebiao.data.local.CourseColorDao): CourseColorRepository =
        CourseColorRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideAdapterCloudHttpClient(client: OkHttpClient): AdapterCloudHttpClient =
        OkHttpAdapterCloudHttpClient(client)

    @Provides
    @Singleton
    fun provideAdapterCloudRepository(httpClient: AdapterCloudHttpClient): AdapterCloudRepository =
        AdapterCloudRepository(httpClient, AdapterCloudConfig.DEFAULT_MANIFEST_URL)

    @Provides
    @Singleton
    fun provideSchoolIndexCloudRepository(httpClient: AdapterCloudHttpClient): SchoolIndexCloudRepository =
        SchoolIndexCloudRepository(httpClient, AdapterCloudConfig.DEFAULT_SCHOOL_INDEX_URL)

    @Provides
    @Singleton
    fun provideWidgetDataLoader(
        courseRepository: CourseRepository,
        classTimeRepository: ClassTimeRepository,
        userPreferences: UserPreferences
    ): WidgetDataLoader =
        WidgetDataLoader(courseRepository, classTimeRepository, userPreferences)

}
