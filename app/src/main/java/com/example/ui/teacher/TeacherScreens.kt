package com.example.ui.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EventEntity
import com.example.data.local.PerformanceEntity
import com.example.data.model.PerformanceStatus
import com.example.data.model.UserRole
import androidx.compose.foundation.background
import com.example.ui.components.*
import com.example.ui.components.EventHealthCard
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.EventSyncViewModel
import kotlinx.coroutines.flow.flowOf

@Composable
fun TeacherDashboardScreen(
    viewModel: EventSyncViewModel,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val assignedEvents by viewModel.teacherAssignedEvents.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val schoolName = currentUser?.schoolName?.ifBlank { "J D International School" } ?: "J D International School"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schoolName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "👩‍🏫 Teacher Coordinator: ${currentUser?.name ?: "Neha Sharma"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "My Assigned Events (${assignedEvents.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(assignedEvents) { event ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEvent(event.id) }
                        .testTag("teacher_event_card_${event.id}")
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

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        if (event.winnersAnnouncement.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = androidx.compose.ui.graphics.Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("🏆 Winners Announcement:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = androidx.compose.ui.graphics.Color(0xFFE65100))
                                    Text(event.winnersAnnouncement, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${event.date} • ${event.startTime}", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onOpenEvent(event.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open Event & Manage Performances")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherEventDetailScreen(
    viewModel: EventSyncViewModel,
    onNavigateToAddPerformance: (String) -> Unit,
    onNavigateToEditPerformance: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val event by viewModel.currentEvent.collectAsState()
    val performances by viewModel.currentPerformances.collectAsState()
    val timeEngine by viewModel.timeEngineResult.collectAsState()
    val eventHealth by viewModel.eventHealth.collectAsState()
    val updates by viewModel.currentEventUpdates.collectAsState()

    var selectedTab by remember { mutableIntStateOf(1) } // Default to Performances tab
    val tabTitles = listOf("Overview", "Performances", "Schedule", "Issues", "Activity", "Event Chat")


    var selectedPerformanceForUpdate by remember { mutableStateOf<PerformanceEntity?>(null) }

    val activeEvent = event ?: return Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No event selected")
    }

    Column(modifier = modifier.fillMaxSize()) {
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
                    Text(
                        text = activeEvent.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(statusName = activeEvent.status.name)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${activeEvent.date} • ${activeEvent.startTime}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = activeEvent.venue, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Segmented Tabs
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
                    modifier = Modifier.testTag("teacher_event_tab_$index")
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: OVERVIEW
                    val avgProgress = if (performances.isNotEmpty()) performances.map { it.progressPercentage }.average().toInt() else 80
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            EventHealthCard(health = eventHealth, progressPercentage = avgProgress)
                        }
                        item {
                            com.example.ui.components.RealtimeEventCountdownCard(
                                eventDateStr = activeEvent.date,
                                startTimeStr = activeEvent.startTime,
                                eventStatus = activeEvent.status,
                                actualStartTimeMillis = activeEvent.actualStartTimeMillis,
                                actualEndTimeMillis = activeEvent.actualEndTimeMillis,
                                pausedAtMillis = activeEvent.pausedAtMillis,
                                totalPauseDurationMillis = activeEvent.totalPauseDurationMillis
                            )
                        }
                        item {
                            com.example.ui.components.TimeSummaryBanner(timeResult = timeEngine)
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
                                    Text(text = activeEvent.description.ifBlank { "Annual school event coordinator view." }, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Total Programs: ${performances.size}", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                        item {
                            QRCodeView(
                                eventSlug = activeEvent.publicSlug,
                                eventName = activeEvent.name,
                                eventDate = activeEvent.date,
                                venue = activeEvent.venue,
                                onOpenPublicPreview = {
                                    viewModel.selectRole(UserRole.PUBLIC)
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // TAB 1: PERFORMANCES & QUICK STATUS UPDATES
                    val currentUser by viewModel.currentUser.collectAsState()
                    val filteredPerformances = remember(performances, currentUser) {
                        val user = currentUser
                        if (user == null || user.role == UserRole.ADMIN) {
                            performances
                        } else {
                            performances.filter { perf ->
                                perf.createdBy == user.id ||
                                perf.requestedByTeacherId == user.id ||
                                (perf.assignedTeacherIds.isNotBlank() && perf.assignedTeacherIds.split(",").map { it.trim() }.contains(user.id))
                            }
                        }
                    }

                    val isEventCompleted = activeEvent.status == com.example.data.model.EventStatus.COMPLETED || activeEvent.status == com.example.data.model.EventStatus.ARCHIVED

                    Scaffold(
                        floatingActionButton = {
                            if (!isEventCompleted) {
                                ExtendedFloatingActionButton(
                                    onClick = { onNavigateToAddPerformance(activeEvent.id) },
                                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                                    text = { Text("Add Performance") },
                                    modifier = Modifier.testTag("teacher_add_performance_fab")
                                )
                            }
                        }
                    ) { innerPadding ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
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
                                        text = "Your Assigned Performances (${filteredPerformances.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (isEventCompleted) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "🔒 Read Only (Event Finished)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (filteredPerformances.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No performances created or assigned to you for this event.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            items(filteredPerformances.sortedBy { it.sequenceNumber }) { perf ->
                                val eventIssues by viewModel.currentEventIssues.collectAsState()
                                val perfIssues = eventIssues.filter { it.performanceId == perf.id && !it.isResolved }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${perf.sequenceNumber}. ${perf.name}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.weight(1f)
                                            )
                                            StatusBadge(statusName = perf.status.name)
                                            if (!isEventCompleted) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { onNavigateToEditPerformance(activeEvent.id, perf.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Performance",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        val teacherCoord = when {
                                            perf.assignedTeacherNames.isNotBlank() -> perf.assignedTeacherNames
                                            perf.requestedByTeacherName.isNotBlank() -> perf.requestedByTeacherName
                                            else -> ""
                                        }
                                        if (teacherCoord.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "👤 Coordinator: $teacherCoord",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
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

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Duration: ${perf.plannedDurationMinutes}m • Start: ${perf.plannedStartTime.ifBlank { "09:00 AM" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (perf.props.isNotBlank() || perf.costume.isNotBlank() || perf.musicName.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Props: ${perf.props.ifBlank { "None" }} | Costume: ${perf.costume.ifBlank { "Standard" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Rehearsal Progress % Slider
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "🎯 Rehearsal Prep Progress",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    color = if (perf.progressPercentage >= 100) GreenBackground else if (perf.progressPercentage >= 40) YellowBackground else RedBackground,
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        text = if (perf.progressPercentage >= 100) "100% Completed" else "${perf.progressPercentage}% Preparing",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = if (perf.progressPercentage >= 100) GreenStatus else if (perf.progressPercentage >= 40) YellowStatus else RedStatus,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            var tempProgress by remember(perf.progressPercentage) { mutableFloatStateOf(perf.progressPercentage.toFloat()) }
                                            Slider(
                                                value = tempProgress,
                                                onValueChange = { tempProgress = it },
                                                onValueChangeFinished = { viewModel.updatePerformanceProgress(perf.id, tempProgress.toInt()) },
                                                valueRange = 0f..100f,
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Issue / No Issue Toggle
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (perf.status == PerformanceStatus.ISSUE) Icons.Default.Error else Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (perf.status == PerformanceStatus.ISSUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (perf.status == PerformanceStatus.ISSUE) "Issue Flagged" else if (perf.progressPercentage >= 100) "Completed" else "Preparing Status",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (perf.status == PerformanceStatus.ISSUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (perf.status == PerformanceStatus.ISSUE) "Has Issue" else "No Issue",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (perf.status == PerformanceStatus.ISSUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Switch(
                                                    checked = perf.status == PerformanceStatus.ISSUE,
                                                    onCheckedChange = { isChecked ->
                                                        if (isChecked) {
                                                            selectedPerformanceForUpdate = perf
                                                        } else {
                                                            viewModel.togglePerformanceIssue(perf.id, false)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    com.example.ui.admin.EventScheduleTab(
                        event = activeEvent,
                        performances = performances.filter { it.isApproved },
                        showPublicQr = false,
                        viewModel = viewModel,
                        canMarkPerformed = false
                    )
                }
                3 -> {
                    com.example.ui.admin.EventIssuesTab(
                        eventId = activeEvent.id,
                        performances = performances,
                        viewModel = viewModel,
                        isAdmin = false
                    )
                }
                4 -> {
                    com.example.ui.admin.EventActivityTab(
                        updates = updates
                    )
                }
                5 -> {
                    com.example.ui.admin.EventChatTab(
                        eventId = activeEvent.id,
                        viewModel = viewModel
                    )
                }

            }
        }
    }

    selectedPerformanceForUpdate?.let { perf ->
        var noteMessage by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedPerformanceForUpdate = null },
            title = { Text("Report Issue for ${perf.name}") },
            text = {
                Column {
                    Text("Describe the issue (e.g. costume fit, missing props, audio cable issue):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteMessage,
                        onValueChange = { noteMessage = it },
                        label = { Text("Issue Message") },
                        placeholder = { Text("e.g. Prop matka broken, need replacement") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("issue_message_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val issueContent = noteMessage.ifBlank { "Issue reported for ${perf.name}" }
                        viewModel.updatePerformanceStatus(
                            performanceId = perf.id,
                            eventId = activeEvent.id,
                            newStatus = PerformanceStatus.ISSUE,
                            message = issueContent
                        )
                        viewModel.raiseIssue(
                            eventId = activeEvent.id,
                            performanceId = perf.id,
                            performanceName = perf.name,
                            issueText = issueContent
                        )
                        selectedPerformanceForUpdate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Report Issue")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPerformanceForUpdate = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun QuickStatusChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .clickable { onClick() }
            .testTag("quick_status_${label.lowercase().replace(" ", "_")}")
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AddPerformanceScreen(
    viewModel: EventSyncViewModel,
    eventId: String,
    existingPerformanceId: String?,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val perfTypes by viewModel.performanceTypesForCurrentSchool.collectAsState()
    val students by viewModel.students.collectAsState()
    val currentPerformances by viewModel.currentPerformances.collectAsState()

    val existingPerf = remember(existingPerformanceId, currentPerformances) {
        currentPerformances.find { it.id == existingPerformanceId }
    }

    var name by remember { mutableStateOf(existingPerf?.name ?: "") }
    var selectedTypeId by remember { mutableStateOf(existingPerf?.performanceTypeId ?: (perfTypes.firstOrNull()?.id ?: "pt_1")) }
    var sequenceNumber by remember { mutableIntStateOf(existingPerf?.sequenceNumber ?: (currentPerformances.size + 1)) }
    var durationMins by remember { mutableIntStateOf(existingPerf?.plannedDurationMinutes ?: 10) }
    var description by remember { mutableStateOf(existingPerf?.description ?: "") }

    var musicName by remember { mutableStateOf(existingPerf?.musicName ?: "") }
    var youtubeLink by remember { mutableStateOf(existingPerf?.youtubeLink ?: "") }
    var props by remember { mutableStateOf(existingPerf?.props ?: "") }
    var costume by remember { mutableStateOf(existingPerf?.costume ?: "") }
    var stageReqs by remember { mutableStateOf(existingPerf?.stageRequirements ?: "") }
    var notes by remember { mutableStateOf(existingPerf?.notes ?: "") }

    val allClasses by viewModel.classes.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val eligibleClassIds by viewModel.getEligibleClassIdsForEvent(eventId).collectAsState(initial = emptyList())

    val eligibleStudents = remember(allStudents, eligibleClassIds) {
        if (eligibleClassIds.isNotEmpty()) {
            allStudents.filter { it.classId in eligibleClassIds }
        } else {
            allStudents
        }
    }

    val selectedStudentIds = remember { mutableStateListOf<String>() }
    val existingStudentIds by remember(existingPerformanceId) {
        if (existingPerformanceId != null) {
            viewModel.getStudentIdsForPerformance(existingPerformanceId)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    LaunchedEffect(existingStudentIds) {
        if (existingStudentIds.isNotEmpty() && selectedStudentIds.isEmpty()) {
            selectedStudentIds.clear()
            selectedStudentIds.addAll(existingStudentIds)
        }
    }

    var studentSearchQuery by remember { mutableStateOf("") }
    var selectedClassFilterId by remember { mutableStateOf<String?>(null) }

    val eventClasses = remember(allClasses, eligibleClassIds) {
        if (eligibleClassIds.isNotEmpty()) {
            allClasses.filter { it.id in eligibleClassIds }
        } else {
            allClasses
        }
    }

    val filteredStudents = remember(eligibleStudents, studentSearchQuery, selectedClassFilterId) {
        eligibleStudents.filter { student ->
            val matchesClass = selectedClassFilterId == null || student.classId == selectedClassFilterId
            val matchesQuery = studentSearchQuery.isBlank() ||
                    student.name.contains(studentSearchQuery, ignoreCase = true) ||
                    student.admissionNumber.contains(studentSearchQuery, ignoreCase = true)
            matchesClass && matchesQuery
        }
    }

    val currentEvent by viewModel.currentEvent.collectAsState()
    val isArchived = currentEvent?.status == com.example.data.model.EventStatus.COMPLETED || currentEvent?.status == com.example.data.model.EventStatus.ARCHIVED

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (isArchived) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Read Only",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "This event is Completed / Archived. Performance details are strictly Read-Only.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Text(
            text = if (isArchived) "View Performance (Read Only)" else if (existingPerf == null) "Add New Performance" else "Edit Performance",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { if (!isArchived) name = it },
            readOnly = isArchived,
            label = { Text("Performance Name *") },
            placeholder = { Text("e.g. Patriotic Folk Dance") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("perf_name_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = "$durationMins",
            onValueChange = { if (!isArchived) durationMins = it.toIntOrNull() ?: 10 },
            readOnly = isArchived,
            label = { Text("Duration (Mins) *") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Performance Type Selector (Scoped to School)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Performance Type *",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "(Provided by School Admin & Catalog)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (perfTypes.isEmpty()) {
            Text(
                text = "No performance types found for your school. Contact School Admin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                perfTypes.forEach { type ->
                    val isSelected = selectedTypeId == type.id
                    val isCustom = type.schoolId.isNotBlank()
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeId = type.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCustom) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    type.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Teacher Coordinators Multi-Select (Only visible to Admin)
        val currentRole by viewModel.currentRole.collectAsState()
        val isTeacher = currentRole == UserRole.TEACHER

        val teachers by viewModel.teachers.collectAsState()
        val selectedTeacherIds = remember { mutableStateListOf<String>() }

        LaunchedEffect(existingPerf) {
            if (existingPerf != null && existingPerf.assignedTeacherIds.isNotBlank() && selectedTeacherIds.isEmpty()) {
                selectedTeacherIds.clear()
                selectedTeacherIds.addAll(existingPerf.assignedTeacherIds.split(",").map { it.trim() }.filter { it.isNotBlank() })
            }
        }

        if (!isTeacher) {
            Text(
                text = "Teacher Coordinators (Multi-Select)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Assign teachers who can manage and edit this performance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                teachers.forEach { teacher ->
                    val isSelected = selectedTeacherIds.contains(teacher.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selectedTeacherIds.remove(teacher.id)
                            else selectedTeacherIds.add(teacher.id)
                        },
                        label = { Text(teacher.name, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Once Admin accepts your performance request, you will automatically become the Teacher Coordinator for it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-select Eligible Students for Performance
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Participating Students",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Only showing students from classes added to this event (${selectedStudentIds.size}/${eligibleStudents.size} selected)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedStudentIds.clear()
                            selectedStudentIds.addAll(eligibleStudents.map { it.id })
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_all_students_btn")
                    ) {
                        Text("Select All (${eligibleStudents.size})", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { selectedStudentIds.clear() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_student_selection_btn")
                    ) {
                        Text("Clear All", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Filter Students by Class:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedClassFilterId == null,
                        onClick = { selectedClassFilterId = null },
                        label = { Text("All Event Classes (${eligibleStudents.size})", fontSize = 11.sp) }
                    )
                    eventClasses.forEach { cls ->
                        val count = eligibleStudents.count { it.classId == cls.id }
                        FilterChip(
                            selected = selectedClassFilterId == cls.id,
                            onClick = {
                                selectedClassFilterId = if (selectedClassFilterId == cls.id) null else cls.id
                            },
                            label = { Text("${cls.name}-${cls.section} ($count)", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = studentSearchQuery,
                    onValueChange = { studentSearchQuery = it },
                    label = { Text("Search Student Name or Roll #") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredStudents.isEmpty()) {
                    Text(
                        text = "No eligible students found matching query.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        filteredStudents.forEach { student ->
                            val isSelected = selectedStudentIds.contains(student.id)
                            val cls = allClasses.find { it.id == student.classId }
                            val classLabel = if (cls != null) "${cls.name} (Sec ${cls.section})" else "Class ${student.section}"

                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedStudentIds.remove(student.id)
                                        else selectedStudentIds.add(student.id)
                                    }
                                    .testTag("student_item_${student.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                if (it) selectedStudentIds.add(student.id)
                                                else selectedStudentIds.remove(student.id)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = student.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Adm #: ${student.admissionNumber} • $classLabel",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Text(
                                            text = "Selected",
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Music Details (Optional)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = musicName,
            onValueChange = { musicName = it },
            label = { Text("Song / Track Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = youtubeLink,
            onValueChange = { youtubeLink = it },
            label = { Text("YouTube Link") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Props & Costume (Optional)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = props,
            onValueChange = { props = it },
            label = { Text("Props Required") },
            placeholder = { Text("e.g. Matka × 6, Flags × 10") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = costume,
            onValueChange = { costume = it },
            label = { Text("Costume Details") },
            placeholder = { Text("e.g. Traditional Ghagra Choli") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }

            val currentRole by viewModel.currentRole.collectAsState()
            val isTeacher = currentRole == com.example.data.model.UserRole.TEACHER

            if (!isArchived) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val assignedIdsString = selectedTeacherIds.joinToString(",")
                            val assignedNamesString = teachers.filter { it.id in selectedTeacherIds }.joinToString(", ") { it.name }

                            viewModel.savePerformance(
                                id = existingPerformanceId,
                                eventId = eventId,
                                name = name,
                                performanceTypeId = selectedTypeId,
                                sequenceNumber = sequenceNumber,
                                durationMinutes = durationMins,
                                description = description,
                                musicName = musicName,
                                youtubeLink = youtubeLink,
                                props = props,
                                costume = costume,
                                stageReqs = stageReqs,
                                notes = notes,
                                selectedStudentIds = selectedStudentIds,
                                assignedTeacherIds = assignedIdsString,
                                assignedTeacherNames = assignedNamesString
                            )
                            onSaved()
                        }
                    },
                    modifier = Modifier.testTag("save_performance_submit")
                ) {
                    Icon(if (isTeacher) Icons.Default.Send else Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isTeacher) "Submit Performance Request" else "Save Performance")
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Read Only Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
