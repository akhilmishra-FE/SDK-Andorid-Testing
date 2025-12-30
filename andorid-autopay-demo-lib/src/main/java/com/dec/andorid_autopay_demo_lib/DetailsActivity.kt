package com.dec.andorid_autopay_demo_lib

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("NAME")
        val accountNumber = intent.getStringExtra("ACCOUNT_NUMBER")
        val ifsc = intent.getStringExtra("IFSC")
        val upiVpa = intent.getStringExtra("UPI_VPA")
        val txnId = intent.getStringExtra("TXN_ID")
        val amount = intent.getStringExtra("AMOUNT")

        setContent {
            UpiautopaysdkTheme {
                DetailsScreen(
                    name = name,
                    accountNumber = accountNumber,
                    ifsc = ifsc,
                    upiVpa = upiVpa,
                    txnId = txnId,
                    amount = amount,
                    onBack = { finish() }
                )
            }
        }
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
    onBack: () -> Unit
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
            AccountDetailsCard(name, accountNumber, ifsc, upiVpa)
            Spacer(modifier = Modifier.height(24.dp))
            PayViaOtherAppCard()
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
fun AccountDetailsCard(name: String?, accountNumber: String?, ifsc: String?, upiVpa: String?) {
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
                        onClick = { /*TODO*/ },
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
                onClick = { /*TODO*/ },
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
fun PayViaOtherAppCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO */ }
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
            onBack = {}
        )
    }
}

