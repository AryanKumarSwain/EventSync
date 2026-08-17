package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.EventStatus
import com.example.data.model.EventType
import com.example.data.model.PerformanceStatus
import com.example.data.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val passwordHash: String = "password123",
    val role: UserRole,
    val phone: String = "",
    val profilePhoto: String = "",
    val schoolId: String = "sch_jdis",
    val schoolName: String = "J D International School",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "performance_types")
data class PerformanceTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val schoolId: String = "", // empty = global / all schools, non-empty = specific school admin
    val schoolName: String = "",
    val createdByRole: String = "SUPER_ADMIN", // "SUPER_ADMIN" or "ADMIN"
    val createdByUserId: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val id: String,
    val name: String, // e.g. "Class 8"
    val section: String, // e.g. "A"
    val academicYear: String = "2026-2027",
    val active: Boolean = true
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val admissionNumber: String,
    val classId: String,
    val section: String,
    val contactNumber: String = "",
    val parentContact: String = "",
    val parentName: String = "",
    val photo: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: EventType,
    val description: String = "",
    val date: String, // YYYY-MM-DD or formatted "15 August 2026"
    val startTime: String, // "09:00 AM"
    val endTime: String, // "12:00 PM"
    val venue: String,
    val interPerformanceGapMinutes: Int = 3,
    val actualStartTimeMillis: Long? = null,
    val actualEndTimeMillis: Long? = null,
    val pausedAtMillis: Long? = null,
    val totalPauseDurationMillis: Long = 0L,
    val status: EventStatus = EventStatus.UPCOMING,
    val publicSlug: String,
    val publicEnabled: Boolean = true,
    val publicScheduleEnabled: Boolean = true,
    val publicStatusEnabled: Boolean = true,
    val publicUpdatesEnabled: Boolean = true,
    val winnersAnnouncement: String = "",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "event_eligible_classes", primaryKeys = ["eventId", "classId"])
data class EventEligibleClassCrossRef(
    val eventId: String,
    val classId: String
)

@Entity(tableName = "event_coordinators", primaryKeys = ["eventId", "teacherId"])
data class EventCoordinatorCrossRef(
    val eventId: String,
    val teacherId: String
)

@Entity(tableName = "performances")
data class PerformanceEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val name: String,
    val performanceTypeId: String,
    val sequenceNumber: Int,
    val plannedDurationMinutes: Int,
    val gapMinutes: Int = 3,
    val plannedStartTime: String = "",
    val status: PerformanceStatus = PerformanceStatus.NOT_STARTED,
    val description: String = "",
    val musicName: String = "",
    val youtubeLink: String = "",
    val audioFile: String = "",
    val props: String = "",
    val costume: String = "",
    val stageRequirements: String = "",
    val notes: String = "",
    val isApproved: Boolean = true,
    val progressPercentage: Int = 0,
    val requestedByTeacherName: String = "",
    val requestedByTeacherId: String = "",
    val assignedTeacherIds: String = "",
    val assignedTeacherNames: String = "",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "performance_students", primaryKeys = ["performanceId", "studentId"])
data class PerformanceStudentCrossRef(
    val performanceId: String,
    val studentId: String,
    val roleDescription: String = ""
)

@Entity(tableName = "event_updates")
data class EventUpdateEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val performanceId: String? = null,
    val performanceName: String? = null,
    val userId: String,
    val userName: String,
    val userRole: String,
    val status: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "event_issues")
data class EventIssueEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val performanceId: String? = null,
    val performanceName: String? = null,
    val raisedByUserId: String,
    val raisedByUserName: String,
    val issueText: String,
    val isResolved: Boolean = false,
    val raisedAtMillis: Long = System.currentTimeMillis(),
    val resolvedAtMillis: Long? = null
)

@Entity(tableName = "school_subscriptions")
data class SchoolSubscriptionEntity(
    @PrimaryKey val schoolId: String,
    val schoolName: String,
    val credits: Int = 1,
    val validUntilMillis: Long = System.currentTimeMillis() + (90L * 24 * 3600 * 1000), // 3 months default
    val planName: String = "1 Credit Plan (3 Months)",
    val lastPaymentId: String = "pay_demo_initial",
    val isActive: Boolean = true,
    val isSuspended: Boolean = false,
    val suspensionReason: String = "",
    val suspendedAtMillis: Long? = null,
    val purgeWarningSent: Boolean = false,
    val purgeWarningSentAt: Long? = null,
    val adminEmail: String = "",
    val adminPhone: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_report_issues")
data class AdminReportIssueEntity(
    @PrimaryKey val id: String,
    val schoolId: String,
    val schoolName: String,
    val adminUserId: String,
    val adminName: String,
    val adminContact: String = "",
    val title: String,
    val description: String,
    val category: String = "General Issue",
    val status: String = "OPEN", // "OPEN", "IN_PROGRESS", "RESOLVED"
    val superAdminResponse: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @PrimaryKey val id: String,
    val schoolId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val receiverId: String,
    val message: String,
    val messageType: String = "TEXT", // "TEXT", "PAYMENT_RECEIPT"
    val receiptId: String = "",
    val receiptAmount: Double = 0.0,
    val receiptPlanName: String = "",
    val receiptCredits: Int = 0,
    val receiptPaymentId: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

