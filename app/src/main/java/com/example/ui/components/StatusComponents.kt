package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EventHealth
import com.example.data.model.EventStatus
import com.example.data.model.PerformanceStatus
import com.example.data.model.TimeCalculationResult
import com.example.data.model.UserRole

val GreenStatus = Color(0xFF2E7D32)
val GreenBackground = Color(0xFFE8F5E9)
val YellowStatus = Color(0xFFF57F17)
val YellowBackground = Color(0xFFFFFDE7)
val RedStatus = Color(0xFFC62828)
val RedBackground = Color(0xFFFFEBEE)
val BlueStatus = Color(0xFF1565C0)
val BlueBackground = Color(0xFFE3F2FD)
val GrayStatus = Color(0xFF616161)
val GrayBackground = Color(0xFFF5F5F5)

@Composable
fun StatusBadge(
    statusName: String,
    modifier: Modifier = Modifier
) {
    val uppercaseStatus = statusName.uppercase()
    val (bgColor, textColor, icon) = when (uppercaseStatus) {
        "READY", "NO_ISSUES", "NO ISSUES", "COMPLETED", "ON_TRACK", "ACTIVE" -> Triple(GreenBackground, GreenStatus, Icons.Default.CheckCircle)
        "IN_PROGRESS", "NEEDS_ATTENTION", "PREPARING" -> Triple(YellowBackground, YellowStatus, Icons.Default.Pending)
        "PAUSED" -> Triple(YellowBackground, YellowStatus, Icons.Default.PauseCircle)
        "ARCHIVED" -> Triple(GrayBackground, GrayStatus, Icons.Default.Archive)
        "ISSUE", "CRITICAL", "CANCELLED" -> Triple(RedBackground, RedStatus, Icons.Default.Error)
        "INFORMATION" -> Triple(BlueBackground, BlueStatus, Icons.Default.Info)
        else -> Triple(GrayBackground, GrayStatus, Icons.Default.RadioButtonUnchecked)
    }

    val displayText = when (uppercaseStatus) {
        "READY" -> "NO ISSUES"
        else -> statusName.replace("_", " ")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.testTag("status_badge_${statusName.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = displayText,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun EventHealthCard(
    health: EventHealth,
    progressPercentage: Int,
    modifier: Modifier = Modifier
) {
    val (healthLabel, healthColor, healthBg) = when (health) {
        EventHealth.ON_TRACK -> Triple("🟢 ON TRACK", GreenStatus, GreenBackground)
        EventHealth.NEEDS_ATTENTION -> Triple("🟡 NEEDS ATTENTION", YellowStatus, YellowBackground)
        EventHealth.CRITICAL -> Triple("🔴 CRITICAL", RedStatus, RedBackground)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = healthBg),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, healthColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .testTag("event_health_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EVENT HEALTH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = healthLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = healthColor
                    )
                }

                Surface(
                    color = healthColor,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "$progressPercentage% Progress",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = healthColor,
                trackColor = healthColor.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun TimeSummaryBanner(
    timeResult: TimeCalculationResult,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (timeResult.isFinished) GreenBackground else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("time_summary_banner")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (timeResult.isFinished) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = "Timing",
                        tint = if (timeResult.isFinished) GreenStatus else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (timeResult.isFinished) "Final Event Time Calculation" else "Estimated Event Timing",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (timeResult.isFinished) GreenStatus else MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = if (timeResult.isFinished) GreenStatus.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (timeResult.isFinished) "COMPLETED" else "ASSUMED",
                        color = if (timeResult.isFinished) GreenStatus else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (timeResult.isFinished) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeStatBox(
                        label = "Actual Duration",
                        value = if (timeResult.actualDurationFormatted.isNotBlank()) timeResult.actualDurationFormatted else "0m",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeStatBox(
                        label = "Started At",
                        value = if (timeResult.actualStartTimeFormatted.isNotBlank()) timeResult.actualStartTimeFormatted else "-",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeStatBox(
                        label = "Ended At",
                        value = if (timeResult.actualEndTimeFormatted.isNotBlank()) timeResult.actualEndTimeFormatted else "-",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeStatBox(
                        label = "Items Done",
                        value = "${timeResult.completedPerformancesCount}/${timeResult.totalPerformancesCount}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🎉 Event has finished. Final time calculation reflects actual stage execution time (excluding pause intervals).",
                    color = GreenStatus,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeStatBox(
                        label = "Total Assumed",
                        value = formatMinutes(timeResult.totalEstimatedTimeMinutes),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeStatBox(
                        label = "Performances",
                        value = formatMinutes(timeResult.plannedPerformanceDurationMinutes),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeStatBox(
                        label = "Gaps (${timeResult.interPerformanceGapMinutes}m)",
                        value = formatMinutes(timeResult.totalGapTimeMinutes),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TimeStatBox(
                        label = "Est. End Time",
                        value = if (timeResult.estimatedEndTimeFormatted.isNotBlank()) timeResult.estimatedEndTimeFormatted else "-",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Assumed timing (${formatMinutes(timeResult.totalEstimatedTimeMinutes)}, ending around ${timeResult.estimatedEndTimeFormatted}) is calculated from all planned performance durations plus ${timeResult.interPerformanceGapMinutes}m transition gaps between items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RealtimeEventCountdownCard(
    eventDateStr: String,
    startTimeStr: String,
    eventStatus: EventStatus,
    actualStartTimeMillis: Long? = null,
    actualEndTimeMillis: Long? = null,
    pausedAtMillis: Long? = null,
    totalPauseDurationMillis: Long = 0L,
    modifier: Modifier = Modifier
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val targetMillis = remember(eventDateStr, startTimeStr) {
        parseEventStartMillis(eventDateStr, startTimeStr)
    }

    val diffMillis = targetMillis - currentTimeMillis

    val cardColor = when (eventStatus) {
        EventStatus.COMPLETED, EventStatus.ARCHIVED -> GreenBackground
        EventStatus.PAUSED -> Color(0xFFFFF8E1)
        EventStatus.IN_PROGRESS -> BlueBackground
        EventStatus.UPCOMING -> if (diffMillis <= 0) RedBackground else MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_event_countdown")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (eventStatus) {
                            EventStatus.COMPLETED, EventStatus.ARCHIVED -> Icons.Default.CheckCircle
                            EventStatus.PAUSED -> Icons.Default.PauseCircle
                            EventStatus.IN_PROGRESS -> Icons.Default.PlayCircle
                            EventStatus.UPCOMING -> if (diffMillis <= 0) Icons.Default.Warning else Icons.Default.Timer
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = "Realtime Timer",
                        tint = when (eventStatus) {
                            EventStatus.COMPLETED, EventStatus.ARCHIVED -> GreenStatus
                            EventStatus.PAUSED -> Color(0xFFE65100)
                            EventStatus.IN_PROGRESS -> BlueStatus
                            EventStatus.UPCOMING -> if (diffMillis <= 0) RedStatus else MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (eventStatus) {
                            EventStatus.COMPLETED, EventStatus.ARCHIVED -> "EVENT COMPLETED"
                            EventStatus.PAUSED -> "⏸️ EVENT TEMPORARILY PAUSED"
                            EventStatus.IN_PROGRESS -> "LIVE EVENT IN PROGRESS"
                            EventStatus.UPCOMING -> if (diffMillis <= 0) "⚠️ EVENT START DELAYED" else "REAL-TIME COUNTDOWN"
                            else -> "EVENT SCHEDULED"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = when (eventStatus) {
                            EventStatus.COMPLETED, EventStatus.ARCHIVED -> GreenStatus
                            EventStatus.PAUSED -> Color(0xFFE65100)
                            EventStatus.IN_PROGRESS -> BlueStatus
                            EventStatus.UPCOMING -> if (diffMillis <= 0) RedStatus else MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (eventStatus) {
                            EventStatus.COMPLETED, EventStatus.ARCHIVED -> "FINAL TIME"
                            EventStatus.PAUSED -> "PAUSED ⏸️"
                            EventStatus.IN_PROGRESS -> "LIVE TICKER ⏱️"
                            EventStatus.UPCOMING -> if (diffMillis <= 0) "DELAYED" else "COUNTDOWN"
                            else -> "STATUS"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (eventStatus) {
                EventStatus.COMPLETED, EventStatus.ARCHIVED -> {
                    val startReference = actualStartTimeMillis ?: targetMillis
                    val endReference = actualEndTimeMillis ?: currentTimeMillis
                    val rawDiff = endReference - startReference
                    val activeMillis = maxOf(0L, rawDiff - totalPauseDurationMillis)
                    val finSecs = activeMillis / 1000
                    val finMins = finSecs / 60
                    val finHours = finMins / 60
                    val remainingMins = finMins % 60
                    val remainingSecs = finSecs % 60

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimerDigitBox(value = String.format("%02dh", finHours), label = "HOURS", color = GreenStatus)
                        TimerDigitBox(value = String.format("%02dm", remainingMins), label = "MINS", color = GreenStatus)
                        TimerDigitBox(value = String.format("%02ds", remainingSecs), label = "TOTAL", color = GreenStatus)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🎉 All performances completed successfully! Total runtime recorded.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = GreenStatus
                    )
                }

                EventStatus.PAUSED -> {
                    val startReference = actualStartTimeMillis ?: targetMillis
                    val pauseReference = pausedAtMillis ?: currentTimeMillis
                    val rawDiff = pauseReference - startReference
                    val activeMillis = maxOf(0L, rawDiff - totalPauseDurationMillis)
                    val pausedSecs = activeMillis / 1000
                    val pausedMins = pausedSecs / 60
                    val pausedHours = pausedMins / 60
                    val remainingMins = pausedMins % 60
                    val remainingSecs = pausedSecs % 60

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimerDigitBox(value = String.format("%02dh", pausedHours), label = "HOURS", color = Color(0xFFE65100))
                        TimerDigitBox(value = String.format("%02dm", remainingMins), label = "MINS", color = Color(0xFFE65100))
                        TimerDigitBox(value = String.format("%02ds", remainingSecs), label = "PAUSED", color = Color(0xFFE65100))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Event is paused. Live elapsed timer is frozen and will resume automatically when stage action continues.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }

                EventStatus.IN_PROGRESS -> {
                    val startReference = actualStartTimeMillis ?: targetMillis
                    val rawDiff = currentTimeMillis - startReference
                    val activeMillis = maxOf(0L, rawDiff - totalPauseDurationMillis)
                    val elapsedSecs = activeMillis / 1000
                    val elapsedMins = elapsedSecs / 60
                    val elapsedHours = elapsedMins / 60
                    val remainingMins = elapsedMins % 60
                    val remainingSecs = elapsedSecs % 60

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimerDigitBox(value = String.format("%02dh", elapsedHours), label = "HOURS", color = BlueStatus)
                        TimerDigitBox(value = String.format("%02dm", remainingMins), label = "MINS", color = BlueStatus)
                        TimerDigitBox(value = String.format("%02ds", remainingSecs), label = "RUNNING", color = BlueStatus)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (totalPauseDurationMillis > 0) {
                            "⏱️ Live active elapsed time (${totalPauseDurationMillis / 60000}m paused duration excluded)."
                        } else {
                            "⏱️ Live active stage timer running in real-time."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = BlueStatus
                    )
                }

                EventStatus.UPCOMING -> {
                    if (diffMillis <= 0) {
                        val delaySecs = (-diffMillis) / 1000
                        val delayMins = delaySecs / 60
                        val delayHours = delayMins / 60
                        val remainingMins = delayMins % 60
                        val remainingSecs = delaySecs % 60

                        Text(
                            text = "Scheduled start ($startTimeStr) passed. Event has not been started yet:",
                            style = MaterialTheme.typography.bodySmall,
                            color = RedStatus
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimerDigitBox(value = String.format("%02dh", delayHours), label = "HOURS", color = RedStatus)
                            TimerDigitBox(value = String.format("%02dm", remainingMins), label = "MINS", color = RedStatus)
                            TimerDigitBox(value = String.format("%02ds", remainingSecs), label = "DELAY", color = RedStatus)
                        }
                    } else {
                        val totalSecs = diffMillis / 1000
                        val days = totalSecs / (24 * 3600)
                        val hours = (totalSecs % (24 * 3600)) / 3600
                        val minutes = (totalSecs % 3600) / 60
                        val seconds = totalSecs % 60

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (days > 0) {
                                TimerDigitBox(value = String.format("%02dd", days), label = "DAYS")
                            }
                            TimerDigitBox(value = String.format("%02dh", hours), label = "HOURS")
                            TimerDigitBox(value = String.format("%02dm", minutes), label = "MINS")
                            TimerDigitBox(value = String.format("%02ds", seconds), label = "SECS")
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Event scheduled for $startTimeStr.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerDigitBox(
    value: String,
    label: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun parseEventStartMillis(dateStr: String, startTimeStr: String): Long {
    return try {
        val fullStr = "$dateStr $startTimeStr"
        val sdf = java.text.SimpleDateFormat("dd MMMM yyyy hh:mm a", java.util.Locale.US)
        sdf.parse(fullStr)?.time ?: (System.currentTimeMillis() + 86400000)
    } catch (e: Exception) {
        try {
            val sdf2 = java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.US)
            sdf2.parse("$dateStr $startTimeStr")?.time ?: (System.currentTimeMillis() + 86400000)
        } catch (e2: Exception) {
            System.currentTimeMillis() + 86400000
        }
    }
}

@Composable
private fun TimeStatBox(
    label: String,
    value: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isWarning) RedStatus.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isWarning) RedStatus else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun QRCodeView(
    eventSlug: String,
    eventName: String,
    modifier: Modifier = Modifier,
    eventDate: String = "",
    venue: String = "",
    onOpenPublicPreview: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val fullUrl = remember(eventSlug) { QRCodeUtils.buildPublicEventUrl(eventSlug) }
    var copied by remember { mutableStateOf(false) }

    // Generate genuine scannable QR Bitmap
    val qrBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = fullUrl) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            QRCodeUtils.generateQRCodeBitmap(fullUrl, sizePx = 600)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("qr_code_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Public Event QR Code & Link",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Scan with any camera or share live link",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Live Scannable",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Genuine High-Resolution Scannable QR Code Box
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "Event QR Code for $eventName",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Centered Badge Overlay
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = "EventSync",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generating QR Code...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Full URL Box with Copy Button
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PUBLIC SHARING LINK",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = fullUrl,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(fullUrl))
                            copied = true
                            android.widget.Toast.makeText(context, "✅ Public link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copied) GreenStatus else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("copy_link_button")
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "Copy Link",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (copied) "Copied!" else "Copy")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Share Link + Share QR Image + Download
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        QRCodeUtils.shareEventViaApps(
                            context = context,
                            eventName = eventName,
                            eventDate = eventDate,
                            venue = venue,
                            publicUrl = fullUrl
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_qr_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share Link")
                }

                if (qrBitmap != null) {
                    OutlinedButton(
                        onClick = {
                            QRCodeUtils.shareQRCodeImage(
                                context = context,
                                eventName = eventName,
                                bitmap = qrBitmap!!,
                                publicUrl = fullUrl
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = "Share QR Image", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Share QR")
                    }

                    FilledTonalIconButton(
                        onClick = {
                            QRCodeUtils.saveQRCodeToGallery(
                                context = context,
                                eventName = eventName,
                                bitmap = qrBitmap!!
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Download QR Code")
                    }
                }
            }

            if (onOpenPublicPreview != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onOpenPublicPreview,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview Public Page Live")
                }
            }
        }
    }
}


@Composable
fun EventTypeTag(
    eventType: com.example.data.model.EventType,
    modifier: Modifier = Modifier
) {
    val (label, containerColor, contentColor) = when (eventType) {
        com.example.data.model.EventType.COMPETITION -> Triple("🏆 Competition", Color(0xFFFFF3E0), Color(0xFFE65100))
        com.example.data.model.EventType.FUNCTION -> Triple("🎉 Celebration / Function", Color(0xFFF3E5F5), Color(0xFF7B1FA2))
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
