package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        PerformanceTypeEntity::class,
        ClassEntity::class,
        StudentEntity::class,
        EventEntity::class,
        EventEligibleClassCrossRef::class,
        EventCoordinatorCrossRef::class,
        PerformanceEntity::class,
        PerformanceStudentCrossRef::class,
        EventUpdateEntity::class,
        ActivityLogEntity::class,
        ChatMessageEntity::class,
        EventIssueEntity::class,
        SchoolSubscriptionEntity::class,
        AdminReportIssueEntity::class,
        DirectMessageEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun performanceTypeDao(): PerformanceTypeDao
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun eventDao(): EventDao
    abstract fun performanceDao(): PerformanceDao
    abstract fun eventUpdateDao(): EventUpdateDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun eventIssueDao(): EventIssueDao
    abstract fun schoolSubscriptionDao(): SchoolSubscriptionDao
    abstract fun adminReportIssueDao(): AdminReportIssueDao
    abstract fun directMessageDao(): DirectMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eventsync_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
