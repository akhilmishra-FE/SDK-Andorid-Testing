package com.dec.andorid_autopay_demo_lib

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dec.andorid_autopay_demo_lib.ui.theme.UpiautopaysdkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MandateStatusActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MandateStatusActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mandateId = intent.getStringExtra("MANDATE_ID") ?: ""
        val merchantPackage = intent.getStringExtra("MERCHANT_PACKAGE")
        
        Log.d(TAG, "🎬 ========================================")
        Log.d(TAG, "🎬 === MANDATE STATUS ACTIVITY CREATED ===")
        Log.d(TAG, "🎬 ========================================")
        Log.d(TAG, "🎬 Activity Details:")
        Log.d(TAG, "   📋 Mandate ID: $mandateId")
        Log.d(TAG, "   📦 Merchant Package: $merchantPackage")
        Log.d(TAG, "   🕐 Creation Time: ${System.currentTimeMillis()}")
        Log.d(TAG, "   📱 Thread: ${Thread.currentThread().name}")
        
        // Debug: Check if merchant package is valid
        if (merchantPackage != null) {
            val merchantIntent = packageManager.getLaunchIntentForPackage(merchantPackage)
            Log.d(TAG, "🔍 Merchant package validation:")
            Log.d(TAG, "   📦 Package: $merchantPackage")
            Log.d(TAG, "   ✅ Valid: ${merchantIntent != null}")
            if (merchantIntent != null) {
                Log.d(TAG, "   🎯 Target Activity: ${merchantIntent.component?.className}")
            }
        } else {
            Log.w(TAG, "⚠️ No merchant package provided - will use fallback redirect")
        }
        
        setContent {
            UpiautopaysdkTheme {
                MandateStatusScreen(
                    mandateId = mandateId,
                    onComplete = { 
                        // After showing status for 5 seconds, redirect to merchant app
                        Log.d(TAG, "🎯 ========================================")
                        Log.d(TAG, "🎯 === COMPLETION CALLBACK TRIGGERED ===")
                        Log.d(TAG, "🎯 ========================================")
                        Log.d(TAG, "🎯 Completion Details:")
                        Log.d(TAG, "   📦 Merchant Package: $merchantPackage")
                        Log.d(TAG, "   🕐 Completion Time: ${System.currentTimeMillis()}")
                        
                        // Try multiple redirect approaches
                        var redirectSuccessful = false
                        
                        // Approach 1: Launch specific merchant app
                        if (merchantPackage != null && !redirectSuccessful) {
                            try {
                                Log.d(TAG, "🚀 === APPROACH 1: LAUNCHING MERCHANT APP ===")
                                Log.d(TAG, "🚀 Attempting to launch: $merchantPackage")
                                
                                val merchantIntent = packageManager.getLaunchIntentForPackage(merchantPackage)
                                if (merchantIntent != null) {
                                    merchantIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    Log.d(TAG, "🚀 Merchant intent created successfully")
                                    Log.d(TAG, "🚀 Intent flags: NEW_TASK | CLEAR_TOP")
                                    startActivity(merchantIntent)
                                    Log.d(TAG, "✅ Merchant app launched successfully")
                                    redirectSuccessful = true
                                } else {
                                    Log.e(TAG, "❌ Merchant app not found in package manager")
                                    Log.e(TAG, "❌ Package: $merchantPackage")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "💥 Error launching merchant app: ${e.message}")
                            }
                        }
                        
                        // Approach 2: Try to go back to previous app using task stack
                        if (!redirectSuccessful) {
                            try {
                                Log.d(TAG, "🔄 === APPROACH 2: USING TASK STACK ===")
                                moveTaskToBack(true)
                                Log.d(TAG, "✅ Moved task to back - should show previous app")
                                redirectSuccessful = true
                            } catch (e: Exception) {
                                Log.e(TAG, "💥 Error moving task to back: ${e.message}")
                            }
                        }
                        
                        // Approach 3: Launch home screen as final fallback
                        if (!redirectSuccessful) {
                            try {
                                Log.d(TAG, "🏠 === APPROACH 3: LAUNCHING HOME SCREEN ===")
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(homeIntent)
                                Log.d(TAG, "✅ Home screen launched successfully")
                                redirectSuccessful = true
                            } catch (e: Exception) {
                                Log.e(TAG, "💥 Error launching home screen: ${e.message}")
                            }
                        }
                        
                        Log.d(TAG, "📊 === REDIRECT SUMMARY ===")
                        Log.d(TAG, "📊 Merchant Package: $merchantPackage")
                        Log.d(TAG, "📊 Redirect Successful: $redirectSuccessful")
                        Log.d(TAG, "📊 Final Status: ${if (redirectSuccessful) "SUCCESS" else "FAILED"}")
                        
                        // GUARANTEED SDK CLOSURE: Always close SDK regardless of merchant app launch result
                        Log.d(TAG, "🏁 === CLOSING SDK (GUARANTEED) ===")
                        Log.d(TAG, "🏁 Calling finishAffinity() to close all SDK activities")
                        Log.d(TAG, "🏁 Final timestamp: ${System.currentTimeMillis()}")
                        
                        try {
                            finishAffinity()
                            Log.d(TAG, "✅ SDK closed successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "💥 Error closing SDK: ${e.message}")
                            // Force close with finish() as fallback
                            try {
                                finish()
                                Log.d(TAG, "✅ SDK closed with fallback finish()")
                            } catch (e2: Exception) {
                                Log.e(TAG, "💥 Critical: Cannot close SDK: ${e2.message}")
                            }
                        }
                        
                        Log.d(TAG, "🏁 === SDK CLOSURE COMPLETED ===")
                    }
                )
            }
        }
    }
}

