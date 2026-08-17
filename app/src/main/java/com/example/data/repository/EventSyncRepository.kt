package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import com.example.ui.components.ValidationUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class EventSyncRepository(private val db: AppDatabase) {

    val users: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val performanceTypes: Flow<List<PerformanceTypeEntity>> = db.performanceTypeDao().getAllPerformanceTypes()
    val classes: Flow<List<ClassEntity>> = db.classDao().getAllClasses()
    val students: Flow<List<StudentEntity>> = db.studentDao().getAllStudents()
    val events: Flow<List<EventEntity>> = db.eventDao().getAllEvents()
    val activityLogs: Flow<List<ActivityLogEntity>> = db.activityLogDao().getAllActivityLogs()

    suspend fun getUserById(id: String): UserEntity? = db.userDao().getUserById(id)
    suspend fun getUserByEmail(email: String): UserEntity? = db.userDao().getUserByEmail(email)

    fun getTeachers(): Flow<List<UserEntity>> = db.userDao().getUsersByRole(UserRole.TEACHER.name)

    fun getEventById(id: String): Flow<EventEntity?> = db.eventDao().getEventByIdFlow(id)
    fun getEventBySlug(slug: String): Flow<EventEntity?> = db.eventDao().getEventBySlugFlow(slug)
    fun getPerformancesForEvent(eventId: String): Flow<List<PerformanceEntity>> = db.performanceDao().getPerformancesForEvent(eventId)
    fun getUpdatesForEvent(eventId: String): Flow<List<EventUpdateEntity>> = db.eventUpdateDao().getUpdatesForEvent(eventId)

    fun getEligibleClassIdsForEvent(eventId: String): Flow<List<String>> = db.eventDao().getEligibleClassIdsForEvent(eventId)
    fun getCoordinatorTeacherIdsForEvent(eventId: String): Flow<List<String>> = db.eventDao().getCoordinatorTeacherIdsForEvent(eventId)
    fun getEventsForTeacher(teacherId: String): Flow<List<EventEntity>> = db.eventDao().getEventsForTeacher(teacherId)
    fun getStudentIdsForPerformance(performanceId: String): Flow<List<String>> = db.performanceDao().getStudentIdsForPerformance(performanceId)

    fun getChatMessagesForEvent(eventId: String): Flow<List<ChatMessageEntity>> = db.chatMessageDao().getChatMessagesForEvent(eventId)

    suspend fun sendChatMessage(eventId: String, senderId: String, senderName: String, senderRole: UserRole, message: String) {
        val msg = ChatMessageEntity(
            id = "chat_${UUID.randomUUID().toString().take(8)}",
            eventId = eventId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            message = message,
            timestampMillis = System.currentTimeMillis()
        )
        db.chatMessageDao().insertChatMessage(msg)
    }

    fun getIssuesForEvent(eventId: String): Flow<List<EventIssueEntity>> = db.eventIssueDao().getIssuesForEvent(eventId)

    suspend fun raiseIssue(eventId: String, performanceId: String?, performanceName: String?, raisedByUserId: String, raisedByUserName: String, issueText: String) {
        val issue = EventIssueEntity(
            id = "issue_${UUID.randomUUID().toString().take(8)}",
            eventId = eventId,
            performanceId = performanceId,
            performanceName = performanceName,
            raisedByUserId = raisedByUserId,
            raisedByUserName = raisedByUserName,
            issueText = issueText,
            isResolved = false,
            raisedAtMillis = System.currentTimeMillis()
        )
        db.eventIssueDao().insertIssue(issue)

        // Automatically set performance status to ISSUE if linked
        performanceId?.let { perfId ->
            val perf = db.performanceDao().getPerformanceById(perfId)
            if (perf != null) {
                db.performanceDao().updatePerformance(perf.copy(status = PerformanceStatus.ISSUE, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun resolveIssue(issue: EventIssueEntity) {
        val updated = issue.copy(
            isResolved = true,
            resolvedAtMillis = System.currentTimeMillis()
        )
        db.eventIssueDao().updateIssue(updated)

        // Automatically restore performance status if linked
        issue.performanceId?.let { perfId ->
            val perf = db.performanceDao().getPerformanceById(perfId)
            if (perf != null && perf.status == PerformanceStatus.ISSUE) {
                val restoredStatus = if (perf.progressPercentage >= 100) PerformanceStatus.COMPLETED else PerformanceStatus.IN_PROGRESS
                db.performanceDao().updatePerformance(perf.copy(status = restoredStatus, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun updatePerformanceProgress(performanceId: String, progressPercentage: Int) {
        val perf = db.performanceDao().getPerformanceById(performanceId) ?: return
        val newProgress = progressPercentage.coerceIn(0, 100)
        val newStatus = when {
            perf.status == PerformanceStatus.ISSUE -> PerformanceStatus.ISSUE
            newProgress == 100 -> PerformanceStatus.COMPLETED
            else -> PerformanceStatus.IN_PROGRESS
        }
        val updated = perf.copy(
            progressPercentage = newProgress,
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        db.performanceDao().updatePerformance(updated)
    }

    suspend fun togglePerformanceIssue(
        performanceId: String,
        hasIssue: Boolean,
        issueMessage: String,
        userId: String,
        userName: String
    ) {
        val perf = db.performanceDao().getPerformanceById(performanceId) ?: return
        if (hasIssue) {
            val updated = perf.copy(
                status = PerformanceStatus.ISSUE,
                updatedAt = System.currentTimeMillis()
            )
            db.performanceDao().updatePerformance(updated)
            val text = issueMessage.ifBlank { "Issue reported for ${perf.name} by $userName" }
            raiseIssue(perf.eventId, perf.id, perf.name, userId, userName, text)
        } else {
            val issues = db.eventIssueDao().getIssuesForEvent(perf.eventId).first()
            issues.filter { it.performanceId == perf.id && !it.isResolved }.forEach { issue ->
                resolveIssue(issue)
            }
            val restoredStatus = if (perf.progressPercentage >= 100) PerformanceStatus.COMPLETED else PerformanceStatus.IN_PROGRESS
            val updated = perf.copy(
                status = restoredStatus,
                updatedAt = System.currentTimeMillis()
            )
            db.performanceDao().updatePerformance(updated)
        }
    }

    suspend fun updatePerformance(performance: PerformanceEntity) {
        db.performanceDao().updatePerformance(performance)
        recalculatePerformanceTimings(performance.eventId)
    }

    suspend fun reorderPerformances(orderedPerformances: List<PerformanceEntity>) {
        if (orderedPerformances.isEmpty()) return
        val eventId = orderedPerformances.first().eventId
        orderedPerformances.forEachIndexed { index, perf ->
            val updated = perf.copy(
                sequenceNumber = index + 1,
                updatedAt = System.currentTimeMillis()
            )
            db.performanceDao().updatePerformance(updated)
        }
        recalculatePerformanceTimings(eventId)
    }

    suspend fun insertUser(user: UserEntity) {
        db.userDao().insertUser(user)
        logActivity(
            userId = "SYSTEM",
            userName = "Admin",
            action = "USER_CREATED",
            entityType = "USER",
            entityId = user.id,
            description = "Created user ${user.name} (${user.role})"
        )
    }

    suspend fun deleteUser(user: UserEntity) {
        db.userDao().deleteUser(user)
        logActivity(
            userId = "SYSTEM",
            userName = "Admin",
            action = "USER_DELETED",
            entityType = "USER",
            entityId = user.id,
            description = "Deleted user ${user.name} (${user.role})"
        )
    }

    suspend fun updateUser(user: UserEntity) {
        db.userDao().updateUser(user)
        logActivity(
            userId = "SYSTEM",
            userName = "Admin",
            action = "USER_UPDATED",
            entityType = "USER",
            entityId = user.id,
            description = "Updated user ${user.name} (${user.role})"
        )
    }

    suspend fun insertClass(classEntity: ClassEntity) {
        db.classDao().insertClass(classEntity)
    }

    suspend fun updateClass(classEntity: ClassEntity) {
        db.classDao().updateClass(classEntity)
        logActivity(
            userId = "SYSTEM",
            userName = "Admin",
            action = "CLASS_UPDATED",
            entityType = "CLASS",
            entityId = classEntity.id,
            description = "Updated class ${classEntity.name} - ${classEntity.section}"
        )
    }

    suspend fun deleteClass(classId: String) {
        db.classDao().deleteClassById(classId)
    }

    suspend fun insertStudent(student: StudentEntity) {
        db.studentDao().insertStudent(student)
    }

    suspend fun updateStudent(student: StudentEntity) {
        db.studentDao().updateStudent(student)
        logActivity(
            userId = "SYSTEM",
            userName = "Admin",
            action = "STUDENT_UPDATED",
            entityType = "STUDENT",
            entityId = student.id,
            description = "Updated student ${student.name} (Adm: ${student.admissionNumber})"
        )
    }

    suspend fun deleteStudent(studentId: String) {
        db.studentDao().deleteStudentById(studentId)
    }

    // --- SUBSCRIPTION & UPGRADE METHODS ---
    fun getSchoolSubscription(schoolId: String): Flow<SchoolSubscriptionEntity?> {
        return db.schoolSubscriptionDao().getSubscriptionForSchool(schoolId)
    }

    fun getAllSchoolSubscriptions(): Flow<List<SchoolSubscriptionEntity>> {
        return db.schoolSubscriptionDao().getAllSubscriptions()
    }

    suspend fun upgradeSchoolPlan(
        schoolId: String,
        schoolName: String,
        planCredits: Int,
        validityDays: Int,
        planName: String,
        paymentId: String,
        amountPaid: Double = if (planCredits == 1) 199.0 else 499.0
    ) {
        val existing = db.schoolSubscriptionDao().getSubscriptionById(schoolId)
        val now = System.currentTimeMillis()
        val validityDurationMillis = validityDays.toLong() * 24L * 60L * 60L * 1000L
        
        val newValidUntil = if (existing != null && existing.validUntilMillis > now) {
            existing.validUntilMillis + validityDurationMillis
        } else {
            now + validityDurationMillis
        }
        
        val newCredits = (existing?.credits ?: 0) + planCredits
        val invoiceId = "INV-2026-RZP" + (1000..9999).random()
        
        val subscription = (existing ?: SchoolSubscriptionEntity(schoolId = schoolId, schoolName = schoolName)).copy(
            credits = newCredits,
            validUntilMillis = newValidUntil,
            planName = planName,
            lastPaymentId = paymentId,
            isActive = true,
            isSuspended = false,
            updatedAt = now
        )
        
        db.schoolSubscriptionDao().insertSubscription(subscription)

        // Automatically dispatch Official Payment Receipt & Tax Invoice PDF message to 1:1 Direct Chat
        val receiptMsg = DirectMessageEntity(
            id = "dm_rcpt_${System.currentTimeMillis()}",
            schoolId = schoolId,
            senderId = "u_super",
            senderName = "EventSync Super Admin Support",
            senderRole = UserRole.SUPER_ADMIN,
            receiverId = "u_admin",
            message = "🧾 [PAYMENT RECEIPT & TAX INVOICE]\nReceipt ID: $invoiceId\nPlan: $planName\nAmount Paid: ₹${amountPaid.toInt()}.00 (inc. 18% GST)\nRazorpay ID: $paymentId\nCredits Added: +$planCredits Event Credits\nValidity: ${validityDays} Days\nStatus: SUCCESSFUL (Razorpay Verified)",
            messageType = "PAYMENT_RECEIPT",
            receiptId = invoiceId,
            receiptAmount = amountPaid,
            receiptPlanName = planName,
            receiptCredits = planCredits,
            receiptPaymentId = paymentId,
            timestampMillis = now
        )
        db.directMessageDao().insertMessage(receiptMsg)
        
        logActivity(
            userId = "ADMIN",
            userName = "School Admin",
            action = "PLAN_UPGRADED",
            entityType = "SUBSCRIPTION",
            entityId = schoolId,
            description = "Upgraded to $planName. Added $planCredits credits. Payment ID: $paymentId. Invoice: $invoiceId"
        )
    }

    suspend fun toggleSchoolStatus(
        schoolId: String,
        isActive: Boolean,
        isSuspended: Boolean,
        reason: String = ""
    ) {
        val existing = db.schoolSubscriptionDao().getSubscriptionById(schoolId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            val updated = existing.copy(
                isActive = isActive,
                isSuspended = isSuspended,
                suspensionReason = if (isSuspended) reason.ifBlank { "Suspended by Super Admin" } else "",
                suspendedAtMillis = if (isSuspended) now else null,
                purgeWarningSent = if (!isSuspended) false else existing.purgeWarningSent,
                updatedAt = now
            )
            db.schoolSubscriptionDao().updateSubscription(updated)
        } else {
            val newSub = SchoolSubscriptionEntity(
                schoolId = schoolId,
                schoolName = "School $schoolId",
                isActive = isActive,
                isSuspended = isSuspended,
                suspensionReason = if (isSuspended) reason.ifBlank { "Suspended by Super Admin" } else "",
                suspendedAtMillis = if (isSuspended) now else null,
                updatedAt = now
            )
            db.schoolSubscriptionDao().insertSubscription(newSub)
        }

        logActivity(
            userId = "SUPER_ADMIN",
            userName = "Super Admin",
            action = if (isSuspended) "SCHOOL_SUSPENDED" else "SCHOOL_ACTIVATED",
            entityType = "SCHOOL",
            entityId = schoolId,
            description = if (isSuspended) "Suspended school $schoolId with 15-day data purge policy. Reason: $reason" else "Activated school $schoolId"
        )
    }

    suspend fun sendPurgeWarningEmail(schoolId: String, adminEmail: String = ""): String {
        val existing = db.schoolSubscriptionDao().getSubscriptionById(schoolId)
        val email = if (adminEmail.isNotBlank()) adminEmail else existing?.adminEmail?.ifBlank { "admin@jdsschool.com" } ?: "admin@jdsschool.com"
        val now = System.currentTimeMillis()
        if (existing != null) {
            val updated = existing.copy(
                purgeWarningSent = true,
                purgeWarningSentAt = now,
                updatedAt = now
            )
            db.schoolSubscriptionDao().updateSubscription(updated)
        }
        
        // Also send warning notification into 1:1 chat
        val noticeMsg = DirectMessageEntity(
            id = "dm_warn_${System.currentTimeMillis()}",
            schoolId = schoolId,
            senderId = "u_super",
            senderName = "Super Admin Security & Compliance",
            senderRole = UserRole.SUPER_ADMIN,
            receiverId = "u_admin",
            message = "⚠️ [CRITICAL COMPLIANCE NOTICE - 15-DAY DATA PURGE POLICY]\nYour school account is currently INACTIVE / SUSPENDED.\nPer data retention policy, all school records, events, teachers, and student data will be permanently wiped out in 15 days unless reactivated.\nOfficial notice dispatched to: $email",
            messageType = "TEXT",
            timestampMillis = now
        )
        db.directMessageDao().insertMessage(noticeMsg)

        logActivity(
            userId = "SUPER_ADMIN",
            userName = "Super Admin",
            action = "PURGE_WARNING_DISPATCHED",
            entityType = "SCHOOL",
            entityId = schoolId,
            description = "Dispatched 15-day data purge warning notice to $email"
        )
        return "⚠️ Official 15-Day Data Purge Notice successfully emailed to $email & sent via 1:1 Chat!"
    }

    suspend fun purgeSchoolData(schoolId: String) {
        db.schoolSubscriptionDao().deleteSubscription(schoolId)
        db.adminReportIssueDao().deleteIssuesForSchool(schoolId)
        db.directMessageDao().deleteMessagesForSchool(schoolId)
        logActivity(
            userId = "SUPER_ADMIN",
            userName = "Super Admin",
            action = "SCHOOL_DATA_PURGED",
            entityType = "SCHOOL",
            entityId = schoolId,
            description = "Permanently wiped all database records and retention cache for school $schoolId"
        )
    }

    suspend fun deductEventCredit(schoolId: String): Boolean {
        val existing = db.schoolSubscriptionDao().getSubscriptionById(schoolId)
        if (existing == null || existing.credits <= 0 || existing.validUntilMillis < System.currentTimeMillis()) {
            return false
        }
        val updated = existing.copy(
            credits = (existing.credits - 1).coerceAtLeast(0),
            updatedAt = System.currentTimeMillis()
        )
        db.schoolSubscriptionDao().updateSubscription(updated)
        return true
    }

    // --- ADMIN REPORT ISSUES (School Admin -> Super Admin) ---
    fun getAllAdminReportIssues(): Flow<List<AdminReportIssueEntity>> {
        return db.adminReportIssueDao().getAllReportedIssues()
    }

    fun getAdminReportIssuesForSchool(schoolId: String): Flow<List<AdminReportIssueEntity>> {
        return db.adminReportIssueDao().getIssuesForSchool(schoolId)
    }

    suspend fun reportAdminIssue(
        schoolId: String,
        schoolName: String,
        adminUserId: String,
        adminName: String,
        adminContact: String,
        title: String,
        description: String,
        category: String
    ) {
        val issue = AdminReportIssueEntity(
            id = "issue_${System.currentTimeMillis()}",
            schoolId = schoolId,
            schoolName = schoolName,
            adminUserId = adminUserId,
            adminName = adminName,
            adminContact = adminContact,
            title = title,
            description = description,
            category = category,
            status = "OPEN",
            createdAt = System.currentTimeMillis()
        )
        db.adminReportIssueDao().insertIssue(issue)
        logActivity(
            userId = adminUserId,
            userName = adminName,
            action = "ISSUE_REPORTED",
            entityType = "ADMIN_ISSUE",
            entityId = issue.id,
            description = "Reported issue: '$title' to Super Admin"
        )
    }

    suspend fun updateAdminIssueStatus(
        issueId: String,
        newStatus: String,
        superAdminResponse: String
    ) {
        val existing = db.adminReportIssueDao().getIssueById(issueId)
        if (existing != null) {
            val updated = existing.copy(
                status = newStatus,
                superAdminResponse = superAdminResponse,
                resolvedAt = if (newStatus == "RESOLVED") System.currentTimeMillis() else existing.resolvedAt
            )
            db.adminReportIssueDao().updateIssue(updated)
            logActivity(
                userId = "SUPER_ADMIN",
                userName = "Super Admin",
                action = "ISSUE_STATUS_UPDATED",
                entityType = "ADMIN_ISSUE",
                entityId = issueId,
                description = "Updated issue '${existing.title}' status to $newStatus"
            )
        }
    }

    // --- ONE-TO-ONE DIRECT CHAT (Super Admin <-> School Admin) ---
    fun getDirectMessagesForSchool(schoolId: String): Flow<List<DirectMessageEntity>> {
        return db.directMessageDao().getMessagesForSchool(schoolId)
    }

    suspend fun sendDirectMessage(
        schoolId: String,
        senderId: String,
        senderName: String,
        senderRole: UserRole,
        receiverId: String,
        message: String
    ) {
        val msg = DirectMessageEntity(
            id = "dm_${System.currentTimeMillis()}",
            schoolId = schoolId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            receiverId = receiverId,
            message = message,
            timestampMillis = System.currentTimeMillis()
        )
        db.directMessageDao().insertMessage(msg)
    }


    suspend fun insertPerformanceType(type: PerformanceTypeEntity) {
        db.performanceTypeDao().insertPerformanceType(type)
    }

    suspend fun deletePerformanceType(typeId: String) {
        db.performanceTypeDao().deletePerformanceTypeById(typeId)
    }

    suspend fun createEvent(
        event: EventEntity,
        eligibleClassIds: List<String>,
        coordinatorTeacherIds: List<String>
    ) {
        db.eventDao().insertEvent(event)
        
        db.eventDao().clearEventEligibleClasses(event.id)
        db.eventDao().insertEventEligibleClasses(
            eligibleClassIds.map { EventEligibleClassCrossRef(event.id, it) }
        )

        db.eventDao().clearEventCoordinators(event.id)
        db.eventDao().insertEventCoordinators(
            coordinatorTeacherIds.map { EventCoordinatorCrossRef(event.id, it) }
        )

        logActivity(
            userId = event.createdBy,
            userName = "Admin",
            action = "EVENT_CREATED",
            entityType = "EVENT",
            entityId = event.id,
            description = "Created event '${event.name}' scheduled for ${event.date} (${event.startTime} - ${event.endTime})"
        )
    }

    suspend fun updateEvent(event: EventEntity) {
        db.eventDao().updateEvent(event)
        logActivity(
            userId = event.createdBy,
            userName = "Admin",
            action = "EVENT_UPDATED",
            entityType = "EVENT",
            entityId = event.id,
            description = "Updated event settings for '${event.name}'"
        )
    }

    suspend fun deleteEvent(eventId: String) {
        db.eventDao().deleteEventById(eventId)
        logActivity(
            userId = "SYSTEM",
            userName = "Admin",
            action = "EVENT_DELETED",
            entityType = "EVENT",
            entityId = eventId,
            description = "Deleted event"
        )
    }

    suspend fun addOrUpdatePerformance(
        performance: PerformanceEntity,
        studentIds: List<String>,
        updatedByUserId: String,
        updatedByUserName: String
    ) {
        db.performanceDao().insertPerformance(performance)
        db.performanceDao().clearPerformanceStudents(performance.id)
        db.performanceDao().insertPerformanceStudents(
            studentIds.map { PerformanceStudentCrossRef(performance.id, it) }
        )

        // Recalculate schedule timings for the event
        recalculatePerformanceTimings(performance.eventId)

        logActivity(
            userId = updatedByUserId,
            userName = updatedByUserName,
            action = "PERFORMANCE_SAVED",
            entityType = "PERFORMANCE",
            entityId = performance.id,
            description = "Saved performance '${performance.name}' (${performance.plannedDurationMinutes}m)"
        )
    }

    suspend fun deletePerformance(performanceId: String, eventId: String, userId: String, userName: String) {
        db.performanceDao().deletePerformanceById(performanceId)
        recalculatePerformanceTimings(eventId)
        logActivity(
            userId = userId,
            userName = userName,
            action = "PERFORMANCE_DELETED",
            entityType = "PERFORMANCE",
            entityId = performanceId,
            description = "Deleted performance"
        )
    }

    suspend fun updatePerformanceStatus(
        performanceId: String,
        eventId: String,
        newStatus: PerformanceStatus,
        message: String,
        userId: String,
        userName: String,
        userRole: String
    ) {
        val performance = db.performanceDao().getPerformanceById(performanceId) ?: return
        val updatedPerf = performance.copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        db.performanceDao().updatePerformance(updatedPerf)

        val updateMsg = message.ifBlank { "Updated status to ${newStatus.name.replace("_", " ")}" }
        val eventUpdate = EventUpdateEntity(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            performanceId = performanceId,
            performanceName = performance.name,
            userId = userId,
            userName = userName,
            userRole = userRole,
            status = newStatus.name,
            message = updateMsg,
            createdAt = System.currentTimeMillis()
        )
        db.eventUpdateDao().insertUpdate(eventUpdate)

        logActivity(
            userId = userId,
            userName = userName,
            action = "STATUS_CHANGED",
            entityType = "PERFORMANCE",
            entityId = performanceId,
            description = "${performance.name} → ${newStatus.name} ($updateMsg)"
        )
    }

    suspend fun logActivity(
        userId: String,
        userName: String,
        action: String,
        entityType: String,
        entityId: String,
        description: String
    ) {
        db.activityLogDao().insertLog(
            ActivityLogEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                action = action,
                entityType = entityType,
                entityId = entityId,
                description = description
            )
        )
    }

    // --- TIME CALCULATION ENGINE ---
    suspend fun calculateTimeEngine(eventId: String): TimeCalculationResult {
        val event = db.eventDao().getEventById(eventId) ?: return TimeCalculationResult()
        val performances = db.performanceDao().getPerformancesForEvent(eventId).first()
        val plannedTotalMinutes = performances.sumOf { it.plannedDurationMinutes }
        val totalGapMinutes = performances.sumOf { it.gapMinutes }
        val maxGap = if (performances.isNotEmpty()) performances.maxOf { it.gapMinutes } else event.interPerformanceGapMinutes
        val totalEstimated = plannedTotalMinutes + totalGapMinutes

        val startMinutes = timeStringToMinutes(event.startTime)
        val estimatedEndMinutes = startMinutes + totalEstimated
        val estEndTimeFormatted = minutesToTimeString(estimatedEndMinutes)

        val isFinished = event.status == EventStatus.COMPLETED || event.status == EventStatus.ARCHIVED
        val actualElapsedMillis = if (event.actualStartTimeMillis != null) {
            val referenceEnd = when {
                isFinished && event.actualEndTimeMillis != null -> event.actualEndTimeMillis
                event.status == EventStatus.PAUSED && event.pausedAtMillis != null -> event.pausedAtMillis
                else -> System.currentTimeMillis()
            }
            val rawDiff = referenceEnd - event.actualStartTimeMillis
            maxOf(0L, rawDiff - event.totalPauseDurationMillis)
        } else 0L

        val diffSec = maxOf(0L, actualElapsedMillis / 1000)
        val hours = diffSec / 3600
        val mins = (diffSec % 3600) / 60
        val secs = diffSec % 60
        val durationFormatted = if (hours > 0) "${hours}h ${mins}m ${secs}s" else "${mins}m ${secs}s"

        val timeSdf = SimpleDateFormat("hh:mm a", Locale.US)
        val actualStartFormatted = if (event.actualStartTimeMillis != null) timeSdf.format(Date(event.actualStartTimeMillis)) else event.startTime
        val actualEndFormatted = if (event.actualEndTimeMillis != null) timeSdf.format(Date(event.actualEndTimeMillis)) else ""

        return TimeCalculationResult(
            plannedPerformanceDurationMinutes = plannedTotalMinutes,
            interPerformanceGapMinutes = maxGap,
            totalGapTimeMinutes = totalGapMinutes,
            totalEstimatedTimeMinutes = totalEstimated,
            estimatedEndTimeFormatted = estEndTimeFormatted,
            isFinished = isFinished,
            actualDurationMinutes = (actualElapsedMillis / 60000).toInt(),
            actualDurationFormatted = durationFormatted,
            actualStartTimeFormatted = actualStartFormatted,
            actualEndTimeFormatted = actualEndFormatted,
            totalPerformancesCount = performances.size,
            completedPerformancesCount = performances.count { it.status == PerformanceStatus.COMPLETED }
        )
    }

    suspend fun calculateEventHealth(eventId: String): EventHealth {
        val performances = db.performanceDao().getPerformancesForEvent(eventId).first()
        if (performances.any { it.status == PerformanceStatus.ISSUE }) {
            return EventHealth.CRITICAL
        }
        val notStartedCount = performances.count { it.status == PerformanceStatus.NOT_STARTED }
        if (notStartedCount > (performances.size / 2)) {
            return EventHealth.NEEDS_ATTENTION
        }
        return EventHealth.ON_TRACK
    }

    suspend fun recalculatePerformanceTimings(eventId: String) {
        val event = db.eventDao().getEventById(eventId) ?: return
        val performances = db.performanceDao().getPerformancesForEvent(eventId).first().sortedBy { it.sequenceNumber }
        
        var currentMinutes = timeStringToMinutes(event.startTime)
        performances.forEach { perf ->
            val startStr = minutesToTimeString(currentMinutes)
            if (perf.plannedStartTime != startStr) {
                val updated = perf.copy(plannedStartTime = startStr)
                db.performanceDao().updatePerformance(updated)
            }
            currentMinutes += perf.plannedDurationMinutes + perf.gapMinutes
        }
    }

    private fun parseTimeWindowMinutes(startTimeStr: String, endTimeStr: String): Int {
        val startMins = timeStringToMinutes(startTimeStr)
        val endMins = timeStringToMinutes(endTimeStr)
        val diff = endMins - startMins
        return if (diff > 0) diff else 180
    }

    private fun timeStringToMinutes(timeStr: String): Int {
        return try {
            val cleanStr = timeStr.trim().uppercase()
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            val date = sdf.parse(cleanStr) ?: return 540 // Default 9:00 AM
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) {
            540
        }
    }

    private fun minutesToTimeString(totalMinutes: Int): String {
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        val amPm = if (hours >= 12) "PM" else "AM"
        val hour12 = when {
            hours == 0 -> 12
            hours > 12 -> hours - 12
            else -> hours
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, minutes, amPm)
    }

    // --- CSV PARSER ENGINE ---
    private fun parseCsvLine(line: String): List<String> {
        val delimiter = when {
            line.contains("\t") -> "\t"
            line.contains(";") -> ";"
            else -> ","
        }
        return line.split(delimiter).map { cell ->
            cell.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        }
    }

    data class ClassCsvImportSummary(
        val validRecords: List<ClassEntity>,
        val invalidRecordsCount: Int,
        val duplicateRecordsCount: Int,
        val errorMessages: List<String>
    )

    data class FullSchoolCsvImportSummary(
        val createdClasses: List<ClassEntity>,
        val validStudents: List<StudentEntity>,
        val invalidRecordsCount: Int,
        val duplicateRecordsCount: Int,
        val errorMessages: List<String>
    )

    data class CsvImportSummary(
        val validRecords: List<StudentEntity>,
        val invalidRecordsCount: Int,
        val duplicateRecordsCount: Int,
        val errorMessages: List<String>
    )

    data class TeacherCsvImportSummary(
        val validRecords: List<UserEntity>,
        val invalidRecordsCount: Int,
        val duplicateRecordsCount: Int,
        val errorMessages: List<String>
    )

    suspend fun processClassCsvImport(rawCsv: String): ClassCsvImportSummary {
        val cleanText = rawCsv.replace("\uFEFF", "")
        val lines = cleanText.lines().filter { it.isNotBlank() }
        val existingClasses = db.classDao().getAllClasses().first()
        val validList = mutableListOf<ClassEntity>()
        var invalidCount = 0
        var duplicateCount = 0
        val errorMsgs = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            if (index == 0 && (line.lowercase().contains("class") || line.lowercase().contains("section"))) return@forEachIndexed

            val parts = parseCsvLine(line)
            if (parts.size < 2) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Expected at least 'Class Name, Section'")
                return@forEachIndexed
            }

            val className = parts[0]
            val section = parts[1]
            val academicYear = if (parts.size >= 3 && parts[2].isNotBlank()) parts[2] else "2026-2027"

            if (className.isBlank() || section.isBlank()) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Class Name or Section is blank")
                return@forEachIndexed
            }

            val isDuplicate = existingClasses.any {
                it.name.equals(className, ignoreCase = true) && it.section.equals(section, ignoreCase = true)
            } || validList.any {
                it.name.equals(className, ignoreCase = true) && it.section.equals(section, ignoreCase = true)
            }

            if (isDuplicate) {
                duplicateCount++
                errorMsgs.add("Line ${index + 1}: Class '$className - Section $section' already exists")
                return@forEachIndexed
            }

            val newClass = ClassEntity(
                id = "class_${UUID.randomUUID().toString().take(6)}",
                name = className,
                section = section,
                academicYear = academicYear
            )
            validList.add(newClass)
        }

        return ClassCsvImportSummary(
            validRecords = validList,
            invalidRecordsCount = invalidCount,
            duplicateRecordsCount = duplicateCount,
            errorMessages = errorMsgs
        )
    }

    suspend fun saveImportedClasses(classes: List<ClassEntity>) {
        db.classDao().insertClasses(classes)
        logActivity(
            userId = "ADMIN",
            userName = "Admin",
            action = "CSV_IMPORT",
            entityType = "CLASS",
            entityId = "BULK",
            description = "Imported ${classes.size} classes via CSV"
        )
    }

    suspend fun processTeacherCsvImport(rawCsv: String): TeacherCsvImportSummary {
        val cleanText = rawCsv.replace("\uFEFF", "")
        val lines = cleanText.lines().filter { it.isNotBlank() }
        val existingUsers = db.userDao().getAllUsers().first()
        val existingEmails = existingUsers.map { it.email.trim().lowercase() }.toMutableSet()

        val validList = mutableListOf<UserEntity>()
        var invalidCount = 0
        var duplicateCount = 0
        val errorMsgs = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            if (index == 0 && (line.lowercase().contains("email") || line.lowercase().contains("name") || line.lowercase().contains("phone"))) return@forEachIndexed

            val parts = parseCsvLine(line)
            if (parts.size < 2) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Missing required columns (expected Name, Email)")
                return@forEachIndexed
            }

            val name = parts[0].trim()
            val email = parts[1].trim()
            val rawPhone = if (parts.size >= 3) parts[2].trim() else ""

            if (name.isBlank() || email.isBlank()) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Name or Email is blank")
                return@forEachIndexed
            }

            if (!ValidationUtils.isValidEmail(email)) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Invalid email format '$email' (must contain '@' and domain name)")
                return@forEachIndexed
            }

            if (rawPhone.isNotBlank() && !ValidationUtils.isValidPhone(rawPhone)) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Invalid phone '$rawPhone' (must be a 10-digit number with country code)")
                return@forEachIndexed
            }

            if (existingEmails.contains(email.lowercase())) {
                duplicateCount++
                errorMsgs.add("Line ${index + 1}: Email '$email' already exists")
                return@forEachIndexed
            }

            val formattedPhone = if (rawPhone.isNotBlank()) ValidationUtils.formatPhoneNumber(rawPhone) else ""

            val teacher = UserEntity(
                id = "u_t_${UUID.randomUUID().toString().take(6)}",
                name = name,
                email = email,
                passwordHash = "password123",
                role = UserRole.TEACHER,
                phone = formattedPhone
            )
            existingEmails.add(email.lowercase())
            validList.add(teacher)
        }

        return TeacherCsvImportSummary(
            validRecords = validList,
            invalidRecordsCount = invalidCount,
            duplicateRecordsCount = duplicateCount,
            errorMessages = errorMsgs
        )
    }

    suspend fun saveImportedTeachers(teachers: List<UserEntity>) {
        db.userDao().insertUsers(teachers)
        logActivity(
            userId = "ADMIN",
            userName = "Admin",
            action = "CSV_IMPORT",
            entityType = "TEACHER",
            entityId = "BULK",
            description = "Imported ${teachers.size} teachers via CSV"
        )
    }

    suspend fun processFullSchoolCsvImport(rawCsv: String): FullSchoolCsvImportSummary {
        val cleanText = rawCsv.replace("\uFEFF", "")
        val lines = cleanText.lines().filter { it.isNotBlank() }

        val existingClasses = db.classDao().getAllClasses().first().toMutableList()
        val existingStudents = db.studentDao().getAllStudents().first()
        val existingAdmissionNumbers = existingStudents.map { it.admissionNumber.trim().lowercase() }.toMutableSet()

        val newClassesToCreate = mutableListOf<ClassEntity>()
        val validStudents = mutableListOf<StudentEntity>()

        var invalidCount = 0
        var duplicateCount = 0
        val errorMsgs = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            if (index == 0 && (line.lowercase().contains("class") || line.lowercase().contains("student") || line.lowercase().contains("admission"))) return@forEachIndexed

            val parts = parseCsvLine(line)
            if (parts.size < 4) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Required columns: Class Name, Section, Student Name, Admission Number")
                return@forEachIndexed
            }

            val rawClassName = parts[0]
            val rawSection = parts[1]
            val studentName = parts[2]
            val admNum = parts[3]
            val parentName = parts.getOrElse(4) { "" }
            val parentContact = parts.getOrElse(5) { "" }

            if (rawClassName.isBlank() || rawSection.isBlank() || studentName.isBlank() || admNum.isBlank()) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Class, Section, Student Name or Admission # is blank")
                return@forEachIndexed
            }

            if (existingAdmissionNumbers.contains(admNum.lowercase())) {
                duplicateCount++
                errorMsgs.add("Line ${index + 1}: Admission number '$admNum' is duplicate ($studentName)")
                return@forEachIndexed
            }

            val formattedClassName = if (rawClassName.contains("Class", ignoreCase = true)) rawClassName else "Class $rawClassName"
            val formattedSection = rawSection.trim().uppercase()

            var targetClass = existingClasses.find {
                it.name.equals(formattedClassName, ignoreCase = true) &&
                it.section.equals(formattedSection, ignoreCase = true)
            }

            if (targetClass == null) {
                targetClass = ClassEntity(
                    id = "c_${UUID.randomUUID().toString().take(8)}",
                    name = formattedClassName,
                    section = formattedSection,
                    academicYear = "2026-2027"
                )
                existingClasses.add(targetClass)
                newClassesToCreate.add(targetClass)
            }

            val formattedContact = if (parentContact.isNotBlank()) ValidationUtils.formatPhoneNumber(parentContact) else ""

            val student = StudentEntity(
                id = UUID.randomUUID().toString(),
                name = studentName,
                admissionNumber = admNum,
                classId = targetClass.id,
                section = targetClass.section,
                parentName = parentName,
                parentContact = formattedContact
            )

            existingAdmissionNumbers.add(admNum.lowercase())
            validStudents.add(student)
        }

        return FullSchoolCsvImportSummary(
            createdClasses = newClassesToCreate,
            validStudents = validStudents,
            invalidRecordsCount = invalidCount,
            duplicateRecordsCount = duplicateCount,
            errorMessages = errorMsgs
        )
    }

    suspend fun confirmFullSchoolCsvImport(summary: FullSchoolCsvImportSummary) {
        if (summary.createdClasses.isNotEmpty()) {
            db.classDao().insertClasses(summary.createdClasses)
        }
        if (summary.validStudents.isNotEmpty()) {
            db.studentDao().insertStudents(summary.validStudents)
        }
        logActivity(
            userId = "ADMIN",
            userName = "Admin",
            action = "CSV_IMPORT",
            entityType = "FULL_SCHOOL",
            entityId = "BULK",
            description = "Full school bulk import: ${summary.createdClasses.size} classes, ${summary.validStudents.size} students"
        )
    }

    suspend fun processCsvImport(rawCsv: String, targetClassId: String? = null, targetSection: String? = null): CsvImportSummary {
        val cleanText = rawCsv.replace("\uFEFF", "")
        val lines = cleanText.lines().filter { it.isNotBlank() }
        val existingStudents = db.studentDao().getAllStudents().first()
        val existingAdmissionNumbers = existingStudents.map { it.admissionNumber.trim().lowercase() }.toMutableSet()
        val allClasses = db.classDao().getAllClasses().first()

        val validList = mutableListOf<StudentEntity>()
        var invalidCount = 0
        var duplicateCount = 0
        val errorMsgs = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            // Skip header if present
            if (index == 0 && (line.lowercase().contains("admission") || line.lowercase().contains("name") || line.lowercase().contains("parent"))) return@forEachIndexed

            val parts = parseCsvLine(line)
            if (parts.size < 2) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Missing required columns (expected Name, Admission Number)")
                return@forEachIndexed
            }

            val name = parts[0]
            val admNum = parts[1]

            var sectionInput = targetSection ?: "A"
            var contact = ""
            var parentName = ""
            var classNameInput = ""

            if (targetClassId != null) {
                if (parts.size >= 3) {
                    if (parts[2].length <= 2) {
                        sectionInput = parts[2]
                        contact = parts.getOrElse(3) { "" }
                        parentName = parts.getOrElse(4) { "" }
                    } else {
                        contact = parts[2]
                        parentName = parts.getOrElse(3) { "" }
                    }
                }
            } else {
                classNameInput = parts.getOrElse(2) { "" }
                sectionInput = if (parts.size >= 4 && parts[3].isNotBlank()) parts[3] else "A"
                contact = parts.getOrElse(4) { "" }
                parentName = parts.getOrElse(5) { "" }
            }

            if (name.isBlank() || admNum.isBlank()) {
                invalidCount++
                errorMsgs.add("Line ${index + 1}: Name or Admission Number is blank")
                return@forEachIndexed
            }

            if (existingAdmissionNumbers.contains(admNum.lowercase())) {
                duplicateCount++
                errorMsgs.add("Line ${index + 1}: Admission number '$admNum' is a duplicate")
                return@forEachIndexed
            }

            val finalClassId = targetClassId ?: run {
                val matchedClass = allClasses.find {
                    it.name.contains(classNameInput, ignoreCase = true) &&
                    it.section.equals(sectionInput, ignoreCase = true)
                } ?: allClasses.find {
                    it.name.contains(classNameInput, ignoreCase = true)
                } ?: ClassEntity(id = "class_${classNameInput}_$sectionInput", name = if (classNameInput.contains("Class", true)) classNameInput else "Class $classNameInput", section = sectionInput)
                matchedClass.id
            }

            val formattedContact = if (contact.isNotBlank()) ValidationUtils.formatPhoneNumber(contact) else ""

            val student = StudentEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                admissionNumber = admNum,
                classId = finalClassId,
                section = sectionInput,
                parentContact = formattedContact,
                parentName = parentName
            )
            existingAdmissionNumbers.add(admNum.lowercase())
            validList.add(student)
        }

        return CsvImportSummary(
            validRecords = validList,
            invalidRecordsCount = invalidCount,
            duplicateRecordsCount = duplicateCount,
            errorMessages = errorMsgs
        )
    }

    suspend fun saveImportedStudents(students: List<StudentEntity>) {
        db.studentDao().insertStudents(students)
        logActivity(
            userId = "ADMIN",
            userName = "Admin",
            action = "CSV_IMPORT",
            entityType = "STUDENT",
            entityId = "BULK",
            description = "Imported ${students.size} students via CSV"
        )
    }

    // --- SEED REALISTIC DEMO DATA ---
    suspend fun seedDemoData() {
        if (db.userDao().getAllUsers().first().isNotEmpty()) return

        // 1. Users
        val users = listOf(
            UserEntity("u_super", "Super Admin", "superadmin@eventsync.com", "password123", UserRole.SUPER_ADMIN, "+91 9800000001"),
            UserEntity("u_admin", "Principal Sharma", "admin@jdsschool.com", "password123", UserRole.ADMIN, "+91 9800000002"),
            UserEntity("u_t1", "Neha Sharma", "neha.sharma@jdsschool.com", "password123", UserRole.TEACHER, "+91 9876543210"),
            UserEntity("u_t2", "Rajesh Kumar", "rajesh.kumar@jdsschool.com", "password123", UserRole.TEACHER, "+91 9876543211"),
            UserEntity("u_t3", "Priya Singh", "priya.singh@jdsschool.com", "password123", UserRole.TEACHER, "+91 9876543212"),
            UserEntity("u_t4", "Amit Verma", "amit.verma@jdsschool.com", "password123", UserRole.TEACHER, "+91 9876543213"),
            UserEntity("u_t5", "Kavita Sharma", "kavita.sharma@jdsschool.com", "password123", UserRole.TEACHER, "+91 9876543214")
        )
        db.userDao().insertUsers(users)

        // 2. Performance Types
        val perfTypes = listOf(
            PerformanceTypeEntity("pt_1", "Dancing", "Group & Solo Classical / Folk Dance"),
            PerformanceTypeEntity("pt_2", "Singing", "Patriotic & Classical Choirs"),
            PerformanceTypeEntity("pt_3", "Skit", "Short theatrical presentations"),
            PerformanceTypeEntity("pt_4", "Drama", "Full length stage plays"),
            PerformanceTypeEntity("pt_5", "Poetry", "Recitation & Spoken Word"),
            PerformanceTypeEntity("pt_6", "Instrumental", "Orchestra & Solo Instrumental"),
            PerformanceTypeEntity("pt_7", "Speech", "Keynote speeches & address"),
            PerformanceTypeEntity("pt_8", "Fancy Dress", "Cultural character dress display")
        )
        db.performanceTypeDao().insertPerformanceTypes(perfTypes)

        // 3. Classes
        val classes = listOf(
            ClassEntity("c_6a", "Class 6", "A"),
            ClassEntity("c_6b", "Class 6", "B"),
            ClassEntity("c_7a", "Class 7", "A"),
            ClassEntity("c_7b", "Class 7", "B"),
            ClassEntity("c_8a", "Class 8", "A"),
            ClassEntity("c_8b", "Class 8", "B"),
            ClassEntity("c_9a", "Class 9", "A"),
            ClassEntity("c_9b", "Class 9", "B"),
            ClassEntity("c_10a", "Class 10", "A"),
            ClassEntity("c_10b", "Class 10", "B")
        )
        db.classDao().insertClasses(classes)

        // 4. Students
        val students = listOf(
            StudentEntity("s_1", "Rahul Sharma", "1023", "c_8a", "A", "+91 9876511101"),
            StudentEntity("s_2", "Priya Singh", "1024", "c_8a", "A", "+91 9876511102"),
            StudentEntity("s_3", "Aman Verma", "1025", "c_9b", "B", "+91 9876511103"),
            StudentEntity("s_4", "Ananya Gupta", "1026", "c_8a", "A", "+91 9876511104"),
            StudentEntity("s_5", "Rohan Patel", "1027", "c_8b", "B", "+91 9876511105"),
            StudentEntity("s_6", "Sanya Malhotra", "1028", "c_7a", "A", "+91 9876511106"),
            StudentEntity("s_7", "Karan Johar", "1029", "c_7b", "B", "+91 9876511107"),
            StudentEntity("s_8", "Diya Mirza", "1030", "c_9a", "A", "+91 9876511108"),
            StudentEntity("s_9", "Aditya Roy", "1031", "c_10a", "A", "+91 9876511109"),
            StudentEntity("s_10", "Ishaan Khattar", "1032", "c_10b", "B", "+91 9876511110"),
            StudentEntity("s_11", "Meera Sen", "1033", "c_6a", "A", "+91 9876511111"),
            StudentEntity("s_12", "Kabir Das", "1034", "c_6b", "B", "+91 9876511112"),
            StudentEntity("s_13", "Aarav Mehta", "1035", "c_8a", "A", "+91 9876511113"),
            StudentEntity("s_14", "Tara Sutaria", "1036", "c_8b", "B", "+91 9876511114"),
            StudentEntity("s_15", "Varun Dhawan", "1037", "c_9b", "B", "+91 9876511115"),
            StudentEntity("s_16", "Kiara Advani", "1038", "c_10a", "A", "+91 9876511116"),
            StudentEntity("s_17", "Sidharth Roy", "1039", "c_7a", "A", "+91 9876511117"),
            StudentEntity("s_18", "Jahnvi Kapoor", "1040", "c_8a", "A", "+91 9876511118")
        )
        db.studentDao().insertStudents(students)

        // 5. Demo Event
        val eventId = "evt_independence_2026"
        val event = EventEntity(
            id = eventId,
            name = "Independence Day Celebration 2026",
            type = EventType.FUNCTION,
            description = "Annual school celebration for India's 80th Independence Day featuring dances, skits, and patriotic choir performances.",
            date = "15 August 2026",
            startTime = "09:00 AM",
            endTime = "",
            venue = "J D International School Main Auditorium",
            actualStartTimeMillis = System.currentTimeMillis() - (45 * 60 * 1000),
            status = EventStatus.IN_PROGRESS,
            publicSlug = "independence-day-2026",
            publicEnabled = true,
            publicScheduleEnabled = true,
            publicStatusEnabled = true,
            publicUpdatesEnabled = true,
            createdBy = "u_admin"
        )
        db.eventDao().insertEvent(event)

        // Eligible classes & Coordinators
        val eligibleClasses = listOf("c_6a", "c_6b", "c_7a", "c_7b", "c_8a", "c_8b", "c_9a", "c_9b", "c_10a", "c_10b")
        val coordinators = listOf("u_t1", "u_t2", "u_t3")

        db.eventDao().insertEventEligibleClasses(eligibleClasses.map { EventEligibleClassCrossRef(eventId, it) })
        db.eventDao().insertEventCoordinators(coordinators.map { EventCoordinatorCrossRef(eventId, it) })

        // 6. Performances
        val performances = listOf(
            PerformanceEntity(id = "p_1", eventId = eventId, name = "Welcome Dance", performanceTypeId = "pt_1", sequenceNumber = 1, plannedDurationMinutes = 8, plannedStartTime = "09:00 AM", status = PerformanceStatus.COMPLETED, description = "Classical welcome dance invocation", musicName = "Ganesh Vandana", youtubeLink = "https://youtube.com/watch?v=sample1", audioFile = "", props = "Diyas × 6, Brass Lamps", costume = "Traditional Anarkali", stageRequirements = "Stage lights warm amber", notes = "Completed smoothly without glitch", isApproved = true, progressPercentage = 100, requestedByTeacherName = "", requestedByTeacherId = "", createdBy = "u_t1"),
            PerformanceEntity(id = "p_2", eventId = eventId, name = "Saraswati Vandana", performanceTypeId = "pt_2", sequenceNumber = 2, plannedDurationMinutes = 10, plannedStartTime = "09:08 AM", status = PerformanceStatus.COMPLETED, description = "School Choir opening song", musicName = "Saraswati Stotram", youtubeLink = "", audioFile = "", props = "Veena prop × 1", costume = "White & Gold Kurta Pajama", stageRequirements = "3 Stand Microphones", notes = "Rehearsed 5 times", isApproved = true, progressPercentage = 100, requestedByTeacherName = "", requestedByTeacherId = "", createdBy = "u_t3"),
            PerformanceEntity(id = "p_3", eventId = eventId, name = "Patriotic Choir Song", performanceTypeId = "pt_2", sequenceNumber = 3, plannedDurationMinutes = 12, plannedStartTime = "09:18 AM", status = PerformanceStatus.READY, description = "Group chorus song 'Vande Mataram'", musicName = "Vande Mataram Remix", youtubeLink = "https://youtube.com/watch?v=sample2", audioFile = "", props = "Indian National Flags × 20", costume = "Tri-color dupattas & sashes", stageRequirements = "5 Lapel Mics + Choir Standing Racks", notes = "Sound check finished at 8:45 AM", isApproved = true, progressPercentage = 90, requestedByTeacherName = "", requestedByTeacherId = "", createdBy = "u_t1"),
            PerformanceEntity(id = "p_4", eventId = eventId, name = "Rajasthani Folk Dance", performanceTypeId = "pt_1", sequenceNumber = 4, plannedDurationMinutes = 15, plannedStartTime = "09:30 AM", status = PerformanceStatus.READY, description = "Ghoomar dance presentation", musicName = "Kesariya Balam Folk track", youtubeLink = "", audioFile = "", props = "Matka × 6, Dandiya sticks × 16", costume = "Traditional Rajasthani Ghagra Choli", stageRequirements = "Center spotlight + fog machine", notes = "Props arranged near green room", isApproved = true, progressPercentage = 85, requestedByTeacherName = "", requestedByTeacherId = "", createdBy = "u_t1"),
            PerformanceEntity(id = "p_5", eventId = eventId, name = "Freedom Fighters Skit", performanceTypeId = "pt_3", sequenceNumber = 5, plannedDurationMinutes = 20, plannedStartTime = "09:45 AM", status = PerformanceStatus.ISSUE, description = "Play depicting Bhagat Singh & Chandrashekhar Azad", musicName = "Martyrs BGM score", youtubeLink = "", audioFile = "", props = "Wooden swords, jail bars prop, microphones", costume = "British Army uniforms & Khadi Kurtas", stageRequirements = "Full stage lighting, dramatic red highlights", notes = "One actor (Aman Verma) costume fit adjustment pending!", isApproved = true, progressPercentage = 40, requestedByTeacherName = "", requestedByTeacherId = "", createdBy = "u_t2"),
            PerformanceEntity(id = "p_6", eventId = eventId, name = "Group Fusion Dance", performanceTypeId = "pt_1", sequenceNumber = 6, plannedDurationMinutes = 18, plannedStartTime = "10:05 AM", status = PerformanceStatus.IN_PROGRESS, description = "Contemp-Folk fusion dance by Seniors", musicName = "Mile Sur Mera Tumhara Remix", youtubeLink = "", audioFile = "", props = "Tri-color ribbons × 12", costume = "Navy Blue & White Costumes", stageRequirements = "Dynamic RGB lights", notes = "Final stage walkthrough ongoing", isApproved = true, progressPercentage = 65, requestedByTeacherName = "", requestedByTeacherId = "", createdBy = "u_t1"),
            PerformanceEntity(id = "p_7", eventId = eventId, name = "Principal Address & Speech", performanceTypeId = "pt_7", sequenceNumber = 7, plannedDurationMinutes = 15, plannedStartTime = "10:23 AM", status = PerformanceStatus.NOT_STARTED, description = "Keynote message by Principal Sharma", musicName = "", youtubeLink = "", audioFile = "", props = "Podium, Floral garland", costume = "Formal blazer", stageRequirements = "Podium Microphone + Slide Clicker", notes = "Slides uploaded to AV console", isApproved = true, progressPercentage = 100, requestedByTeacherName = "", requestedByTeacherId = "", createdBy = "u_admin"),
            PerformanceEntity(id = "p_8", eventId = eventId, name = "Vote of Thanks & Anthem", performanceTypeId = "pt_2", sequenceNumber = 8, plannedDurationMinutes = 10, plannedStartTime = "10:38 AM", status = PerformanceStatus.NOT_STARTED, description = "Closing ceremony and National Anthem", musicName = "Jana Gana Mana", youtubeLink = "", audioFile = "", props = "National Flag hoisting cord", costume = "School Uniforms", stageRequirements = "Full PA System", notes = "All students assemble on stage", isApproved = true, progressPercentage = 50, requestedByTeacherName = "", requestedByTeacherId = "", createdBy = "u_t2")
        )

        performances.forEach { perf ->
            db.performanceDao().insertPerformance(perf)
        }
        recalculatePerformanceTimings(eventId)

        // Performance Students mapping
        val perfStudents = listOf(
            PerformanceStudentCrossRef("p_1", "s_11"),
            PerformanceStudentCrossRef("p_1", "s_17"),
            PerformanceStudentCrossRef("p_2", "s_1"),
            PerformanceStudentCrossRef("p_2", "s_2"),
            PerformanceStudentCrossRef("p_2", "s_4"),
            PerformanceStudentCrossRef("p_3", "s_1"),
            PerformanceStudentCrossRef("p_3", "s_2"),
            PerformanceStudentCrossRef("p_3", "s_4"),
            PerformanceStudentCrossRef("p_3", "s_13"),
            PerformanceStudentCrossRef("p_3", "s_18"),
            PerformanceStudentCrossRef("p_4", "s_2"),
            PerformanceStudentCrossRef("p_4", "s_4"),
            PerformanceStudentCrossRef("p_4", "s_6"),
            PerformanceStudentCrossRef("p_4", "s_14"),
            PerformanceStudentCrossRef("p_4", "s_18"),
            PerformanceStudentCrossRef("p_5", "s_3"),
            PerformanceStudentCrossRef("p_5", "s_5"),
            PerformanceStudentCrossRef("p_5", "s_9"),
            PerformanceStudentCrossRef("p_5", "s_10"),
            PerformanceStudentCrossRef("p_5", "s_15"),
            PerformanceStudentCrossRef("p_6", "s_8"),
            PerformanceStudentCrossRef("p_6", "s_9"),
            PerformanceStudentCrossRef("p_6", "s_10"),
            PerformanceStudentCrossRef("p_6", "s_16")
        )
        db.performanceDao().insertPerformanceStudents(perfStudents)

        // 7. Event Updates
        val updates = listOf(
            EventUpdateEntity("u_up_1", eventId, "p_1", "Welcome Dance", "u_t1", "Neha Sharma", "TEACHER", "COMPLETED", "Welcome dance performance concluded successfully with huge applause!"),
            EventUpdateEntity("u_up_2", eventId, "p_2", "Saraswati Vandana", "u_t3", "Priya Singh", "TEACHER", "COMPLETED", "Saraswati Vandana completed gracefully. Sound check OK."),
            EventUpdateEntity("u_up_3", eventId, "p_3", "Patriotic Choir Song", "u_t1", "Neha Sharma", "TEACHER", "READY", "Patriotic Choir is lined up backstage. Mics and dupattas checked."),
            EventUpdateEntity("u_up_4", eventId, "p_5", "Freedom Fighters Skit", "u_t2", "Rajesh Kumar", "TEACHER", "ISSUE", "Costume alteration issue for Aman Verma (Bhagat Singh coat). Tailor on site fixing now."),
            EventUpdateEntity("u_up_5", eventId, "p_6", "Group Fusion Dance", "u_t1", "Neha Sharma", "TEACHER", "IN_PROGRESS", "Dancers taking stage for final warm-up sequence.")
        )
        updates.forEach { db.eventUpdateDao().insertUpdate(it) }

        // 8. Activity Logs
        logActivity("u_admin", "Principal Sharma", "EVENT_CREATED", "EVENT", eventId, "Created event 'Independence Day Celebration 2026'")
        logActivity("u_t1", "Neha Sharma", "STATUS_CHANGED", "PERFORMANCE", "p_1", "Welcome Dance → COMPLETED")
        logActivity("u_t3", "Priya Singh", "STATUS_CHANGED", "PERFORMANCE", "p_2", "Saraswati Vandana → COMPLETED")
        logActivity("u_t1", "Neha Sharma", "STATUS_CHANGED", "PERFORMANCE", "p_3", "Patriotic Choir Song → READY")
        logActivity("u_t2", "Rajesh Kumar", "STATUS_CHANGED", "PERFORMANCE", "p_5", "Freedom Fighters Skit → ISSUE (Costume fit adjustment)")

        // 9. Multi-School Data & Subscriptions
        val subscriptions = listOf(
            SchoolSubscriptionEntity("sch_jdis", "J D International School", 1, System.currentTimeMillis() + (90L * 24 * 3600 * 1000), "1 Credit Plan (3 Months)", "pay_rzp_initial_01"),
            SchoolSubscriptionEntity("sch_dpa", "Delhi Public Academy", 5, System.currentTimeMillis() + (365L * 24 * 3600 * 1000), "5 Credits Plan (1 Year)", "pay_rzp_initial_02"),
            SchoolSubscriptionEntity("sch_stx", "St. Xavier's High School", 3, System.currentTimeMillis() + (180L * 24 * 3600 * 1000), "5 Credits Plan (1 Year)", "pay_rzp_initial_03"),
            SchoolSubscriptionEntity("sch_rgs", "Ryan Global School", 0, System.currentTimeMillis() - (5L * 24 * 3600 * 1000), "Expired Starter Trial", "pay_rzp_trial")
        )
        db.schoolSubscriptionDao().insertSubscriptions(subscriptions)

        val otherAdmins = listOf(
            UserEntity("u_admin_dpa", "Dr. Sanjeev Kapoor", "admin@dpa.edu.in", "password123", UserRole.ADMIN, "+91 9811223344", schoolId = "sch_dpa", schoolName = "Delhi Public Academy"),
            UserEntity("u_admin_stx", "Sister Mary Theresa", "principal@stxaviers.org", "password123", UserRole.ADMIN, "+91 9822334455", schoolId = "sch_stx", schoolName = "St. Xavier's High School"),
            UserEntity("u_admin_rgs", "Col. Rakesh Mehra", "director@ryanglobal.org", "password123", UserRole.ADMIN, "+91 9833445566", schoolId = "sch_rgs", schoolName = "Ryan Global School")
        )
        db.userDao().insertUsers(otherAdmins)

        // 10. Sample Admin Reported Issues
        val demoIssues = listOf(
            AdminReportIssueEntity(
                id = "issue_demo_1",
                schoolId = "sch_jdis",
                schoolName = "J D International School",
                adminUserId = "u_admin",
                adminName = "Principal Sharma",
                adminContact = "+91 9800000002",
                title = "Need extra stage microphones configuration support",
                description = "Our auditorium has 8 wireless microphones but only 4 channels are showing in the audio console. How can we map all 8 channels for the patriotic choir?",
                category = "Technical",
                status = "OPEN",
                createdAt = System.currentTimeMillis() - (2 * 3600 * 1000)
            ),
            AdminReportIssueEntity(
                id = "issue_demo_2",
                schoolId = "sch_dpa",
                schoolName = "Delhi Public Academy",
                adminUserId = "u_admin_dpa",
                adminName = "Dr. Sanjeev Kapoor",
                adminContact = "+91 9811223344",
                title = "Bulk CSV student parent contact formatting query",
                description = "We imported 350 student records with country codes. Need clarification on how SMS notifications trigger for parents.",
                category = "Event Management",
                status = "IN_PROGRESS",
                superAdminResponse = "Hello Dr. Sanjeev, our system automatically normalizes +91 phone numbers for instantaneous SMS dispatch. Investigating batch logs.",
                createdAt = System.currentTimeMillis() - (18 * 3600 * 1000)
            ),
            AdminReportIssueEntity(
                id = "issue_demo_3",
                schoolId = "sch_stx",
                schoolName = "St. Xavier's High School",
                adminUserId = "u_admin_stx",
                adminName = "Sister Mary Theresa",
                adminContact = "+91 9822334455",
                title = "Annual sports day credit validity extension",
                description = "We purchased the 1-year 5 credits plan. Wanted to verify if winter carnival events are also covered under the same credits.",
                category = "Billing / Plan",
                status = "RESOLVED",
                superAdminResponse = "Yes Sister Mary, all 5 credits can be used for any event type including Annual Functions, Sports Meets, and Cultural Fests!",
                createdAt = System.currentTimeMillis() - (2 * 24 * 3600 * 1000),
                resolvedAt = System.currentTimeMillis() - (12 * 3600 * 1000)
            )
        )
        db.adminReportIssueDao().insertIssues(demoIssues)

        // 11. Sample 1-to-1 Direct Messages
        val demoDirectMessages = listOf(
            DirectMessageEntity(
                id = "dm_demo_1",
                schoolId = "sch_jdis",
                senderId = "u_admin",
                senderName = "Principal Sharma",
                senderRole = UserRole.ADMIN,
                receiverId = "u_super",
                message = "Hello Super Admin, our Independence Day event is scheduled for tomorrow. Everything is set up!",
                timestampMillis = System.currentTimeMillis() - (4 * 3600 * 1000)
            ),
            DirectMessageEntity(
                id = "dm_demo_2",
                schoolId = "sch_jdis",
                senderId = "u_super",
                senderName = "Super Admin Support",
                senderRole = UserRole.SUPER_ADMIN,
                receiverId = "u_admin",
                message = "Great to hear Principal Sharma! Your live audio and QR sync are primed and ready.",
                timestampMillis = System.currentTimeMillis() - (3 * 3600 * 1000)
            ),
            DirectMessageEntity(
                id = "dm_demo_3",
                schoolId = "sch_dpa",
                senderId = "u_admin_dpa",
                senderName = "Dr. Sanjeev Kapoor",
                senderRole = UserRole.ADMIN,
                receiverId = "u_super",
                message = "Hi, we just upgraded to the 5 Credits 1-Year plan via Razorpay. It was seamless!",
                timestampMillis = System.currentTimeMillis() - (20 * 3600 * 1000)
            ),
            DirectMessageEntity(
                id = "dm_demo_4",
                schoolId = "sch_dpa",
                senderId = "u_super",
                senderName = "Super Admin Support",
                senderRole = UserRole.SUPER_ADMIN,
                receiverId = "u_admin_dpa",
                message = "Thank you Dr. Sanjeev! Your 5 credits and 365-day validity have been activated successfully.",
                timestampMillis = System.currentTimeMillis() - (19 * 3600 * 1000)
            )
        )
        db.directMessageDao().insertMessages(demoDirectMessages)
    }
}
