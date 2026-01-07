package com.example.upi_autopay_sdk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dec.andorid_autopay_demo_lib.UPIAutoPaySDK

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoAppScreen()
        }
    }
}

@Composable
fun DemoAppScreen() {
    val context = LocalContext.current
        Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
                        Text(
                text = "UPI AutoPay SDK Demo",
                style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                    UPIAutoPaySDK.launchLoginScreen(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Launch SDK Login Screen")
            }
        }
    }
}
