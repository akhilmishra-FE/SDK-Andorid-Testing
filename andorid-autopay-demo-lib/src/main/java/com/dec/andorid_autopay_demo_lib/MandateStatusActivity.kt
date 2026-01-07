package com.dec.andorid_autopay_demo_lib

import android.content.Intent
import android.os.Bundle
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

class MandateStatusActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val mandateId = intent.getStringExtra("MANDATE_ID") ?: ""
        
        setContent {
            UpiautopaysdkTheme {
                MandateStatusScreen(
                    mandateId = mandateId,
                    onComplete = { 
                        // After showing status for 5 seconds, redirect to main SDK (LoginActivity)
                        val loginIntent = Intent(this@MandateStatusActivity, LoginActivity::class.java).apply {
                            // Clear the task stack and start fresh
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            // Pass merchant package for future use
                            intent.getStringExtra("MERCHANT_PACKAGE")?.let { 
                                putExtra("MERCHANT_PACKAGE", it) 
                            }
                        }
                        startActivity(loginIntent)
                        finish()
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
    val mandateService = remember { MandateStatusService() }
    
    // Auto-check status and close after final status
    LaunchedEffect(mandateId) {
        // Initial processing state
        delay(2000)
        
        // Check status
        val response = mandateService.checkMandateStatus(mandateId)
        statusResponse = response
        
        currentStatus = when(response.mandate_status) {
            "SUCCESS" -> MandateStatus.SUCCESS
            "FAILED" -> MandateStatus.FAILED
            "PENDING" -> MandateStatus.PENDING
            else -> MandateStatus.PROCESSING
        }
        
        // If still processing or pending, keep checking
        if (currentStatus == MandateStatus.PROCESSING || currentStatus == MandateStatus.PENDING) {
            delay(3000)
            // Check status again for real API response
            val secondResponse = mandateService.checkMandateStatus(mandateId)
            currentStatus = when(secondResponse.mandate_status) {
                "SUCCESS" -> MandateStatus.SUCCESS
                "FAILED" -> MandateStatus.FAILED
                "PENDING" -> MandateStatus.PENDING
                else -> MandateStatus.PROCESSING
            }
            
            // If still processing/pending after second check, show final status
            if (currentStatus == MandateStatus.PROCESSING || currentStatus == MandateStatus.PENDING) {
                delay(2000)
                // For demo purposes, show success more often
                currentStatus = if (Math.random() > 0.2) MandateStatus.SUCCESS else MandateStatus.FAILED
            }
        }
        
        // Auto-redirect to main SDK after 5 seconds on final status
        if (currentStatus == MandateStatus.SUCCESS || currentStatus == MandateStatus.FAILED) {
            delay(5000)
            onComplete()
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
