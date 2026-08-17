package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.local.AdminReportIssueEntity
import com.example.data.local.DirectMessageEntity
import com.example.data.local.SchoolSubscriptionEntity
import com.example.data.model.PerformanceStatus
import com.example.data.model.UserRole
import com.example.ui.components.EventHealthCard
import com.example.ui.components.RazorpayCheckoutModal
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaxInvoicePdfDialog
import com.example.ui.components.TimeSummaryBanner
import com.example.ui.viewmodel.EventSyncViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    viewModel: EventSyncViewModel,
    onNavigateToEventDetail: (String) -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToTeachers: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToEditEvent: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val schoolName = currentUser?.schoolName?.ifBlank { "J D International School" } ?: "J D International School"
    val currentEvent by viewModel.currentEvent.collectAsState()
    val performances by viewModel.currentPerformances.collectAsState()
    val timeEngine by viewModel.timeEngineResult.collectAsState()
    val eventHealth by viewModel.eventHealth.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val teachers by viewModel.teachers.collectAsState()
    val students by viewModel.students.collectAsState()
    val events by viewModel.events.collectAsState()
    val performanceTypes by viewModel.performanceTypesForCurrentSchool.collectAsState()
    val subscription by viewModel.schoolSubscription.collectAsState()
    val myReportIssues by viewModel.currentSchoolReportIssues.collectAsState()
    val directMessages by viewModel.directMessagesForSelectedSchool.collectAsState()

    var showManagePerfTypesDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showReportIssueDialog by remember { mutableStateOf(false) }
    var showDirectChatDialog by remember { mutableStateOf(false) }

    // Calculate progress
    val totalPerformances = performances.size
    val readyCount = performances.count { it.status == PerformanceStatus.READY }
    val completedCount = performances.count { it.status == PerformanceStatus.COMPLETED }
    val inProgressCount = performances.count { it.status == PerformanceStatus.IN_PROGRESS }
    val issueCount = performances.count { it.status == PerformanceStatus.ISSUE }
    val notStartedCount = performances.count { it.status == PerformanceStatus.NOT_STARTED }

    val progressPercent = if (totalPerformances > 0) {
        ((completedCount * 1.0f + readyCount * 0.8f + inProgressCount * 0.4f) / totalPerformances * 100).toInt()
    } else 0

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if ((subscription?.credits ?: 1) <= 0) {
                        viewModel.showUserToast("⚠️ No event credits remaining. Please upgrade your plan!")
                        showUpgradeDialog = true
                    } else {
                        onNavigateToCreateEvent()
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Create Event") },
                text = { Text("Create Event", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
                containerColor = Color(0xFFBAE6FD),
                contentColor = Color(0xFF0369A1),
                modifier = Modifier.testTag("admin_create_event_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp)
                .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Suspended / Inactive School Warning Banner
            if (subscription?.isSuspended == true || subscription?.isActive == false) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⚠️ School Account Suspended by Super Admin",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "15-Day Inactivity Data Retention Policy is currently active. New event creation is temporarily restricted. Please contact Super Admin via 1:1 Direct Hotline to restore active access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7F1D1D)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.setSelectedSchoolForChat(currentUser?.schoolId?.ifBlank { "sch_jdis" } ?: "sch_jdis")
                                    showDirectChatDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open 1:1 Hotline to Reactivate", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Top Bar with Top-Left Upgrade Button & Support Quick Links
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showUpgradeDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEAB308),
                            contentColor = Color(0xFF78350F)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_top_left_upgrade_btn")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "⚡ Upgrade (${subscription?.credits ?: 1} Credit)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { showReportIssueDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_report_issue_btn")
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Report Problem", style = MaterialTheme.typography.labelSmall)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.setSelectedSchoolForChat(currentUser?.schoolId?.ifBlank { "sch_jdis" } ?: "sch_jdis")
                                showDirectChatDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_chat_super_admin_btn")
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1:1 Chat", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            // School Name Hero Banner
            item {
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
                                text = "Admin Portal • ${currentUser?.name ?: "Principal Sharma"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Quick Action Bar - Manage Performance Types
            item {
                FilledTonalButton(
                    onClick = { showManagePerfTypesDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_manage_perf_types_btn")
                ) {
                    Icon(imageVector = Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage School Performance Types (${performanceTypes.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            // Section Title: All Events
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "School Events (${events.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            items(events, key = { it.id }) { event ->
                CreatedEventCard(
                    event = event,
                    onOpen = {
                        viewModel.setSelectedEvent(event.id)
                        onNavigateToEventDetail(event.id)
                    },
                    onEdit = {
                        onNavigateToEditEvent(event.id)
                    }
                )
            }
        }
    }

    if (showManagePerfTypesDialog) {
        AdminManagePerformanceTypesDialog(
            schoolName = schoolName,
            performanceTypes = performanceTypes,
            onAddType = { name, desc ->
                viewModel.createPerformanceType(name, desc)
            },
            onDeleteType = { typeId ->
                viewModel.deletePerformanceType(typeId)
            },
            onDismiss = { showManagePerfTypesDialog = false }
        )
    }

    if (showUpgradeDialog) {
        AdminUpgradeRazorpayDialog(
            subscription = subscription,
            schoolName = schoolName,
            adminEmail = currentUser?.email?.ifBlank { "admin@jdsschool.com" } ?: "admin@jdsschool.com",
            adminPhone = currentUser?.phone?.ifBlank { "9800000002" } ?: "9800000002",
            onUpgrade = { planName, credits, amountRupees, validityDays, paymentId ->
                viewModel.processUpgradePayment(planName, credits, amountRupees, validityDays, paymentId)
                showUpgradeDialog = false
            },
            onDismiss = { showUpgradeDialog = false }
        )
    }

    if (showReportIssueDialog) {
        AdminReportIssueDialog(
            myIssues = myReportIssues,
            onSubmitIssue = { category, title, desc ->
                viewModel.submitAdminIssue(category, title, desc)
            },
            onCloseTicket = { issueId ->
                viewModel.closeAdminTicket(issueId)
            },
            onReopenTicket = { issueId ->
                viewModel.reopenAdminTicket(issueId)
            },
            onDismiss = { showReportIssueDialog = false }
        )
    }

    if (showDirectChatDialog) {
        AdminDirectChatDialog(
            directMessages = directMessages,
            onSendMessage = { msg ->
                val schoolId = currentUser?.schoolId?.ifBlank { "sch_jdis" } ?: "sch_jdis"
                viewModel.sendDirectMessage(schoolId, msg)
            },
            onDownloadPdf = { msg ->
                viewModel.downloadTaxInvoicePdf(
                    invoiceId = msg.receiptId.ifBlank { "INV-2026-RZP5501" },
                    schoolName = "J D International School",
                    planName = msg.receiptPlanName.ifBlank { "Annual Pro Plan" },
                    amountRupees = if (msg.receiptAmount > 0) msg.receiptAmount.toInt() else 499
                )
            },
            onDismiss = { showDirectChatDialog = false }
        )
    }
}

@Composable
private fun CreatedEventCard(
    event: com.example.data.local.EventEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("created_event_card_${event.id}")
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${event.date} • ${event.startTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = event.venue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            if (event.winnersAnnouncement.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("🏆 Winners Announcement:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFE65100))
                        Text(event.winnersAnnouncement, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isArchived = event.status == com.example.data.model.EventStatus.COMPLETED || event.status == com.example.data.model.EventStatus.ARCHIVED

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isArchived) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_event_btn_${event.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }

                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(if (!isArchived) 1.2f else 1f)
                        .testTag("open_event_btn_${event.id}")
                ) {
                    Text("Open")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun EditEventDialog(
    event: com.example.data.local.EventEntity,
    onDismiss: () -> Unit,
    onSave: (com.example.data.local.EventEntity) -> Unit
) {
    var name by remember { mutableStateOf(event.name) }
    var venue by remember { mutableStateOf(event.venue) }
    var date by remember { mutableStateOf(event.date) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var description by remember { mutableStateOf(event.description) }
    var selectedStatus by remember { mutableStateOf(event.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Event Details")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Event Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_event_name_input")
                )

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Location *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Event Status",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    com.example.data.model.EventStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st.name, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && venue.isNotBlank()) {
                        val updated = event.copy(
                            name = name.trim(),
                            venue = venue.trim(),
                            date = date.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            description = description.trim(),
                            status = selectedStatus,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(updated)
                    }
                },
                modifier = Modifier.testTag("save_edited_event_btn")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private val RedCardBg = com.example.ui.components.RedBackground

@Composable
private fun StatSummaryCard(
    label: String,
    value: String,
    bgColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetricText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActivityLogItem(log: com.example.data.local.ActivityLogEntity) {
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.US) }
    val timeStr = remember(log.createdAt) { sdf.format(Date(log.createdAt)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (log.action) {
                            "STATUS_CHANGED" -> Icons.Default.Update
                            "EVENT_CREATED" -> Icons.Default.Event
                            "PERFORMANCE_SAVED" -> Icons.Default.Queue
                            else -> Icons.Default.History
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.userName,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AdminManagePerformanceTypesDialog(
    schoolName: String,
    performanceTypes: List<com.example.data.local.PerformanceTypeEntity>,
    onAddType: (String, String) -> Unit,
    onDeleteType: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var newTypeName by remember { mutableStateOf("") }
    var newTypeDescription by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTypes = remember(performanceTypes, searchQuery) {
        if (searchQuery.isBlank()) performanceTypes
        else performanceTypes.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val schoolCustomTypes = remember(filteredTypes) {
        filteredTypes.filter { it.schoolId.isNotBlank() }
    }

    val globalDefaultTypes = remember(filteredTypes) {
        filteredTypes.filter { it.schoolId.isBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Performance Types",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = schoolName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                Text(
                    text = "Performance types created here are visible to all teachers of $schoolName for event scheduling.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search & Add Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search types...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = { showAddForm = !showAddForm },
                        shape = RoundedCornerShape(10.dp),
                        colors = if (showAddForm) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors(),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("admin_add_perf_type_toggle_btn")
                    ) {
                        Icon(if (showAddForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAddForm) "Cancel" else "Add Type", fontSize = 12.sp)
                    }
                }

                // Add Form Expandable Card
                if (showAddForm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Add New School Performance Type",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newTypeName,
                                onValueChange = { newTypeName = it },
                                label = { Text("Performance Type Name *", fontSize = 12.sp) },
                                placeholder = { Text("e.g. Puppet Show, Folk Dance, Martial Arts", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_new_perf_type_name_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newTypeDescription,
                                onValueChange = { newTypeDescription = it },
                                label = { Text("Description (Optional)", fontSize = 12.sp) },
                                placeholder = { Text("Brief description or category notes", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_new_perf_type_desc_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (newTypeName.isNotBlank()) {
                                        onAddType(newTypeName.trim(), newTypeDescription.trim())
                                        newTypeName = ""
                                        newTypeDescription = ""
                                        showAddForm = false
                                    }
                                },
                                enabled = newTypeName.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_save_perf_type_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Performance Type for $schoolName")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Types
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (schoolCustomTypes.isNotEmpty()) {
                        item {
                            Text(
                                text = "🏫 Custom Types Added by $schoolName (${schoolCustomTypes.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(schoolCustomTypes, key = { it.id }) { type ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = type.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "School Custom",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (type.description.isNotBlank()) {
                                            Text(
                                                text = type.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteType(type.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🌐 Global Default Types (${globalDefaultTypes.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(globalDefaultTypes, key = { it.id }) { type ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = type.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "System Default",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (type.description.isNotBlank()) {
                                        Text(
                                            text = type.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Done")
            }
        }
    )
}

// ==========================================
// UPGRADE SUBSCRIPTION DIALOG (RAZORPAY)
// ==========================================
@Composable
fun AdminUpgradeRazorpayDialog(
    subscription: SchoolSubscriptionEntity?,
    schoolName: String = "J D International School",
    adminEmail: String = "admin@jdsschool.com",
    adminPhone: String = "9800000002",
    onUpgrade: (planName: String, credits: Int, amountRupees: Int, validityDays: Int, paymentId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    var selectedPlanIndex by remember { mutableStateOf(0) } // 0: Plan 1 (199), 1: Plan 2 (499)
    var showRazorpayCheckout by remember { mutableStateOf(false) }

    val plans = listOf(
        PlanDetails(
            name = "Starter Event Plan",
            price = 199,
            credits = 1,
            validityDays = 90,
            validityLabel = "3 Months Validity",
            features = listOf("1 Official Event Creation Credit", "Full Timeline & Auto-Pacing Engine", "Unlimited Classes & Student Mapping", "Parent Live View & Notifications")
        ),
        PlanDetails(
            name = "Annual Pro Plan",
            price = 499,
            credits = 5,
            validityDays = 365,
            validityLabel = "1 Year Validity",
            isBestValue = true,
            features = listOf("5 Official Event Creation Credits", "Priority Super Admin Support hotline", "Bulk CSV Students & Classes Imports", "Dedicated Audio Sequences & Timer Sync")
        )
    )

    val currentPlan = plans[selectedPlanIndex]

    if (showRazorpayCheckout) {
        RazorpayCheckoutModal(
            planName = currentPlan.name,
            credits = currentPlan.credits,
            amountRupees = currentPlan.price,
            validityDays = currentPlan.validityDays,
            schoolName = schoolName,
            adminEmail = adminEmail,
            adminPhone = adminPhone,
            onPaymentSuccess = { paymentId, invoiceId ->
                onUpgrade(
                    currentPlan.name,
                    currentPlan.credits,
                    currentPlan.price,
                    currentPlan.validityDays,
                    paymentId
                )
                showRazorpayCheckout = false
            },
            onDismiss = { showRazorpayCheckout = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFEAB308))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upgrade School Plan", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Active Subscription Status Pill
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Current: ${subscription?.planName ?: "1 Credit Plan"}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                val expiry = subscription?.validUntilMillis ?: (System.currentTimeMillis() + 90L * 24 * 3600 * 1000)
                                Text(
                                    text = "Valid till: ${sdf.format(Date(expiry))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${subscription?.credits ?: 1} Credits Left",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Subscription Plan:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Plan Cards
                    plans.forEachIndexed { index, plan ->
                        val isSelected = selectedPlanIndex == index
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlanIndex = index }
                                .padding(vertical = 4.dp)
                                .testTag("subscription_plan_card_${plan.price}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedPlanIndex = index }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(plan.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text("${plan.credits} Event Credit • ${plan.validityLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹${plan.price}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                                        if (plan.isBestValue) {
                                            Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp)) {
                                                Text("BEST VALUE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    plan.features.forEach { feat ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(feat, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Razorpay Security Banner
                    Surface(
                        color = Color(0xFF0C2340).copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0C2340), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Secured by Razorpay • Instant 1:1 Tax Receipt Dispatch",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFF0C2340)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRazorpayCheckout = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0C2340),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("pay_with_razorpay_btn")
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pay ₹${currentPlan.price} with Razorpay")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class PlanDetails(
    val name: String,
    val price: Int,
    val credits: Int,
    val validityDays: Int,
    val validityLabel: String,
    val isBestValue: Boolean = false,
    val features: List<String>
)

// ==========================================
// REPORT PROBLEM DIALOG (ADMIN -> SUPER ADMIN)
// ==========================================
@Composable
fun AdminReportIssueDialog(
    myIssues: List<AdminReportIssueEntity>,
    onSubmitIssue: (category: String, title: String, desc: String) -> Unit,
    onCloseTicket: (String) -> Unit = {},
    onReopenTicket: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }
    var selectedCategory by remember { mutableStateOf("App / Technical") }
    var issueTitle by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Open New Ticket, 1: Open Tickets, 2: Closed Tickets

    val openIssues = remember(myIssues) { myIssues.filter { it.status != "RESOLVED" && it.status != "CLOSED" } }
    val closedIssues = remember(myIssues) { myIssues.filter { it.status == "RESOLVED" || it.status == "CLOSED" } }

    val categories = listOf("App / Technical", "Event & Schedule", "Audio & Sequence", "Billing & Plans", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Report Problem (Tickets)")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Open Ticket", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Active (${openIssues.size})", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("Closed (${closedIssues.size})", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (activeTab) {
                    0 -> {
                        Text("Problem Category:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categories.take(3).forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.take(12), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categories.drop(3).forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = issueTitle,
                            onValueChange = { issueTitle = it },
                            label = { Text("Problem Summary / Title *") },
                            placeholder = { Text("e.g. Audio track not syncing with timer") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_report_title_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = issueDescription,
                            onValueChange = { issueDescription = it },
                            label = { Text("Detailed Problem Description *") },
                            placeholder = { Text("Describe the problem and what help you need from Super Admin...") },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_report_desc_input")
                        )
                    }
                    1 -> {
                        if (openIssues.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("No open tickets!", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("All your reported problems are resolved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(280.dp)
                            ) {
                                items(openIssues, key = { it.id }) { issue ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(issue.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                                    Text("Category: ${issue.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                                StatusBadge(statusName = if (issue.status == "OPEN") "OPEN TICKET" else issue.status)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(issue.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Opened: ${sdf.format(Date(issue.createdAt))}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.outline
                                            )

                                            if (issue.superAdminResponse.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Surface(
                                                    color = Color(0xFFDCFCE7),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text(
                                                            text = "Super Admin Note:",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = Color(0xFF166534)
                                                        )
                                                        Text(
                                                            text = issue.superAdminResponse,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF14532D)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                OutlinedButton(
                                                    onClick = { onCloseTicket(issue.id) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF15803D))
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Close Ticket", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        if (closedIssues.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No closed tickets yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(280.dp)
                            ) {
                                items(closedIssues, key = { it.id }) { issue ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(issue.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                                    Text("Category: ${issue.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                }
                                                Surface(color = Color(0xFFE2E8F0), shape = RoundedCornerShape(4.dp)) {
                                                    Text("CLOSED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(issue.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (issue.superAdminResponse.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Resolution: ${issue.superAdminResponse}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF166534)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                FilledTonalButton(
                                                    onClick = { onReopenTicket(issue.id) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Re-open Ticket", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (activeTab == 0) {
                Button(
                    onClick = {
                        if (issueTitle.isNotBlank() && issueDescription.isNotBlank()) {
                            onSubmitIssue(selectedCategory, issueTitle.trim(), issueDescription.trim())
                            onDismiss()
                        }
                    },
                    enabled = issueTitle.isNotBlank() && issueDescription.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("submit_report_issue_btn")
                ) {
                    Text("Open Ticket")
                }
            } else {
                Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (activeTab == 0) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// ==========================================
// DIRECT CHAT DIALOG (ADMIN <-> SUPER ADMIN)
// ==========================================
@Composable
fun AdminDirectChatDialog(
    directMessages: List<DirectMessageEntity>,
    onSendMessage: (String) -> Unit,
    onDownloadPdf: (DirectMessageEntity) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.US) }
    var chatInput by remember { mutableStateOf("") }
    var previewInvoiceMsg by remember { mutableStateOf<DirectMessageEntity?>(null) }

    if (previewInvoiceMsg != null) {
        val msg = previewInvoiceMsg!!
        TaxInvoicePdfDialog(
            invoiceId = msg.receiptId.ifBlank { "INV-2026-RZP5501" },
            paymentId = msg.receiptPaymentId.ifBlank { "pay_rzp_demo123" },
            schoolName = "J D International School",
            adminEmail = "admin@jdsschool.com",
            adminPhone = "9800000002",
            planName = msg.receiptPlanName.ifBlank { "Annual Pro Plan" },
            credits = if (msg.receiptCredits > 0) msg.receiptCredits else 5,
            amountRupees = if (msg.receiptAmount > 0) msg.receiptAmount.toInt() else 499,
            onDismiss = { previewInvoiceMsg = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Super Admin Direct Hotline", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("1-to-1 Instant Hotline & Receipt PDFs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (directMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No messages yet.", style = MaterialTheme.typography.bodySmall)
                            Text("Ask any question or request help directly!", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(directMessages.reversed(), key = { it.id }) { msg ->
                            val isMe = msg.senderRole == UserRole.ADMIN
                            val isReceipt = msg.messageType == "PAYMENT_RECEIPT" || msg.message.contains("[PAYMENT RECEIPT")

                            if (isReceipt) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF0C2340), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Tax Invoice & Receipt", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0C2340))
                                            }
                                            Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp)) {
                                                Text("VERIFIED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(msg.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(sdf.format(Date(msg.timestampMillis)), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { onDownloadPdf(msg) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Download PDF", fontSize = 10.sp)
                                                }
                                                Button(
                                                    onClick = { previewInvoiceMsg = msg },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C2340)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("View", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMe) 12.dp else 2.dp,
                                            bottomEnd = if (isMe) 2.dp else 12.dp
                                        ),
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = msg.senderName,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = sdf.format(Date(msg.timestampMillis)),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Type message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_direct_chat_input")
                    )

                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                onSendMessage(chatInput.trim())
                                chatInput = ""
                            }
                        },
                        enabled = chatInput.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("admin_send_chat_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Close")
            }
        }
    )
}
