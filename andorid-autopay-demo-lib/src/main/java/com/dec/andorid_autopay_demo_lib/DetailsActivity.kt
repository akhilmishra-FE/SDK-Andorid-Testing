package com.dec.andorid_autopay_demo_lib

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dec.andorid_autopay_demo_lib.ui.theme.ButtonGradientEnd
import com.dec.andorid_autopay_demo_lib.ui.theme.ButtonGradientStart
import com.dec.andorid_autopay_demo_lib.ui.theme.UpiautopaysdkTheme

val LightBlue = Color(0xFFE9F5FF)
val BlueText = Color(0xFF4A90E2)
val GrayText = Color(0xFF757575)
val MainBackground = Color(0xFFFFFFFF)
val CardBackground = Color(0xFFFFFFFF)
val LightGrayBackground = Color(0xFFF7F7F7)

// Data class to hold app information for the custom chooser
data class PaymentAppInfo(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?,
    val intent: Intent,
    val isUpiCompatible: Boolean
)

class DetailsActivity : ComponentActivity() {
    
    private lateinit var currentName: String
    private lateinit var currentAccountNumber: String
    private lateinit var currentIfsc: String
    private lateinit var currentUpiVpa: String
    private lateinit var currentTxnId: String
    private lateinit var currentAmount: String
    private var merchantPackage: String? = null
    
    // State for custom UPI app chooser dialog
    private var showAppChooser by mutableStateOf(false)
    private var detectedApps by mutableStateOf<List<PaymentAppInfo>>(emptyList())
    
    // Cache for UPI apps - populated during initialization for instant access
    private var cachedUpiApps: List<android.content.pm.ResolveInfo>? = null
    private var isAppDetectionInProgress = false
    
    // UPI flow tracking (auto status checking removed)
    
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
        
        // Initialize UPI app detection in background for instant access
        initializeUpiAppDetection()

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
                
                // Custom UPI App Chooser Dialog
                if (showAppChooser) {
                    CustomUPIAppChooserDialog(
                        apps = detectedApps,
                        onAppSelected = { appInfo ->
                            showAppChooser = false
                            launchSelectedApp(appInfo)
                        },
                        onDismiss = { showAppChooser = false }
                    )
                }
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
        
        Log.d("DetailsActivity", "🚀 Deep Link: $deepLink")
        Log.d("DetailsActivity", "🚀 Mandate ID: $mandateId")
        
        // Skip debug methods for faster launch - use cached detection
        Log.d("DetailsActivity", "🚀 === INSTANT UPI APP LAUNCH (CACHED) ===")
        
        // Check if cache is ready
        if (cachedUpiApps == null && isAppDetectionInProgress) {
            Log.d("DetailsActivity", "⏳ Cache not ready yet, initializing detection...")
            // Cache will be ready very soon, proceed with detection
        }
        
        try {
            val uri = Uri.parse(deepLink)
            Log.d("DetailsActivity", "📱 Parsed URI: $uri")
            
            // Create intent for UPI - try mandate first, fallback to payment
            val upiIntent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                // Do NOT use FLAG_ACTIVITY_NEW_TASK - it breaks return navigation
            }
            