@Composable
fun MandateStatusScreen(
    mandateId: String,
    onComplete: () -> Unit
) {
    var currentStatus by remember { mutableStateOf(MandateStatus.PROCESSING) }
    var statusResponse by remember { mutableStateOf<MandateStatusResponse?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val mandateService = remember { MandateStatusService(context) }
    
    // Auto-check status and close after final status
    // GUARANTEED TIMEOUT: Always redirect within 30 seconds maximum
    LaunchedEffect(mandateId) {
        // REMOVED: Auto-close timeout - app will NOT close automatically
        // User must see the status screen and it will only close after 5-second display
        // No timeout job - let the normal flow complete naturally
        Log.d("MandateStatusScreen", "🔄 ========================================")
        Log.d("MandateStatusScreen", "🔄 === STATUS CHECK LAUNCHED EFFECT ===")
        Log.d("MandateStatusScreen", "🔄 ========================================")
        Log.d("MandateStatusScreen", "🔄 LaunchedEffect Details:")
        Log.d("MandateStatusScreen", "   📋 Mandate ID: $mandateId")
        Log.d("MandateStatusScreen", "   🕐 Start Time: ${System.currentTimeMillis()}")
        
        // Show processing immediately, then check status IMMEDIATELY
        Log.d("MandateStatusScreen", "⏳ === STARTING STATUS CHECK FLOW ===")
        Log.d("MandateStatusScreen", "⏳ NO DELAY - Starting API call immediately")
        Log.d("MandateStatusScreen", "⏳ Start time: ${System.currentTimeMillis()}")
        // NO DELAY - Start API call immediately when user returns
        
        // First API call - immediate status check
        Log.d("MandateStatusScreen", "🚀 ========================================")
        Log.d("MandateStatusScreen", "🚀 === FIRST API CALL STARTING ===")
        Log.d("MandateStatusScreen", "🚀 ========================================")
        Log.d("MandateStatusScreen", "🚀 Mandate ID: $mandateId")
        Log.d("MandateStatusScreen", "🚀 Call time: ${System.currentTimeMillis()}")
        Log.d("MandateStatusScreen", "🚀 Calling mandateService.checkMandateStatus()...")
        
        val startTime = System.currentTimeMillis()
        val response = mandateService.checkMandateStatus(mandateId)
        val endTime = System.currentTimeMillis()
        
        Log.d("MandateStatusScreen", "📡 ========================================")
        Log.d("MandateStatusScreen", "📡 === FIRST API CALL COMPLETED ===")
        Log.d("MandateStatusScreen", "📡 ========================================")
        Log.d("MandateStatusScreen", "📡 API call duration: ${endTime - startTime}ms")
        statusResponse = response
        
        Log.d("MandateStatusScreen", "📡 === FIRST API RESPONSE ===")
        Log.d("MandateStatusScreen", "📡 Response: $response")
        Log.d("MandateStatusScreen", "📡 Status: ${response.mandate_status}")
        
        currentStatus = when(response.mandate_status.uppercase()) {
            "SUCCESS", "COMPLETED" -> {
                Log.d("MandateStatusScreen", "✅ Status mapped to SUCCESS")
                MandateStatus.SUCCESS
            }
            "FAILED", "FAILURE", "ERROR" -> {
                Log.d("MandateStatusScreen", "❌ Status mapped to FAILED")
                MandateStatus.FAILED
            }
            "PENDING", "INITIATED" -> {
                Log.d("MandateStatusScreen", "⏳ Status mapped to PENDING")
                MandateStatus.PENDING
            }
            else -> {
                Log.d("MandateStatusScreen", "🔄 Status mapped to PROCESSING (default)")
                MandateStatus.PROCESSING
            }
        }
        
        Log.d("MandateStatusScreen", "📊 Current Status after first check: $currentStatus")
        
        // If still processing or pending, do a second check after shorter delay
        if (currentStatus == MandateStatus.PROCESSING || currentStatus == MandateStatus.PENDING) {
            Log.d("MandateStatusScreen", "🔄 === SECOND CHECK NEEDED ===")
            Log.d("MandateStatusScreen", "🔄 Current status requires retry: $currentStatus")
            Log.d("MandateStatusScreen", "⏳ No delay - checking immediately...")
            delay(500) // IMMEDIATE: Minimal delay for status update
            
            // Second API call for updated status
            Log.d("MandateStatusScreen", "🚀 === SECOND API CALL ===")
            Log.d("MandateStatusScreen", "🚀 Calling mandateService.checkMandateStatus($mandateId)")
            val secondResponse = mandateService.checkMandateStatus(mandateId)
            statusResponse = secondResponse
            
            Log.d("MandateStatusScreen", "📡 === SECOND API RESPONSE ===")
            Log.d("MandateStatusScreen", "📡 Response: $secondResponse")
            Log.d("MandateStatusScreen", "📡 Status: ${secondResponse.mandate_status}")
            
            currentStatus = when(secondResponse.mandate_status.uppercase()) {
                "SUCCESS", "COMPLETED" -> {
                    Log.d("MandateStatusScreen", "✅ Second check: Status mapped to SUCCESS")
                    MandateStatus.SUCCESS
                }
                "FAILED", "FAILURE", "ERROR" -> {
                    Log.d("MandateStatusScreen", "❌ Second check: Status mapped to FAILED")
                    MandateStatus.FAILED
                }
                "PENDING", "INITIATED" -> {
                    Log.d("MandateStatusScreen", "⏳ Second check: Status mapped to PENDING")
                    MandateStatus.PENDING
                }
                else -> {
                    Log.d("MandateStatusScreen", "🔄 Second check: Status mapped to PROCESSING")
                    MandateStatus.PROCESSING
                }
            }
            
            Log.d("MandateStatusScreen", "📊 Current Status after second check: $currentStatus")
            
            // If still processing/pending after second check, do one final check
            if (currentStatus == MandateStatus.PROCESSING || currentStatus == MandateStatus.PENDING) {
                Log.d("MandateStatusScreen", "🔄 === FINAL CHECK NEEDED ===")
                Log.d("MandateStatusScreen", "🔄 Still not final status: $currentStatus")
                Log.d("MandateStatusScreen", "⏳ No delay - final check immediately...")
                delay(500) // IMMEDIATE: Minimal delay for final check
                
                Log.d("MandateStatusScreen", "🚀 === FINAL API CALL ===")
                Log.d("MandateStatusScreen", "🚀 Calling mandateService.checkMandateStatus($mandateId)")
                val finalResponse = mandateService.checkMandateStatus(mandateId)
                
                Log.d("MandateStatusScreen", "📡 === FINAL API RESPONSE ===")
                Log.d("MandateStatusScreen", "📡 Response: $finalResponse")
                Log.d("MandateStatusScreen", "📡 Status: ${finalResponse.mandate_status}")
                
                currentStatus = when(finalResponse.mandate_status.uppercase()) {
                    "SUCCESS", "COMPLETED" -> {
                        Log.d("MandateStatusScreen", "✅ Final check: Status mapped to SUCCESS")
                        MandateStatus.SUCCESS
                    }
                    "FAILED", "FAILURE", "ERROR" -> {
                        Log.d("MandateStatusScreen", "❌ Final check: Status mapped to FAILED")
                        MandateStatus.FAILED
                    }
                    else -> {
                        Log.e("MandateStatusScreen", "❌ Final check: Unknown status - showing FAILED")
                        MandateStatus.FAILED // Show failed for unknown/error states
                    }
                }
                
                Log.d("MandateStatusScreen", "📊 Final Status: $currentStatus")
            }
        }
        
        // Auto-redirect to merchant app after 5 seconds on ANY final status
        // Always ensure we redirect to merchant app regardless of status
        Log.d("MandateStatusScreen", "🎯 ========================================")
        Log.d("MandateStatusScreen", "🎯 === FINAL STATUS REACHED ===")
        Log.d("MandateStatusScreen", "🎯 ========================================")
        Log.d("MandateStatusScreen", "🎯 Final Status: $currentStatus")
        Log.d("MandateStatusScreen", "🎯 Status Response: $statusResponse")
        Log.d("MandateStatusScreen", "🎯 Timer Start: ${System.currentTimeMillis()}")
        
        if (currentStatus == MandateStatus.SUCCESS || currentStatus == MandateStatus.FAILED) {
            Log.d("MandateStatusScreen", "✅ Normal completion - SUCCESS or FAILED status")
            Log.d("MandateStatusScreen", "✅ Will display status screen for exactly 5 seconds")
        } else {
            Log.w("MandateStatusScreen", "⚠️ Unexpected status: $currentStatus - but still redirecting to merchant app")
        }
        
        Log.d("MandateStatusScreen", "📊 === STATUS CHECK COMPLETED ===")
        Log.d("MandateStatusScreen", "📊 Final Status: $currentStatus")
        Log.d("MandateStatusScreen", "📊 Starting 5-second display timer...")
        
        // Show status for 5 seconds, then redirect to client app
        Log.d("MandateStatusScreen", "⏳ === STARTING 5-SECOND DISPLAY ===")
        Log.d("MandateStatusScreen", "⏳ User will see status screen for 5 seconds...")
        Log.d("MandateStatusScreen", "⏳ Display start time: ${System.currentTimeMillis()}")
        
        delay(5000) // Show status for exactly 5 seconds
        
        Log.d("MandateStatusScreen", "✅ === 5-SECOND DISPLAY COMPLETED ===")
        Log.d("MandateStatusScreen", "✅ Display end time: ${System.currentTimeMillis()}")
        Log.d("MandateStatusScreen", "✅ Now redirecting to client app...")
        
        // Redirect to client app
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground),
        contentAlignment = Alignment.Center
    ) {
        when (currentStatus) {
            MandateStatus.PROCESSING -> ProcessingStatusCard()
            MandateStatus.PENDING -> PendingStatusCard()
            MandateStatus.SUCCESS -> SuccessStatusCard(statusResponse?.message)
            MandateStatus.FAILED -> FailedStatusCard(statusResponse?.message)
        }
    }
}

