package com.dec.andorid_autopay_demo_lib

import android.app.Activity
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
                        
                        // This callback will be triggered from the LaunchedEffect
                        // The actual result handling is done there
                    }
                )
            }
        }
    }
    
    /**
     * Returns success result to the original calling app (not just DetailsActivity)
     */
    fun returnSuccessResult(response: MandateStatusResponse?) {
        Log.d(TAG, "✅ === RETURNING SUCCESS RESULT TO MAIN APP ===")
        Log.d(TAG, "✅ Mandate Status: ${response?.mandate_status}")
        Log.d(TAG, "✅ Message: ${response?.message}")
        
        val resultIntent = Intent().apply {
            putExtra("MANDATE_STATUS", response?.mandate_status ?: "SUCCESS")
            putExtra("DECENTRO_MANDATE_ID", response?.decentro_mandate_id ?: "")
            putExtra("MESSAGE", response?.message ?: "Payment completed successfully")
            putExtra("TIMESTAMP", System.currentTimeMillis())
        }
        
        // Set result and finish this activity
        setResult(RESULT_OK, resultIntent)
        Log.d(TAG, "✅ Result set to RESULT_OK")
        
        // Close all SDK activities and return to main app
        finishAffinity()
        Log.d(TAG, "✅ All SDK activities closed - returned to main app")
    }
    
    /**
     * Returns error result to the original calling app (not just DetailsActivity)
     */
    fun returnErrorResult(response: MandateStatusResponse?) {
        Log.d(TAG, "❌ === RETURNING ERROR RESULT TO MAIN APP ===")
        Log.d(TAG, "❌ Mandate Status: ${response?.mandate_status}")
        Log.d(TAG, "❌ Message: ${response?.message}")
        
        val resultIntent = Intent().apply {
            putExtra("MANDATE_STATUS", response?.mandate_status ?: "FAILED")
            putExtra("DECENTRO_MANDATE_ID", response?.decentro_mandate_id ?: "")
            putExtra("ERROR_MESSAGE", response?.message ?: "Payment failed")
            putExtra("TIMESTAMP", System.currentTimeMillis())
        }
        
        // Set result and finish this activity
        setResult(RESULT_CANCELED, resultIntent)
        Log.d(TAG, "❌ Result set to RESULT_CANCELED")
        
        // Close all SDK activities and return to main app
        finishAffinity()
        Log.d(TAG, "❌ All SDK activities closed - returned to main app")
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
    val activity = context as MandateStatusActivity
    val mandateService = remember { MandateStatusService(context) }
    
    LaunchedEffect(mandateId) {
        Log.d("MandateStatusScreen", "Starting status check for mandate: $mandateId")
        Log.d("MandateStatusScreen", "🚀 Calling mandateService.checkMandateStatus()...")
        
        val startTime = System.currentTimeMillis()
        val response = mandateService.checkMandateStatus(mandateId)
        val endTime = System.currentTimeMillis()
        
        Log.d("MandateStatusScreen", "API call completed in ${endTime - startTime}ms")
        statusResponse = response
        Log.d("MandateStatusScreen", "Status: ${response.mandate_status}")
        
        currentStatus = when(response.mandate_status.uppercase()) {
            "SUCCESS", "COMPLETED" -> MandateStatus.SUCCESS
            "FAILED", "FAILURE", "ERROR" -> MandateStatus.FAILED
            "PENDING", "INITIATED" -> MandateStatus.PENDING
            else -> MandateStatus.PROCESSING
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
        
        // IMPORTANT: Auto-redirect to merchant app ONLY for SUCCESS and FAILED statuses
        // PENDING/PROCESSING statuses will keep SDK open for user interaction
        Log.d("MandateStatusScreen", "🎯 ========================================")
        Log.d("MandateStatusScreen", "🎯 === FINAL STATUS REACHED ===")
        Log.d("MandateStatusScreen", "🎯 ========================================")
        Log.d("MandateStatusScreen", "🎯 Final Status: $currentStatus")
        Log.d("MandateStatusScreen", "🎯 Status Response: $statusResponse")
        Log.d("MandateStatusScreen", "🎯 Timer Start: ${System.currentTimeMillis()}")
        
        Log.d("MandateStatusScreen", "📊 === STATUS CHECK COMPLETED ===")
        Log.d("MandateStatusScreen", "📊 Final Status: $currentStatus")
        
        // Only redirect for SUCCESS and FAILED statuses
        if (currentStatus == MandateStatus.SUCCESS || currentStatus == MandateStatus.FAILED) {
            Log.d("MandateStatusScreen", "✅ === FINAL STATUS - REDIRECT TO MERCHANT APP ===")
            Log.d("MandateStatusScreen", "✅ Status: $currentStatus")
            Log.d("MandateStatusScreen", "✅ Will display status screen for exactly 5 seconds")
            Log.d("MandateStatusScreen", "✅ Then redirect to merchant app")
            
            // Show status for 5 seconds, then redirect to client app
            Log.d("MandateStatusScreen", "⏳ === STARTING 5-SECOND DISPLAY ===")
            Log.d("MandateStatusScreen", "⏳ User will see status screen for 5 seconds...")
            Log.d("MandateStatusScreen", "⏳ Display start time: ${System.currentTimeMillis()}")
            
            delay(5000) // Show status for exactly 5 seconds
            
            Log.d("MandateStatusScreen", "✅ === 5-SECOND DISPLAY COMPLETED ===")
            Log.d("MandateStatusScreen", "✅ Display end time: ${System.currentTimeMillis()}")
            Log.d("MandateStatusScreen", "✅ Now returning result to calling app...")
            
            // PROPER ANDROID WAY: Return result to calling app
            Log.d("MandateStatusScreen", "🏁 === RETURNING RESULT TO CALLING APP ===")
            Log.d("MandateStatusScreen", "🏁 Status: $currentStatus")
            Log.d("MandateStatusScreen", "🏁 Response: $statusResponse")
            
            if (currentStatus == MandateStatus.SUCCESS) {
                activity.returnSuccessResult(statusResponse)
            } else {
                activity.returnErrorResult(statusResponse)
            }
        } else {
            Log.d("MandateStatusScreen", "⏳ === INTERMEDIATE STATUS - STAY IN SDK ===")
            Log.d("MandateStatusScreen", "⏳ Status: $currentStatus")
            Log.d("MandateStatusScreen", "⏳ SDK will stay open - no redirect")
            Log.d("MandateStatusScreen", "⏳ User can manually close or wait for status updates")
            
            // For PENDING/PROCESSING - SDK stays open, no redirect
            // User can manually close or the status will update
        }
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
