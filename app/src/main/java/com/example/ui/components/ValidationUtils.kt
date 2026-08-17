package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

object ValidationUtils {

    // Common Country Codes
    val COUNTRY_CODES = listOf("+91", "+1", "+44", "+61", "+86", "+971", "+65", "+49")

    /**
     * Validates if the email address contains '@' and a valid domain format.
     */
    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return false
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(trimmed)
    }

    /**
     * Validates if a phone number contains a 10-digit number along with a valid country code.
     */
    fun isValidPhone(phone: String): Boolean {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) return false

        val digitsOnly = trimmed.filter { it.isDigit() }
        val hasPlus = trimmed.startsWith("+")

        return if (hasPlus) {
            // Must have country code (1-3 digits) + 10 digit phone number = 11 to 14 total digits
            digitsOnly.length in 11..14
        } else {
            // Pure 10 digit number (we will auto-prefix country code)
            digitsOnly.length == 10
        }
    }

    /**
     * Formats phone number ensuring it has country code prefix (+91 default if missing) and 10 digits.
     */
    fun formatPhoneNumber(phone: String, defaultCountryCode: String = "+91"): String {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) return ""

        val digitsOnly = trimmed.filter { it.isDigit() }
        if (trimmed.startsWith("+")) {
            return trimmed
        }

        if (digitsOnly.length == 10) {
            return "$defaultCountryCode $digitsOnly"
        }

        return "$defaultCountryCode $digitsOnly"
    }

    /**
     * Extracts the country code from a stored full phone string (or returns default).
     */
    fun extractCountryCode(fullPhone: String, defaultCode: String = "+91"): String {
        val trimmed = fullPhone.trim()
        for (code in COUNTRY_CODES) {
            if (trimmed.startsWith(code)) return code
        }
        return defaultCode
    }

    /**
     * Extracts only the 10-digit national number from a formatted or raw phone string.
     */
    fun extractPhoneNumberOnly(fullPhone: String): String {
        val trimmed = fullPhone.trim()
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (digitsOnly.length >= 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
    }

    /**
     * Helper error message for Email field
     */
    fun getEmailError(email: String): String? {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return "Email is required"
        if (!trimmed.contains("@")) return "Email must contain '@' symbol"
        if (!isValidEmail(trimmed)) return "Enter a valid email (e.g., name@domain.com)"
        return null
    }

    /**
     * Helper error message for Phone field
     */
    fun getPhoneError(phone: String): String? {
        val trimmed = phone.trim()
        if (trimmed.isBlank()) return "Phone number is required"
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length < 10) return "Phone number must be a 10-digit number"
        if (!isValidPhone(trimmed)) return "Must include 10 digits with country code (e.g. +91 9876543210)"
        return null
    }
}

/**
     Reusable Phone Input Component with Country Code Selector
 */
@Composable
fun PhoneInputField(
    phone: String,
    onPhoneChange: (String) -> Unit,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Phone Number (10 Digits) *",
    isMandatory: Boolean = true
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val phoneError = if (isMandatory || phone.isNotBlank()) ValidationUtils.getPhoneError("$countryCode $phone") else null
    val isError = phoneError != null && phone.isNotBlank()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country Code Dropdown
            Box {
                Surface(
                    onClick = { dropdownExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = countryCode,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Country Code"
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    ValidationUtils.COUNTRY_CODES.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(code) },
                            onClick = {
                                onCountryCodeChange(code)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 10-digit Phone Number Input
            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    // Limit input to digits only, up to 10 digits
                    val filtered = input.filter { it.isDigit() }.take(10)
                    onPhoneChange(filtered)
                },
                label = { Text(label) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = isError,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        if (phoneError != null && (phone.isNotBlank() || isMandatory)) {
            Text(
                text = if (phone.isBlank() && isMandatory) "⚠️ 10-digit phone number with country code is required" else "⚠️ $phoneError",
                style = MaterialTheme.typography.labelSmall,
                color = if (phone.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Reusable Email Input Component with @ and domain validation
 */
@Composable
fun EmailInputField(
    email: String,
    onEmailChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Email Address *",
    isMandatory: Boolean = true
) {
    val emailError = ValidationUtils.getEmailError(email)
    val isError = emailError != null && email.isNotBlank()

    Column(modifier = modifier) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = isError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (emailError != null) {
            Text(
                text = if (email.isBlank() && isMandatory) "⚠️ Valid email with '@' and domain is required" else "⚠️ $emailError",
                style = MaterialTheme.typography.labelSmall,
                color = if (email.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
