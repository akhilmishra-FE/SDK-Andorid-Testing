package com.dec.andorid_autopay_demo_lib

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dec.andorid_autopay_demo_lib.ui.theme.ButtonGradientEnd
import com.dec.andorid_autopay_demo_lib.ui.theme.ButtonGradientStart
import com.dec.andorid_autopay_demo_lib.ui.theme.UpiautopaysdkTheme

class EditAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val name = intent.getStringExtra("NAME") ?: ""
        val accountNumber = intent.getStringExtra("ACCOUNT_NUMBER") ?: ""
        val ifsc = intent.getStringExtra("IFSC") ?: ""
        val upiVpa = intent.getStringExtra("UPI_VPA") ?: ""
        val txnId = intent.getStringExtra("TXN_ID") ?: ""
        val amount = intent.getStringExtra("AMOUNT") ?: ""

        setContent {
            UpiautopaysdkTheme {
                EditAccountScreen(
                    initialName = name,
                    initialAccountNumber = accountNumber,
                    initialIfsc = ifsc,
                    initialUpiVpa = upiVpa,
                    txnId = txnId,
                    amount = amount,
                    onBack = { finish() },
                    onSave = { updatedName, updatedAccount, updatedIfsc, updatedVpa ->
                        val resultIntent = Intent().apply {
                            putExtra("NAME", updatedName)
                            putExtra("ACCOUNT_NUMBER", updatedAccount)
                            putExtra("IFSC", updatedIfsc)
                            putExtra("UPI_VPA", updatedVpa)
                            putExtra("TXN_ID", txnId)
                            putExtra("AMOUNT", amount)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    initialName: String,
    initialAccountNumber: String,
    initialIfsc: String,
    initialUpiVpa: String,
    txnId: String,
    amount: String,
    onBack: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var accountNumber by remember { mutableStateOf(initialAccountNumber) }
    var ifsc by remember { mutableStateOf(initialIfsc) }
    var upiVpa by remember { mutableStateOf(initialUpiVpa) }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground),
        topBar = {
            TopAppBar(
                title = { Text("Merchant Logo", fontWeight = FontWeight.SemiBold) },
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
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Edit Account Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Account Number Field
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text("Account Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // IFSC Code Field
            OutlinedTextField(
                value = ifsc,
                onValueChange = { ifsc = it.uppercase() },
                label = { Text("IFSC Code") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // VPA Field
            OutlinedTextField(
                value = upiVpa,
                onValueChange = { upiVpa = it },
                label = { Text("VPA") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("username@bank", color = Color.Gray) },
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Confirm & Save Button
            Button(
                onClick = { 
                    onSave(name, accountNumber, ifsc, upiVpa)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(ButtonGradientStart, ButtonGradientEnd),
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f)
                        )
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                enabled = accountNumber.isNotBlank() && ifsc.isNotBlank() && upiVpa.isNotBlank()
            ) {
                Text(
                    "Confirm & Save", 
                    fontSize = 18.sp, 
                    color = Color.White, 
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditAccountScreenPreview() {
    UpiautopaysdkTheme {
        EditAccountScreen(
            initialName = "Vidhi Aggarwal",
            initialAccountNumber = "9212300030002617",
            initialIfsc = "ICIC0009211",
            initialUpiVpa = "vidhiaggarwal@yescred",
            txnId = "98789723754642342764723423",
            amount = "101.11",
            onBack = {},
            onSave = { _, _, _, _ -> }
        )
    }
}
