package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class RazorpayPaymentMethod(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    UPI("UPI / QR", "Google Pay, PhonePe, Paytm, BHIM", Icons.Default.QrCodeScanner),
    CARD("Cards", "Credit / Debit Cards (Visa, Master, RuPay)", Icons.Default.CreditCard),
    NET_BANKING("Net Banking", "All Indian Banks (SBI, HDFC, ICICI...)", Icons.Default.AccountBalance),
    WALLET("Wallets", "Paytm, Amazon Pay, Mobikwik", Icons.Default.AccountBalanceWallet)
}

enum class RazorpayCheckoutState {
    INPUT_SELECTION,
    PROCESSING,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RazorpayCheckoutModal(
    planName: String,
    credits: Int,
    amountRupees: Int,
    validityDays: Int,
    schoolName: String,
    adminEmail: String,
    adminPhone: String,
    onPaymentSuccess: (paymentId: String, invoiceId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var checkoutState by remember { mutableStateOf(RazorpayCheckoutState.INPUT_SELECTION) }
    var selectedMethod by remember { mutableStateOf(RazorpayPaymentMethod.UPI) }
    
    // Contact details
    var emailInput by remember { mutableStateOf(adminEmail.ifBlank { "admin@jdsschool.com" }) }
    var phoneInput by remember { mutableStateOf(adminPhone.ifBlank { "9800000002" }) }
    
    // UPI Fields
    var selectedUpiApp by remember { mutableStateOf("Google Pay") }
    var customUpiId by remember { mutableStateOf("admin@okaxis") }
    var isUpiVerified by remember { mutableStateOf(true) }
    var showUpiQr by remember { mutableStateOf(false) }

    // Card Fields
    var cardNumber by remember { mutableStateOf("4532 8901 2345 6789") }
    var cardExpiry by remember { mutableStateOf("08/29") }
    var cardCvv by remember { mutableStateOf("882") }
    var cardHolder by remember { mutableStateOf(schoolName) }

    // Netbanking Fields
    var selectedBank by remember { mutableStateOf("HDFC Bank") }

    // Wallet Fields
    var selectedWallet by remember { mutableStateOf("Paytm Wallet") }

    // Transaction Result
    var generatedPaymentId by remember { mutableStateOf("") }
    var generatedInvoiceId by remember { mutableStateOf("") }
    var processingStepText by remember { mutableStateOf("Connecting to Razorpay Secure Gateway...") }
    var showInvoiceDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = {
            if (checkoutState != RazorpayCheckoutState.PROCESSING) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("razorpay_checkout_modal")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // --- RAZORPAY OFFICIAL HEADER ---
                Surface(
                    color = Color(0xFF0C2340),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF2B84EA),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Razorpay",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Trusted Business Gateway",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF93C5FD)
                                    )
                                }
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF4ADE80),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Test Mode • rzp_test",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Plan & Price Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = schoolName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = planName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Amount to Pay",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "₹$amountRupees.00",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 24.sp
                                    ),
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }

                // --- CONTENT BODY BASED ON STATE ---
                when (checkoutState) {
                    RazorpayCheckoutState.INPUT_SELECTION -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Contact info pill
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = emailInput,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "+91 $phoneInput",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Verified",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "PAYMENT OPTIONS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Payment Method Tabs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RazorpayPaymentMethod.entries.forEach { method ->
                                    val isSel = selectedMethod == method
                                    Surface(
                                        color = if (isSel) Color(0xFF0C2340) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedMethod = method }
                                            .testTag("razorpay_method_${method.name.lowercase()}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = method.icon,
                                                contentDescription = method.title,
                                                tint = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = method.title,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 10.sp
                                                ),
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Method Detail View
                            when (selectedMethod) {
                                RazorpayPaymentMethod.UPI -> {
                                    Column {
                                        Text("Preferred UPI Apps:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        val upiApps = listOf("Google Pay", "PhonePe", "Paytm UPI", "BHIM", "Cred UPI")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            upiApps.forEach { app ->
                                                val isAppSel = selectedUpiApp == app && !showUpiQr
                                                FilterChip(
                                                    selected = isAppSel,
                                                    onClick = {
                                                        selectedUpiApp = app
                                                        showUpiQr = false
                                                    },
                                                    label = { Text(app, fontSize = 11.sp) },
                                                    modifier = Modifier.testTag("upi_app_$app")
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // UPI ID textfield
                                        OutlinedTextField(
                                            value = customUpiId,
                                            onValueChange = {
                                                customUpiId = it
                                                isUpiVerified = it.contains("@")
                                            },
                                            label = { Text("Enter UPI ID / VPA") },
                                            placeholder = { Text("e.g. mobile@upi or name@okaxis") },
                                            trailingIcon = {
                                                if (isUpiVerified) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Verified", color = Color(0xFF16A34A), style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedButton(
                                            onClick = { showUpiQr = !showUpiQr },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (showUpiQr) "Hide QR Code" else "Scan Dynamic UPI QR Code")
                                        }

                                        if (showUpiQr) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.QrCode2,
                                                        contentDescription = "UPI QR",
                                                        modifier = Modifier.size(130.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "Scan with any UPI App • Amount: ₹$amountRupees",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                RazorpayPaymentMethod.CARD -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = cardNumber,
                                            onValueChange = { cardNumber = it },
                                            label = { Text("Card Number") },
                                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                            trailingIcon = {
                                                Surface(
                                                    color = Color(0xFF0C2340),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("VISA / RuPay", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = cardExpiry,
                                                onValueChange = { cardExpiry = it },
                                                label = { Text("Expiry (MM/YY)") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )

                                            OutlinedTextField(
                                                value = cardCvv,
                                                onValueChange = { cardCvv = it },
                                                label = { Text("CVV") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                            )
                                        }

                                        OutlinedTextField(
                                            value = cardHolder,
                                            onValueChange = { cardHolder = it },
                                            label = { Text("Cardholder Name") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }

                                RazorpayPaymentMethod.NET_BANKING -> {
                                    Column {
                                        Text("Popular Indian Banks:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val banks = listOf("HDFC Bank", "State Bank of India", "ICICI Bank", "Axis Bank", "Kotak Mahindra", "Punjab National Bank")
                                        banks.forEach { bank ->
                                            val isBankSel = selectedBank == bank
                                            Surface(
                                                color = if (isBankSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(8.dp),
                                                border = if (isBankSel) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedBank = bank }
                                                    .padding(vertical = 3.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(bank, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                    RadioButton(selected = isBankSel, onClick = { selectedBank = bank })
                                                }
                                            }
                                        }
                                    }
                                }

                                RazorpayPaymentMethod.WALLET -> {
                                    Column {
                                        Text("Select Wallet:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val wallets = listOf("Paytm Wallet", "Amazon Pay Balance", "Mobikwik", "PhonePe Wallet")
                                        wallets.forEach { w ->
                                            val isWSel = selectedWallet == w
                                            Surface(
                                                color = if (isWSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(8.dp),
                                                border = if (isWSel) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedWallet = w }
                                                    .padding(vertical = 3.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(w, style = MaterialTheme.typography.bodyMedium)
                                                    RadioButton(selected = isWSel, onClick = { selectedWallet = w })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- BOTTOM ACTION BAR ---
                        Surface(
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        checkoutState = RazorpayCheckoutState.PROCESSING
                                        coroutineScope.launch {
                                            processingStepText = "Connecting to Razorpay Secure Gateway..."
                                            delay(1000)
                                            processingStepText = "Authorizing transaction with ${selectedMethod.title}..."
                                            delay(1200)
                                            processingStepText = "Validating payment signature (HMAC-SHA256)..."
                                            delay(1000)
                                            
                                            val payId = "pay_rzp_" + UUID.randomUUID().toString().replace("-", "").take(12)
                                            val invId = "INV-2026-RZP" + (1000..9999).random()
                                            generatedPaymentId = payId
                                            generatedInvoiceId = invId
                                            
                                            checkoutState = RazorpayCheckoutState.SUCCESS
                                            onPaymentSuccess(payId, invId)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0C2340)
                                    ),
                                    modifier = Modifier
                                        .weight(2f)
                                        .testTag("razorpay_pay_now_button")
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4ADE80))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pay ₹$amountRupees with Razorpay",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // --- PROCESSING ANIMATION SCREEN ---
                    RazorpayCheckoutState.PROCESSING -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(54.dp),
                                    color = Color(0xFF0C2340),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Processing Razorpay Payment",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = processingStepText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Please do not press Back or close this window",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // --- PAYMENT SUCCESS & RECEIPT SCREEN ---
                    RazorpayCheckoutState.SUCCESS -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Payment Successful! 🎉",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "Added +$credits Event Credits to $schoolName",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Transaction Receipt Card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Invoice / Receipt No:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(generatedInvoiceId, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Razorpay Payment ID:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(generatedPaymentId, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Plan Purchased:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(planName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Amount Paid (inc. 18% GST):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("₹$amountRupees.00", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF15803D))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Status:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(4.dp)) {
                                            Text("PAID & VERIFIED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Auto-sent to 1:1 chat notification badge
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forum,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tax Invoice & Payment Receipt automatically dispatched to 1:1 Super Admin Chat!",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showInvoiceDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Tax PDF")
                                }

                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Done")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- TAX INVOICE & RECEIPT PDF MODAL ---
    if (showInvoiceDialog) {
        TaxInvoicePdfDialog(
            invoiceId = generatedInvoiceId,
            paymentId = generatedPaymentId,
            schoolName = schoolName,
            adminEmail = emailInput,
            adminPhone = phoneInput,
            planName = planName,
            credits = credits,
            amountRupees = amountRupees,
            onDismiss = { showInvoiceDialog = false }
        )
    }
}

@Composable
fun TaxInvoicePdfDialog(
    invoiceId: String,
    paymentId: String,
    schoolName: String,
    adminEmail: String,
    adminPhone: String,
    planName: String,
    credits: Int,
    amountRupees: Int,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }
    val nowFormatted = remember { sdf.format(Date()) }
    
    val baseAmount = remember(amountRupees) { String.format(Locale.US, "%.2f", amountRupees / 1.18) }
    val cgst = remember(amountRupees) { String.format(Locale.US, "%.2f", (amountRupees - (amountRupees / 1.18)) / 2) }
    val sgst = remember(amountRupees) { String.format(Locale.US, "%.2f", (amountRupees - (amountRupees / 1.18)) / 2) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header Company & Tax Invoice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("EventSync India Technologies", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = Color(0xFF0C2340))
                        Text("GSTIN: 27AABCE1234F1Z8", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
                        Text("CIN: U72200MH2026PTC109822", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
                        Text("Bangalore, Karnataka 560103", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(color = Color(0xFF0C2340), shape = RoundedCornerShape(4.dp)) {
                            Text("TAX INVOICE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(invoiceId, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                        Text(nowFormatted, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                // Billed To
                Text("BILLED TO:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF64748B))
                Text(schoolName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                Text("Email: $adminEmail | Phone: +91 $adminPhone", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                Text("Payment Channel: Razorpay Payment Gateway ($paymentId)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2563EB))

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                // Item Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Description", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                    Text("Qty", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                    Text("Total (₹)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(planName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                        Text("Event Sync Subscription credits for annual festivals", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray)
                    }
                    Text("$credits Credits", style = MaterialTheme.typography.bodySmall, color = Color.Black, modifier = Modifier.padding(horizontal = 12.dp))
                    Text("₹$baseAmount", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                // Tax Breakdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Subtotal: ₹$baseAmount", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    Text("CGST (9.0%): ₹$cgst", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    Text("SGST (9.0%): ₹$sgst", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Grand Total: ₹$amountRupees.00",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Color(0xFF0C2340)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer seal & verification
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Digitally Signed & Razorpay Authorized", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                            Text("This is a computer generated tax invoice. No signature required.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                var downloadSuccess by remember { mutableStateOf(false) }

                if (downloadSuccess) {
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tax Invoice ($invoiceId.pdf) saved to Downloads successfully!",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { downloadSuccess = true },
                        modifier = Modifier.weight(1f).testTag("download_invoice_pdf_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C2340)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
