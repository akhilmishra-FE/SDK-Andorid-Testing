package com.dec.andorid_autopay_demo_lib

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dec.andorid_autopay_demo_lib.ui.theme.ButtonGradientEnd
import com.dec.andorid_autopay_demo_lib.ui.theme.ButtonGradientStart
import com.dec.andorid_autopay_demo_lib.ui.theme.UpiautopaysdkTheme

val LightBlue = Color(0xFFE9F5FF)
val BlueText = Color(0xFF4A90E2)
val GrayText = Color(0xFF757575)
val MainBackground = Color(0xFFFFFFFF)
val CardBackground = Color(0xFFFFFFFF)
val LightGrayBackground = Color(0xFFF7F7F7)

class DetailsActivity : ComponentActivity() {
    
    private lateinit var currentName: String
    private lateinit var currentAccountNumber: String
    private lateinit var currentIfsc: String
    private lateinit var currentUpiVpa: String
    private lateinit var currentTxnId: String
    private lateinit var currentAmount: String
    private var merchantPackage: String? = null
    
    private val editAccountLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                // Update current values
                currentName = data.getStringExtra("NAME") ?: currentName
                currentAccountNumber = data.getStringExtra("ACCOUNT_NUMBER") ?: currentAccountNumber
                currentIfsc = data.getStringExtra("IFSC") ?: currentIfsc
                currentUpiVpa = data.getStringExtra("UPI_VPA") ?: currentUpiVpa
                currentTxnId = data.getStringExtra("TXN_ID") ?: currentTxnId
                currentAmount = data.getStringExtra("AMOUNT") ?: currentAmount
                
                // Update the intent extras for future use
                intent.putExtra("NAME", currentName)
                intent.putExtra("ACCOUNT_NUMBER", currentAccountNumber)
                intent.putExtra("IFSC", currentIfsc)
                intent.putExtra("UPI_VPA", currentUpiVpa)
                intent.putExtra("TXN_ID", currentTxnId)
                intent.putExtra("AMOUNT", currentAmount)
                
                // Recreate activity to show updated data
                recreate()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize current values
        currentName = intent.getStringExtra("NAME") ?: ""
        currentAccountNumber = intent.getStringExtra("ACCOUNT_NUMBER") ?: ""
        currentIfsc = intent.getStringExtra("IFSC") ?: ""
        currentUpiVpa = intent.getStringExtra("UPI_VPA") ?: ""
        currentTxnId = intent.getStringExtra("TXN_ID") ?: ""
        currentAmount = intent.getStringExtra("AMOUNT") ?: ""
        merchantPackage = intent.getStringExtra("MERCHANT_PACKAGE")