            // Also create a fallback basic UPI payment intent
            val fallbackUri = Uri.parse("upi://pay?pa=neowisedemo.decfin@ypbiz&pn=Merchant&am=2.00&cu=INR&tn=UPI%20Payment")
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                data = fallbackUri
            }
            
            Log.d("DetailsActivity", "🔍 Checking for available UPI apps...")
            
            // Try multiple approaches to find UPI apps
            val allUpiApps = findAllUPIApps()
            
            Log.d("DetailsActivity", "📱 Found ${allUpiApps.size} total UPI apps:")
            allUpiApps.forEach { resolveInfo ->
                Log.d("DetailsActivity", "   📱 ${resolveInfo.activityInfo.packageName} - ${resolveInfo.loadLabel(packageManager)}")
            }
            
            if (allUpiApps.isNotEmpty()) {
                // Store mandate ID BEFORE launching UPI app
                Log.d("DetailsActivity", "💾 Mandate ID: $mandateId")
                
                // Try mandate first, if no apps support it, use basic payment
                val mandateApps = packageManager.queryIntentActivities(upiIntent, 0)
                val intentToUse = if (mandateApps.isNotEmpty()) {
                    Log.d("DetailsActivity", "✅ Using UPI Mandate intent (${mandateApps.size} apps support it)")
                    upiIntent
                } else {
                    Log.d("DetailsActivity", "⚠️ No apps support UPI Mandate, using basic payment intent")
                    fallbackIntent
                }
                
                // Always use the custom chooser for better intent validation
                Log.d("DetailsActivity", "🚀 Launching UPI apps with safe intent handling...")
                showUPIAppChooser(allUpiApps, intentToUse)
                
                Log.d("DetailsActivity", "🚀 UPI app launched successfully")
                
            } else {
                Log.e("DetailsActivity", "❌ No UPI apps found that can handle mandate")
                
                // This should not happen since findAllUPIApps() should find apps
                Log.e("DetailsActivity", "🔄 This is unexpected - findAllUPIApps() found apps but they're not available")
                val genericResolveInfos = packageManager.queryIntentActivities(fallbackIntent, 0)
                Log.d("DetailsActivity", "📱 Found ${genericResolveInfos.size} generic UPI apps:")
                genericResolveInfos.forEach { resolveInfo ->
                    Log.d("DetailsActivity", "   📱 ${resolveInfo.activityInfo.packageName} - ${resolveInfo.loadLabel(packageManager)}")
                }
                
                if (genericResolveInfos.isNotEmpty()) {
                    // Show error but also try to launch with generic UPI payment
                    Log.d("DetailsActivity", "⚠️ Mandate not supported, trying generic UPI payment as fallback")
                    
                    // Store mandate ID anyway
                    Log.d("DetailsActivity", "💾 Mandate ID: $mandateId")
                    
                    // Try to launch generic UPI payment
                    val chooser = Intent.createChooser(fallbackIntent, "Select UPI App (Mandate not supported)")
                    startActivity(chooser)
                    
                    Toast.makeText(this, 
                        "UPI Mandate not directly supported. Launching generic UPI payment.", 
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, 
                        "No UPI apps found. Please install PhonePe, Google Pay, or CRED.", 
                        Toast.LENGTH_LONG).show()
                }
            }
            
        } catch (e: Exception) {
            Log.e("DetailsActivity", "❌ Error launching UPI intent: ${e.message}", e)
            android.widget.Toast.makeText(this, 
                "Error launching UPI app: ${e.message}", 
                android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Handle result from MandateStatusActivity (manual checks only)
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == MANDATE_STATUS_REQUEST_CODE) {
            Log.d("DetailsActivity", "✅ Manual status check result")
            Log.d("DetailsActivity", "✅ Result Code: $resultCode")
            
            // Pass the result to merchant app and finish
            setResult(resultCode, data)
            finish()
            Log.d("DetailsActivity", "✅ DetailsActivity finished - returning to merchant")
        }
    }
    
    /**
     * Test different UPI schemes to see which apps support what
     */
    private fun debugUPIAppSupport() {
        Log.d("DetailsActivity", "🔍 === COMPREHENSIVE UPI APP DEBUG ===")
        
        val testSchemes = listOf(
            "upi://mandate?pa=test@upi" to "UPI Mandate",
            "upi://pay?pa=test@upi&pn=Test&am=1&cu=INR" to "UPI Payment",
            "upi://pay" to "Basic UPI",
            "phonepe://pay" to "PhonePe Specific",
            "gpay://upi/pay" to "Google Pay Specific", 
            "paytm://pay" to "Paytm Specific",
            "bhim://pay" to "BHIM Specific",
            "mobikwik://pay" to "MobiKwik Specific"
        )
        
        testSchemes.forEach { (uriString, description) ->
            try {
                val testIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                val resolveInfos = packageManager.queryIntentActivities(testIntent, 0)
                Log.d("DetailsActivity", "📱 $description: ${resolveInfos.size} apps")
                resolveInfos.forEach { resolveInfo ->
                    val appName = resolveInfo.loadLabel(packageManager)
                    Log.d("DetailsActivity", "   ✅ ${resolveInfo.activityInfo.packageName} - $appName")
                }
            } catch (e: Exception) {
                Log.e("DetailsActivity", "❌ Error testing $description: ${e.message}")
            }
        }
        
        // Also test for installed UPI apps by package name
        Log.d("DetailsActivity", "🔍 === CHECKING SPECIFIC UPI APPS ===")
        val knownUpiApps = mapOf(
            "com.phonepe.app" to "PhonePe",
            "com.google.android.apps.nfc.payment" to "Google Pay", 
            "com.dreamplug.androidapp" to "CRED",
            "net.one97.paytm" to "Paytm",
            "in.org.npci.upiapp" to "BHIM UPI",
            "com.mobikwik_new" to "MobiKwik",
            "com.freecharge.android" to "Freecharge",
            "com.amazon.mShop.android.shopping" to "Amazon Pay"
        )
        
        knownUpiApps.forEach { (packageName, appName) ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                Log.d("DetailsActivity", "✅ $appName ($packageName) is INSTALLED")
            } catch (e: Exception) {
                Log.d("DetailsActivity", "❌ $appName ($packageName) is NOT installed")
            }
        }
        
        Log.d("DetailsActivity", "🔍 === END COMPREHENSIVE DEBUG ===")
    }

    /**
     * Initialize UPI app detection in background thread for instant access
     */
    private fun initializeUpiAppDetection() {
        if (isAppDetectionInProgress || cachedUpiApps != null) {
            Log.d("DetailsActivity", "🔄 UPI app detection already in progress or completed")
            return
        }
        
        isAppDetectionInProgress = true
        Log.d("DetailsActivity", "🚀 === INITIALIZING UPI APP DETECTION IN BACKGROUND ===")
        
        // Run detection in background thread to avoid blocking UI
        Thread {
            try {
                val startTime = System.currentTimeMillis()
                val apps = performUpiAppDetection()
                val endTime = System.currentTimeMillis()
                
                Log.d("DetailsActivity", "✅ UPI app detection completed in ${endTime - startTime}ms")
                Log.d("DetailsActivity", "📱 Found ${apps.size} UPI apps and cached for instant access")
                
                // Cache the results on main thread
                runOnUiThread {
                    cachedUpiApps = apps
                    isAppDetectionInProgress = false
                }
            } catch (e: Exception) {
                Log.e("DetailsActivity", "❌ Error during UPI app detection: ${e.message}")
                runOnUiThread {
                    isAppDetectionInProgress = false
                }
            }
        }.start()
    }
    
    /**
     * Find ALL UPI apps installed on device - comprehensive detection
     * Now uses caching for instant access after first detection
     */
    private fun findAllUPIApps(): List<android.content.pm.ResolveInfo> {
        // Return cached results if available
        cachedUpiApps?.let { cached ->
            Log.d("DetailsActivity", "⚡ Using cached UPI apps (${cached.size} apps) - INSTANT ACCESS!")
            return cached
        }
        
        // If not cached yet, perform detection now (fallback)
        Log.d("DetailsActivity", "🔄 Cache not ready, performing detection now...")
        return performUpiAppDetection().also { apps ->
            cachedUpiApps = apps // Cache for next time
        }
    }
    
    /**
     * Actual UPI app detection logic (extracted for reuse)
     */
    private fun performUpiAppDetection(): List<android.content.pm.ResolveInfo> {
        Log.d("DetailsActivity", "🔍 === FINDING ALL UPI APPS ON DEVICE ===")
        
        val allUpiApps = mutableSetOf<android.content.pm.ResolveInfo>()
        
        // Method 1: Comprehensive list of known UPI apps (expanded)
        val knownUpiApps = mapOf(
            // Popular UPI Apps
            "com.phonepe.app" to "PhonePe",
            "com.google.android.apps.nfc.payment" to "Google Pay",
            "com.dreamplug.androidapp" to "CRED",
            "net.one97.paytm" to "Paytm",
            "in.org.npci.upiapp" to "BHIM UPI",
            "com.mobikwik_new" to "MobiKwik",
            "com.freecharge.android" to "Freecharge",
            
            // Bank UPI Apps
            "com.csam.icici.bank.imobile" to "iMobile Pay",
            "com.axis.mobile" to "Axis Mobile",
            "com.sbi.upi" to "SBI Pay",
            "com.unionbank.ebanking" to "Union Bank UPI",
            "com.infosys.finacle.digimoney" to "Canara Bank UPI",
            "com.snapwork.hdfc" to "HDFC Bank MobileBanking",
            "com.kotakbank.pcbanking" to "Kotak Mobile Banking",
            "com.rblbank.mobank" to "RBL MoBank",
            "com.yesbank.yesmobile" to "Yes Mobile",
            "com.idbi.mbanking" to "IDBI Bank GO Mobile+",
            
            // Other UPI Apps
            "com.amazon.mShop.android.shopping" to "Amazon Pay",
            "com.whatsapp" to "WhatsApp",
            "com.facebook.orca" to "Facebook Messenger",
            "com.jio.myjio" to "MyJio",
            "com.airtel.money" to "Airtel Thanks",
            "com.bsb.hike" to "Hike Sticker Chat",
            "com.truecaller" to "Truecaller",
            "com.razorpay.payments.app" to "Razorpay",
            "com.phonepe.simulator" to "PhonePe Simulator",
            "com.fino.paytech" to "Fino Payments Bank"
        )
        
        Log.d("DetailsActivity", "🔍 Method 1: Checking ${knownUpiApps.size} known UPI apps...")
        var installedCount = 0
        knownUpiApps.forEach { (packageName, appName) ->
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                Log.d("DetailsActivity", "✅ $appName ($packageName) is INSTALLED")
                installedCount++
                
                // Create a ResolveInfo for this app
                val resolveInfo = android.content.pm.ResolveInfo()
                resolveInfo.activityInfo = android.content.pm.ActivityInfo()
                resolveInfo.activityInfo.packageName = packageName
                resolveInfo.activityInfo.applicationInfo = packageInfo.applicationInfo
                allUpiApps.add(resolveInfo)
                
            } catch (e: Exception) {
                // App not installed - this is normal, don't log as error
            }
        }
        Log.d("DetailsActivity", "✅ Found $installedCount installed UPI apps from known list")
        
        // Method 2: Query for apps that can handle various UPI schemes
        val upiSchemes = listOf(
            "upi://pay?pa=test@upi&pn=Test&am=1&cu=INR" to "Standard UPI Payment",
            "upi://pay" to "Basic UPI",
            "upi://mandate" to "UPI Mandate",
            "phonepe://pay" to "PhonePe Scheme",
            "gpay://upi/pay" to "Google Pay Scheme",
            "paytm://pay" to "Paytm Scheme"
        )
        
        Log.d("DetailsActivity", "🔍 Method 2: Querying apps for different UPI schemes...")
        upiSchemes.forEach { (scheme, schemeName) ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scheme))
                val apps = packageManager.queryIntentActivities(intent, 0)
                Log.d("DetailsActivity", "🔍 $schemeName: Found ${apps.size} apps")
                apps.forEach { resolveInfo ->
                    allUpiApps.add(resolveInfo)
                    val appName = try {
                        resolveInfo.loadLabel(packageManager)
                    } catch (e: Exception) {
                        resolveInfo.activityInfo.packageName
                    }
                    Log.d("DetailsActivity", "   📱 $appName (${resolveInfo.activityInfo.packageName})")
                }
            } catch (e: Exception) {
                Log.e("DetailsActivity", "❌ Error querying $schemeName: ${e.message}")
            }
        }
        
        // Method 3: Search for apps with "UPI" or "Pay" in their name
        Log.d("DetailsActivity", "🔍 Method 3: Searching all installed apps for UPI/Payment apps...")
        try {
            val allInstalledApps = packageManager.getInstalledApplications(0)
            val paymentKeywords = listOf("upi", "pay", "bank", "wallet", "money", "payment", "finance")
            
            allInstalledApps.forEach { appInfo ->
                try {
                    val appName = appInfo.loadLabel(packageManager).toString().lowercase()
                    val packageName = appInfo.packageName.lowercase()
                    
                    // Check if app name or package contains payment-related keywords
                    val isPaymentApp = paymentKeywords.any { keyword ->
                        appName.contains(keyword) || packageName.contains(keyword)
                    }
                    
                    if (isPaymentApp && !appInfo.packageName.startsWith("com.android.") && 
                        !appInfo.packageName.startsWith("android.") &&
                        appInfo.packageName != packageName) { // Don't include our own app
                        
                        Log.d("DetailsActivity", "🔍 Found potential payment app: ${appInfo.loadLabel(packageManager)} (${appInfo.packageName})")
                        
                        // Create a ResolveInfo for this app
                        val resolveInfo = android.content.pm.ResolveInfo()
                        resolveInfo.activityInfo = android.content.pm.ActivityInfo()
                        resolveInfo.activityInfo.packageName = appInfo.packageName
                        resolveInfo.activityInfo.applicationInfo = appInfo
                        allUpiApps.add(resolveInfo)
                    }
                } catch (e: Exception) {
                    // Skip apps we can't read
                }
            }
        } catch (e: Exception) {
            Log.e("DetailsActivity", "❌ Error searching installed apps: ${e.message}")
        }
        
        val uniqueApps = allUpiApps.distinctBy { it.activityInfo.packageName }
        Log.d("DetailsActivity", "🔍 === FINAL COMPREHENSIVE RESULT: ${uniqueApps.size} total apps found ===")
        
        // Sort apps by name for better display
        val sortedApps = uniqueApps.sortedBy { resolveInfo ->
            try {
                resolveInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                resolveInfo.activityInfo.packageName
            }
        }
        
        sortedApps.forEach { resolveInfo ->
            try {
                val appName = resolveInfo.loadLabel(packageManager)
                Log.d("DetailsActivity", "   📱 $appName (${resolveInfo.activityInfo.packageName})")
            } catch (e: Exception) {
                Log.d("DetailsActivity", "   📱 ${resolveInfo.activityInfo.packageName} (name unavailable)")
            }
        }
        
        return sortedApps
    }

    /**
     * Show custom scrollable dialog with ALL detected UPI apps
     */
    private fun showUPIAppChooser(upiApps: List<android.content.pm.ResolveInfo>, originalIntent: Intent) {
        Log.d("DetailsActivity", "🎯 === CREATING CUSTOM SCROLLABLE UPI CHOOSER ===")
        
        try {
            val appInfoList = mutableListOf<PaymentAppInfo>()
            
            Log.d("DetailsActivity", "📱 Preparing ${upiApps.size} apps for custom chooser...")
            
            upiApps.forEach { resolveInfo ->
                val appName = try {
                    resolveInfo.loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    resolveInfo.activityInfo.packageName
                }
                
                val appIcon = try {
                    resolveInfo.loadIcon(packageManager)
                } catch (e: Exception) {
                    null
                }
                
                Log.d("DetailsActivity", "🔧 Processing app: $appName")
                
                // Try multiple intent types for each app
                val intentTypes = listOf(
                    "upi://pay?pa=neowisedemo.decfin@ypbiz&pn=Merchant&am=2.00&cu=INR&tn=Payment" to "UPI Payment",
                    "upi://mandate?pa=neowisedemo.decfin@ypbiz&pn=Merchant&am=2.00&cu=INR&tn=Mandate" to "UPI Mandate", 
                    "upi://pay" to "Basic UPI"
                )
                
                var appIntent: Intent? = null
                var isUpiCompatible = false
                
                // Try UPI intents first
                for ((uriString, intentType) in intentTypes) {
                    try {
                        val testIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                            setPackage(resolveInfo.activityInfo.packageName)
                            // CRITICAL: Ensure proper task management for background behavior
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            // Do NOT use FLAG_ACTIVITY_NEW_TASK - keeps apps in same task stack
                        }
                        
                        // Test if this app can handle this intent
                        val canHandle = packageManager.queryIntentActivities(testIntent, 0)
                            .any { it.activityInfo.packageName == resolveInfo.activityInfo.packageName }
                        
                        if (canHandle) {
                            appIntent = testIntent
                            isUpiCompatible = true
                            Log.d("DetailsActivity", "✅ $appName supports $intentType")
                            break // Use first working UPI intent
                        }
                    } catch (e: Exception) {
                        Log.d("DetailsActivity", "❌ $intentType failed for $appName: ${e.message}")
                    }
                }
                
                // If no UPI intent worked, create direct launch intent
                if (appIntent == null) {
                    try {
                        appIntent = packageManager.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
                        if (appIntent != null) {
                            Log.d("DetailsActivity", "✅ Created direct launch intent for $appName")
                        }
                    } catch (e: Exception) {
                        Log.e("DetailsActivity", "❌ Cannot create launch intent for $appName: ${e.message}")
                    }
                }
                
                // Add to list if we have a valid intent
                if (appIntent != null) {
                    appInfoList.add(
                        PaymentAppInfo(
                            name = appName,
                            packageName = resolveInfo.activityInfo.packageName,
                            icon = appIcon,
                            intent = appIntent,
                            isUpiCompatible = isUpiCompatible
                        )
                    )
                    Log.d("DetailsActivity", "✅ Added $appName to custom chooser")
                } else {
                    Log.w("DetailsActivity", "⚠️ Could not create intent for $appName")
                }
            }
            
            Log.d("DetailsActivity", "🎯 Prepared ${appInfoList.size} apps for custom chooser")
            
            if (appInfoList.isNotEmpty()) {
                // Filter to show ONLY UPI-compatible apps
                val upiOnlyApps = appInfoList.filter { it.isUpiCompatible }
                
                if (upiOnlyApps.isNotEmpty()) {
                    // Sort UPI apps alphabetically
                    val sortedUpiApps = upiOnlyApps.sortedBy { it.name }
                    
                // If only one UPI app, launch immediately without showing dialog
                if (sortedUpiApps.size == 1) {
                    Log.d("DetailsActivity", "✅ Only one UPI app found, launching immediately: ${sortedUpiApps[0].name}")
                    launchSelectedApp(sortedUpiApps[0])
                } else {
                    // Show custom dialog for multiple UPI apps
                    detectedApps = sortedUpiApps
                    showAppChooser = true
                    
                    Log.d("DetailsActivity", "✅ Custom UPI-only chooser dialog shown with ${sortedUpiApps.size} UPI apps")
                    
                    // Show toast
                    Toast.makeText(this, 
                        "Found ${sortedUpiApps.size} UPI-compatible apps", 
                        Toast.LENGTH_SHORT).show()
                }
                } else {
                    Log.w("DetailsActivity", "⚠️ No UPI-compatible apps found")
                    Toast.makeText(this, 
                        "No UPI-compatible apps found. Please install PhonePe, Google Pay, or CRED.", 
                        Toast.LENGTH_LONG).show()
                }
                    
            } else {
                Log.e("DetailsActivity", "❌ No valid apps for custom chooser")
                Toast.makeText(this, 
                    "Found ${upiApps.size} payment apps but cannot launch them. Please check app permissions.", 
                    Toast.LENGTH_LONG).show()
            }
            
        } catch (e: Exception) {
            Log.e("DetailsActivity", "❌ Error creating custom UPI chooser: ${e.message}", e)
            Toast.makeText(this, "Error creating app chooser: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Launch the selected app ensuring SDK stays in background (not replaced)
     */
    private fun launchSelectedApp(appInfo: PaymentAppInfo) {
        Log.d("DetailsActivity", "🚀 Launching selected app: ${appInfo.name}")
        
        try {
            // CRITICAL: Create intent that opens UPI app WITHOUT replacing SDK
            val finalIntent = Intent(appInfo.intent).apply {
                // Clear all existing flags that might cause replacement
                flags = 0
                
                // CRITICAL FLAGS: These ensure UPI app opens in NEW TASK, SDK stays in background
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                
                // Additional flags for proper behavior
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            Log.d("DetailsActivity", "🚀 === LAUNCHING UPI APP ===")
            Log.d("DetailsActivity", "🚀 App: ${appInfo.name}")
            Log.d("DetailsActivity", "🚀 Package: ${appInfo.packageName}")
            Log.d("DetailsActivity", "🚀 Intent flags: ${finalIntent.flags}")
            Log.d("DetailsActivity", "🚀 Intent action: ${finalIntent.action}")
            Log.d("DetailsActivity", "🚀 Intent data: ${finalIntent.data}")
            
            // Store mandate ID BEFORE launching app
            val mandateService = MandateStatusService()
            val mandateId = mandateService.generateMandateId()
            Log.d("DetailsActivity", "💾 Mandate ID: $mandateId")
            
            // Store mandate ID for status checking when user returns
            storeMandateId(mandateId)
            Log.d("DetailsActivity", "💾 Stored mandate ID for return: $mandateId")
            
            // Launch UPI app in new task
            startActivity(finalIntent)
            
            Log.d("DetailsActivity", "✅ Successfully launched ${appInfo.name}")
            Log.d("DetailsActivity", "✅ SDK remains in background, ${appInfo.name} opened in new task")
            Log.d("DetailsActivity", "✅ User will return to SDK after completing payment")
            
            Log.d("DetailsActivity", "✅ UPI app launched - user will return manually")
            
        } catch (e: Exception) {
            Log.e("DetailsActivity", "❌ Error launching ${appInfo.name}: ${e.message}")
            Toast.makeText(this, "Error launching ${appInfo.name}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Simple test to verify UPI app detection is working
     */
    private fun testSimpleUPIDetection() {
        Log.d("DetailsActivity", "🧪 === SIMPLE UPI DETECTION TEST ===")
        
        // Test 1: Check if PhonePe is installed
        try {
            packageManager.getPackageInfo("com.phonepe.app", 0)
            Log.d("DetailsActivity", "✅ PhonePe is installed")
        } catch (e: Exception) {
            Log.d("DetailsActivity", "❌ PhonePe not found")
        }
        
        // Test 2: Check if Google Pay is installed
        try {
            packageManager.getPackageInfo("com.google.android.apps.nfc.payment", 0)
            Log.d("DetailsActivity", "✅ Google Pay is installed")
        } catch (e: Exception) {
            Log.d("DetailsActivity", "❌ Google Pay not found")
        }
        
        // Test 3: Check if CRED is installed
        try {
            packageManager.getPackageInfo("com.dreamplug.androidapp", 0)
            Log.d("DetailsActivity", "✅ CRED is installed")
        } catch (e: Exception) {
            Log.d("DetailsActivity", "❌ CRED not found")
        }
        
        // Test 4: Simple UPI intent test
        val testIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay?pa=test@upi&pn=Test&am=1&cu=INR"))
        val apps = packageManager.queryIntentActivities(testIntent, 0)
        Log.d("DetailsActivity", "🧪 Simple UPI intent found ${apps.size} apps")
        
        Log.d("DetailsActivity", "🧪 === END SIMPLE TEST ===")
    }

    /**
     * Validate if an intent can be handled before launching
     */
    private fun canHandleIntent(intent: Intent): Boolean {
        return try {
            val activities = packageManager.queryIntentActivities(intent, 0)
            val canHandle = activities.isNotEmpty()
            Log.d("DetailsActivity", "🔍 Intent validation: ${if (canHandle) "✅ CAN handle" else "❌ CANNOT handle"} - ${activities.size} activities found")
            canHandle
        } catch (e: Exception) {
            Log.e("DetailsActivity", "❌ Error validating intent: ${e.message}")
            false
        }
    }

    /**
     * Create a safe UPI intent that apps can actually handle
     */
    private fun createSafeUPIIntent(): Intent {
        Log.d("DetailsActivity", "🛡️ === CREATING SAFE UPI INTENT ===")
        
        // Try different UPI schemes in order of preference
        val upiSchemes = listOf(
            "upi://pay?pa=neowisedemo.decfin@ypbiz&pn=Merchant&am=2.00&cu=INR&tn=Payment",
            "upi://pay?pa=test@upi&pn=Test&am=1&cu=INR",
            "upi://pay",
            "https://pay.google.com/gp/v/pay?pa=neowisedemo.decfin@ypbiz&pn=Merchant&am=2.00&cu=INR"
        )
        
        for (scheme in upiSchemes) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scheme))
            if (canHandleIntent(intent)) {
                Log.d("DetailsActivity", "✅ Safe UPI intent created with scheme: $scheme")
                return intent
            }
        }
        
        // Fallback: create basic intent
        Log.d("DetailsActivity", "⚠️ No UPI scheme worked, creating basic ACTION_VIEW intent")
        return Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
    }

    /**
     * Final fallback when no UPI apps are detected
     */
    private fun tryFinalUPIFallback() {
        Log.d("DetailsActivity", "🆘 === FINAL UPI FALLBACK ===")
        
        try {
            // Try the most basic UPI intent possible
            val basicIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
            
            if (canHandleIntent(basicIntent)) {
                Log.d("DetailsActivity", "✅ Basic UPI intent can be handled")
                startActivity(basicIntent)
                return
            }
            
            // Try web-based UPI
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pay.google.com/gp/v/pay?pa=test@upi"))
            if (canHandleIntent(webIntent)) {
                Log.d("DetailsActivity", "✅ Web UPI intent can be handled")
                startActivity(webIntent)
                return
            }
            
            // Try opening any of the known UPI apps directly
            val knownApps = listOf(
                "com.phonepe.app",
                "com.google.android.apps.nfc.payment", 
                "com.dreamplug.androidapp"
            )
            
            for (packageName in knownApps) {
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        Log.d("DetailsActivity", "✅ Launching $packageName directly")
                        startActivity(launchIntent)
                        Toast.makeText(this, "Opened UPI app. Please complete payment manually.", Toast.LENGTH_LONG).show()
                        return
                    }
                } catch (e: Exception) {
                    Log.d("DetailsActivity", "❌ Cannot launch $packageName: ${e.message}")
                }
            }
            
            // Absolute final fallback
            Toast.makeText(this, 
                "No UPI apps found. Please install PhonePe, Google Pay, or CRED from Play Store.", 
                Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e("DetailsActivity", "❌ Final fallback failed: ${e.message}")
            Toast.makeText(this, "Unable to launch UPI payment: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Show a comprehensive summary of all detected payment apps
     */
    private fun showAppDetectionSummary() {
        Log.d("DetailsActivity", "📊 === COMPREHENSIVE APP DETECTION SUMMARY ===")
        
        try {
            // Get all detected apps
            val allApps = findAllUPIApps()
            
            Log.d("DetailsActivity", "📊 TOTAL DETECTED APPS: ${allApps.size}")
            Log.d("DetailsActivity", "📊 ========================================")
            
            // Categorize apps
            val knownUpiApps = mutableListOf<String>()
            val bankApps = mutableListOf<String>()
            val walletApps = mutableListOf<String>()
            val otherApps = mutableListOf<String>()
            
            allApps.forEach { resolveInfo ->
                try {
                    val appName = resolveInfo.loadLabel(packageManager).toString()
                    val packageName = resolveInfo.activityInfo.packageName
                    
                    when {
                        packageName.contains("phonepe") || packageName.contains("google.android.apps.nfc") || 
                        packageName.contains("dreamplug") || packageName.contains("paytm") -> {
                            knownUpiApps.add("$appName ($packageName)")
                        }
                        packageName.contains("bank") || appName.lowercase().contains("bank") -> {
                            bankApps.add("$appName ($packageName)")
                        }
                        packageName.contains("wallet") || packageName.contains("money") || 
                        packageName.contains("pay") -> {
                            walletApps.add("$appName ($packageName)")
                        }
                        else -> {
                            otherApps.add("$appName ($packageName)")
                        }
                    }
                } catch (e: Exception) {
                    otherApps.add("${resolveInfo.activityInfo.packageName} (name unavailable)")
                }
            }
            
            // Log categorized results
            if (knownUpiApps.isNotEmpty()) {
                Log.d("DetailsActivity", "📊 POPULAR UPI APPS (${knownUpiApps.size}):")
                knownUpiApps.forEach { app -> Log.d("DetailsActivity", "   🔥 $app") }
            }
            
            if (bankApps.isNotEmpty()) {
                Log.d("DetailsActivity", "📊 BANK APPS (${bankApps.size}):")
                bankApps.forEach { app -> Log.d("DetailsActivity", "   🏦 $app") }
            }
            
            if (walletApps.isNotEmpty()) {
                Log.d("DetailsActivity", "📊 WALLET/PAYMENT APPS (${walletApps.size}):")
                walletApps.forEach { app -> Log.d("DetailsActivity", "   💳 $app") }
            }
            
            if (otherApps.isNotEmpty()) {
                Log.d("DetailsActivity", "📊 OTHER PAYMENT APPS (${otherApps.size}):")
                otherApps.forEach { app -> Log.d("DetailsActivity", "   📱 $app") }
            }
            
            Log.d("DetailsActivity", "📊 ========================================")
            Log.d("DetailsActivity", "📊 SUMMARY: Found ${allApps.size} total payment-capable apps")
            Log.d("DetailsActivity", "📊 - Popular UPI: ${knownUpiApps.size}")
            Log.d("DetailsActivity", "📊 - Bank Apps: ${bankApps.size}")
            Log.d("DetailsActivity", "📊 - Wallets: ${walletApps.size}")
            Log.d("DetailsActivity", "📊 - Others: ${otherApps.size}")
            
        } catch (e: Exception) {
            Log.e("DetailsActivity", "❌ Error creating app summary: ${e.message}")
        }
    }







    override fun onResume() {
        super.onResume()
        Log.d("DetailsActivity", "🔄 ========================================")
        Log.d("DetailsActivity", "🔄 === onResume() CALLED ===")
        Log.d("DetailsActivity", "🔄 ========================================")
        Log.d("DetailsActivity", "🔄 User returned to DetailsActivity")
        Log.d("DetailsActivity", "🔄 Resume Timestamp: ${System.currentTimeMillis()}")
        
        // Check if user returned from PSP app and invoke status API
        val storedMandateId = getStoredMandateId()
        if (storedMandateId != null) {
            Log.d("DetailsActivity", "🔄 Found stored mandate ID: $storedMandateId")
            Log.d("DetailsActivity", "🔄 User returned from PSP app - invoking status API")
            clearStoredMandateId() // Clear immediately to avoid duplicate calls
            invokeStatusAPI(storedMandateId)
        } else {
            Log.d("DetailsActivity", "🔄 No stored mandate ID - user did not return from PSP app")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DetailsActivity", "🔄 DetailsActivity destroyed")
    }

    /**
     * Store mandate ID in SharedPreferences when launching PSP app
     */
    private fun storeMandateId(mandateId: String) {
        val sharedPrefs = getSharedPreferences("UPI_SDK_PREFS", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("PENDING_MANDATE_ID", mandateId).apply()
        Log.d("DetailsActivity", "💾 Stored mandate ID: $mandateId")
    }
    
    /**
     * Get stored mandate ID from SharedPreferences
     */
    private fun getStoredMandateId(): String? {
        val sharedPrefs = getSharedPreferences("UPI_SDK_PREFS", Context.MODE_PRIVATE)
        return sharedPrefs.getString("PENDING_MANDATE_ID", null)
    }
    
    /**
     * Clear stored mandate ID from SharedPreferences
     */
    private fun clearStoredMandateId() {
        val sharedPrefs = getSharedPreferences("UPI_SDK_PREFS", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("PENDING_MANDATE_ID").apply()
        Log.d("DetailsActivity", "🗑️ Cleared stored mandate ID")
    }
    
    /**
     * Invoke status API when returning from PSP app
     */
    private fun invokeStatusAPI(mandateId: String) {
        Log.d("DetailsActivity", "📡 ========================================")
        Log.d("DetailsActivity", "📡 === INVOKING STATUS API ===")
        Log.d("DetailsActivity", "📡 ========================================")
        Log.d("DetailsActivity", "📡 Mandate ID: $mandateId")
        
        // Launch MandateStatusActivity to check status
        val intent = Intent(this, MandateStatusActivity::class.java).apply {
            putExtra("MANDATE_ID", mandateId)
        }
        startActivityForResult(intent, MANDATE_STATUS_REQUEST_CODE)
    }

    companion object {
        private const val MANDATE_STATUS_REQUEST_CODE = 1002
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


/**
 * Custom scrollable dialog to show all detected UPI/Payment apps
 */
@Composable
fun CustomUPIAppChooserDialog(
    apps: List<PaymentAppInfo>,
    onAppSelected: (PaymentAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                    text = "Select UPI App",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Text(
                text = "Found ${apps.size} UPI-compatible apps",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                
                // Scrollable app list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps) { appInfo ->
                        PaymentAppItem(
                            appInfo = appInfo,
                            onClick = { onAppSelected(appInfo) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual app item in the chooser dialog
 */
@Composable
fun PaymentAppItem(
    appInfo: PaymentAppInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightBlue // All apps shown are UPI-compatible
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (appInfo.icon != null) {
                    // Convert drawable to ImageBitmap and display
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        appInfo.icon.intrinsicWidth.takeIf { it > 0 } ?: 48,
                        appInfo.icon.intrinsicHeight.takeIf { it > 0 } ?: 48,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    appInfo.icon.setBounds(0, 0, canvas.width, canvas.height)
                    appInfo.icon.draw(canvas)
                    
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = appInfo.name,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    // Fallback icon
                    Icon(
                        Icons.Outlined.Wallet,
                        contentDescription = appInfo.name,
                        modifier = Modifier.size(32.dp),
                        tint = BlueText
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // App info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appInfo.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "UPI Compatible", // All shown apps are UPI-compatible
                    style = MaterialTheme.typography.bodySmall,
                    color = BlueText
                )
                
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Arrow indicator
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Launch",
                tint = BlueText, // All shown apps are UPI-compatible
                modifier = Modifier.size(20.dp)
            )
        }
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

