package com.example.ui.superadmin

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import com.example.data.model.UserRole
import com.example.ui.components.StatusBadge
import com.example.ui.components.TaxInvoicePdfDialog
import com.example.ui.viewmodel.EventSyncViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

enum class SuperAdminTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ANALYTICS("Analytics", Icons.Default.Analytics),
    REPORT_BOX("Report Box", Icons.Default.ReportProblem),
    DIRECT_CHAT("1:1 Chat", Icons.Default.Forum),
    SCHOOL_CONTROL("School Access", Icons.Default.Shield)
}

data class SchoolData(
    val schoolId: String,
    val schoolName: String,
    val adminName: String,
    val adminEmail: String,
    val adminContact: String,
    val teacherCount: Int,
    val classCount: Int,
    val studentCount: Int,
    val credits: Int,
    val planName: String,
    val expiryTime: Long,
    val isActive: Boolean = true,
    val isSuspended: Boolean = false,
    val suspensionReason: String = "",
    val suspendedAtMillis: Long? = null,
    val purgeWarningSent: Boolean = false,
    val purgeWarningSentAt: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboardScreen(
    viewModel: EventSyncViewModel,
    initialTab: SuperAdminTab = SuperAdminTab.ANALYTICS,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()
    val classes by viewModel.classes.collectAsState()
    val students by viewModel.students.collectAsState()
    val subscriptions by viewModel.allSchoolSubscriptions.collectAsState()
    val reportedIssues by viewModel.allAdminReportIssues.collectAsState()
    val selectedSchoolForChat by viewModel.selectedSchoolIdForChat.collectAsState()
    val directMessages by viewModel.directMessagesForSelectedSchool.collectAsState()

    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var respondingIssue by remember { mutableStateOf<AdminReportIssueEntity?>(null) }
    var chatMessageInput by remember { mutableStateOf("") }
    
    // Suspend School Dialog State
    var suspendingSchool by remember { mutableStateOf<SchoolData?>(null) }
    var purgeConfirmSchool by remember { mutableStateOf<SchoolData?>(null) }
    
    // PDF Viewer Dialog
    var viewingInvoiceMessage by remember { mutableStateOf<DirectMessageEntity?>(null) }

    // Aggregate Analytics Data
    val schoolAdmins = users.filter { it.role == UserRole.ADMIN }
    val schoolMap = remember(users, subscriptions, classes, students) {
        val list = mutableListOf<SchoolData>()

        // 1. Primary school
        val defaultAdmin = schoolAdmins.find { it.schoolId == "sch_jdis" || it.id == "u_admin" }
        val defaultSub = subscriptions.find { it.schoolId == "sch_jdis" }
        list.add(
            SchoolData(
                schoolId = "sch_jdis",
                schoolName = "J D International School",
                adminName = defaultAdmin?.name ?: "Principal Sharma",
                adminEmail = defaultAdmin?.email ?: "admin@jdsschool.com",
                adminContact = defaultAdmin?.phone?.ifBlank { "+91 9800000002" } ?: "+91 9800000002",
                teacherCount = users.count { it.role == UserRole.TEACHER && (it.schoolId == "sch_jdis" || it.schoolId.isBlank()) },
                classCount = classes.size,
                studentCount = students.size,
                credits = defaultSub?.credits ?: 1,
                planName = defaultSub?.planName ?: "1 Credit Plan (3 Months)",
                expiryTime = defaultSub?.validUntilMillis ?: (System.currentTimeMillis() + 90L * 24 * 3600 * 1000),
                isActive = defaultSub?.isActive ?: true,
                isSuspended = defaultSub?.isSuspended ?: false,
                suspensionReason = defaultSub?.suspensionReason ?: "",
                suspendedAtMillis = defaultSub?.suspendedAtMillis,
                purgeWarningSent = defaultSub?.purgeWarningSent ?: false,
                purgeWarningSentAt = defaultSub?.purgeWarningSentAt
            )
        )

        // 2. Additional registered schools
        val otherSchools = listOf(
            SchoolData("sch_dpa", "Delhi Public Academy", "Dr. Sanjeev Kapoor", "admin@dpa.edu.in", "+91 9811223344", 18, 12, 450, 5, "5 Credits Plan (1 Year)", System.currentTimeMillis() + 365L * 24 * 3600 * 1000),
            SchoolData("sch_stx", "St. Xavier's High School", "Sister Mary Theresa", "principal@stxaviers.org", "+91 9822334455", 14, 10, 380, 3, "5 Credits Plan (1 Year)", System.currentTimeMillis() + 180L * 24 * 3600 * 1000),
            SchoolData("sch_rgs", "Ryan Global School", "Col. Rakesh Mehra", "director@ryanglobal.org", "+91 9833445566", 22, 16, 620, 0, "Expired Starter Trial", System.currentTimeMillis() - 5L * 24 * 3600 * 1000)
        )

        otherSchools.forEach { s ->
            val sub = subscriptions.find { it.schoolId == s.schoolId }
            val adm = schoolAdmins.find { it.schoolId == s.schoolId }
            list.add(
                s.copy(
                    adminName = adm?.name ?: s.adminName,
                    adminContact = adm?.phone?.ifBlank { s.adminContact } ?: s.adminContact,
                    credits = sub?.credits ?: s.credits,
                    planName = sub?.planName ?: s.planName,
                    isActive = sub?.isActive ?: s.isActive,
                    isSuspended = sub?.isSuspended ?: s.isSuspended,
                    suspensionReason = sub?.suspensionReason ?: s.suspensionReason,
                    suspendedAtMillis = sub?.suspendedAtMillis ?: s.suspendedAtMillis,
                    purgeWarningSent = sub?.purgeWarningSent ?: s.purgeWarningSent,
                    purgeWarningSentAt = sub?.purgeWarningSentAt ?: s.purgeWarningSentAt
                )
            )
        }
        list
    }

    val totalSchools = schoolMap.size
    val totalTeachers = schoolMap.sumOf { it.teacherCount }
    val totalClasses = schoolMap.sumOf { it.classCount }
    val totalStudents = schoolMap.sumOf { it.studentCount }
    val openIssuesCount = reportedIssues.count { it.status == "OPEN" }
    val suspendedCount = schoolMap.count { it.isSuspended || !it.isActive }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp)
    ) {
        // Super Admin Header Card
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
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚡ Super Admin HQ",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Multi-School Hub • Suspend & Purge • 1:1 Receipts Chat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Tabs Row
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                SuperAdminTab.entries.forEach { tab ->
                    val badgeCount = when (tab) {
                        SuperAdminTab.REPORT_BOX -> if (openIssuesCount > 0) openIssuesCount else null
                        SuperAdminTab.SCHOOL_CONTROL -> if (suspendedCount > 0) suspendedCount else null
                        else -> null
                    }
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tab.title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                if (badgeCount != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = if (tab == SuperAdminTab.SCHOOL_CONTROL) Color(0xFFDC2626) else MaterialTheme.colorScheme.error,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "$badgeCount",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                SuperAdminTab.ANALYTICS -> {
                    SuperAdminAnalyticsView(
                        totalSchools = totalSchools,
                        totalTeachers = totalTeachers,
                        totalClasses = totalClasses,
                        totalStudents = totalStudents,
                        schoolList = schoolMap,
                        onChatWithSchool = { schoolId ->
                            viewModel.setSelectedSchoolForChat(schoolId)
                            selectedTab = SuperAdminTab.DIRECT_CHAT
                        },
                        onManageAccess = {
                            selectedTab = SuperAdminTab.SCHOOL_CONTROL
                        }
                    )
                }

                SuperAdminTab.REPORT_BOX -> {
                    SuperAdminReportBoxView(
                        issues = reportedIssues,
                        onRespond = { issue -> respondingIssue = issue }
                    )
                }

                SuperAdminTab.DIRECT_CHAT -> {
                    SuperAdminDirectChatView(
                        schoolList = schoolMap,
                        selectedSchoolId = selectedSchoolForChat,
                        directMessages = directMessages,
                        chatInput = chatMessageInput,
                        onChatInputChange = { chatMessageInput = it },
                        onSelectSchool = { schoolId ->
                            viewModel.setSelectedSchoolForChat(schoolId)
                        },
                        onSendMessage = {
                            viewModel.sendDirectMessage(selectedSchoolForChat, chatMessageInput)
                            chatMessageInput = ""
                        },
                        onViewTaxInvoice = { msg ->
                            viewingInvoiceMessage = msg
                        }
                    )
                }

                SuperAdminTab.SCHOOL_CONTROL -> {
                    SuperAdminSchoolControlView(
                        schoolList = schoolMap,
                        onToggleActive = { school, active ->
                            viewModel.toggleSchoolStatus(school.schoolId, isActive = active, isSuspended = !active)
                        },
                        onSuspendClick = { school ->
                            suspendingSchool = school
                        },
                        onReactivateClick = { school ->
                            viewModel.toggleSchoolStatus(school.schoolId, isActive = true, isSuspended = false)
                        },
                        onSendPurgeWarningEmail = { school ->
                            viewModel.sendPurgeWarningEmail(school.schoolId, school.adminEmail)
                        },
                        onPurgeDataClick = { school ->
                            purgeConfirmSchool = school
                        }
                    )
                }
            }
        }
    }

    // --- ISSUE RESPONSE DIALOG ---
    if (respondingIssue != null) {
        val issue = respondingIssue!!
        var responseInput by remember { mutableStateOf(issue.superAdminResponse) }
        var statusSelect by remember { mutableStateOf(if (issue.status == "OPEN") "IN_PROGRESS" else issue.status) }

        AlertDialog(
            onDismissRequest = { respondingIssue = null },
            title = { Text("Resolve School Admin Issue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "School: ${issue.schoolName} (${issue.adminName})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Issue: ${issue.title}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = issue.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Update Status:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("OPEN", "IN_PROGRESS", "RESOLVED").forEach { st ->
                            FilterChip(
                                selected = statusSelect == st,
                                onClick = { statusSelect = st },
                                label = { Text(st) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = responseInput,
                        onValueChange = { responseInput = it },
                        label = { Text("Super Admin Response / Instructions") },
                        placeholder = { Text("Write response to school admin...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.respondToAdminIssue(issue.id, statusSelect, responseInput)
                        respondingIssue = null
                    },
                    modifier = Modifier.testTag("submit_issue_response_btn")
                ) {
                    Text("Save & Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { respondingIssue = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- SUSPEND SCHOOL REASON DIALOG ---
    if (suspendingSchool != null) {
        val sch = suspendingSchool!!
        var reasonText by remember { mutableStateOf("Payment Due & Account Verification") }
        val reasonOptions = listOf(
            "Payment Due & Subscription Expired",
            "Violation of Multi-School Terms of Service",
            "Security / Data Compliance Review",
            "School Management Decommission Request"
        )

        AlertDialog(
            onDismissRequest = { suspendingSchool = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Suspend ${sch.schoolName}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⚠️ Suspending this school initiates a strict 15-Day Data Retention Policy. After 15 days, all student, class, and event data will be permanently wiped.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB91C1C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select Reason:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    reasonOptions.forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reasonText = opt }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = reasonText == opt, onClick = { reasonText = opt })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(opt, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleSchoolStatus(sch.schoolId, isActive = false, isSuspended = true, reason = reasonText)
                        suspendingSchool = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Suspension")
                }
            },
            dismissButton = {
                TextButton(onClick = { suspendingSchool = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- PURGE SCHOOL DATA CONFIRMATION DIALOG ---
    if (purgeConfirmSchool != null) {
        val sch = purgeConfirmSchool!!
        AlertDialog(
            onDismissRequest = { purgeConfirmSchool = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626)) },
            title = { Text("Permanently Purge Data?") },
            text = {
                Text(
                    text = "Are you absolutely sure you want to wipe out all data for ${sch.schoolName}? This action is irreversible and permanently deletes all classes, students, events, and records.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.purgeSchoolData(sch.schoolId)
                        purgeConfirmSchool = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Permanent Wipeout")
                }
            },
            dismissButton = {
                TextButton(onClick = { purgeConfirmSchool = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- TAX INVOICE PDF PREVIEW DIALOG ---
    if (viewingInvoiceMessage != null) {
        val msg = viewingInvoiceMessage!!
        val targetSchool = schoolMap.find { it.schoolId == msg.schoolId }
        TaxInvoicePdfDialog(
            invoiceId = msg.receiptId.ifBlank { "INV-2026-RZP5501" },
            paymentId = msg.receiptPaymentId.ifBlank { "pay_rzp_demo123" },
            schoolName = targetSchool?.schoolName ?: "J D International School",
            adminEmail = targetSchool?.adminEmail ?: "admin@jdsschool.com",
            adminPhone = targetSchool?.adminContact ?: "9800000002",
            planName = msg.receiptPlanName.ifBlank { "Annual Pro Plan" },
            credits = if (msg.receiptCredits > 0) msg.receiptCredits else 5,
            amountRupees = if (msg.receiptAmount > 0) msg.receiptAmount.toInt() else 499,
            onDismiss = { viewingInvoiceMessage = null }
        )
    }
}

// ----------------------------------------------------
// 1. ANALYTICS VIEW
// ----------------------------------------------------
@Composable
private fun SuperAdminAnalyticsView(
    totalSchools: Int,
    totalTeachers: Int,
    totalClasses: Int,
    totalStudents: Int,
    schoolList: List<SchoolData>,
    onChatWithSchool: (String) -> Unit,
    onManageAccess: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Schools",
                    value = "$totalSchools",
                    icon = Icons.Default.School,
                    containerColor = Color(0xFFEFF6FF),
                    contentColor = Color(0xFF1D4ED8),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Teachers",
                    value = "$totalTeachers",
                    icon = Icons.Default.Person,
                    containerColor = Color(0xFFF0FDF4),
                    contentColor = Color(0xFF15803D),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Classes",
                    value = "$totalClasses",
                    icon = Icons.Default.Class,
                    containerColor = Color(0xFFFEF3C7),
                    contentColor = Color(0xFFB45309),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Students",
                    value = "$totalStudents",
                    icon = Icons.Default.Groups,
                    containerColor = Color(0xFFF3E8FF),
                    contentColor = Color(0xFF7E22CE),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏫 School Directory & Plans (${schoolList.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onManageAccess) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Access & Suspend")
                }
            }
        }

        items(schoolList, key = { it.schoolId }) { school ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("school_analytics_card_${school.schoolId}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = school.schoolName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (school.isSuspended || !school.isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(color = Color(0xFFFEE2E2), shape = RoundedCornerShape(4.dp)) {
                                        Text("SUSPENDED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color(0xFF991B1B), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Admin: ${school.adminName} • 📞 ${school.adminContact}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "✉️ ${school.adminEmail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AssistChip(
                            onClick = { },
                            label = { Text("Credits: ${school.credits}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (school.credits > 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                labelColor = if (school.credits > 0) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatPill(label = "Teachers", count = "${school.teacherCount}")
                            StatPill(label = "Classes", count = "${school.classCount}")
                            StatPill(label = "Students", count = "${school.studentCount}")
                        }

                        Button(
                            onClick = { onChatWithSchool(school.schoolId) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("chat_with_school_btn_${school.schoolId}")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1:1 Chat", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Plan: ${school.planName} • Valid till: ${sdf.format(Date(school.expiryTime))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = contentColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = contentColor)
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun StatPill(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ----------------------------------------------------
// 2. REPORT BOX VIEW
// ----------------------------------------------------
@Composable
private fun SuperAdminReportBoxView(
    issues: List<AdminReportIssueEntity>,
    onRespond: (AdminReportIssueEntity) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }
    var filterStatus by remember { mutableStateOf("ALL") }

    val filteredIssues = when (filterStatus) {
        "OPEN" -> issues.filter { it.status == "OPEN" }
        "RESOLVED" -> issues.filter { it.status == "RESOLVED" }
        else -> issues
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filterStatus == "ALL",
                onClick = { filterStatus = "ALL" },
                label = { Text("All (${issues.size})") }
            )
            FilterChip(
                selected = filterStatus == "OPEN",
                onClick = { filterStatus = "OPEN" },
                label = { Text("Open (${issues.count { it.status == "OPEN" }})") }
            )
            FilterChip(
                selected = filterStatus == "RESOLVED",
                onClick = { filterStatus = "RESOLVED" },
                label = { Text("Resolved (${issues.count { it.status == "RESOLVED" }})") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredIssues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No reported issues found!", style = MaterialTheme.typography.titleMedium)
                    Text("School admins' reported problems will appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredIssues, key = { it.id }) { issue ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_issue_card_${issue.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = issue.category,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                StatusBadge(statusName = issue.status)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = issue.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = issue.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Reported by: ${issue.adminName} (${issue.schoolName}) • 📞 ${issue.adminContact}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Time: ${sdf.format(Date(issue.createdAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            if (issue.superAdminResponse.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Super Admin Resolution:",
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

                            Button(
                                onClick = { onRespond(issue) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("respond_issue_btn_${issue.id}")
                            ) {
                                Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Respond / Update Resolution")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. 1:1 DIRECT CHAT VIEW (WITH RECEIPT PDF CARDS)
// ----------------------------------------------------
@Composable
private fun SuperAdminDirectChatView(
    schoolList: List<SchoolData>,
    selectedSchoolId: String,
    directMessages: List<DirectMessageEntity>,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    onSelectSchool: (String) -> Unit,
    onSendMessage: () -> Unit,
    onViewTaxInvoice: (DirectMessageEntity) -> Unit
) {
    val activeSchool = schoolList.find { it.schoolId == selectedSchoolId } ?: schoolList.firstOrNull()
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    Column(modifier = Modifier.fillMaxSize()) {
        // School Selector Bar
        Text(
            text = "Select School Admin for 1:1 Hotline:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            schoolList.forEach { school ->
                FilterChip(
                    selected = school.schoolId == selectedSchoolId,
                    onClick = { onSelectSchool(school.schoolId) },
                    label = { Text(school.schoolName) },
                    leadingIcon = {
                        if (school.isSuspended || !school.isActive) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                        }
                    },
                    modifier = Modifier.testTag("chat_school_chip_${school.schoolId}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Chat Header
        if (activeSchool != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeSchool.schoolName} (${activeSchool.adminName})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Direct Hotline & Invoices • 📞 ${activeSchool.adminContact} • ✉️ ${activeSchool.adminEmail}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Message List
        if (directMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No messages yet with this school admin.", style = MaterialTheme.typography.bodyMedium)
                    Text("Send a message or payment notice below!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(directMessages.reversed(), key = { it.id }) { msg ->
                    val isSuperAdmin = msg.senderRole == UserRole.SUPER_ADMIN
                    val isReceipt = msg.messageType == "PAYMENT_RECEIPT" || msg.message.contains("[PAYMENT RECEIPT")

                    if (isReceipt) {
                        // Dedicated Tax Invoice Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = Color(0xFF0C2340),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Official Tax Invoice & Receipt",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF0C2340)
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "RAZORPAY VERIFIED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = msg.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1E293B)
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sdf.format(Date(msg.timestampMillis)),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.Gray
                                    )

                                    Button(
                                        onClick = { onViewTaxInvoice(msg) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C2340)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View Tax PDF", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard chat bubble
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isSuperAdmin) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (isSuperAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (isSuperAdmin) 14.dp else 2.dp,
                                    bottomEnd = if (isSuperAdmin) 2.dp else 14.dp
                                ),
                                modifier = Modifier.widthIn(max = 290.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = msg.senderName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSuperAdmin) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSuperAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sdf.format(Date(msg.timestampMillis)),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isSuperAdmin) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = onChatInputChange,
                placeholder = { Text("Type message to School Admin...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("super_admin_chat_input")
            )

            IconButton(
                onClick = onSendMessage,
                enabled = chatInput.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("super_admin_send_chat_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ----------------------------------------------------
// 4. SCHOOL ACCESS & 15-DAY PURGE CONTROL VIEW
// ----------------------------------------------------
@Composable
private fun SuperAdminSchoolControlView(
    schoolList: List<SchoolData>,
    onToggleActive: (SchoolData, Boolean) -> Unit,
    onSuspendClick: (SchoolData) -> Unit,
    onReactivateClick: (SchoolData) -> Unit,
    onSendPurgeWarningEmail: (SchoolData) -> Unit,
    onPurgeDataClick: (SchoolData) -> Unit
) {
    val now = remember { System.currentTimeMillis() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "15-Day Inactivity & Data Retention Policy",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "When a school is marked Inactive or Suspended, the 15-day countdown begins. Email warnings notify the admin before complete database deletion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            }
        }

        items(schoolList, key = { it.schoolId }) { school ->
            val isInactiveOrSuspended = school.isSuspended || !school.isActive
            val suspendedTime = school.suspendedAtMillis ?: now
            val daysElapsed = ((now - suspendedTime) / (24L * 3600L * 1000L)).toInt()
            val daysLeft = max(0, 15 - daysElapsed)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isInactiveOrSuspended) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = if (isInactiveOrSuspended) BorderStroke(1.dp, Color(0xFFFDA4AF)) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("school_access_card_${school.schoolId}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header & Active Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = school.schoolName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Admin: ${school.adminName} • ✉️ ${school.adminEmail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (school.isActive && !school.isSuspended) "ACTIVE" else "INACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (school.isActive && !school.isSuspended) Color(0xFF15803D) else Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = school.isActive && !school.isSuspended,
                                onCheckedChange = { active ->
                                    onToggleActive(school, active)
                                },
                                modifier = Modifier.testTag("school_active_toggle_${school.schoolId}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // If Suspended/Inactive: 15-Day Purge Warning Box
                    if (isInactiveOrSuspended) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "15-DAY PURGE COUNTDOWN",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color(0xFF991B1B)
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFFDC2626),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$daysLeft Days Left",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (school.suspensionReason.isNotBlank()) "Reason: ${school.suspensionReason}" else "Status: Inactive / Suspended by Super Admin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7F1D1D)
                                )

                                if (school.purgeWarningSent) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Warning email sent to ${school.adminEmail}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Actions for Suspended School
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onSendPurgeWarningEmail(school) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("send_purge_email_btn_${school.schoolId}")
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Warn via Mail", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onReactivateClick(school) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reactivate_school_btn_${school.schoolId}")
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reactivate", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { onPurgeDataClick(school) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("purge_data_btn_${school.schoolId}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Purge Now", fontSize = 11.sp)
                            }
                        }
                    } else {
                        // School is Active -> Allow Suspend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "All student & event data active and synchronized.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedButton(
                                onClick = { onSuspendClick(school) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("suspend_school_btn_${school.schoolId}")
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Suspend School")
                            }
                        }
                    }
                }
            }
        }
    }
}
