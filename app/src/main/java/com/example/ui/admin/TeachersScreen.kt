package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmailInputField
import com.example.ui.components.PhoneInputField
import com.example.ui.components.StatusBadge
import com.example.ui.components.ValidationUtils
import com.example.ui.components.downloadCsvFile
import com.example.ui.viewmodel.EventSyncViewModel

@Composable
fun TeachersScreen(
    viewModel: EventSyncViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val teachers by viewModel.teachers.collectAsState()
    val teacherCsvSummary by viewModel.teacherCsvImportSummary.collectAsState()

    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var showBulkImportTeachersDialog by remember { mutableStateOf(false) }
    var teacherToDelete by remember { mutableStateOf<com.example.data.local.UserEntity?>(null) }
    var teacherToEdit by remember { mutableStateOf<com.example.data.local.UserEntity?>(null) }

    var rawTeacherCsv by remember {
        mutableStateOf(
            """Name, Email, Phone
Vikram Sharma, vikram.sharma@school.com, +91 9876543201
Ananya Roy, ananya.roy@school.com, +91 9876543202
Suresh Nair, suresh.nair@school.com, +91 9876543203"""
        )
    }

    val teacherFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader(Charsets.UTF_8).readText().replace("\uFEFF", "")
                    if (text.isNotBlank()) {
                        rawTeacherCsv = text
                        viewModel.showUserToast("✅ Teacher file uploaded successfully!")
                    }
                }
            } catch (e: Exception) {
                viewModel.showUserToast("❌ Error reading file: ${e.message}")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Teacher Coordinators",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${teachers.size} / ${EventSyncViewModel.MAX_TEACHERS} Teachers registered",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (teachers.size >= EventSyncViewModel.MAX_TEACHERS) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (teachers.size >= EventSyncViewModel.MAX_TEACHERS) {
                            viewModel.showUserToast("⚠️ Maximum limit of ${EventSyncViewModel.MAX_TEACHERS} teachers reached!")
                        } else {
                            showBulkImportTeachersDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("bulk_import_teachers_btn")
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bulk Import")
                }

                Button(
                    onClick = {
                        if (teachers.size >= EventSyncViewModel.MAX_TEACHERS) {
                            viewModel.showUserToast("⚠️ Maximum limit of ${EventSyncViewModel.MAX_TEACHERS} teachers reached!")
                        } else {
                            showAddTeacherDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_teacher_button")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(teachers) { teacher ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = teacher.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "✉️ ${teacher.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (teacher.phone.isNotBlank()) {
                                Text(
                                    text = "📞 ${teacher.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(statusName = "ACTIVE")
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { teacherToEdit = teacher },
                                modifier = Modifier.testTag("edit_teacher_btn_${teacher.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Teacher",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { teacherToDelete = teacher },
                                modifier = Modifier.testTag("delete_teacher_btn_${teacher.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Teacher",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (teacherToEdit != null) {
        val teacher = teacherToEdit!!
        var editName by remember(teacher.id) { mutableStateOf(teacher.name) }
        var editEmail by remember(teacher.id) { mutableStateOf(teacher.email) }
        val parsedPhone = remember(teacher.id) { ValidationUtils.extractPhoneNumberOnly(teacher.phone) }
        val parsedCode = remember(teacher.id) { ValidationUtils.extractCountryCode(teacher.phone) }
        var editPhone by remember(teacher.id) { mutableStateOf(parsedPhone) }
        var editCountryCode by remember(teacher.id) { mutableStateOf(parsedCode) }

        val isEmailValid = ValidationUtils.isValidEmail(editEmail)
        val fullPhone = "$editCountryCode $editPhone"
        val isPhoneValid = ValidationUtils.isValidPhone(fullPhone)
        val canSubmit = editName.isNotBlank() && isEmailValid && isPhoneValid

        AlertDialog(
            onDismissRequest = { teacherToEdit = null },
            title = { Text("Edit Teacher Coordinator") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_teacher_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EmailInputField(
                        email = editEmail,
                        onEmailChange = { editEmail = it },
                        modifier = Modifier.testTag("edit_teacher_email_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PhoneInputField(
                        phone = editPhone,
                        onPhoneChange = { editPhone = it },
                        countryCode = editCountryCode,
                        onCountryCodeChange = { editCountryCode = it },
                        label = "Phone Number (10 Digits) *",
                        isMandatory = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canSubmit) {
                            val formattedPhone = ValidationUtils.formatPhoneNumber(fullPhone)
                            viewModel.updateTeacher(teacher.id, editName.trim(), editEmail.trim(), formattedPhone)
                            teacherToEdit = null
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.testTag("update_teacher_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddTeacherDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var countryCode by remember { mutableStateOf("+91") }

        val isEmailValid = ValidationUtils.isValidEmail(email)
        val fullPhone = "$countryCode $phone"
        val isPhoneValid = ValidationUtils.isValidPhone(fullPhone)
        val canSubmit = name.isNotBlank() && isEmailValid && isPhoneValid

        AlertDialog(
            onDismissRequest = { showAddTeacherDialog = false },
            title = { Text("Add Teacher Coordinator") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("teacher_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EmailInputField(
                        email = email,
                        onEmailChange = { email = it },
                        modifier = Modifier.testTag("teacher_email_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PhoneInputField(
                        phone = phone,
                        onPhoneChange = { phone = it },
                        countryCode = countryCode,
                        onCountryCodeChange = { countryCode = it },
                        label = "Phone Number (10 Digits) *",
                        isMandatory = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "📧 An onboarding invite email with login credentials will be automatically sent to the teacher.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (canSubmit) {
                            val formattedPhone = ValidationUtils.formatPhoneNumber(fullPhone)
                            viewModel.createTeacher(name.trim(), email.trim(), formattedPhone)
                            showAddTeacherDialog = false
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.testTag("save_teacher_button")
                ) {
                    Text("Add & Send Onboarding")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTeacherDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBulkImportTeachersDialog) {
        AlertDialog(
            onDismissRequest = {
                showBulkImportTeachersDialog = false
                viewModel.clearTeacherCsvImport()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bulk Import Teachers")
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
                                text = "Expected Excel / CSV columns: Name, Email, Phone",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val csvSample = """Name, Email, Phone
Vikram Sharma, vikram.sharma@school.com, +91 9876543201
Ananya Roy, ananya.roy@school.com, +91 9876543202
Suresh Nair, suresh.nair@school.com, +91 9876543203
Pooja Mehta, pooja.mehta@school.com, +91 9876543204"""
                                    rawTeacherCsv = csvSample
                                    downloadCsvFile(context, "Teacher_Import_Sample.csv", csvSample)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("download_teacher_excel_format_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⬇️ Download Excel Sample Format")
                            }
                        }
                    }

                    // STEP 2: UPLOAD OR PASTE DATA
                    Text(
                        text = "Step 2: Upload or Paste Teacher Data",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    OutlinedButton(
                        onClick = { teacherFilePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_teacher_excel_file_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📁 Upload Excel / CSV File")
                    }

                    OutlinedTextField(
                        value = rawTeacherCsv,
                        onValueChange = { rawTeacherCsv = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("teacher_csv_textarea"),
                        label = { Text("Excel / CSV Data Preview") }
                    )

                    Button(
                        onClick = { viewModel.processTeacherCsvImport(rawTeacherCsv) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parse_teacher_csv_btn")
                    ) {
                        Icon(Icons.Default.FindInPage, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate CSV Data")
                    }

                    teacherCsvSummary?.let { summary ->
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
                                Text("✅ ${summary.validRecords.size} valid teachers found")
                                if (summary.invalidRecordsCount > 0) Text("❌ ${summary.invalidRecordsCount} invalid rows")
                                if (summary.duplicateRecordsCount > 0) Text("⚠️ ${summary.duplicateRecordsCount} existing emails skipped")

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
                                        viewModel.confirmTeacherCsvImport()
                                        showBulkImportTeachersDialog = false
                                    },
                                    enabled = summary.validRecords.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_teacher_import_btn")
                                ) {
                                    Text("Confirm & Import ${summary.validRecords.size} Teachers")
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
                        showBulkImportTeachersDialog = false
                        viewModel.clearTeacherCsvImport()
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (teacherToDelete != null) {
        val targetTeacher = teacherToDelete!!
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Teacher Coordinator?") },
            text = {
                Text("Are you sure you want to delete ${targetTeacher.name} (${targetTeacher.email})? They will lose access as event coordinator.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(targetTeacher)
                        teacherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_teacher_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
