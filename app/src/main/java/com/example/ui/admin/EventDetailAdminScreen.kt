package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.data.local.EventEntity
import com.example.data.local.EventIssueEntity
import com.example.data.local.PerformanceEntity
import com.example.data.model.PerformanceStatus
import com.example.data.model.UserRole

import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.*
import com.example.ui.components.EventHealthCard
import com.example.ui.components.QRCodeView
import com.example.ui.components.RealtimeEventCountdownCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TimeSummaryBanner
import com.example.ui.viewmodel.EventSyncViewModel

@Composable
fun EventDetailAdminScreen(
    viewModel: EventSyncViewModel,
    onNavigateToAddPerformance: (String) -> Unit,
    onNavigateToEditPerformance: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentEvent by viewModel.currentEvent.collectAsState()
    val performances by viewModel.currentPerformances.collectAsState()
    val timeEngine by viewModel.timeEngineResult.collectAsState()
    val eventHealth by viewModel.eventHealth.collectAsState()
    val updates by viewModel.currentEventUpdates.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Overview", "Performances", "Sequence", "Schedule", "Issues", "Activity", "Event Chat")

    val event = currentEvent ?: return Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Event not found")
    }

    Column(modifier = modifier.fillMaxSize()) {
        val isArchived = event.status == com.example.data.model.EventStatus.COMPLETED || event.status == com.example.data.model.EventStatus.ARCHIVED

        if (isArchived) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔒 Event Archived (View Only Mode - Editing Disabled)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Event Header Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.components.EventTypeTag(eventType = event.type)
                    StatusBadge(statusName = event.status.name)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = event.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${event.date} • ${event.startTime}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = event.venue, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Horizontal Segmented Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    modifier = Modifier.testTag("event_tab_$index")
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            val approvedPerfs = performances.filter { it.isApproved }
            when (selectedTab) {
                0 -> EventOverviewTab(event, eventHealth, timeEngine, performances.size, updates.size, viewModel)
                1 -> EventPerformancesTab(event.id, performances, onNavigateToAddPerformance, onNavigateToEditPerformance, viewModel)
                2 -> EventSequenceTab(approvedPerfs, viewModel)
                3 -> EventScheduleTab(
                    event = event,
                    performances = approvedPerfs,
                    showPublicQr = true,
                    onOpenPublicPreview = { viewModel.selectRole(UserRole.PUBLIC) },
                    viewModel = viewModel
                )
                4 -> EventIssuesTab(event.id, performances, viewModel, isAdmin = true)
                5 -> EventActivityTab(updates)
                6 -> EventChatTab(event.id, viewModel)
            }
        }
    }

}

private fun formatDurationMillis(
    startMillis: Long,
    endMillis: Long?,
    pausedAtMillis: Long? = null,
    totalPauseDurationMillis: Long = 0L,
    status: com.example.data.model.EventStatus = com.example.data.model.EventStatus.IN_PROGRESS
): String {
    val referenceEnd = when {
        status == com.example.data.model.EventStatus.COMPLETED && endMillis != null -> endMillis
        status == com.example.data.model.EventStatus.PAUSED && pausedAtMillis != null -> pausedAtMillis
        else -> System.currentTimeMillis()
    }
    val rawDiff = referenceEnd - startMillis
    val activeMillis = maxOf(0L, rawDiff - totalPauseDurationMillis)
    val diffSec = activeMillis / 1000
    val hours = diffSec / 3600
    val mins = (diffSec % 3600) / 60
    val secs = diffSec % 60
    return if (hours > 0) {
        "${hours}h ${mins}m ${secs}s"
    } else {
        "${mins}m ${secs}s"
    }
}

