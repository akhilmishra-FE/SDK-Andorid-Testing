package com.dec.andorid_autopay_demo_lib

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dec.andorid_autopay_demo_lib.ui.theme.ButtonGradientEnd
import com.dec.andorid_autopay_demo_lib.ui.theme.ButtonGradientStart
import com.dec.andorid_autopay_demo_lib.ui.theme.UpiautopaysdkTheme
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.gson.annotations.SerializedName

// Data classes for account API
data class AccountRequest(
    @SerializedName("reference_id") val referenceId: String,
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("is_consent_granted") val isConsentGranted: Boolean,
    @SerializedName("fetch_branch_details") val fetchBranchDetails: Boolean,
    @SerializedName("consumer_urn") val consumerUrn: String
)

data class AccountResponse(
    @SerializedName("decentro_txn_id") val decentroTxnId: String,
    @SerializedName("api_status") val status: String,
    val message: String,
    val data: AccountData?,
    @SerializedName("response_key") val responseKey: String
)

data class AccountData(
    @SerializedName("name_as_per_bank") val name: String,
    @SerializedName("account_number") val accountNumber: String,
    val ifsc: String,
    @SerializedName("upi_vpa") val upiVpa: String,
    @SerializedName("payout_amount") val payoutAmount: String?
)

class LoginActivity : ComponentActivity() {
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Get pre-filled mobile number if provided
        val preFillMobile = intent.getStringExtra("MOBILE_NUMBER") ?: ""
        val merchantPackage = intent.getStringExtra("MERCHANT_PACKAGE")
        
