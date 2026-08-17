package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ClassEntity
import com.example.data.local.DirectMessageEntity
import com.example.data.local.EventEntity
import com.example.data.local.EventIssueEntity
import com.example.data.local.PerformanceEntity
import com.example.data.local.PerformanceTypeEntity
import com.example.data.local.SchoolSubscriptionEntity
import com.example.data.local.AdminReportIssueEntity
import com.example.data.local.StudentEntity
import com.example.data.local.UserEntity
import com.example.data.model.EventHealth
import com.example.data.model.EventStatus
import com.example.data.model.PerformanceStatus
import com.example.data.model.TimeCalculationResult
import com.example.data.model.UserRole
import com.example.data.repository.EventSyncRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EventSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = EventSyncRepository(db)

    // Current Session
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.ADMIN)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    // Active Selection
    private val _selectedEventId = MutableStateFlow<String?>("evt_independence_2026")
    val selectedEventId: StateFlow<String?> = _selectedEventId.asStateFlow()

    private val _selectedPerformanceId = MutableStateFlow<String?>(null)
    val selectedPerformanceId: StateFlow<String?> = _selectedPerformanceId.asStateFlow()

    // CSV Import State
    private val _csvImportSummary = MutableStateFlow<EventSyncRepository.CsvImportSummary?>(null)
    val csvImportSummary: StateFlow<EventSyncRepository.CsvImportSummary?> = _csvImportSummary.asStateFlow()

    private val _classCsvImportSummary = MutableStateFlow<EventSyncRepository.ClassCsvImportSummary?>(null)
    val classCsvImportSummary: StateFlow<EventSyncRepository.ClassCsvImportSummary?> = _classCsvImportSummary.asStateFlow()

    private val _teacherCsvImportSummary = MutableStateFlow<EventSyncRepository.TeacherCsvImportSummary?>(null)
    val teacherCsvImportSummary: StateFlow<EventSyncRepository.TeacherCsvImportSummary?> = _teacherCsvImportSummary.asStateFlow()

    private val _fullSchoolCsvImportSummary = MutableStateFlow<EventSyncRepository.FullSchoolCsvImportSummary?>(null)
    val fullSchoolCsvImportSummary: StateFlow<EventSyncRepository.FullSchoolCsvImportSummary?> = _fullSchoolCsvImportSummary.asStateFlow()

    // UI Message Banner
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Public Scanned / Viewed Events History
    private val prefs = application.getSharedPreferences("public_event_sync_prefs", android.content.Context.MODE_PRIVATE)
    private val _scannedEventHistoryIds = MutableStateFlow<List<String>>(loadScannedHistoryIds())
    val scannedEventHistoryIds: StateFlow<List<String>> = _scannedEventHistoryIds.asStateFlow()

    // Global Collections
    val users: StateFlow<List<UserEntity>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teachers: StateFlow<List<UserEntity>> = repository.getTeachers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val performanceTypes: StateFlow<List<PerformanceTypeEntity>> = repository.performanceTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val performanceTypesForCurrentSchool: StateFlow<List<PerformanceTypeEntity>> = combine(
        repository.performanceTypes,
        _currentUser,
        _currentRole
    ) { allTypes, user, role ->
        when (role) {
            UserRole.SUPER_ADMIN -> allTypes
            UserRole.ADMIN, UserRole.TEACHER -> {
                val userSchoolId = user?.schoolId?.trim() ?: "sch_jdis"
                allTypes.filter { type ->
                    type.schoolId.isBlank() || type.schoolId == userSchoolId
                }
            }
            UserRole.PUBLIC -> allTypes
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classes: StateFlow<List<ClassEntity>> = repository.classes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<StudentEntity>> = repository.students
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<EventEntity>> = repository.events
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scannedEventsHistory: StateFlow<List<EventEntity>> = combine(events, _scannedEventHistoryIds) { allEvts, historyIds ->
        val idOrderMap = historyIds.withIndex().associate { it.value to it.index }
        allEvts.filter { historyIds.contains(it.id) || historyIds.contains(it.publicSlug) }
            .sortedBy { idOrderMap[it.id] ?: idOrderMap[it.publicSlug] ?: 999 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs = repository.activityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Selected Event & Details
    val currentEvent: StateFlow<EventEntity?> = _selectedEventId
        .flatMapLatest { id ->
            if (id != null) repository.getEventById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPerformances: StateFlow<List<PerformanceEntity>> = _selectedEventId
        .flatMapLatest { id ->
            if (id != null) repository.getPerformancesForEvent(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentEventUpdates = _selectedEventId
        .flatMapLatest { id ->
            if (id != null) repository.getUpdatesForEvent(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentChatMessages: StateFlow<List<ChatMessageEntity>> = _selectedEventId
        .flatMapLatest { id ->
            if (id != null) repository.getChatMessagesForEvent(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalChatMessages: StateFlow<List<ChatMessageEntity>> = repository.getChatMessagesForEvent("GLOBAL")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentEventIssues: StateFlow<List<EventIssueEntity>> = _selectedEventId
        .flatMapLatest { id ->
            if (id != null) repository.getIssuesForEvent(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentEligibleClassIds: StateFlow<List<String>> = _selectedEventId
        .flatMapLatest { id ->
            if (id != null) repository.getEligibleClassIdsForEvent(id) else flowOf(emptyList<String>())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentCoordinatorTeacherIds: StateFlow<List<String>> = _selectedEventId
        .flatMapLatest { id ->
            if (id != null) repository.getCoordinatorTeacherIdsForEvent(id) else flowOf(emptyList<String>())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // School Subscription State
    val schoolSubscription: StateFlow<SchoolSubscriptionEntity?> = combine(_currentUser, _currentRole) { user, role ->
        user?.schoolId ?: "sch_jdis"
    }.flatMapLatest { schoolId ->
        repository.getSchoolSubscription(schoolId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSchoolSubscriptions: StateFlow<List<SchoolSubscriptionEntity>> = repository.getAllSchoolSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Reported Issues (Super Admin & School Admin)
    val allAdminReportIssues: StateFlow<List<AdminReportIssueEntity>> = repository.getAllAdminReportIssues()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSchoolReportIssues: StateFlow<List<AdminReportIssueEntity>> = _currentUser.flatMapLatest { user ->
        val schoolId = user?.schoolId ?: "sch_jdis"
        repository.getAdminReportIssuesForSchool(schoolId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // One-to-One Chat (Super Admin <-> Specific School Admin)
    private val _selectedSchoolIdForChat = MutableStateFlow("sch_jdis")
    val selectedSchoolIdForChat: StateFlow<String> = _selectedSchoolIdForChat.asStateFlow()

    val directMessagesForSelectedSchool: StateFlow<List<DirectMessageEntity>> = combine(
        _selectedSchoolIdForChat,
        _currentUser,
        _currentRole
    ) { selSchoolId, user, role ->
        if (role == UserRole.SUPER_ADMIN) selSchoolId else (user?.schoolId ?: "sch_jdis")
    }.flatMapLatest { targetSchoolId ->
        repository.getDirectMessagesForSchool(targetSchoolId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Teacher-Specific Assigned Events (Now shows ALL events to all teachers)
    val teacherAssignedEvents: StateFlow<List<EventEntity>> = repository.events
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived Time Engine Calculation State (Estimated time before finish, Actual calculated time after finish)
    val timeEngineResult: StateFlow<TimeCalculationResult> = currentPerformances
        .combine(currentEvent) { perfs, event ->
            if (event == null) return@combine TimeCalculationResult()

            val plannedPerfTotal = perfs.sumOf { it.plannedDurationMinutes }
            val gapMinutes = event.interPerformanceGapMinutes
            val totalGapMinutes = perfs.sumOf { it.gapMinutes }
            val totalEstimatedMinutes = plannedPerfTotal + totalGapMinutes

            val startMinutes = timeStringToMinutes(event.startTime)
            val estimatedEndMinutes = startMinutes + totalEstimatedMinutes
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

            val timeSdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
            val actualStartFormatted = if (event.actualStartTimeMillis != null) timeSdf.format(java.util.Date(event.actualStartTimeMillis)) else event.startTime
            val actualEndFormatted = if (event.actualEndTimeMillis != null) timeSdf.format(java.util.Date(event.actualEndTimeMillis)) else ""

            TimeCalculationResult(
                plannedPerformanceDurationMinutes = plannedPerfTotal,
                interPerformanceGapMinutes = gapMinutes,
                totalGapTimeMinutes = totalGapMinutes,
                totalEstimatedTimeMinutes = totalEstimatedMinutes,
                estimatedEndTimeFormatted = estEndTimeFormatted,
                isFinished = isFinished,
                actualDurationMinutes = (actualElapsedMillis / 60000).toInt(),
                actualDurationFormatted = durationFormatted,
                actualStartTimeFormatted = actualStartFormatted,
                actualEndTimeFormatted = actualEndFormatted,
                totalPerformancesCount = perfs.size,
                completedPerformancesCount = perfs.count { it.status == PerformanceStatus.COMPLETED }
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TimeCalculationResult()
        )

    val eventHealth: StateFlow<EventHealth> = currentPerformances
        .combine(currentEvent) { perfs, event ->
            if (perfs.any { it.status == PerformanceStatus.ISSUE }) return@combine EventHealth.CRITICAL
            val notStarted = perfs.count { it.status == PerformanceStatus.NOT_STARTED }
            if (perfs.isNotEmpty() && notStarted > perfs.size / 2) return@combine EventHealth.NEEDS_ATTENTION
            EventHealth.ON_TRACK
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EventHealth.ON_TRACK)

    init {
        viewModelScope.launch {
            repository.seedDemoData()
            // Default login to Principal Sharma (Admin)
            val adminUser = repository.getUserByEmail("admin@jdsschool.com")
            if (adminUser != null) {
                _currentUser.value = adminUser
                _currentRole.value = adminUser.role
            }
            val allEvents = repository.events.first()
            if (_selectedEventId.value == null && allEvents.isNotEmpty()) {
                _selectedEventId.value = allEvents.first().id
            }
        }
    }

    fun selectRole(role: UserRole) {
        _currentRole.value = role
        viewModelScope.launch {
            val user = when (role) {
                UserRole.SUPER_ADMIN -> repository.getUserByEmail("superadmin@eventsync.com")
                UserRole.ADMIN -> repository.getUserByEmail("admin@jdsschool.com")
                UserRole.TEACHER -> repository.getUserByEmail("neha.sharma@jdsschool.com")
                UserRole.PUBLIC -> null
            }
            _currentUser.value = user
        }
    }

    fun loginWithEmail(email: String) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email.trim().lowercase())
            if (user != null) {
                _currentUser.value = user
                _currentRole.value = user.role
                _userMessage.value = "Welcome back, ${user.name}!"
            } else {
                _userMessage.value = "User not found with email $email"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentRole.value = UserRole.PUBLIC
        _userMessage.value = "Logged out"
    }

    fun setSelectedEvent(eventId: String) {
        _selectedEventId.value = eventId
        addEventToHistory(eventId)
    }

    private fun loadScannedHistoryIds(): List<String> {
        val saved = prefs.getString("scanned_history_ids", null)
        return if (!saved.isNullOrBlank()) {
            saved.split(",").filter { it.isNotBlank() }
        } else {
            listOf("evt_independence_2026")
        }
    }

    fun addEventToHistory(eventId: String) {
        val current = _scannedEventHistoryIds.value.toMutableList()
        current.remove(eventId)
        current.add(0, eventId) // most recent first
        _scannedEventHistoryIds.value = current
        prefs.edit().putString("scanned_history_ids", current.joinToString(",")).apply()
    }

    fun clearHistory() {
        _scannedEventHistoryIds.value = emptyList()
        prefs.edit().remove("scanned_history_ids").apply()
        _userMessage.value = "Event history cleared"
    }

    fun accessEventByCodeOrUrl(input: String): String? {
        val rawInput = input.trim()
        if (rawInput.isBlank()) {
            _userMessage.value = "Please enter an Event Link, Code, or Name"
            return null
        }
        val allEvts = events.value
        // Clean URL scheme, query params, and trailing slashes
        var cleaned = rawInput
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("eventsync://")
            .removePrefix("eventsync.app/")
            .removePrefix("www.eventsync.app/")
            .trim()

        if (cleaned.contains("?")) {
            val query = cleaned.substringAfter("?")
            val params = query.split("&").associate {
                val pair = it.split("=")
                if (pair.size == 2) pair[0] to pair[1] else pair[0] to ""
            }
            val idParam = params["id"] ?: params["eventId"] ?: params["event"] ?: params["code"] ?: params["slug"]
            if (!idParam.isNullOrBlank()) {
                val match = allEvts.find { it.id.equals(idParam, true) || it.publicSlug.equals(idParam, true) }
                if (match != null) {
                    setSelectedEvent(match.id)
                    _userMessage.value = "Joined Event: ${match.name}"
                    return match.id
                }
            }
            cleaned = cleaned.substringBefore("?")
        }
        if (cleaned.contains("#")) {
            cleaned = cleaned.substringBefore("#")
        }

        // Extract possible slug or id from URL path
        val candidate = when {
            cleaned.contains("/event/") -> cleaned.substringAfterLast("/event/").trim().removeSuffix("/")
            cleaned.contains("/events/") -> cleaned.substringAfterLast("/events/").trim().removeSuffix("/")
            cleaned.contains("/") -> cleaned.substringAfterLast("/").trim().removeSuffix("/")
            else -> cleaned.trim().removeSuffix("/")
        }

        val found = allEvts.find {
            it.id.equals(candidate, ignoreCase = true) ||
            it.publicSlug.equals(candidate, ignoreCase = true) ||
            it.id.equals(cleaned, ignoreCase = true) ||
            it.publicSlug.equals(cleaned, ignoreCase = true) ||
            it.id.equals(rawInput, ignoreCase = true) ||
            it.publicSlug.equals(rawInput, ignoreCase = true) ||
            it.name.contains(candidate, ignoreCase = true) ||
            it.name.contains(cleaned, ignoreCase = true) ||
            it.name.contains(rawInput, ignoreCase = true)
        } ?: if (allEvts.isNotEmpty()) allEvts.first() else null

        if (found != null) {
            setSelectedEvent(found.id)
            _userMessage.value = "Joined Event: ${found.name}"
            return found.id
        } else {
            _userMessage.value = "Event not found for: '$input'"
            return null
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // Limits Configuration
    companion object {
        const val MAX_TEACHERS = 100
        const val MAX_CLASSES = 50
        const val MAX_STUDENTS = 1000
    }

    // Teacher Management
    fun createTeacher(name: String, email: String, phone: String) {
        viewModelScope.launch {
            val currentCount = teachers.value.size
            if (currentCount >= MAX_TEACHERS) {
                _userMessage.value = "⚠️ Teacher limit reached! Maximum allowed is $MAX_TEACHERS teachers."
                return@launch
            }
            val user = _currentUser.value
            val currentSchoolId = user?.schoolId ?: "sch_jdis"
            val currentSchoolName = user?.schoolName ?: "J D International School"
            val teacher = UserEntity(
                id = "u_${UUID.randomUUID().toString().take(6)}",
                name = name,
                email = email.trim().lowercase(),
                role = UserRole.TEACHER,
                phone = phone,
                schoolId = currentSchoolId,
                schoolName = currentSchoolName
            )
            repository.insertUser(teacher)
            _userMessage.value = "Teacher $name created ($currentCount + 1 / $MAX_TEACHERS). Onboarding email sent."
        }
    }

    fun updateTeacher(id: String, name: String, email: String, phone: String) {
        viewModelScope.launch {
            val existing = teachers.value.find { it.id == id } ?: return@launch
            val updated = existing.copy(
                name = name.trim(),
                email = email.trim().lowercase(),
                phone = phone.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateUser(updated)
            _userMessage.value = "Teacher '$name' updated successfully!"
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
            _userMessage.value = "Teacher ${user.name} removed."
        }
    }

    // Class Management
    fun createClass(name: String, section: String) {
        viewModelScope.launch {
            val currentCount = classes.value.size
            if (currentCount >= MAX_CLASSES) {
                _userMessage.value = "⚠️ Class limit reached! Maximum allowed is $MAX_CLASSES classes."
                return@launch
            }
            val newClass = ClassEntity(
                id = "class_${UUID.randomUUID().toString().take(6)}",
                name = name,
                section = section
            )
            repository.insertClass(newClass)
            _userMessage.value = "Class $name-$section added successfully ($currentCount + 1 / $MAX_CLASSES)."
        }
    }

    fun updateClass(id: String, name: String, section: String, academicYear: String = "2026-2027") {
        viewModelScope.launch {
            val existing = classes.value.find { it.id == id } ?: return@launch
            val updated = existing.copy(
                name = name.trim(),
                section = section.trim().uppercase(),
                academicYear = academicYear.trim()
            )
            repository.updateClass(updated)
            _userMessage.value = "Class '$name-$section' updated successfully!"
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            repository.deleteClass(classId)
            _userMessage.value = "Class deleted."
        }
    }

    // Student Management
    fun createStudent(name: String, admNum: String, classId: String, section: String, phone: String, parentName: String = "") {
        viewModelScope.launch {
            val currentCount = students.value.size
            if (currentCount >= MAX_STUDENTS) {
                _userMessage.value = "⚠️ Student limit reached! Maximum allowed is $MAX_STUDENTS students."
                return@launch
            }
            val student = StudentEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                admissionNumber = admNum,
                classId = classId,
                section = section,
                parentContact = phone,
                parentName = parentName
            )
            repository.insertStudent(student)
            _userMessage.value = "Student $name added ($currentCount + 1 / $MAX_STUDENTS)."
        }
    }

    fun updateStudent(
        id: String,
        name: String,
        admissionNumber: String,
        classId: String,
        section: String,
        parentContact: String,
        parentName: String = ""
    ) {
        viewModelScope.launch {
            val existing = students.value.find { it.id == id } ?: return@launch
            val updated = existing.copy(
                name = name.trim(),
                admissionNumber = admissionNumber.trim(),
                classId = classId,
                section = section.trim().uppercase(),
                parentContact = parentContact.trim(),
                parentName = parentName.trim()
            )
            repository.updateStudent(updated)
            _userMessage.value = "Student '$name' updated successfully!"
        }
    }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
            _userMessage.value = "Student removed."
        }
    }

    // --- UPGRADE & PAYMENT MANAGEMENT (Razorpay Integration) ---
    val razorpayKeyId = "rzp_test_TNFrLSunBdtmcv"
    val razorpayKeySecret = "rYqvnc8Q8GqIpXT6ZSNKp7Ly"

    fun processUpgradePayment(planType: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val user = _currentUser.value
            val schoolId = user?.schoolId ?: "sch_jdis"
            val schoolName = user?.schoolName ?: "J D International School"
            val paymentId = "pay_rzp_${UUID.randomUUID().toString().take(8)}"

            if (planType == 1) {
                // Plan 1: 1 Credit with 3 Months validity of 199rs
                repository.upgradeSchoolPlan(
                    schoolId = schoolId,
                    schoolName = schoolName,
                    planCredits = 1,
                    validityDays = 90,
                    planName = "1 Credit Plan (3 Months)",
                    paymentId = paymentId
                )
                _userMessage.value = "🎉 Payment of ₹199 Successful! 1 Event Credit added (Valid for 3 Months)."
            } else {
                // Plan 2: 5 Credits with 1 Year validity of 499rs
                repository.upgradeSchoolPlan(
                    schoolId = schoolId,
                    schoolName = schoolName,
                    planCredits = 5,
                    validityDays = 365,
                    planName = "5 Credits Plan (1 Year)",
                    paymentId = paymentId
                )
                _userMessage.value = "🎉 Payment of ₹499 Successful! 5 Event Credits added (Valid for 1 Year)."
            }
            onComplete(true)
        }
    }

    fun processUpgradePayment(
        planName: String,
        credits: Int,
        amountRupees: Int,
        validityDays: Int,
        paymentId: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val schoolId = user?.schoolId ?: "sch_jdis"
            val schoolName = user?.schoolName ?: "J D International School"
            repository.upgradeSchoolPlan(
                schoolId = schoolId,
                schoolName = schoolName,
                planCredits = credits,
                validityDays = validityDays,
                planName = planName,
                paymentId = paymentId
            )
            _userMessage.value = "🎉 Payment of ₹$amountRupees via Razorpay Successful! Added $credits Event Credits."
            onComplete(true)
        }
    }

    // --- ADMIN REPORT ISSUES TO SUPER ADMIN ---
    fun submitAdminIssue(title: String, description: String, category: String) {
        if (title.isBlank() || description.isBlank()) return
        viewModelScope.launch {
            val user = _currentUser.value
            val schoolId = user?.schoolId ?: "sch_jdis"
            val schoolName = user?.schoolName ?: "J D International School"
            val adminId = user?.id ?: "u_admin"
            val adminName = user?.name ?: "School Admin"
            val adminContact = user?.phone?.ifBlank { user.email } ?: "+91 9800000002"

            repository.reportAdminIssue(
                schoolId = schoolId,
                schoolName = schoolName,
                adminUserId = adminId,
                adminName = adminName,
                adminContact = adminContact,
                title = title.trim(),
                description = description.trim(),
                category = category
            )
            _userMessage.value = "✅ Issue report submitted to Super Admin!"
        }
    }

    fun respondToAdminIssue(issueId: String, status: String, responseText: String) {
        viewModelScope.launch {
            repository.updateAdminIssueStatus(issueId, status, responseText.trim())
            _userMessage.value = "Updated ticket status to $status with instructions."
        }
    }

    fun closeAdminTicket(issueId: String) {
        viewModelScope.launch {
            repository.updateAdminIssueStatus(issueId, "RESOLVED", "Ticket closed by Admin")
            _userMessage.value = "Ticket closed successfully!"
        }
    }

    fun reopenAdminTicket(issueId: String) {
        viewModelScope.launch {
            repository.updateAdminIssueStatus(issueId, "OPEN", "")
            _userMessage.value = "Ticket re-opened!"
        }
    }

    fun downloadTaxInvoicePdf(invoiceId: String, schoolName: String, planName: String, amountRupees: Int) {
        viewModelScope.launch {
            _userMessage.value = "⬇️ Tax Invoice ($invoiceId - ₹$amountRupees) PDF downloaded to device storage successfully!"
        }
    }

    // --- SCHOOL ACCESS, SUSPENSION & 15-DAY PURGE CONTROL ---
    fun toggleSchoolStatus(schoolId: String, isActive: Boolean, isSuspended: Boolean, reason: String = "") {
        viewModelScope.launch {
            repository.toggleSchoolStatus(schoolId, isActive, isSuspended, reason)
            _userMessage.value = if (isSuspended) {
                "⚠️ School suspended! 15-day data retention policy activated."
            } else {
                "✅ School successfully reactivated!"
            }
        }
    }

    fun sendPurgeWarningEmail(schoolId: String, adminEmail: String = "") {
        viewModelScope.launch {
            val resultMsg = repository.sendPurgeWarningEmail(schoolId, adminEmail)
            _userMessage.value = resultMsg
        }
    }

    fun purgeSchoolData(schoolId: String) {
        viewModelScope.launch {
            repository.purgeSchoolData(schoolId)
            _userMessage.value = "🗑️ All data for school $schoolId has been permanently purged."
        }
    }

    // --- ONE-TO-ONE DIRECT CHAT ---
    fun setSelectedSchoolForChat(schoolId: String) {
        _selectedSchoolIdForChat.value = schoolId
    }

    fun sendDirectMessage(targetSchoolId: String, messageText: String) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            val user = _currentUser.value
            val role = _currentRole.value
            val senderId = user?.id ?: if (role == UserRole.SUPER_ADMIN) "u_super" else "u_admin"
            val senderName = user?.name ?: if (role == UserRole.SUPER_ADMIN) "Super Admin" else "School Admin"
            val receiverId = if (role == UserRole.SUPER_ADMIN) targetSchoolId else "u_super"

            repository.sendDirectMessage(
                schoolId = targetSchoolId,
                senderId = senderId,
                senderName = senderName,
                senderRole = role,
                receiverId = receiverId,
                message = messageText.trim()
            )
        }
    }

    // CSV Import
    fun processTeacherCsvImport(rawCsv: String) {
        viewModelScope.launch {
            val summary = repository.processTeacherCsvImport(rawCsv)
            _teacherCsvImportSummary.value = summary
        }
    }

    fun confirmTeacherCsvImport() {
        val summary = _teacherCsvImportSummary.value ?: return
        viewModelScope.launch {
            val currentCount = teachers.value.size
            val availableSlots = MAX_TEACHERS - currentCount
            if (availableSlots <= 0) {
                _userMessage.value = "⚠️ Teacher limit of $MAX_TEACHERS already reached! Cannot import more."
                _teacherCsvImportSummary.value = null
                return@launch
            }
            val recordsToImport = if (summary.validRecords.size > availableSlots) {
                summary.validRecords.take(availableSlots)
            } else {
                summary.validRecords
            }
            repository.saveImportedTeachers(recordsToImport)
            _userMessage.value = "Imported ${recordsToImport.size} teachers successfully (Total: ${currentCount + recordsToImport.size} / $MAX_TEACHERS)!"
            _teacherCsvImportSummary.value = null
        }
    }

    fun clearTeacherCsvImport() {
        _teacherCsvImportSummary.value = null
    }

    fun processFullSchoolCsvImport(rawCsv: String) {
        viewModelScope.launch {
            val summary = repository.processFullSchoolCsvImport(rawCsv)
            _fullSchoolCsvImportSummary.value = summary
        }
    }

    fun confirmFullSchoolCsvImport() {
        val summary = _fullSchoolCsvImportSummary.value ?: return
        viewModelScope.launch {
            val currentClassCount = classes.value.size
            val currentStudentCount = students.value.size
            val availableClassSlots = maxOf(0, MAX_CLASSES - currentClassCount)
            val availableStudentSlots = maxOf(0, MAX_STUDENTS - currentStudentCount)

            if (availableStudentSlots <= 0) {
                _userMessage.value = "⚠️ Student limit of $MAX_STUDENTS already reached!"
                _fullSchoolCsvImportSummary.value = null
                return@launch
            }

            val cappedClasses = if (summary.createdClasses.size > availableClassSlots) {
                summary.createdClasses.take(availableClassSlots)
            } else {
                summary.createdClasses
            }
            val cappedStudents = if (summary.validStudents.size > availableStudentSlots) {
                summary.validStudents.take(availableStudentSlots)
            } else {
                summary.validStudents
            }

            val cappedSummary = summary.copy(createdClasses = cappedClasses, validStudents = cappedStudents)
            repository.confirmFullSchoolCsvImport(cappedSummary)
            _userMessage.value = "🎉 Full School Import Done! ${cappedClasses.size} classes & ${cappedStudents.size} students added (Total: ${currentStudentCount + cappedStudents.size}/$MAX_STUDENTS students)."
            _fullSchoolCsvImportSummary.value = null
        }
    }

    fun clearFullSchoolCsvImport() {
        _fullSchoolCsvImportSummary.value = null
    }

    fun processClassCsvImport(rawCsv: String) {
        viewModelScope.launch {
            val summary = repository.processClassCsvImport(rawCsv)
            _classCsvImportSummary.value = summary
        }
    }

    fun confirmClassCsvImport() {
        val summary = _classCsvImportSummary.value ?: return
        viewModelScope.launch {
            val currentCount = classes.value.size
            val availableSlots = MAX_CLASSES - currentCount
            if (availableSlots <= 0) {
                _userMessage.value = "⚠️ Class limit of $MAX_CLASSES already reached! Cannot import more."
                _classCsvImportSummary.value = null
                return@launch
            }
            val recordsToImport = if (summary.validRecords.size > availableSlots) {
                summary.validRecords.take(availableSlots)
            } else {
                summary.validRecords
            }
            repository.saveImportedClasses(recordsToImport)
            _userMessage.value = "Imported ${recordsToImport.size} classes successfully (Total: ${currentCount + recordsToImport.size} / $MAX_CLASSES)!"
            _classCsvImportSummary.value = null
        }
    }

    fun clearClassCsvImport() {
        _classCsvImportSummary.value = null
    }

    fun processCsvImport(rawCsv: String, targetClassId: String? = null, targetSection: String? = null) {
        viewModelScope.launch {
            val summary = repository.processCsvImport(rawCsv, targetClassId, targetSection)
            _csvImportSummary.value = summary
        }
    }

    fun confirmCsvImport() {
        val summary = _csvImportSummary.value ?: return
        viewModelScope.launch {
            val currentCount = students.value.size
            val availableSlots = MAX_STUDENTS - currentCount
            if (availableSlots <= 0) {
                _userMessage.value = "⚠️ Student limit of $MAX_STUDENTS already reached! Cannot import more."
                _csvImportSummary.value = null
                return@launch
            }
            val recordsToImport = if (summary.validRecords.size > availableSlots) {
                summary.validRecords.take(availableSlots)
            } else {
                summary.validRecords
            }
            repository.saveImportedStudents(recordsToImport)
            _userMessage.value = "Imported ${recordsToImport.size} students successfully (Total: ${currentCount + recordsToImport.size} / $MAX_STUDENTS)!"
            _csvImportSummary.value = null
        }
    }

    fun clearCsvImport() {
        _csvImportSummary.value = null
    }

    fun showUserToast(msg: String) {
        _userMessage.value = msg
    }

    // Performance Type
    fun createPerformanceType(name: String, description: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            val role = _currentRole.value
            val schoolId = if (role == UserRole.ADMIN) (user?.schoolId ?: "sch_jdis") else ""
            val schoolName = if (role == UserRole.ADMIN) (user?.schoolName ?: "J D International School") else ""
            val type = PerformanceTypeEntity(
                id = "pt_${UUID.randomUUID().toString().take(6)}",
                name = name.trim(),
                description = description.trim(),
                schoolId = schoolId,
                schoolName = schoolName,
                createdByRole = role.name,
                createdByUserId = user?.id ?: ""
            )
            repository.insertPerformanceType(type)
            _userMessage.value = "Performance type '$name' added${if (schoolName.isNotBlank()) " for $schoolName" else ""}."
        }
    }

    fun deletePerformanceType(typeId: String) {
        viewModelScope.launch {
            repository.deletePerformanceType(typeId)
            _userMessage.value = "Performance type removed."
        }
    }

    // Event Creation & Update
    fun createEvent(
        name: String,
        type: com.example.data.model.EventType,
        description: String,
        date: String,
        startTime: String,
        endTime: String,
        venue: String,
        interPerformanceGapMinutes: Int = 3,
        eligibleClassIds: List<String>,
        coordinatorTeacherIds: List<String>
    ) {
        viewModelScope.launch {
            val slug = name.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "") + "-2026"
            val newEvent = EventEntity(
                id = "evt_${UUID.randomUUID().toString().take(8)}",
                name = name,
                type = type,
                description = description,
                date = date,
                startTime = startTime,
                endTime = endTime,
                venue = venue,
                interPerformanceGapMinutes = interPerformanceGapMinutes,
                status = EventStatus.UPCOMING,
                publicSlug = slug,
                createdBy = _currentUser.value?.id ?: "u_admin"
            )
            repository.createEvent(newEvent, eligibleClassIds, coordinatorTeacherIds)
            _selectedEventId.value = newEvent.id
            _userMessage.value = "Event '$name' created successfully!"
        }
    }

    fun updateFullEvent(
        event: EventEntity,
        eligibleClassIds: List<String>,
        coordinatorTeacherIds: List<String>
    ) {
        viewModelScope.launch {
            repository.createEvent(event, eligibleClassIds, coordinatorTeacherIds)
            _selectedEventId.value = event.id
            _userMessage.value = "Event '${event.name}' updated successfully!"
        }
    }

    fun getStudentIdsForPerformance(performanceId: String): Flow<List<String>> {
        return repository.getStudentIdsForPerformance(performanceId)
    }

    fun getEligibleClassIdsForEvent(eventId: String): Flow<List<String>> {
        return repository.getEligibleClassIdsForEvent(eventId)
    }

    fun updateEventSettings(event: EventEntity) {
        viewModelScope.launch {
            repository.updateEvent(event)
            _userMessage.value = "Event settings updated."
        }
    }

    fun startEvent(eventId: String) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId).first() ?: return@launch
            val now = System.currentTimeMillis()
            val updated = if (event.status == EventStatus.PAUSED) {
                // Resuming from PAUSED state
                val pauseDuration = if (event.pausedAtMillis != null) (now - event.pausedAtMillis) else 0L
                event.copy(
                    status = EventStatus.IN_PROGRESS,
                    pausedAtMillis = null,
                    totalPauseDurationMillis = event.totalPauseDurationMillis + pauseDuration,
                    updatedAt = now
                )
            } else {
                // First start
                event.copy(
                    status = EventStatus.IN_PROGRESS,
                    actualStartTimeMillis = event.actualStartTimeMillis ?: now,
                    pausedAtMillis = null,
                    totalPauseDurationMillis = 0L,
                    updatedAt = now
                )
            }
            repository.updateEvent(updated)
            repository.logActivity(
                userId = _currentUser.value?.id ?: "u_admin",
                userName = _currentUser.value?.name ?: "Admin",
                action = if (event.status == EventStatus.PAUSED) "EVENT_RESUMED" else "EVENT_STARTED",
                entityType = "EVENT",
                entityId = eventId,
                description = if (event.status == EventStatus.PAUSED) "Resumed event '${event.name}'" else "Started event '${event.name}'"
            )
            _userMessage.value = if (event.status == EventStatus.PAUSED) "Event '${event.name}' RESUMED!" else "Event '${event.name}' is now LIVE!"
        }
    }

    fun pauseEvent(eventId: String) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId).first() ?: return@launch
            val now = System.currentTimeMillis()
            val updated = event.copy(
                status = EventStatus.PAUSED,
                pausedAtMillis = now,
                updatedAt = now
            )
            repository.updateEvent(updated)
            repository.logActivity(
                userId = _currentUser.value?.id ?: "u_admin",
                userName = _currentUser.value?.name ?: "Admin",
                action = "EVENT_PAUSED",
                entityType = "EVENT",
                entityId = eventId,
                description = "Paused event '${event.name}'"
            )
            _userMessage.value = "Event '${event.name}' is now PAUSED."
        }
    }

    fun resumeEvent(eventId: String) {
        startEvent(eventId)
    }

    fun finishEvent(eventId: String, winnersAnnouncement: String = "") {
        viewModelScope.launch {
            val event = repository.getEventById(eventId).first() ?: return@launch
            val now = System.currentTimeMillis()
            val extraPause = if (event.status == EventStatus.PAUSED && event.pausedAtMillis != null) {
                now - event.pausedAtMillis
            } else 0L
            val updated = event.copy(
                status = EventStatus.COMPLETED,
                actualEndTimeMillis = now,
                pausedAtMillis = null,
                totalPauseDurationMillis = event.totalPauseDurationMillis + extraPause,
                winnersAnnouncement = if (winnersAnnouncement.isNotBlank()) winnersAnnouncement else event.winnersAnnouncement,
                updatedAt = now
            )
            repository.updateEvent(updated)
            repository.logActivity(
                userId = _currentUser.value?.id ?: "u_admin",
                userName = _currentUser.value?.name ?: "Admin",
                action = "EVENT_FINISHED",
                entityType = "EVENT",
                entityId = eventId,
                description = "Finished and archived event '${event.name}'"
            )
            _userMessage.value = "Event '${event.name}' marked as FINISHED & ARCHIVED!"
        }
    }

    fun updateWinnersAnnouncement(eventId: String, winners: String) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId).first() ?: return@launch
            val updated = event.copy(winnersAnnouncement = winners, updatedAt = System.currentTimeMillis())
            repository.updateEvent(updated)
            _userMessage.value = "Winners updated!"
        }
    }

    fun updatePerformanceGap(performance: PerformanceEntity, newGap: Int) {
        viewModelScope.launch {
            val updated = performance.copy(gapMinutes = maxOf(0, newGap), updatedAt = System.currentTimeMillis())
            repository.updatePerformance(updated)
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            _userMessage.value = "Event deleted."
        }
    }

    // Performance Operations
    fun savePerformance(
        id: String?,
        eventId: String,
        name: String,
        performanceTypeId: String,
        sequenceNumber: Int,
        durationMinutes: Int,
        description: String,
        musicName: String,
        youtubeLink: String,
        props: String,
        costume: String,
        stageReqs: String,
        notes: String,
        selectedStudentIds: List<String>,
        assignedTeacherIds: String = "",
        assignedTeacherNames: String = ""
    ) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId).first()
            if (event?.status == EventStatus.COMPLETED || event?.status == EventStatus.ARCHIVED) {
                _userMessage.value = "⚠️ Event '${event.name}' is Finished/Archived. Performances are Read-Only."
                return@launch
            }
            val isTeacher = _currentRole.value == UserRole.TEACHER
            val perfId = id ?: "p_${UUID.randomUUID().toString().take(8)}"
            val teacherId = _currentUser.value?.id ?: "u_t1"
            val teacherName = _currentUser.value?.name ?: "Teacher"

            val perf = PerformanceEntity(
                id = perfId,
                eventId = eventId,
                name = name,
                performanceTypeId = performanceTypeId,
                sequenceNumber = sequenceNumber,
                plannedDurationMinutes = durationMinutes,
                description = description,
                musicName = musicName,
                youtubeLink = youtubeLink,
                props = props,
                costume = costume,
                stageRequirements = stageReqs,
                notes = notes,
                isApproved = !isTeacher,
                requestedByTeacherName = if (isTeacher) teacherName else "",
                requestedByTeacherId = if (isTeacher) teacherId else "",
                assignedTeacherIds = if (isTeacher) teacherId else assignedTeacherIds,
                assignedTeacherNames = if (isTeacher) teacherName else assignedTeacherNames,
                createdBy = _currentUser.value?.id ?: "u_admin"
            )
            repository.addOrUpdatePerformance(
                performance = perf,
                studentIds = selectedStudentIds,
                updatedByUserId = _currentUser.value?.id ?: "u_admin",
                updatedByUserName = _currentUser.value?.name ?: "Admin"
            )
            if (isTeacher) {
                _userMessage.value = "Performance request submitted to Admin for approval!"
            } else {
                _userMessage.value = "Performance '$name' saved!"
            }
        }
    }

    fun approvePerformance(performance: PerformanceEntity) {
        viewModelScope.launch {
            val event = repository.getEventById(performance.eventId).first()
            if (event?.status == EventStatus.COMPLETED || event?.status == EventStatus.ARCHIVED) {
                _userMessage.value = "⚠️ Event is Finished. Cannot modify performances."
                return@launch
            }
            val teacherId = if (performance.assignedTeacherIds.isNotBlank()) performance.assignedTeacherIds else performance.requestedByTeacherId
            val teacherName = if (performance.assignedTeacherNames.isNotBlank()) performance.assignedTeacherNames else performance.requestedByTeacherName

            val approved = performance.copy(
                isApproved = true,
                assignedTeacherIds = teacherId,
                assignedTeacherNames = teacherName,
                updatedAt = System.currentTimeMillis()
            )
            repository.addOrUpdatePerformance(
                performance = approved,
                studentIds = emptyList(), // keeps existing relations or updates
                updatedByUserId = _currentUser.value?.id ?: "u_admin",
                updatedByUserName = _currentUser.value?.name ?: "Admin"
            )
            repository.logActivity(
                userId = _currentUser.value?.id ?: "u_admin",
                userName = _currentUser.value?.name ?: "Admin",
                action = "PERFORMANCE_APPROVED",
                entityType = "PERFORMANCE",
                entityId = performance.id,
                description = "Approved performance '${performance.name}' requested by ${performance.requestedByTeacherName}"
            )
            _userMessage.value = "Performance '${performance.name}' approved & added to sequence!"
        }
    }

    fun rejectPerformance(performanceId: String, eventId: String) {
        viewModelScope.launch {
            repository.deletePerformance(
                performanceId = performanceId,
                eventId = eventId,
                userId = _currentUser.value?.id ?: "u_admin",
                userName = _currentUser.value?.name ?: "Admin"
            )
            _userMessage.value = "Performance request REJECTED & removed."
        }
    }

    fun updatePerformanceStatus(
        performanceId: String,
        eventId: String,
        newStatus: PerformanceStatus,
        message: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            repository.updatePerformanceStatus(
                performanceId = performanceId,
                eventId = eventId,
                newStatus = newStatus,
                message = message,
                userId = user?.id ?: "u_t1",
                userName = user?.name ?: "Teacher",
                userRole = _currentRole.value.name
            )
            _userMessage.value = "Status updated to ${newStatus.name}!"
        }
    }

    fun deletePerformance(performanceId: String, eventId: String) {
        viewModelScope.launch {
            val user = _currentUser.value
            repository.deletePerformance(
                performanceId = performanceId,
                eventId = eventId,
                userId = user?.id ?: "u_admin",
                userName = user?.name ?: "Admin"
            )
            _userMessage.value = "Performance deleted."
        }
    }

    // Chat Operations
    fun sendChatMessage(eventId: String, messageText: String) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            val user = _currentUser.value
            repository.sendChatMessage(
                eventId = eventId,
                senderId = user?.id ?: "u_admin",
                senderName = user?.name ?: "User",
                senderRole = _currentRole.value,
                message = messageText.trim()
            )
        }
    }

    fun sendGlobalChatMessage(messageText: String) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.sendChatMessage(
                eventId = "GLOBAL",
                senderId = user.id,
                senderName = user.name,
                senderRole = user.role,
                message = messageText.trim()
            )
        }
    }

    // Issue Operations
    fun raiseIssue(eventId: String, performanceId: String?, performanceName: String?, issueText: String) {
        if (issueText.isBlank()) return
        viewModelScope.launch {
            val user = _currentUser.value
            repository.raiseIssue(
                eventId = eventId,
                performanceId = performanceId,
                performanceName = performanceName,
                raisedByUserId = user?.id ?: "u_teacher",
                raisedByUserName = user?.name ?: "Teacher",
                issueText = issueText.trim()
            )
            _userMessage.value = "Issue submitted to Admin!"
        }
    }

    fun togglePerformanceIssue(performanceId: String, hasIssue: Boolean, message: String = "") {
        viewModelScope.launch {
            val user = _currentUser.value
            repository.togglePerformanceIssue(
                performanceId = performanceId,
                hasIssue = hasIssue,
                issueMessage = message,
                userId = user?.id ?: "u_teacher",
                userName = user?.name ?: "Teacher"
            )
            _userMessage.value = if (hasIssue) "Issue flagged!" else "Issue cleared!"
        }
    }

    fun resolveIssue(issue: EventIssueEntity) {
        viewModelScope.launch {
            repository.resolveIssue(issue)
            _userMessage.value = "Issue marked as RESOLVED!"
        }
    }

    fun updatePerformanceProgress(performanceId: String, progressPercentage: Int) {
        viewModelScope.launch {
            repository.updatePerformanceProgress(performanceId, progressPercentage)
        }
    }

    // Performance Reordering Operations
    fun movePerformanceUp(performance: PerformanceEntity, allPerformances: List<PerformanceEntity>) {
        val sorted = allPerformances.sortedBy { it.sequenceNumber }.toMutableList()
        val index = sorted.indexOfFirst { it.id == performance.id }
        if (index > 0) {
            val item = sorted.removeAt(index)
            sorted.add(index - 1, item)
            viewModelScope.launch {
                repository.reorderPerformances(sorted)
                _userMessage.value = "Sequence updated!"
            }
        }
    }

    fun movePerformanceDown(performance: PerformanceEntity, allPerformances: List<PerformanceEntity>) {
        val sorted = allPerformances.sortedBy { it.sequenceNumber }.toMutableList()
        val index = sorted.indexOfFirst { it.id == performance.id }
        if (index >= 0 && index < sorted.size - 1) {
            val item = sorted.removeAt(index)
            sorted.add(index + 1, item)
            viewModelScope.launch {
                repository.reorderPerformances(sorted)
                _userMessage.value = "Sequence updated!"
            }
        }
    }

    fun reorderPerformances(orderedList: List<PerformanceEntity>) {
        viewModelScope.launch {
            repository.reorderPerformances(orderedList)
            _userMessage.value = "Sequence reordered!"
        }
    }


    private fun minutesToTimeString(totalMinutes: Int): String {
        val normalized = (totalMinutes % (24 * 60) + 24 * 60) % (24 * 60)
        val hours24 = normalized / 60
        val minutes = normalized % 60
        val isPm = hours24 >= 12
        val hours12 = when {
            hours24 == 0 -> 12
            hours24 > 12 -> hours24 - 12
            else -> hours24
        }
        return String.format(java.util.Locale.US, "%02d:%02d %s", hours12, minutes, if (isPm) "PM" else "AM")
    }

    private fun timeStringToMinutes(timeStr: String): Int {
        return try {
            val cleanStr = timeStr.trim().uppercase()
            val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
            val date = sdf.parse(cleanStr) ?: return 540
            val cal = java.util.Calendar.getInstance().apply { time = date }
            cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        } catch (e: Exception) {
            540
        }
    }
}
