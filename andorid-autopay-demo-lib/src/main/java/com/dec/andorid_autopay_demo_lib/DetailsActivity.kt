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
                    
                  
                    
                    // Store mandate ID for later use when user returns
                    storeMandateIdForStatusCheck(mandateId)
                    
                    // DO NOT start status checking immediately
                    // Wait for user to return to app manually
                    
                } catch (chooserException: Exception) {
                    Log.e("DetailsActivity", "Chooser launch also failed", chooserException)
                    
                   
                    
                    // Try to copy link to clipboard for manual use
                    try {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("UPI Mandate Link", deepLink)
                        clipboard.setPrimaryClip(clip)
                        
                       
                    } catch (clipboardException: Exception) {
                        Log.e("DetailsActivity", "Failed to copy to clipboard", clipboardException)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("DetailsActivity", "Complete failure in UPI launch", e)
            
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
        
       
    }
    
    // Check if there's a pending mandate to check status for
    private fun checkForPendingMandateStatus() {
        Log.d("DetailsActivity", "🔍 === CHECKING FOR PENDING MANDATE STATUS ===")
        
        val prefs = getSharedPreferences("UPI_MANDATE_PREFS", MODE_PRIVATE)
        val pendingMandateId = prefs.getString("PENDING_MANDATE_ID", null)
        val timestamp = prefs.getLong("MANDATE_TIMESTAMP", 0)
        
        Log.d("DetailsActivity", "🔍 SharedPreferences Check:")
        Log.d("DetailsActivity", "   📋 Pending Mandate ID: $pendingMandateId")
        Log.d("DetailsActivity", "   ⏰ Stored Timestamp: $timestamp")
        
        if (pendingMandateId != null) {
            // Check if mandate was created within last 10 minutes (reasonable time window)
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - timestamp
            val tenMinutesInMillis = 10 * 60 * 1000
            
            Log.d("DetailsActivity", "🔍 Time Analysis:")
            Log.d("DetailsActivity", "   🕐 Current time: $currentTime")
            Log.d("DetailsActivity", "   ⏳ Time difference: ${timeDiff / 1000} seconds")
            Log.d("DetailsActivity", "   ⏱️ Max allowed: ${tenMinutesInMillis / 1000} seconds")
            
            if (timeDiff < tenMinutesInMillis) {
                Log.d("DetailsActivity", "✅ === VALID PENDING MANDATE FOUND ===")
                Log.d("DetailsActivity", "✅ Mandate ID: $pendingMandateId")
                Log.d("DetailsActivity", "✅ Age: ${timeDiff / 1000}s (valid)")
                Log.d("DetailsActivity", "✅ Launching MandateStatusActivity...")
                
                // Clear the stored mandate ID
                prefs.edit().remove("PENDING_MANDATE_ID").remove("MANDATE_TIMESTAMP").apply()
                Log.d("DetailsActivity", "🧹 Cleared stored mandate from SharedPreferences")
                
                // Launch status activity
                launchStatusActivity(pendingMandateId)
            } else {
                Log.d("DetailsActivity", "❌ === PENDING MANDATE TOO OLD ===")
                Log.d("DetailsActivity", "❌ Mandate ID: $pendingMandateId")
                Log.d("DetailsActivity", "❌ Age: ${timeDiff / 1000}s (expired)")
                Log.d("DetailsActivity", "❌ Cleaning up expired mandate...")
                prefs.edit().remove("PENDING_MANDATE_ID").remove("MANDATE_TIMESTAMP").apply()
                Log.d("DetailsActivity", "🧹 Removed expired mandate from SharedPreferences")
            }
        } else {
            Log.d("DetailsActivity", "ℹ️ === NO PENDING MANDATE FOUND ===")
            Log.d("DetailsActivity", "ℹ️ SharedPreferences is clean - no pending payment to check")
        }
    }
    
    // Launch status activity and wait for result
    private fun launchStatusActivity(mandateId: String) {
        Log.d("DetailsActivity", "🚀 === LAUNCHING MANDATE STATUS ACTIVITY ===")
        Log.d("DetailsActivity", "🚀 Mandate ID: $mandateId")
        Log.d("DetailsActivity", "🚀 Merchant Package: $merchantPackage")
        Log.d("DetailsActivity", "🚀 Timestamp: ${System.currentTimeMillis()}")
        
        val statusIntent = Intent(this, MandateStatusActivity::class.java).apply {
            putExtra("MANDATE_ID", mandateId)
            // Pass merchant package name if available
            merchantPackage?.let {
                putExtra("MERCHANT_PACKAGE", it)
                Log.d("DetailsActivity", "🚀 Added MERCHANT_PACKAGE to intent: $it")
            }
        }
        
        Log.d("DetailsActivity", "🚀 Starting MandateStatusActivity for result...")
        startActivityForResult(statusIntent, MANDATE_STATUS_REQUEST_CODE)
        Log.d("DetailsActivity", "🚀 MandateStatusActivity launched successfully")
    }
    
    /**
     * CRITICAL FIX: Handle result from MandateStatusActivity
     * When MandateStatusActivity finishes, we also need to finish DetailsActivity
     * and pass the result to the merchant app
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == MANDATE_STATUS_REQUEST_CODE) {
            Log.d("DetailsActivity", "✅ Received result from MandateStatusActivity")
            Log.d("DetailsActivity", "✅ Result Code: $resultCode")
            
            // Pass the result to whoever called us (LoginActivity or merchant app)
            setResult(resultCode, data)
            
            // CRITICAL: Finish this activity so we don't stay on this page
            finish()
            Log.d("DetailsActivity", "✅ DetailsActivity finished - returning to caller")
        }
    }
    
    companion object {
        private const val MANDATE_STATUS_REQUEST_CODE = 1002
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("DetailsActivity", "🔄 ========================================")
        Log.d("DetailsActivity", "🔄 === onResume() CALLED ===")
        Log.d("DetailsActivity", "🔄 ========================================")
        Log.d("DetailsActivity", "🔄 User returned to DetailsActivity")
        Log.d("DetailsActivity", "🔄 Resume Timestamp: ${System.currentTimeMillis()}")
        Log.d("DetailsActivity", "🔄 Thread: ${Thread.currentThread().name}")
        Log.d("DetailsActivity", "🔄 Lifecycle State: RESUMED")
        
        // Immediate check for pending mandate status when user returns
        // Use a small delay to ensure UI is ready, then check immediately
        Log.d("DetailsActivity", "🔄 Scheduling mandate check with 100ms delay")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d("DetailsActivity", "🔄 Handler callback executing - checking mandate status")
            checkForPendingMandateStatus()
        }, 100) // Very small delay for immediate response
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

