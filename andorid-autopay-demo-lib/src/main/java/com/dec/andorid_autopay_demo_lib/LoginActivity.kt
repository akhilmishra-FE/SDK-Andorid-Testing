package com.dec.andorid_autopay_demo_lib

import android.content.Intent
import android.os.Bundle
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

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UpiautopaysdkTheme {
                LoginScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var mobileNumber by remember { mutableStateOf("") }
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
                                            }
                                            context.startActivity(intent)
                                            Toast.makeText(context, "Demo Mode: Account details fetched successfully", Toast.LENGTH_SHORT).show()
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

