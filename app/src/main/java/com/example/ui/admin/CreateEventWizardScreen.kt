package com.example.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EventType
import com.example.ui.viewmodel.EventSyncViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreateEventWizardScreen(
    viewModel: EventSyncViewModel,
    editingEventId: String? = null,
    onEventCreated: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }

    val allEvents by viewModel.events.collectAsState()
    val existingEvent = remember(editingEventId, allEvents) {
        if (editingEventId != null) allEvents.find { it.id == editingEventId } else null
    }

    // Form state
    var selectedType by remember { mutableStateOf(existingEvent?.type ?: EventType.FUNCTION) }
    var eventName by remember { mutableStateOf(existingEvent?.name ?: "") }
    var description by remember { mutableStateOf(existingEvent?.description ?: "") }
    var venue by remember { mutableStateOf(existingEvent?.venue ?: "School Auditorium") }
    var date by remember { mutableStateOf(existingEvent?.date ?: "15 August 2026") }
    var startTime by remember { mutableStateOf(existingEvent?.startTime ?: "09:00 AM") }
    var endTime by remember { mutableStateOf(existingEvent?.endTime ?: "12:00 PM") }
    var gapMinutes by remember { mutableIntStateOf(existingEvent?.interPerformanceGapMinutes ?: 3) }

    val classes by viewModel.classes.collectAsState()
    val performanceTypes by viewModel.performanceTypesForCurrentSchool.collectAsState()
    val teachers by viewModel.teachers.collectAsState()

    val selectedClassIds = remember { mutableStateListOf<String>() }
    val selectedPerfTypeIds = remember { mutableStateListOf<String>() }
    val selectedTeacherIds = remember { mutableStateListOf<String>() }

    val existingClassIds by viewModel.getEligibleClassIdsForEvent(editingEventId ?: "").collectAsState(initial = emptyList())
    val existingTeacherIds by viewModel.currentCoordinatorTeacherIds.collectAsState(initial = emptyList())

    // Populating existing event data when in edit mode
    LaunchedEffect(existingEvent, existingClassIds, existingTeacherIds) {
        if (existingEvent != null) {
            selectedType = existingEvent.type
            eventName = existingEvent.name
            description = existingEvent.description
            venue = existingEvent.venue
            date = existingEvent.date
            startTime = existingEvent.startTime
            endTime = existingEvent.endTime

            if (existingClassIds.isNotEmpty() && selectedClassIds.isEmpty()) {
                selectedClassIds.clear()
                selectedClassIds.addAll(existingClassIds)
            }
            if (existingTeacherIds.isNotEmpty() && selectedTeacherIds.isEmpty()) {
                selectedTeacherIds.clear()
                selectedTeacherIds.addAll(existingTeacherIds)
            }
        }
    }

    // Automatically initialize defaults on first load if creating new event
    LaunchedEffect(classes, performanceTypes, teachers) {
        if (existingEvent == null) {
            if (selectedClassIds.isEmpty() && classes.isNotEmpty()) {
                selectedClassIds.addAll(classes.map { it.id })
            }
            if (selectedPerfTypeIds.isEmpty() && performanceTypes.isNotEmpty()) {
                selectedPerfTypeIds.addAll(performanceTypes.take(5).map { it.id })
            }
            if (selectedTeacherIds.isEmpty() && teachers.isNotEmpty()) {
                selectedTeacherIds.addAll(teachers.take(2).map { it.id })
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Step Indicator Progress
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (existingEvent != null) "Editing Event • Step $step of 2" else "Step $step of 2",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                LinearProgressIndicator(
                    progress = { step / 2f },
                    modifier = Modifier
                        .width(160.dp)
                        .height(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (step) {
                1 -> Step1EventType(
                    selectedType = selectedType,
                    onSelect = { selectedType = it }
                )
                else -> Step2BasicDetails(
                    eventName = eventName,
                    onEventNameChange = { eventName = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    venue = venue,
                    onVenueChange = { venue = it },
                    date = date,
                    onDateChange = { date = it },
                    startTime = startTime,
                    onStartTimeChange = { startTime = it },
                    gapMinutes = gapMinutes,
                    onGapMinutesChange = { gapMinutes = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wizard Bottom Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { step-- },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back")
                }
            } else {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }

            if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("wizard_next_button")
                ) {
                    Text("Next Step (Event Details)")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                }
            } else {
                Button(
                    onClick = {
                        val finalName = eventName.ifBlank { "Annual School Event" }
                        val allClasses = classes.map { it.id }
                        val allTeachers = teachers.map { it.id }
                        if (existingEvent != null) {
                            val updated = existingEvent.copy(
                                name = finalName,
                                type = selectedType,
                                description = description,
                                date = date,
                                startTime = startTime,
                                endTime = endTime,
                                venue = venue,
                                interPerformanceGapMinutes = gapMinutes,
                                updatedAt = System.currentTimeMillis()
                            )
                            viewModel.updateFullEvent(
                                event = updated,
                                eligibleClassIds = allClasses,
                                coordinatorTeacherIds = allTeachers
                            )
                        } else {
                            viewModel.createEvent(
                                name = finalName,
                                type = selectedType,
                                description = description,
                                date = date,
                                startTime = startTime,
                                endTime = endTime,
                                venue = venue,
                                interPerformanceGapMinutes = gapMinutes,
                                eligibleClassIds = allClasses,
                                coordinatorTeacherIds = allTeachers
                            )
                        }
                        onEventCreated()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_event_submit")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (existingEvent != null) "Save Event Changes" else "Create Event Now")
                }
            }
        }
    }
}

private fun SimpleDateFormatter(): String {
    return SimpleDateFormat("yyyy", Locale.US).format(Date())
}

@Composable
private fun Step1EventType(
    selectedType: EventType,
    onSelect: (EventType) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "STEP 1 — What type of event are you creating?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Cards for FUNCTION vs COMPETITION
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selectedType == EventType.FUNCTION) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(EventType.FUNCTION) }
                .border(
                    width = if (selectedType == EventType.FUNCTION) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("event_type_function")
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎉", fontSize = 36.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "FUNCTION / CELEBRATION",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Independence Day, Annual Function, Republic Day, Teacher's Day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selectedType == EventType.COMPETITION) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(EventType.COMPETITION) }
                .border(
                    width = if (selectedType == EventType.COMPETITION) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("event_type_competition")
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🏆", fontSize = 36.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "COMPETITION",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dance Competition, Singing Championship, Debate, Quiz, Drama Fest",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Step2BasicDetails(
    eventName: String, onEventNameChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    venue: String, onVenueChange: (String) -> Unit,
    date: String, onDateChange: (String) -> Unit,
    startTime: String, onStartTimeChange: (String) -> Unit,
    gapMinutes: Int, onGapMinutesChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    val datePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.US)
                onDateChange(sdf.format(cal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val startTimePickerDialog = remember(context) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val isPm = hourOfDay >= 12
                val h12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                onStartTimeChange(String.format(Locale.US, "%02d:%02d %s", h12, minute, if (isPm) "PM" else "AM"))
            },
            9, 0, false
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "STEP 2 — Basic Event Details",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = eventName,
            onValueChange = onEventNameChange,
            label = { Text("Event Name *") },
            placeholder = { Text("e.g. Annual Cultural Fest 2026") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_event_name")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = venue,
            onValueChange = onVenueChange,
            label = { Text("Venue") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Date Picker Field (Calendar Dialog)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() }
        ) {
            OutlinedTextField(
                value = date,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Event Date 📅 (Tap for Calendar)") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Calendar Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.primary,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_event_date")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Start Time Setter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { startTimePickerDialog.show() }
        ) {
            OutlinedTextField(
                value = startTime,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Start Time ⏰ (Tap to change)") },
                trailingIcon = {
                    IconButton(onClick = { startTimePickerDialog.show() }) {
                        Icon(Icons.Default.Schedule, contentDescription = "Start Time", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Inter-Performance Gap / Transition Time Setter
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
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
                            text = "Inter-Performance Gap / Transition Time",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Time reserved between items for anchoring, stage prep & announcements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { if (gapMinutes > 1) onGapMinutesChange(gapMinutes - 1) },
                            enabled = gapMinutes > 1
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease Gap")
                        }
                        Text(
                            text = "$gapMinutes min",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { if (gapMinutes < 30) onGapMinutesChange(gapMinutes + 1) }
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase Gap")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(2, 3, 5).forEach { preset ->
                        FilterChip(
                            selected = gapMinutes == preset,
                            onClick = { onGapMinutesChange(preset) },
                            label = { Text("$preset min gap") }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun Step3EligibleClasses(
    classList: List<com.example.data.local.ClassEntity>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "STEP 3 — Eligible Classes",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Select which classes can participate in this event:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(classList) { cls ->
                val checked = selectedIds.contains(cls.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(cls.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggle(cls.id) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${cls.name} - Section ${cls.section}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step4PerformanceTypes(
    perfTypes: List<com.example.data.local.PerformanceTypeEntity>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "STEP 4 — Allowed Performance Types",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(perfTypes) { pt ->
                val checked = selectedIds.contains(pt.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(pt.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggle(pt.id) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pt.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step5Teachers(
    teachers: List<com.example.data.local.UserEntity>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "STEP 5 — Assign Teacher Coordinators",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(teachers) { teacher ->
                val checked = selectedIds.contains(teacher.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(teacher.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggle(teacher.id) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = teacher.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = teacher.email,
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

@Composable
private fun Step6Review(
    name: String, type: EventType, date: String, time: String, venue: String,
    classCount: Int, perfTypeCount: Int, teacherCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "STEP 6 — Review & Confirm",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ReviewRow("Event Name", name.ifBlank { "Annual School Function" })
                ReviewRow("Type", type.name)
                ReviewRow("Date", date)
                ReviewRow("Time & Duration", time)
                ReviewRow("Venue", venue)
                ReviewRow("Eligible Classes", "$classCount classes selected")
                ReviewRow("Performance Types", "$perfTypeCount types selected")
                ReviewRow("Teacher Coordinators", "$teacherCount teachers assigned")
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}

private fun calculateDurationText(start: String, end: String): String {
    return try {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        val d1 = sdf.parse(start.trim().uppercase())
        val d2 = sdf.parse(end.trim().uppercase())
        if (d1 != null && d2 != null) {
            val diffMs = d2.time - d1.time
            val diffMins = (diffMs / (1000 * 60)).toInt()
            if (diffMins > 0) {
                val h = diffMins / 60
                val m = diffMins % 60
                if (h > 0 && m > 0) "$h hours $m mins" else if (h > 0) "$h hours" else "$m minutes"
            } else "3 hours (Default)"
        } else "3 hours"
    } catch (e: Exception) {
        "3 hours"
    }
}