@Composable
private fun EventOverviewTab(
    event: EventEntity,
    health: com.example.data.model.EventHealth,
    timeEngine: com.example.data.model.TimeCalculationResult,
    performanceCount: Int,
    updateCount: Int,
    viewModel: EventSyncViewModel
) {
    val performances by viewModel.currentPerformances.collectAsState()
    var showStartConfirmDialog by remember { mutableStateOf(false) }
    var showPauseConfirmDialog by remember { mutableStateOf(false) }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }
    var showWinnersDialog by remember { mutableStateOf(false) }
    var winnersTextState by remember(event.winnersAnnouncement) { mutableStateOf(event.winnersAnnouncement) }

    val isArchived = event.status == com.example.data.model.EventStatus.COMPLETED || event.status == com.example.data.model.EventStatus.ARCHIVED

    // Start Event Alert
    if (showStartConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStartConfirmDialog = false },
            icon = { Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Start Live Event?") },
            text = { Text("Are you sure you want to start '${event.name}' live now?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.startEvent(event.id)
                    showStartConfirmDialog = false
                }) {
                    Text("Yes, Start Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Pause Event Alert
    if (showPauseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPauseConfirmDialog = false },
            icon = { Icon(Icons.Default.PauseCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
            title = { Text("Pause Event?") },
            text = { Text("Are you sure you want to pause '${event.name}'?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.pauseEvent(event.id)
                    showPauseConfirmDialog = false
                }) {
                    Text("Yes, Pause Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPauseConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Finish Event Alert (Function / Celebration)
    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            icon = { Icon(Icons.Default.StopCircle, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Finish & Archive Event?") },
            text = { Text("Are you sure you want to finish '${event.name}'? The event will be archived and become View Only.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishEvent(event.id)
                        showFinishConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Finish & Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Competition Winners Dialog (When Ending Event or Editing Winners)
    if (showWinnersDialog) {
        AlertDialog(
            onDismissRequest = { showWinnersDialog = false },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFE65100)) },
            title = { Text("🏆 Competition Winners") },
            text = {
                Column {
                    Text(
                        text = "Enter position holders / winners (1st, 2nd, 3rd position, etc.) to announce publicly:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = winnersTextState,
                        onValueChange = { winnersTextState = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("1st Position: Class 10A (Dance)\n2nd Position: Class 8B (Skit)\n3rd Position: Class 9C (Music)") },
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishEvent(event.id, winnersTextState)
                        showWinnersDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("Save Winners & Finish Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWinnersDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Winners Card (If Competition or Winners set)
        if (event.type == com.example.data.model.EventType.COMPETITION || event.winnersAnnouncement.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🏆 Competition Winners",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFFE65100)
                                )
                            }

                            TextButton(onClick = { showWinnersDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Winners", color = Color(0xFFE65100))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        if (event.winnersAnnouncement.isNotBlank()) {
                            Text(
                                text = event.winnersAnnouncement,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = "No winners announced yet. Tap 'Edit Winners' or finish the event to set winners.",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Start Event / Pause Event / Finish Event Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚡ Event Execution & Live Tracker",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (event.actualStartTimeMillis != null) {
                        val duration = formatDurationMillis(
                            startMillis = event.actualStartTimeMillis,
                            endMillis = event.actualEndTimeMillis,
                            pausedAtMillis = event.pausedAtMillis,
                            totalPauseDurationMillis = event.totalPauseDurationMillis,
                            status = event.status
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (event.status) {
                                    com.example.data.model.EventStatus.COMPLETED, com.example.data.model.EventStatus.ARCHIVED -> "Total Calculated Duration: $duration"
                                    com.example.data.model.EventStatus.PAUSED -> "Live Time Elapsed (Paused): $duration"
                                    else -> "Live Time Elapsed: $duration"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Start / Resume Button
                        Button(
                            onClick = { showStartConfirmDialog = true },
                            enabled = !isArchived && event.status != com.example.data.model.EventStatus.IN_PROGRESS,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (event.status == com.example.data.model.EventStatus.PAUSED) "Resume" else "Start",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }

                        // Pause Button
                        Button(
                            onClick = { showPauseConfirmDialog = true },
                            enabled = !isArchived && event.status == com.example.data.model.EventStatus.IN_PROGRESS,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Pause",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }

                        // Finish Button
                        Button(
                            onClick = {
                                if (event.type == com.example.data.model.EventType.COMPETITION) {
                                    showWinnersDialog = true
                                } else {
                                    showFinishConfirmDialog = true
                                }
                            },
                            enabled = !isArchived,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Finish",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        item {
            val avgProgress = if (performanceCount > 0) performances.map { it.progressPercentage }.average().toInt() else 80
            EventHealthCard(health = health, progressPercentage = avgProgress)
        }

        item {
            RealtimeEventCountdownCard(
                eventDateStr = event.date,
                startTimeStr = event.startTime,
                eventStatus = event.status,
                actualStartTimeMillis = event.actualStartTimeMillis,
                actualEndTimeMillis = event.actualEndTimeMillis,
                pausedAtMillis = event.pausedAtMillis,
                totalPauseDurationMillis = event.totalPauseDurationMillis
            )
        }

        item {
            TimeSummaryBanner(timeResult = timeEngine)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About Event",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Programs: $performanceCount", style = MaterialTheme.typography.labelLarge)
                        Text("Live Updates: $updateCount", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventPerformancesTab(
    eventId: String,
    performances: List<PerformanceEntity>,
    onNavigateToAddPerformance: (String) -> Unit,
    onNavigateToEditPerformance: (String, String) -> Unit,
    viewModel: EventSyncViewModel
) {
    val currentEvent by viewModel.currentEvent.collectAsState()
    val isArchived = currentEvent?.status == com.example.data.model.EventStatus.COMPLETED || currentEvent?.status == com.example.data.model.EventStatus.ARCHIVED

    val pendingRequests = performances.filter { !it.isApproved }
    val approvedPerformances = performances.filter { it.isApproved }.sortedBy { it.sequenceNumber }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Approved Programs (${approvedPerformances.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (!isArchived) {
                    Button(
                        onClick = { onNavigateToAddPerformance(eventId) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_performance_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Performance")
                    }
                }
            }
        }

        // Pending Requests Approval Section
        if (pendingRequests.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PendingActions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pending Teacher Requests (${pendingRequests.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        pendingRequests.forEach { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = req.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${req.plannedDurationMinutes} mins",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (req.requestedByTeacherName.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Requested By: Tr. ${req.requestedByTeacherName}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (req.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = req.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.approvePerformance(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve")
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.rejectPerformance(req.id, eventId) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        items(approvedPerformances) { perf ->
            val eventIssues by viewModel.currentEventIssues.collectAsState()
            val perfIssues = eventIssues.filter { it.performanceId == perf.id && !it.isResolved }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isArchived) Modifier.clickable { onNavigateToEditPerformance(eventId, perf.id) }
                        else Modifier
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${perf.sequenceNumber}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = perf.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isArchived) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "🔒 Read Only",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            StatusBadge(statusName = perf.status.name)
                        }
                    }

                    if (perfIssues.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "⚠️ Issue: ${perfIssues.first().issueText} (by ${perfIssues.first().raisedByUserName})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Teacher Coordinator Badge
                    val teacherCoordinator = when {
                        perf.assignedTeacherNames.isNotBlank() -> perf.assignedTeacherNames
                        perf.requestedByTeacherName.isNotBlank() -> perf.requestedByTeacherName
                        else -> "Unassigned"
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Teacher Coordinator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Teacher Coordinator: $teacherCoordinator",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${perf.plannedDurationMinutes} minutes • Start: ${perf.plannedStartTime.ifBlank { "09:00 AM" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (perf.musicName.isNotBlank() || perf.props.isNotBlank() || perf.costume.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (perf.musicName.isNotBlank()) {
                                Text("🎵 ${perf.musicName}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (perf.props.isNotBlank()) {
                                Text("🎭 ${perf.props}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }

                    // Teacher Rehearsal Progress Indicator (Viewable by Admin)
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 Teacher Rehearsal Progress",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${perf.progressPercentage}% Ready",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (perf.progressPercentage >= 80) GreenStatus else if (perf.progressPercentage >= 40) YellowStatus else RedStatus
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { perf.progressPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (perf.progressPercentage >= 80) GreenStatus else if (perf.progressPercentage >= 40) YellowStatus else RedStatus,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventSequenceTab(
    performances: List<PerformanceEntity>,
    viewModel: EventSyncViewModel
) {
    val sortedList = remember(performances) { performances.sortedBy { it.sequenceNumber } }
    val currentEvent by viewModel.currentEvent.collectAsState()
    val isArchived = currentEvent?.status == com.example.data.model.EventStatus.COMPLETED || currentEvent?.status == com.example.data.model.EventStatus.ARCHIVED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Reorder Performance Sequence & Edit Inter-Performance Gap (in minutes). Adjusting gaps recalculates overall schedule dynamically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        items(sortedList, key = { it.id }) { perf ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${perf.sequenceNumber}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = perf.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                val teacherCoord = when {
                                    perf.assignedTeacherNames.isNotBlank() -> perf.assignedTeacherNames
                                    perf.requestedByTeacherName.isNotBlank() -> perf.requestedByTeacherName
                                    else -> ""
                                }
                                Text(
                                    text = "Duration: ${perf.plannedDurationMinutes} mins" + if (teacherCoord.isNotBlank()) " • Tr. $teacherCoord" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!isArchived) {
                            Row {
                                IconButton(
                                    onClick = { viewModel.movePerformanceUp(perf, sortedList) },
                                    enabled = sortedList.firstOrNull()?.id != perf.id
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                                }

                                IconButton(
                                    onClick = { viewModel.movePerformanceDown(perf, sortedList) },
                                    enabled = sortedList.lastOrNull()?.id != perf.id
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                                }
                            }
                        }
                    }

                    // Gap Control Section
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MoreTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gap after performance:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isArchived) {
                                IconButton(
                                    onClick = { viewModel.updatePerformanceGap(perf, maxOf(0, perf.gapMinutes - 1)) },
                                    modifier = Modifier.size(28.dp),
                                    enabled = perf.gapMinutes > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveCircleOutline,
                                        contentDescription = "Decrease Gap",
                                        tint = if (perf.gapMinutes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "${perf.gapMinutes} mins gap",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            if (!isArchived) {
                                IconButton(
                                    onClick = { viewModel.updatePerformanceGap(perf, perf.gapMinutes + 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = "Increase Gap",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventScheduleTab(
    event: EventEntity,
    performances: List<PerformanceEntity>,
    showPublicQr: Boolean = false,
    onOpenPublicPreview: (() -> Unit)? = null,
    viewModel: EventSyncViewModel? = null,
    canMarkPerformed: Boolean = true
) {
    val context = LocalContext.current
    val sortedPerfs = remember(performances) { performances.sortedBy { it.sequenceNumber } }
    val totalCount = sortedPerfs.size
    val performedCount = sortedPerfs.count { it.status == PerformanceStatus.COMPLETED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Schedule Timeline: ${event.name}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$performedCount of $totalCount Performed (${if (totalCount > 0) (performedCount * 100 / totalCount) else 0}%)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (performedCount == totalCount && totalCount > 0) GreenStatus else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = { generateEventSchedulePdf(context, event, sortedPerfs) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .size(36.dp)
                                .testTag("download_pdf_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share PDF",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        items(sortedPerfs, key = { it.id }) { perf ->
            val isPerformed = perf.status == PerformanceStatus.COMPLETED

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isPerformed) GreenBackground.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isPerformed) androidx.compose.foundation.BorderStroke(1.dp, GreenStatus.copy(alpha = 0.5f)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (canMarkPerformed) {
                        // Checkbox to mark as Performed (Admin only)
                        Checkbox(
                            checked = isPerformed,
                            onCheckedChange = { checked ->
                                if (viewModel != null) {
                                    val nextStatus = if (checked) PerformanceStatus.COMPLETED else PerformanceStatus.READY
                                    val msg = if (checked) "Marked '${perf.name}' as Performed" else "Reset '${perf.name}' to Ready"
                                    viewModel.updatePerformanceStatus(
                                        performanceId = perf.id,
                                        eventId = event.id,
                                        newStatus = nextStatus,
                                        message = msg
                                    )
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        if (checked) "Marked as Performed" else "Marked as Scheduled",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GreenStatus,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.testTag("checkbox_performed_${perf.id}")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        // Read-only indicator for Teachers / Non-admin
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = if (isPerformed) GreenStatus else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPerformed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Performed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "${perf.sequenceNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "#${perf.sequenceNumber}. ${perf.name}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (isPerformed) androidx.compose.ui.text.style.TextDecoration.None else null
                                ),
                                color = if (isPerformed) GreenStatus else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Est. Start: ${perf.plannedStartTime.ifBlank { "09:00 AM" }} • Duration: ${perf.plannedDurationMinutes} mins",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val teacherCoordSchedule = when {
                            perf.assignedTeacherNames.isNotBlank() -> perf.assignedTeacherNames
                            perf.requestedByTeacherName.isNotBlank() -> perf.requestedByTeacherName
                            else -> ""
                        }
                        if (teacherCoordSchedule.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "👤 Teacher Coordinator: $teacherCoordSchedule",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (perf.props.isNotBlank() || perf.costume.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOfNotNull(
                                    perf.props.takeIf { it.isNotBlank() }?.let { "Props: $it" },
                                    perf.costume.takeIf { it.isNotBlank() }?.let { "Costume: $it" }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status Badge
                    if (isPerformed) {
                        Surface(
                            color = GreenStatus,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PERFORMED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "NOT PERFORMED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showPublicQr) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Public Event QR Code & Link",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Parents and guests can scan this QR code to view live event status and schedule.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        QRCodeView(
                            eventSlug = event.publicSlug,
                            eventName = event.name,
                            eventDate = event.date,
                            venue = event.venue,
                            onOpenPublicPreview = onOpenPublicPreview
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventIssuesTab(
    eventId: String,
    performances: List<PerformanceEntity>,
    viewModel: EventSyncViewModel,
    isAdmin: Boolean
) {
    val issues by viewModel.currentEventIssues.collectAsState()
    var showRaiseIssueDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reported Issues (${issues.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Button(
                onClick = { showRaiseIssueDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Raise Issue")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (issues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No issues reported yet. All clear!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(issues, key = { it.id }) { issue ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (issue.isResolved) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (issue.isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (issue.isResolved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (issue.isResolved) "RESOLVED" else "PENDING",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (issue.isResolved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }

                                if (isAdmin && !issue.isResolved) {
                                    Button(
                                        onClick = { viewModel.resolveIssue(issue) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Mark Resolved", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (!issue.performanceName.isNullOrBlank()) {
                                Text(
                                    text = "Program: ${issue.performanceName}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            Text(
                                text = issue.issueText,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Raised by ${issue.raisedByUserName} on ${dateFormat.format(java.util.Date(issue.raisedAtMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (issue.isResolved && issue.resolvedAtMillis != null) {
                                Text(
                                    text = "Resolved on ${dateFormat.format(java.util.Date(issue.resolvedAtMillis))}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRaiseIssueDialog) {
        RaiseIssueDialog(
            eventId = eventId,
            performances = performances,
            onDismiss = { showRaiseIssueDialog = false },
            onSubmit = { perfId, perfName, text ->
                viewModel.raiseIssue(eventId, perfId, perfName, text)
                showRaiseIssueDialog = false
            }
        )
    }
}

@Composable
private fun RaiseIssueDialog(
    eventId: String,
    performances: List<PerformanceEntity>,
    onDismiss: () -> Unit,
    onSubmit: (String?, String?, String) -> Unit
) {
    var selectedPerf by remember { mutableStateOf<PerformanceEntity?>(null) }
    var issueText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Issue / Problem", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select related program (Optional):", style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedPerf == null,
                        onClick = { selectedPerf = null },
                        label = { Text("General Event Issue") }
                    )
                    performances.forEach { perf ->
                        FilterChip(
                            selected = selectedPerf?.id == perf.id,
                            onClick = { selectedPerf = perf },
                            label = { Text("🎭 #${perf.sequenceNumber}. ${perf.name}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = issueText,
                    onValueChange = { issueText = it },
                    label = { Text("Describe Issue *") },
                    placeholder = { Text("e.g. Mic noise, missing prop, costume issue") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (issueText.isNotBlank()) {
                        onSubmit(selectedPerf?.id, selectedPerf?.name, issueText.trim())
                    }
                },
                enabled = issueText.isNotBlank()
            ) {
                Text("Submit Issue")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EventChatTab(
    eventId: String,
    viewModel: EventSyncViewModel
) {
    val messages by viewModel.currentChatMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var messageInput by remember { mutableStateOf("") }

    val dateFormat = remember { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == currentUser?.id

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = msg.senderName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = if (msg.senderRole == com.example.data.model.UserRole.ADMIN) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = msg.senderRole.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = dateFormat.format(java.util.Date(msg.timestampMillis)),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Surface(
                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isMe) 12.dp else 2.dp,
                            bottomEnd = if (isMe) 2.dp else 12.dp
                        )
                    ) {
                        Text(
                            text = msg.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Type chat message...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        viewModel.sendChatMessage(eventId, messageInput)
                        messageInput = ""
                    }
                },
                enabled = messageInput.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EventActivityTab(updates: List<com.example.data.local.EventUpdateEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Realtime Event Activity & Status Feed",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (updates.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activity logs recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(updates) { upd ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = upd.userName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            StatusBadge(statusName = upd.status)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${upd.performanceName ?: "Event"}: ${upd.message}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

fun generateEventSchedulePdf(context: android.content.Context, event: EventEntity, performances: List<PerformanceEntity>) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 page size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }
        val subTitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 11f
        }
        val headerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(25, 118, 210) // Primary Blue
            textSize = 11f
            isFakeBoldText = true
        }
        val bodyPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }
        val linePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 40f
        canvas.drawText("EVENT SCHEDULE: ${event.name.uppercase()}", 40f, y, titlePaint)
        y += 20f
        canvas.drawText("Date: ${event.date}   |   Time: ${event.startTime} - ${event.endTime}   |   Venue: ${event.venue}", 40f, y, subTitlePaint)
        y += 14f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        // Table Header (Clean Schedule only)
        canvas.drawText("#", 40f, y, headerPaint)
        canvas.drawText("Performance Name", 75f, y, headerPaint)
        canvas.drawText("Est. Start Time", 370f, y, headerPaint)
        canvas.drawText("Duration", 475f, y, headerPaint)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f
        
        // Table Rows
        performances.sortedBy { it.sequenceNumber }.forEach { perf ->
            if (y > 780f) return@forEach
            canvas.drawText("${perf.sequenceNumber}", 40f, y, bodyPaint)
            val nameText = if (perf.name.length > 45) perf.name.take(42) + "..." else perf.name
            canvas.drawText(nameText, 75f, y, bodyPaint)
            canvas.drawText(perf.plannedStartTime.ifBlank { "09:00 AM" }, 370f, y, bodyPaint)
            canvas.drawText("${perf.plannedDurationMinutes} mins", 475f, y, bodyPaint)
            y += 20f
        }

        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 15f
        canvas.drawText("Generated via EventSync Management Platform", 40f, y, subTitlePaint)

        pdfDocument.finishPage(page)

        val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = java.io.File(downloadsDir, "Schedule_${event.name.replace(" ", "_")}.pdf")
        val outputStream = java.io.FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Event Schedule - ${event.name}")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Download / Share Schedule PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Error generating PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

