package com.example.data.model

enum class UserRole {
    SUPER_ADMIN,
    ADMIN,
    TEACHER,
    PUBLIC
}

enum class EventType {
    FUNCTION,
    COMPETITION
}

enum class EventStatus {
    DRAFT,
    UPCOMING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED,
    ARCHIVED
}

enum class PerformanceStatus {
    NOT_STARTED,
    IN_PROGRESS,
    READY,
    ISSUE,
    COMPLETED
}

enum class EventHealth {
    ON_TRACK,
    NEEDS_ATTENTION,
    CRITICAL
}

data class TimeCalculationResult(
    val plannedPerformanceDurationMinutes: Int = 0,
    val interPerformanceGapMinutes: Int = 3,
    val totalGapTimeMinutes: Int = 0,
    val totalEstimatedTimeMinutes: Int = 0,
    val estimatedEndTimeFormatted: String = "",
    val isFinished: Boolean = false,
    val actualDurationMinutes: Int = 0,
    val actualDurationFormatted: String = "",
    val actualStartTimeFormatted: String = "",
    val actualEndTimeFormatted: String = "",
    val totalPerformancesCount: Int = 0,
    val completedPerformancesCount: Int = 0
)