        setContent {
            UpiautopaysdkTheme {
                DetailsScreen(
                    name = currentName,
                    accountNumber = currentAccountNumber,
                    ifsc = currentIfsc,
                    upiVpa = currentUpiVpa,
                    txnId = currentTxnId,
                    amount = currentAmount,
                    onBack = { finish() },
                    onEdit = { openEditScreen() },
                    onPayViaCred = { launchUPIMandateFlow() }
                )
            }
        }
    }
    
    private fun openEditScreen() {
        val intent = Intent(this, EditAccountActivity::class.java).apply {
            putExtra("NAME", currentName)
            putExtra("ACCOUNT_NUMBER", currentAccountNumber)
            putExtra("IFSC", currentIfsc)
            putExtra("UPI_VPA", currentUpiVpa)
            putExtra("TXN_ID", currentTxnId)
            putExtra("AMOUNT", currentAmount)
        }
        editAccountLauncher.launch(intent)
    }
    
    private fun launchUPIMandateFlow() {
        val mandateService = MandateStatusService()
        val deepLink = mandateService.generateMandateDeepLink()
        val mandateId = mandateService.generateMandateId()
        
        Log.d("DetailsActivity", "Deep Link: $deepLink")
        Log.d("DetailsActivity", "Mandate ID: $mandateId")
        
        try {
            // Since the deep link works when opened directly, let's just launch it
            val uri = Uri.parse(deepLink)
            Log.d("DetailsActivity", "Parsed URI: $uri")
            
            // Create intent for UPI mandate
            val upiIntent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            Log.d("DetailsActivity", "Attempting to launch UPI intent directly...")
            
            // Try direct launch first (since you confirmed the link works)
            try {
                startActivity(upiIntent)
                Log.d("DetailsActivity", "Successfully launched UPI intent directly")
                
                // Show success toast
                android.widget.Toast.makeText(
                    this, 
                    "UPI app launched. Complete the mandate and return.", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // Store mandate ID for later use when user returns
                storeMandateIdForStatusCheck(mandateId)
                
                // DO NOT start status checking immediately
                // Wait for user to return to app manually
                
            } catch (directLaunchException: Exception) {
                Log.w("DetailsActivity", "Direct launch failed, trying with chooser", directLaunchException)
                
                // If direct launch fails, try with chooser
                try {
                    val chooser = Intent.createChooser(upiIntent, "Complete UPI Mandate")
                    startActivity(chooser)
                    Log.d("DetailsActivity", "Successfully launched UPI chooser")
                    
                    android.widget.Toast.makeText(
                        this, 
                        "Select your UPI app to complete the mandate.", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    
                    // Store mandate ID for later use when user returns
                    storeMandateIdForStatusCheck(mandateId)
                    
                    // DO NOT start status checking immediately
                    // Wait for user to return to app manually
                    
                } catch (chooserException: Exception) {
                    Log.e("DetailsActivity", "Chooser launch also failed", chooserException)
                    
                    // Last resort: Show error but provide manual option
                    android.widget.Toast.makeText(
                        this, 
                        "Unable to launch UPI app automatically. Please open your UPI app manually and use this link: $deepLink", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    
                    // Try to copy link to clipboard for manual use
                    try {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("UPI Mandate Link", deepLink)
                        clipboard.setPrimaryClip(clip)
                        
                        android.widget.Toast.makeText(
                            this, 
                            "UPI link copied to clipboard. Paste it in your browser or UPI app.", 
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } catch (clipboardException: Exception) {
                        Log.e("DetailsActivity", "Failed to copy to clipboard", clipboardException)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("DetailsActivity", "Complete failure in UPI launch", e)
            android.widget.Toast.makeText(
                this, 
                "Error: ${e.message}. Please try opening your UPI app manually.", 
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Store mandate ID in SharedPreferences for later use
    private fun storeMandateIdForStatusCheck(mandateId: String) {
        val prefs = getSharedPreferences("UPI_MANDATE_PREFS", MODE_PRIVATE)
        val timestamp = System.currentTimeMillis()
        
        prefs.edit().apply {
            putString("PENDING_MANDATE_ID", mandateId)
            putLong("MANDATE_TIMESTAMP", timestamp)
            apply()
        }
        
        Log.d("DetailsActivity", "💾 === STORED MANDATE FOR LATER ===")
        Log.d("DetailsActivity", "💾 Mandate ID: $mandateId")
        Log.d("DetailsActivity", "💾 Timestamp: $timestamp")
        
        android.widget.Toast.makeText(this, "Mandate stored! Complete payment and return to app", android.widget.Toast.LENGTH_LONG).show()
    }
    
    // Check if there's a pending mandate to check status for
    private fun checkForPendingMandateStatus() {
        Log.d("DetailsActivity", "=== Checking for pending mandate status ===")
        android.widget.Toast.makeText(this, "Checking for pending mandate...", android.widget.Toast.LENGTH_SHORT).show()
        
        val prefs = getSharedPreferences("UPI_MANDATE_PREFS", MODE_PRIVATE)
        val pendingMandateId = prefs.getString("PENDING_MANDATE_ID", null)
        val timestamp = prefs.getLong("MANDATE_TIMESTAMP", 0)
        
        Log.d("DetailsActivity", "Pending Mandate ID: $pendingMandateId")
        Log.d("DetailsActivity", "Timestamp: $timestamp")
        
        if (pendingMandateId != null) {
            // Check if mandate was created within last 10 minutes (reasonable time window)
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - timestamp
            val tenMinutesInMillis = 10 * 60 * 1000
            
            Log.d("DetailsActivity", "Current time: $currentTime")
            Log.d("DetailsActivity", "Time difference: ${timeDiff / 1000} seconds")
            
            if (timeDiff < tenMinutesInMillis) {
                Log.d("DetailsActivity", "✅ Found pending mandate, launching status check: $pendingMandateId")
                android.widget.Toast.makeText(this, "Found pending mandate! Launching status check...", android.widget.Toast.LENGTH_LONG).show()
                
                // Clear the stored mandate ID
                prefs.edit().remove("PENDING_MANDATE_ID").remove("MANDATE_TIMESTAMP").apply()
                
                // Launch status activity
                launchStatusActivity(pendingMandateId)
            } else {
                Log.d("DetailsActivity", "❌ Pending mandate too old (${timeDiff / 1000}s), ignoring")
                android.widget.Toast.makeText(this, "Pending mandate too old, ignoring", android.widget.Toast.LENGTH_SHORT).show()
                prefs.edit().remove("PENDING_MANDATE_ID").remove("MANDATE_TIMESTAMP").apply()
            }
        } else {
            Log.d("DetailsActivity", "❌ No pending mandate found")
            android.widget.Toast.makeText(this, "No pending mandate found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // Launch status activity
    private fun launchStatusActivity(mandateId: String) {
        val statusIntent = Intent(this, MandateStatusActivity::class.java).apply {
            putExtra("MANDATE_ID", mandateId)
            // Pass merchant package name if available
            merchantPackage?.let {
                putExtra("MERCHANT_PACKAGE", it)
            }
        }
        startActivity(statusIntent)
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("DetailsActivity", "=== onResume() called ===")
        android.widget.Toast.makeText(this, "DetailsActivity resumed - checking for pending mandate", android.widget.Toast.LENGTH_SHORT).show()
        
        // Check for pending mandate status when user returns to this activity
        checkForPendingMandateStatus()
    }
    
    // Debug function to check all installed apps
    private fun debugInstalledApps() {
        val packageManager = packageManager
        val installedApps = packageManager.getInstalledApplications(0)
        
        Log.d("DetailsActivity", "=== ALL INSTALLED APPS ===")
        installedApps.forEach { app ->
            if (app.packageName.contains("pay", ignoreCase = true) || 
                app.packageName.contains("upi", ignoreCase = true) ||
                app.packageName.contains("phonepe", ignoreCase = true) ||
                app.packageName.contains("paytm", ignoreCase = true) ||
                app.packageName.contains("google", ignoreCase = true)) {
                Log.d("DetailsActivity", "Potential UPI app: ${app.packageName}")
            }
        }
        Log.d("DetailsActivity", "=== END INSTALLED APPS ===")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    name: String?,
    accountNumber: String?,
    ifsc: String?,
    upiVpa: String?,
    txnId: String?,
    amount: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onPayViaCred: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground),
        topBar = {
            TopAppBar(
                title = { Text("Merchants Logo", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MainBackground,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = MainBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MerchantDetailsCard(txnId, amount)
            Spacer(modifier = Modifier.height(24.dp))
            AccountDetailsCard(
                name = name,
                accountNumber = accountNumber,
                ifsc = ifsc,
                upiVpa = upiVpa,
                onEdit = onEdit,
                onPayViaCred = onPayViaCred
            )
            Spacer(modifier = Modifier.height(24.dp))
            PayViaOtherAppCard(onPayViaOtherApp = onPayViaCred)
            Spacer(modifier = Modifier.weight(1f))
            TrustedAndSecurePayments()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MerchantDetailsCard(txnId: String?, amount: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightBlue)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Merchant Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("merchant@upi", style = MaterialTheme.typography.bodyMedium, color = GrayText)
                }
                Text("₹ ${amount ?: "N/A"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Txn ID", style = MaterialTheme.typography.bodySmall, color = GrayText)
            Text(txnId ?: "N/A", style = MaterialTheme.typography.bodySmall, color = Color.Black, maxLines = 1)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoColumn(title = "Start Date", value = "20/12/2025", modifier = Modifier.weight(1f))
                InfoColumn(title = "End Date", value = "19/12/2035", modifier = Modifier.weight(1f))
                InfoColumn(title = "Frequency", value = "As Presented", modifier = Modifier.weight(1f), alignment = Alignment.End)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Expires in 60 sec", style = MaterialTheme.typography.bodyMedium, color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun AccountDetailsCard(
    name: String?, 
    accountNumber: String?, 
    ifsc: String?, 
    upiVpa: String?,
    onEdit: () -> Unit = {},
    onPayViaCred: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name ?: "N/A", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // --- EDIT BUTTON ---
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(ButtonGradientStart, ButtonGradientEnd),
                                start = Offset(0f, Float.POSITIVE_INFINITY),
                                end = Offset(Float.POSITIVE_INFINITY, 0f)
                            )
                        )
                ) {
                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text("EDIT", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoColumn(title = "Account", value = accountNumber ?: "N/A", modifier = Modifier.weight(1f))
                InfoColumn(title = "IFSC Code", value = ifsc ?: "N/A", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            InfoColumn(title = "VPA", value = upiVpa ?: "N/A")
            Spacer(modifier = Modifier.height(24.dp))

            // --- PAY VIA CRED BUTTON ---
            Button(
                onClick = onPayViaCred,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(ButtonGradientStart, ButtonGradientEnd),
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Pay via. CRED", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PayViaOtherAppCard(onPayViaOtherApp: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPayViaOtherApp() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LightGrayBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Wallet, contentDescription = "Wallet", tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Pay via Other App", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Arrow", tint = Color.Gray)
        }
    }
}

@Composable
fun TrustedAndSecurePayments() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Shield, contentDescription = "Security", tint = BlueText, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("100% Trusted & Secure Payments", style = MaterialTheme.typography.bodySmall, color = BlueText)
    }
}

@Composable
fun InfoColumn(title: String, value: String, modifier: Modifier = Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = BlueText, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.SemiBold)
    }
}


@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DetailsScreenPreview() {
    UpiautopaysdkTheme {
        DetailsScreen(
            name = "Vidhi Aggarwal",
            accountNumber = "9212300030002617",
            ifsc = "ICIC0009211",
            upiVpa = "vidhiaggarwal@yescred",
            txnId = "98789723754642342764723423",
            amount = "101.11",
            onBack = {},
            onEdit = {},
            onPayViaCred = {}
        )
    }
}