@Composable
fun ProcessingStatusCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    StatusCard(
        icon = Icons.Default.HourglassEmpty,
        iconColor = Color(0xFFFF9800),
        title = "Mandate registration in progress...",
        message = "Please wait while we process your mandate registration.",
        backgroundColor = Color(0xFFFFF3E0),
        iconModifier = Modifier.rotate(rotation)
    )
}

@Composable
fun PendingStatusCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "pending")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            repeat(8) { index ->
                val delay = index * 100
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, delayMillis = delay, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$index"
                )
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3).copy(alpha = dotAlpha))
                )
            }
        }
        
        Text(
            text = "Awaiting Confirmation...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black
        )
    }
}

@Composable
fun SuccessStatusCard(message: String?) {
    StatusCard(
        icon = Icons.Default.CheckCircle,
        iconColor = Color(0xFF4CAF50),
        title = "Mandate Registered!",
        message = message ?: "Your mandate has been successfully registered.",
        backgroundColor = Color(0xFFE8F5E8)
    )
}

@Composable
fun FailedStatusCard(message: String?) {
    StatusCard(
        icon = Icons.Default.Error,
        iconColor = Color(0xFFF44336),
        title = "Mandate Registration Failed!",
        message = message ?: "There was an issue registering your mandate. Please try again.",
        backgroundColor = Color(0xFFFFEBEE)
    )
}

@Composable
fun StatusCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    message: String,
    backgroundColor: Color,
    iconModifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = iconModifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                lineHeight = 24.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProcessingStatusPreview() {
    UpiautopaysdkTheme {
        ProcessingStatusCard()
    }
}

@Preview(showBackground = true)
@Composable
fun PendingStatusPreview() {
    UpiautopaysdkTheme {
        PendingStatusCard()
    }
}

@Preview(showBackground = true)
@Composable
fun SuccessStatusPreview() {
    UpiautopaysdkTheme {
        SuccessStatusCard("Mandate created successfully!")
    }
}

@Preview(showBackground = true)
@Composable
fun FailedStatusPreview() {
    UpiautopaysdkTheme {
        FailedStatusCard("Registration failed. Please try again.")
    }
}
