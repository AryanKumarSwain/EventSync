package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.EventStatus
import com.example.data.model.EventType
import com.example.data.model.PerformanceStatus
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.TEACHER)

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = runCatching { EventType.valueOf(value) }.getOrDefault(EventType.FUNCTION)

    @TypeConverter
    fun fromEventStatus(value: EventStatus): String = value.name

    @TypeConverter
    fun toEventStatus(value: String): EventStatus = runCatching { EventStatus.valueOf(value) }.getOrDefault(EventStatus.UPCOMING)

    @TypeConverter
    fun fromPerformanceStatus(value: PerformanceStatus): String = value.name

    @TypeConverter
    fun toPerformanceStatus(value: String): PerformanceStatus = runCatching { PerformanceStatus.valueOf(value) }.getOrDefault(PerformanceStatus.NOT_STARTED)
}