        setContent {
            UpiautopaysdkTheme {
                LoginScreen(
                    onBack = { finish() },
                    initialMobileNumber = preFillMobile,
                    merchantPackage = merchantPackage
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("LoginActivity", "=== onResume() called ===")
   
        
        // Check for pending mandate status when user returns to main SDK
        checkForPendingMandateStatus()
    }
    
    // Check if there's a pending mandate to check status for
    private fun checkForPendingMandateStatus() {
        Log.d("LoginActivity", "=== Checking for pending mandate status ===")
       
        
        val prefs = getSharedPreferences("UPI_MANDATE_PREFS", MODE_PRIVATE)
        val pendingMandateId = prefs.getString("PENDING_MANDATE_ID", null)
        val timestamp = prefs.getLong("MANDATE_TIMESTAMP", 0)
        
        Log.d("LoginActivity", "Pending Mandate ID: $pendingMandateId")
        Log.d("LoginActivity", "Timestamp: $timestamp")
        
        if (pendingMandateId != null) {
            // Check if mandate was created within last 10 minutes
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - timestamp
            val tenMinutesInMillis = 10 * 60 * 1000
            
            Log.d("LoginActivity", "Current time: $currentTime")
            Log.d("LoginActivity", "Time difference: ${timeDiff / 1000} seconds")
            
            if (timeDiff < tenMinutesInMillis) {
                Log.d("LoginActivity", "✅ Found pending mandate, launching status check: $pendingMandateId")
                
                
                // Clear the stored mandate ID
                prefs.edit().remove("PENDING_MANDATE_ID").remove("MANDATE_TIMESTAMP").apply()
                
                // Launch status activity and wait for result
                val statusIntent = Intent(this, MandateStatusActivity::class.java).apply {
                    putExtra("MANDATE_ID", pendingMandateId)
                    intent.getStringExtra("MERCHANT_PACKAGE")?.let {
                        putExtra("MERCHANT_PACKAGE", it)
                    }
                }
                startActivity(statusIntent)
            } else {
                Log.d("LoginActivity", "❌ Pending mandate too old (${timeDiff / 1000}s), ignoring")
                        prefs.edit().remove("PENDING_MANDATE_ID").remove("MANDATE_TIMESTAMP").apply()
            }
        }
       
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier, 
    onBack: () -> Unit,
    initialMobileNumber: String = "",
    merchantPackage: String? = null
) {
    var mobileNumber by remember { mutableStateOf(initialMobileNumber) }
    var isLoading by remember { mutableStateOf(false) }
    var consentChecked by remember { mutableStateOf(false) }
    var animate by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isButtonEnabled = !isLoading && consentChecked && mobileNumber.length == 10

    LaunchedEffect(Unit) {
        animate = true
        focusManager.clearFocus()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Merchants Logo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                AnimatedVisibility(
                    visible = animate,
                    enter = fadeIn(animationSpec = tween(500))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Enter mobile number linked with your bank",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { if (it.length <= 10) mobileNumber = it },
                            label = { Text("Mobile Number") },
                            placeholder = { Text("10-digit mobile number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                AnimatedVisibility(
                    visible = animate,
                    enter = fadeIn(animationSpec = tween(500, 200))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = consentChecked,
                            onCheckedChange = { consentChecked = it },
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I consent to fetch my bank account details and UPI VPA linked to this mobile number for verification.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                AnimatedVisibility(
                    visible = animate,
                    enter = fadeIn(animationSpec = tween(500, 400))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isButtonEnabled) {
                                        Brush.linearGradient(
                                            colors = listOf(ButtonGradientStart, ButtonGradientEnd),
                                            start = Offset(0f, Float.POSITIVE_INFINITY),
                                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                                        )
                                    } else {
                                        SolidColor(Color.LightGray)
                                    }
                                )
                        ) {
                            Button(
                                onClick = {
                                    isLoading = true
                                    scope.launch {
                                        val request = AccountRequest(
                                            referenceId = UUID.randomUUID().toString(),
                                            mobileNumber = mobileNumber,
                                            isConsentGranted = consentChecked,
                                            fetchBranchDetails = true,
                                            consumerUrn = "7B6DDA82DACA42C5BFF82970802B0DB6"
                                        )
                                        try {
                                            // Generate dummy data directly based on mobile number
                                            val dummyData = when {
                                                mobileNumber.endsWith("1") -> mapOf(
                                                    "name" to "John Doe",
                                                    "account" to "1234567890123456",
                                                    "ifsc" to "HDFC0001234",
                                                    "upi" to "john.doe@paytm",
                                                    "amount" to "1000.00"
                                                )
                                                mobileNumber.endsWith("2") -> mapOf(
                                                    "name" to "Jane Smith",
                                                    "account" to "9876543210987654",
                                                    "ifsc" to "ICIC0005678",
                                                    "upi" to "jane.smith@gpay",
                                                    "amount" to "2500.50"
                                                )
                                                mobileNumber.endsWith("3") -> mapOf(
                                                    "name" to "Raj Patel",
                                                    "account" to "5555666677778888",
                                                    "ifsc" to "SBIN0009999",
                                                    "upi" to "raj.patel@phonepe",
                                                    "amount" to "750.25"
                                                )
                                                else -> mapOf(
                                                    "name" to "Demo User",
                                                    "account" to "1111222233334444",
                                                    "ifsc" to "AXIS0001111",
                                                    "upi" to "demo.user@upi",
                                                    "amount" to "500.00"
                                                )
                                            }
                                            
                                            val intent = Intent(context, DetailsActivity::class.java).apply {
                                                putExtra("NAME", dummyData["name"])
                                                putExtra("ACCOUNT_NUMBER", dummyData["account"])
                                                putExtra("IFSC", dummyData["ifsc"])
                                                putExtra("UPI_VPA", dummyData["upi"])
                                                putExtra("TXN_ID", "DEMO_TXN_${UUID.randomUUID().toString().take(8).uppercase()}")
                                                putExtra("AMOUNT", dummyData["amount"])
                                                // Pass merchant package for return navigation
                                                merchantPackage?.let { putExtra("MERCHANT_PACKAGE", it) }
                                            }
                                            context.startActivity(intent)
                                       
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                modifier = Modifier.fillMaxSize(),
                                enabled = isButtonEnabled
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        "Fetch Details",
                                        fontSize = 18.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    UpiautopaysdkTheme {
        LoginScreen(onBack = {})
    }
}

