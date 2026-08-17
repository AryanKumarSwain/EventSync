package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role ORDER BY name ASC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface PerformanceTypeDao {
    @Query("SELECT * FROM performance_types WHERE active = 1 ORDER BY name ASC")
    fun getAllPerformanceTypes(): Flow<List<PerformanceTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformanceType(type: PerformanceTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformanceTypes(types: List<PerformanceTypeEntity>)

    @Query("UPDATE performance_types SET active = :active WHERE id = :id")
    suspend fun setPerformanceTypeActive(id: String, active: Boolean)

    @Query("DELETE FROM performance_types WHERE id = :id")
    suspend fun deletePerformanceTypeById(id: String)
}

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes WHERE active = 1 ORDER BY name ASC, section ASC")
    fun getAllClasses(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id")
    suspend fun getClassById(id: String): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ClassEntity>)

    @Update
    suspend fun updateClass(classEntity: ClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClassById(id: String)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE active = 1 ORDER BY name ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE classId = :classId AND active = 1 ORDER BY name ASC")
    fun getStudentsByClass(classId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: String)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventByIdFlow(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE publicSlug = :slug LIMIT 1")
    fun getEventBySlugFlow(slug: String): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE publicSlug = :slug LIMIT 1")
    suspend fun getEventBySlug(slug: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: String)

    // Event Eligible Classes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventEligibleClasses(crossRefs: List<EventEligibleClassCrossRef>)

    @Query("DELETE FROM event_eligible_classes WHERE eventId = :eventId")
    suspend fun clearEventEligibleClasses(eventId: String)

    @Query("SELECT classId FROM event_eligible_classes WHERE eventId = :eventId")
    fun getEligibleClassIdsForEvent(eventId: String): Flow<List<String>>

    // Event Coordinators
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventCoordinators(crossRefs: List<EventCoordinatorCrossRef>)

    @Query("DELETE FROM event_coordinators WHERE eventId = :eventId")
    suspend fun clearEventCoordinators(eventId: String)

    @Query("SELECT teacherId FROM event_coordinators WHERE eventId = :eventId")
    fun getCoordinatorTeacherIdsForEvent(eventId: String): Flow<List<String>>

    @Query("SELECT e.* FROM events e INNER JOIN event_coordinators ec ON e.id = ec.eventId WHERE ec.teacherId = :teacherId ORDER BY e.createdAt DESC")
    fun getEventsForTeacher(teacherId: String): Flow<List<EventEntity>>
}

@Dao
interface PerformanceDao {
    @Query("SELECT * FROM performances WHERE eventId = :eventId ORDER BY sequenceNumber ASC")
    fun getPerformancesForEvent(eventId: String): Flow<List<PerformanceEntity>>

    @Query("SELECT * FROM performances WHERE id = :id")
    suspend fun getPerformanceById(id: String): PerformanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformance(performance: PerformanceEntity)

    @Update
    suspend fun updatePerformance(performance: PerformanceEntity)

    @Query("DELETE FROM performances WHERE id = :id")
    suspend fun deletePerformanceById(id: String)

    // Performance Students
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformanceStudents(crossRefs: List<PerformanceStudentCrossRef>)

    @Query("DELETE FROM performance_students WHERE performanceId = :performanceId")
    suspend fun clearPerformanceStudents(performanceId: String)

    @Query("SELECT studentId FROM performance_students WHERE performanceId = :performanceId")
    fun getStudentIdsForPerformance(performanceId: String): Flow<List<String>>
}

@Dao
interface EventUpdateDao {
    @Query("SELECT * FROM event_updates WHERE eventId = :eventId ORDER BY createdAt DESC")
    fun getUpdatesForEvent(eventId: String): Flow<List<EventUpdateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdate(update: EventUpdateEntity)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY createdAt DESC LIMIT 100")
    fun getAllActivityLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE entityId = :entityId ORDER BY createdAt DESC")
    fun getActivityLogsForEntity(entityId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE eventId = :eventId ORDER BY timestampMillis ASC")
    fun getChatMessagesForEvent(eventId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)
}

@Dao
interface EventIssueDao {
    @Query("SELECT * FROM event_issues WHERE eventId = :eventId ORDER BY raisedAtMillis DESC")
    fun getIssuesForEvent(eventId: String): Flow<List<EventIssueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: EventIssueEntity)

    @Update
    suspend fun updateIssue(issue: EventIssueEntity)
}

@Dao
interface SchoolSubscriptionDao {
    @Query("SELECT * FROM school_subscriptions WHERE schoolId = :schoolId LIMIT 1")
    fun getSubscriptionForSchool(schoolId: String): Flow<SchoolSubscriptionEntity?>

    @Query("SELECT * FROM school_subscriptions WHERE schoolId = :schoolId LIMIT 1")
    suspend fun getSubscriptionById(schoolId: String): SchoolSubscriptionEntity?

    @Query("SELECT * FROM school_subscriptions ORDER BY schoolName ASC")
    fun getAllSubscriptions(): Flow<List<SchoolSubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SchoolSubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subscriptions: List<SchoolSubscriptionEntity>)

    @Update
    suspend fun updateSubscription(subscription: SchoolSubscriptionEntity)

    @Query("DELETE FROM school_subscriptions WHERE schoolId = :schoolId")
    suspend fun deleteSubscription(schoolId: String)
}

@Dao
interface AdminReportIssueDao {
    @Query("SELECT * FROM admin_report_issues ORDER BY createdAt DESC")
    fun getAllReportedIssues(): Flow<List<AdminReportIssueEntity>>

    @Query("SELECT * FROM admin_report_issues WHERE schoolId = :schoolId ORDER BY createdAt DESC")
    fun getIssuesForSchool(schoolId: String): Flow<List<AdminReportIssueEntity>>

    @Query("SELECT * FROM admin_report_issues WHERE id = :id LIMIT 1")
    suspend fun getIssueById(id: String): AdminReportIssueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: AdminReportIssueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssues(issues: List<AdminReportIssueEntity>)

    @Update
    suspend fun updateIssue(issue: AdminReportIssueEntity)

    @Query("DELETE FROM admin_report_issues WHERE id = :id")
    suspend fun deleteIssueById(id: String)

    @Query("DELETE FROM admin_report_issues WHERE schoolId = :schoolId")
    suspend fun deleteIssuesForSchool(schoolId: String)
}

@Dao
interface DirectMessageDao {
    @Query("SELECT * FROM direct_messages WHERE schoolId = :schoolId ORDER BY timestampMillis ASC")
    fun getMessagesForSchool(schoolId: String): Flow<List<DirectMessageEntity>>

    @Query("SELECT * FROM direct_messages ORDER BY timestampMillis ASC")
    fun getAllMessages(): Flow<List<DirectMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DirectMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<DirectMessageEntity>)

    @Query("DELETE FROM direct_messages WHERE schoolId = :schoolId")
    suspend fun deleteMessagesForSchool(schoolId: String)
}

