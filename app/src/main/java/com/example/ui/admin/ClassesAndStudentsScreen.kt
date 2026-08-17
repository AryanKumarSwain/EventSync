package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClassEntity
import com.example.data.local.StudentEntity
import com.example.ui.components.PhoneInputField
import com.example.ui.components.ValidationUtils
import com.example.ui.components.downloadCsvFile
import com.example.ui.viewmodel.EventSyncViewModel

@Composable
fun ClassesAndStudentsScreen(
    viewModel: EventSyncViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val classes by viewModel.classes.collectAsState()
    val students by viewModel.students.collectAsState()
    val classCsvSummary by viewModel.classCsvImportSummary.collectAsState()
    val studentCsvSummary by viewModel.csvImportSummary.collectAsState()
    val fullSchoolCsvSummary by viewModel.fullSchoolCsvImportSummary.collectAsState()

    var selectedClass by remember { mutableStateOf<ClassEntity?>(null) }

    var showAddClassDialog by remember { mutableStateOf(false) }
    var showBulkImportClassesDialog by remember { mutableStateOf(false) }

    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showBulkImportStudentsDialog by remember { mutableStateOf(false) }

    var showFullSchoolBulkImportDialog by remember { mutableStateOf(false) }

    var classToDelete by remember { mutableStateOf<ClassEntity?>(null) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    var classToEdit by remember { mutableStateOf<ClassEntity?>(null) }
    var studentToEdit by remember { mutableStateOf<StudentEntity?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var rawClassCsv by remember { mutableStateOf("") }
    var rawStudentCsv by remember { mutableStateOf("") }
    var rawFullSchoolCsv by remember {
        mutableStateOf(
            """Class Name, Section, Student Name, Admission Number, Parent Name, Parent Contact
Class 10, A, Rahul Sharma, 1001, Manoj Sharma, 9876543210
Class 10, A, Priya Singh, 1002, Rajesh Singh, 9876543211
Class 10, B, Amit Patel, 1003, Suresh Patel, 9876543212
Class 11, Science, Neha Gupta, 1101, Ramesh Gupta, 9876543213
Class 12, Commerce, Vikram Rao, 1201, Ashok Rao, 9876543214"""
        )
    }

    val fullSchoolFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                if (!content.isNullOrBlank()) {
                    rawFullSchoolCsv = content
                    viewModel.showUserToast("✅ Full School Excel/CSV file uploaded successfully!")
                } else {
                    viewModel.showUserToast("⚠️ Selected file was empty.")
                }
            } catch (e: Exception) {
                viewModel.showUserToast("⚠️ Could not read file: ${e.message}")
            }
        }
    }

    val classFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                if (!content.isNullOrBlank()) {
                    rawClassCsv = content
                    viewModel.showUserToast("✅ Excel/CSV file uploaded successfully!")
                } else {
                    viewModel.showUserToast("⚠️ Selected file was empty.")
                }
            } catch (e: Exception) {
                viewModel.showUserToast("⚠️ Could not read file: ${e.message}")
            }
        }
    }

    val studentFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
                if (!content.isNullOrBlank()) {
                    rawStudentCsv = content
                    viewModel.showUserToast("✅ Excel/CSV file uploaded successfully!")
                } else {
                    viewModel.showUserToast("⚠️ Selected file was empty.")
                }
            } catch (e: Exception) {
                viewModel.showUserToast("⚠️ Could not read file: ${e.message}")
            }
        }
    }

    // Keep selectedClass synchronized if classes list updates or selectedClass is deleted
    LaunchedEffect(classes, selectedClass) {
        if (selectedClass != null) {
            val updated = classes.find { it.id == selectedClass?.id }
            selectedClass = updated
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // TOP MAIN TAB HEADER
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Classes Management",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${classes.size} / ${EventSyncViewModel.MAX_CLASSES} Classes • Total Students: ${students.size} / ${EventSyncViewModel.MAX_STUDENTS}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (classes.size >= EventSyncViewModel.MAX_CLASSES || students.size >= EventSyncViewModel.MAX_STUDENTS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val currentClass = selectedClass

        if (currentClass == null) {
            // ==========================================
            // LEVEL 1: ALL CLASSES VIEW
            // ==========================================
            // FULL SCHOOL BULK IMPORT BANNER / BUTTON
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏫 Full School Bulk Upload",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Auto-create classes & students (Max ${EventSyncViewModel.MAX_CLASSES} Classes, ${EventSyncViewModel.MAX_STUDENTS} Students)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (classes.size >= EventSyncViewModel.MAX_CLASSES && students.size >= EventSyncViewModel.MAX_STUDENTS) {
                                viewModel.showUserToast("⚠️ Maximum school limits reached!")
                            } else {
                                showFullSchoolBulkImportDialog = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.testTag("full_school_bulk_import_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload All")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (classes.size >= EventSyncViewModel.MAX_CLASSES) {
                            viewModel.showUserToast("⚠️ Maximum limit of ${EventSyncViewModel.MAX_CLASSES} classes reached!")
                        } else {
                            showBulkImportClassesDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bulk_import_classes_btn")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Classes CSV")
                }

                Button(
                    onClick = {
                        if (classes.size >= EventSyncViewModel.MAX_CLASSES) {
                            viewModel.showUserToast("⚠️ Maximum limit of ${EventSyncViewModel.MAX_CLASSES} classes reached!")
                        } else {
                            showAddClassDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_class_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Class")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (classes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Class,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No classes added yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Click 'Add Class' or 'Bulk Import' to get started",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(classes, key = { it.id }) { cls ->
                        val classStudentsCount = students.count { it.classId == cls.id }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedClass = cls }
                                .testTag("class_card_${cls.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${cls.name} - Section ${cls.section}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AssistChip(
                                                onClick = { },
                                                label = {
                                                    Text(
                                                        text = "$classStudentsCount Students Enrolled",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                                ),
                                                modifier = Modifier.height(24.dp)
                                            )

                                            Text(
                                                text = "AY: ${cls.academicYear}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { classToEdit = cls },
                                        modifier = Modifier.testTag("edit_class_${cls.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Class",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = { classToDelete = cls },
                                        modifier = Modifier.testTag("delete_class_${cls.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Class",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "View Students",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // LEVEL 2: SELECTED CLASS (STUDENTS VIEW)
            // ==========================================
            val classStudents = students.filter { it.classId == currentClass.id }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { selectedClass = null },
                            modifier = Modifier.testTag("back_to_classes_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Classes"
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${currentClass.name} (Sec ${currentClass.section})",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Academic Year: ${currentClass.academicYear} • ${classStudents.size} in Class • School Total: ${students.size} / ${EventSyncViewModel.MAX_STUDENTS}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (students.size >= EventSyncViewModel.MAX_STUDENTS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (students.size >= EventSyncViewModel.MAX_STUDENTS) {
                                    viewModel.showUserToast("⚠️ Maximum limit of ${EventSyncViewModel.MAX_STUDENTS} students reached!")
                                } else {
                                    showBulkImportStudentsDialog = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bulk_import_students_btn")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bulk Import")
                        }

                        Button(
                            onClick = {
                                if (students.size >= EventSyncViewModel.MAX_STUDENTS) {
                                    viewModel.showUserToast("⚠️ Maximum limit of ${EventSyncViewModel.MAX_STUDENTS} students reached!")
                                } else {
                                    showAddStudentDialog = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_student_to_class_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Student")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input inside class
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search student by name or admission #") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("class_student_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            val filteredClassStudents = classStudents.filter { s ->
                searchQuery.isBlank() ||
                        s.name.contains(searchQuery, ignoreCase = true) ||
                        s.admissionNumber.contains(searchQuery)
            }

            if (filteredClassStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No students in this class yet" else "No matching students found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use 'Add Student' or 'Import' to add students to ${currentClass.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredClassStudents, key = { it.id }) { student ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = CircleShape,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = student.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = student.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        val parentDetails = when {
                                            student.parentName.isNotBlank() && student.parentContact.isNotBlank() -> "Parent: ${student.parentName} (${student.parentContact})"
                                            student.parentName.isNotBlank() -> "Parent: ${student.parentName}"
                                            student.parentContact.isNotBlank() -> "Parent Contact: ${student.parentContact}"
                                            else -> "Contact: N/A"
                                        }
                                        Text(
                                            text = "Adm #: ${student.admissionNumber} • $parentDetails",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { studentToEdit = student },
                                        modifier = Modifier.testTag("edit_student_${student.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Student",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = { studentToDelete = student },
                                        modifier = Modifier.testTag("delete_student_${student.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Student",
                                            tint = MaterialTheme.colorScheme.error
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

    // ==========================================
    // DIALOG 1: ADD SINGLE CLASS
    // ==========================================
    if (showAddClassDialog) {
        var className by remember { mutableStateOf("") }
        var section by remember { mutableStateOf("") }
        var academicYear by remember { mutableStateOf("2026-2027") }

        AlertDialog(
            onDismissRequest = { showAddClassDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Class")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Class Name * (e.g. Class 11)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_class_name")
                    )

                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section * (e.g. A)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_class_section")
                    )

                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { academicYear = it },
                        label = { Text("Academic Year") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (className.isNotBlank() && section.isNotBlank()) {
                            viewModel.createClass(className.trim(), section.trim())
                            showAddClassDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_class_confirm_btn")
                ) {
                    Text("Add Class")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClassDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOG 2: BULK IMPORT CLASSES (With Format Download First)
    // ==========================================
    if (showBulkImportClassesDialog) {
        var rawClassCsv by remember {
            mutableStateOf(
                """Class Name, Section, Academic Year
Class 11, A, 2026-2027
Class 11, B, 2026-2027
Class 12, A, 2026-2027
Class 12, B, 2026-2027"""
            )
        }

        AlertDialog(
            onDismissRequest = {
                showBulkImportClassesDialog = false
                viewModel.clearClassCsvImport()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bulk Import Classes")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // STEP 1: EXCEL FORMAT DOWNLOAD BUTTON
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Step 1: Download Excel Format",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Expected Excel / CSV columns: Class Name, Section, Academic Year",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val csvSample = """Class Name, Section, Academic Year
Class 11, A, 2026-2027
Class 11, B, 2026-2027
Class 12, Science, 2026-2027
Class 12, Commerce, 2026-2027"""
                                    rawClassCsv = csvSample
                                    downloadCsvFile(context, "Class_Import_Sample.csv", csvSample)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("download_class_excel_format_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⬇️ Download Excel Sample Format")
                            }
                        }
                    }

                    // STEP 2: UPLOAD OR PASTE DATA
                    Text(
                        text = "Step 2: Upload or Paste Class Data",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedButton(
                        onClick = { classFilePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_class_excel_file_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📁 Upload Excel / CSV File")
                    }

                    OutlinedTextField(
                        value = rawClassCsv,
                        onValueChange = { rawClassCsv = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("class_csv_textarea"),
                        label = { Text("Excel / CSV Data Preview") }
                    )

                    Button(
                        onClick = { viewModel.processClassCsvImport(rawClassCsv) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parse_class_csv_btn")
                    ) {
                        Icon(Icons.Default.FindInPage, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate CSV Data")
                    }

                    classCsvSummary?.let { summary ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (summary.validRecords.isNotEmpty())
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "📊 Import Summary",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("✅ ${summary.validRecords.size} valid classes found")
                                if (summary.invalidRecordsCount > 0) Text("❌ ${summary.invalidRecordsCount} invalid rows")
                                if (summary.duplicateRecordsCount > 0) Text("⚠️ ${summary.duplicateRecordsCount} existing classes skipped")

                                if (summary.errorMessages.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = summary.errorMessages.take(2).joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.confirmClassCsvImport()
                                        showBulkImportClassesDialog = false
                                    },
                                    enabled = summary.validRecords.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_class_import_btn")
                                ) {
                                    Text("Confirm & Import ${summary.validRecords.size} Classes")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showBulkImportClassesDialog = false
                        viewModel.clearClassCsvImport()
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    // ==========================================
    // DIALOG 3: ADD SINGLE STUDENT TO SELECTED CLASS
    // ==========================================
    if (showAddStudentDialog && selectedClass != null) {
        val targetClass = selectedClass!!
        var name by remember { mutableStateOf("") }
        var admNum by remember { mutableStateOf("") }
        var parentName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var countryCode by remember { mutableStateOf("+91") }

        val fullPhone = "$countryCode $phone"
        val isPhoneValid = ValidationUtils.isValidPhone(fullPhone)
        val canSubmit = name.isNotBlank() && admNum.isNotBlank() && isPhoneValid

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Student to ${targetClass.name}")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Adding to Class: ${targetClass.name} - Section ${targetClass.section}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Student Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_name")
                    )

                    OutlinedTextField(
                        value = admNum,
                        onValueChange = { admNum = it },
                        label = { Text("Admission Number *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_student_adm")
                    )

                    OutlinedTextField(
                        value = parentName,
                        onValueChange = { parentName = it },
                        label = { Text("Parent / Guardian Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_parent_name")
                    )

                    PhoneInputField(
                        phone = phone,
                        onPhoneChange = { phone = it },
                        countryCode = countryCode,
                        onCountryCodeChange = { countryCode = it },
                        label = "Parent Contact Number (10 Digits) *",
                        isMandatory = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canSubmit) {
                            val formattedPhone = ValidationUtils.formatPhoneNumber(fullPhone)
                            viewModel.createStudent(
                                name = name.trim(),
                                admNum = admNum.trim(),
                                classId = targetClass.id,
                                section = targetClass.section,
                                phone = formattedPhone,
                                parentName = parentName.trim()
                            )
                            showAddStudentDialog = false
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.testTag("save_student_confirm_btn")
                ) {
                    Text("Save Student")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOG 4: BULK IMPORT STUDENTS FOR SELECTED CLASS (With Format Download First)
    // ==========================================
    if (showBulkImportStudentsDialog && selectedClass != null) {
        val targetClass = selectedClass!!
        var rawStudentCsv by remember {
            mutableStateOf(
                """Name, Admission Number, Section, Contact Number, Parent Name
Rohan Verma, 2001, ${targetClass.section}, 9876543210, Ramesh Verma
Sanya Malhotra, 2002, ${targetClass.section}, 9876543211, Sunita Malhotra
Aarav Gupta, 2003, ${targetClass.section}, 9876543212, Vikash Gupta"""
            )
        }

        AlertDialog(
            onDismissRequest = {
                showBulkImportStudentsDialog = false
                viewModel.clearCsvImport()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bulk Import Students (${targetClass.name})")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // STEP 1: EXCEL FORMAT DOWNLOAD BUTTON
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Step 1: Download Excel Format",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Expected Excel / CSV columns: Name, Admission Number, Section, Contact Number, Parent Name",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val csvSample = """Name, Admission Number, Section, Contact Number, Parent Name
Rohan Verma, 2001, ${targetClass.section}, 9876543210, Ramesh Verma
Sanya Malhotra, 2002, ${targetClass.section}, 9876543211, Sunita Malhotra
Aarav Gupta, 2003, ${targetClass.section}, 9876543212, Vikash Gupta
Diya Sharma, 2004, ${targetClass.section}, 9876543213, Manju Sharma"""
                                    rawStudentCsv = csvSample
                                    downloadCsvFile(context, "Student_Import_Sample_${targetClass.name.replace(" ", "_")}.csv", csvSample)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("download_student_excel_format_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⬇️ Download Excel Sample Format")
                            }
                        }
                    }

                    // STEP 2: UPLOAD OR PASTE DATA
                    Text(
                        text = "Step 2: Upload or Paste Student Data",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedButton(
                        onClick = { studentFilePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_student_excel_file_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📁 Upload Excel / CSV File")
                    }

                    OutlinedTextField(
                        value = rawStudentCsv,
                        onValueChange = { rawStudentCsv = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("student_csv_textarea"),
                        label = { Text("Excel / CSV Data Preview") }
                    )

                    Button(
                        onClick = {
                            viewModel.processCsvImport(
                                rawCsv = rawStudentCsv,
                                targetClassId = targetClass.id,
                                targetSection = targetClass.section
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parse_student_csv_btn")
                    ) {
                        Icon(Icons.Default.FindInPage, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate CSV Data")
                    }

                    studentCsvSummary?.let { summary ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (summary.validRecords.isNotEmpty())
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "📊 Import Summary",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("✅ ${summary.validRecords.size} valid student records found")
                                if (summary.invalidRecordsCount > 0) Text("❌ ${summary.invalidRecordsCount} invalid rows")
                                if (summary.duplicateRecordsCount > 0) Text("⚠️ ${summary.duplicateRecordsCount} duplicate admission numbers skipped")

                                if (summary.errorMessages.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = summary.errorMessages.take(2).joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.confirmCsvImport()
                                        showBulkImportStudentsDialog = false
                                    },
                                    enabled = summary.validRecords.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_student_import_btn")
                                ) {
                                    Text("Confirm & Import ${summary.validRecords.size} Students")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showBulkImportStudentsDialog = false
                        viewModel.clearCsvImport()
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showFullSchoolBulkImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showFullSchoolBulkImportDialog = false
                viewModel.clearFullSchoolCsvImport()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Full School Bulk Upload")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Step 1: Download Full School Excel Format",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Columns: Class Name, Section, Student Name, Admission Number, Parent Name, Parent Contact",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val csvSample = """Class Name, Section, Student Name, Admission Number, Parent Name, Parent Contact
Class 10, A, Rahul Sharma, 1001, Manoj Sharma, 9876543210
Class 10, A, Priya Singh, 1002, Rajesh Singh, 9876543211
Class 10, B, Amit Patel, 1003, Suresh Patel, 9876543212
Class 11, Science, Neha Gupta, 1101, Ramesh Gupta, 9876543213
Class 12, Commerce, Vikram Rao, 1201, Ashok Rao, 9876543214"""
                                    rawFullSchoolCsv = csvSample
                                    downloadCsvFile(context, "Full_School_Classes_And_Students_Sample.csv", csvSample)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("download_full_school_excel_format_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⬇️ Download Full School Excel Format")
                            }
                        }
                    }

                    Text(
                        text = "Step 2: Upload or Paste Full School Data",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedButton(
                        onClick = { fullSchoolFilePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_full_school_excel_file_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📁 Upload Excel / CSV File")
                    }

                    OutlinedTextField(
                        value = rawFullSchoolCsv,
                        onValueChange = { rawFullSchoolCsv = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("full_school_csv_textarea"),
                        label = { Text("Excel / CSV Data Preview") }
                    )

                    Button(
                        onClick = { viewModel.processFullSchoolCsvImport(rawFullSchoolCsv) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parse_full_school_csv_btn")
                    ) {
                        Icon(Icons.Default.FindInPage, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate CSV Data")
                    }

                    fullSchoolCsvSummary?.let { summary ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (summary.validStudents.isNotEmpty() || summary.createdClasses.isNotEmpty())
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "📊 Import Summary",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("🏫 ${summary.createdClasses.size} New Classes to be created")
                                Text("🎓 ${summary.validStudents.size} Valid Students ready to import")
                                if (summary.invalidRecordsCount > 0) Text("❌ ${summary.invalidRecordsCount} invalid rows")
                                if (summary.duplicateRecordsCount > 0) Text("⚠️ ${summary.duplicateRecordsCount} duplicate admission numbers skipped")

                                if (summary.errorMessages.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = summary.errorMessages.take(2).joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.confirmFullSchoolCsvImport()
                                        showFullSchoolBulkImportDialog = false
                                    },
                                    enabled = summary.validStudents.isNotEmpty() || summary.createdClasses.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_full_school_import_btn")
                                ) {
                                    Text("Confirm & Import All (${summary.validStudents.size} Students)")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showFullSchoolBulkImportDialog = false
                        viewModel.clearFullSchoolCsvImport()
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (classToDelete != null) {
        val targetClass = classToDelete!!
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Class?") },
            text = {
                Text("Are you sure you want to delete ${targetClass.name} (Section ${targetClass.section})? This will also remove all associated students.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClass(targetClass.id)
                        if (selectedClass?.id == targetClass.id) {
                            selectedClass = null
                        }
                        classToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_class_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (studentToDelete != null) {
        val targetStudent = studentToDelete!!
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Student?") },
            text = {
                Text("Are you sure you want to delete student ${targetStudent.name} (Admission #: ${targetStudent.admissionNumber})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(targetStudent.id)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_student_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOG: EDIT CLASS
    // ==========================================
    if (classToEdit != null) {
        val cls = classToEdit!!
        var editClassName by remember(cls.id) { mutableStateOf(cls.name) }
        var editSection by remember(cls.id) { mutableStateOf(cls.section) }
        var editAcademicYear by remember(cls.id) { mutableStateOf(cls.academicYear) }

        val canSubmit = editClassName.isNotBlank() && editSection.isNotBlank()

        AlertDialog(
            onDismissRequest = { classToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Class")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editClassName,
                        onValueChange = { editClassName = it },
                        label = { Text("Class Name (e.g. Class 10)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_class_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editSection,
                        onValueChange = { editSection = it },
                        label = { Text("Section (e.g. A, B, Science)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_class_section_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editAcademicYear,
                        onValueChange = { editAcademicYear = it },
                        label = { Text("Academic Year") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_class_academic_year_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canSubmit) {
                            viewModel.updateClass(
                                id = cls.id,
                                name = editClassName.trim(),
                                section = editSection.trim(),
                                academicYear = editAcademicYear.trim()
                            )
                            if (selectedClass?.id == cls.id) {
                                selectedClass = cls.copy(
                                    name = editClassName.trim(),
                                    section = editSection.trim().uppercase(),
                                    academicYear = editAcademicYear.trim()
                                )
                            }
                            classToEdit = null
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.testTag("submit_edit_class_btn")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { classToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOG: EDIT STUDENT
    // ==========================================
    if (studentToEdit != null) {
        val st = studentToEdit!!
        var editStudentName by remember(st.id) { mutableStateOf(st.name) }
        var editAdmNum by remember(st.id) { mutableStateOf(st.admissionNumber) }
        var editParentName by remember(st.id) { mutableStateOf(st.parentName) }
        val parsedPhone = remember(st.id) { ValidationUtils.extractPhoneNumberOnly(st.parentContact) }
        val parsedCode = remember(st.id) { ValidationUtils.extractCountryCode(st.parentContact) }
        var editPhone by remember(st.id) { mutableStateOf(parsedPhone) }
        var editCountryCode by remember(st.id) { mutableStateOf(parsedCode) }

        val fullParentPhone = if (editPhone.isNotBlank()) "$editCountryCode $editPhone" else ""
        val isPhoneValid = fullParentPhone.isBlank() || ValidationUtils.isValidPhone(fullParentPhone)
        val canSubmit = editStudentName.isNotBlank() && editAdmNum.isNotBlank() && isPhoneValid

        AlertDialog(
            onDismissRequest = { studentToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Student")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editStudentName,
                        onValueChange = { editStudentName = it },
                        label = { Text("Student Full Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_student_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editAdmNum,
                        onValueChange = { editAdmNum = it },
                        label = { Text("Admission Number *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_student_adm_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editParentName,
                        onValueChange = { editParentName = it },
                        label = { Text("Parent / Guardian Name (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_student_parent_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PhoneInputField(
                        phone = editPhone,
                        onPhoneChange = { editPhone = it },
                        countryCode = editCountryCode,
                        onCountryCodeChange = { editCountryCode = it },
                        label = "Parent Phone Number",
                        isMandatory = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canSubmit) {
                            val formattedPhone = if (fullParentPhone.isNotBlank()) ValidationUtils.formatPhoneNumber(fullParentPhone) else ""
                            viewModel.updateStudent(
                                id = st.id,
                                name = editStudentName.trim(),
                                admissionNumber = editAdmNum.trim(),
                                classId = st.classId,
                                section = st.section,
                                parentContact = formattedPhone,
                                parentName = editParentName.trim()
                            )
                            studentToEdit = null
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.testTag("submit_edit_student_btn")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
