package com.dec.andorid_autopay_demo_lib

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.*

// Note: MandateData is defined in DecentroApiService.kt to avoid duplication

// Data class for the service's final, simplified response
data class MandateStatusResponse(
    val mandate_status: String,
    val decentro_mandate_id: String,
    val message: String? = null
)

// Enum for consistent status handling
enum class MandateStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}

class MandateStatusService(private val context: Context? = null) {

    companion object {
        private const val TAG = "MandateStatusService"
    }

    /**
     * Checks if the device has an active and validated network connection.
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isNetworkAvailable(): Boolean {
        if (context == null) {
            Log.w(TAG, "⚠️ [Network Check] No context provided; assuming network is available.")
            return true
        }
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            if (network == null) {
                Log.e(TAG, "❌ [Network Check] Failed: No active network.")
                return false
            }
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities == null) {
                Log.e(TAG, "❌ [Network Check] Failed: Cannot get network capabilities.")
                return false
            }
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            Log.d(TAG, "✅ [Network Check] Status: hasInternet=$hasInternet, isValidated=$isValidated")
            return hasInternet && isValidated
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Network Check] Exception: ${e.message}", e)
            false
        }
    }

    /**
     * Pauses execution to allow the network to stabilize, especially after returning from another app.
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private suspend fun waitForNetworkStabilization() {
        Log.d(TAG, "⏳ [Stabilization] Starting network stabilization period...")
        delay(1000) // 1-second delay
        val isReady = isNetworkAvailable() // Re-check the network status after the delay
        if (isReady) {
            Log.d(TAG, "✅ [Stabilization] Network is stable and ready.")
            // Test DNS connectivity after network is stable
            testDNSConnectivity()
        } else {
            Log.w(TAG, "⚠️ [Stabilization] Network is still not ready after initial delay.")
        }
    }
    
    /**
     * Test DNS connectivity to help diagnose UnknownHostException issues
     */
    private suspend fun testDNSConnectivity() {
        Log.d(TAG, "🔍 === DNS CONNECTIVITY TEST ===")
        
        // Test 1: Try to resolve our API host
        val apiHost = "api.decentro.tech"
        var apiResolved = false
        try {
            Log.d(TAG, "🔍 Testing DNS resolution for: $apiHost")
            withContext(Dispatchers.IO) {
                val addresses = java.net.InetAddress.getAllByName(apiHost)
                Log.d(TAG, "✅ DNS Resolution Success:")
                addresses.forEach { address ->
                    Log.d(TAG, "   📍 IP: ${address.hostAddress}")
                }
                apiResolved = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ DNS Resolution Failed for $apiHost: ${e.message}")
        }
        
        // Test 2: Try alternative DNS servers
        if (!apiResolved) {
            Log.d(TAG, "🔄 Testing alternative connectivity...")
            
            // Test Google DNS
            try {
                withContext(Dispatchers.IO) {
                    val googleDNS = java.net.InetAddress.getAllByName("8.8.8.8")
                    Log.d(TAG, "✅ Google DNS (8.8.8.8) reachable: ${googleDNS[0].hostAddress}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Google DNS failed: ${e.message}")
            }
            
            // Test Cloudflare DNS
            try {
                withContext(Dispatchers.IO) {
                    val cloudflareDNS = java.net.InetAddress.getAllByName("1.1.1.1")
                    Log.d(TAG, "✅ Cloudflare DNS (1.1.1.1) reachable: ${cloudflareDNS[0].hostAddress}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Cloudflare DNS failed: ${e.message}")
            }
            
            // Test a well-known host
            try {
                withContext(Dispatchers.IO) {
                    val googleHost = java.net.InetAddress.getAllByName("google.com")
                    Log.d(TAG, "✅ Google.com resolved: ${googleHost[0].hostAddress}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Google.com resolution failed: ${e.message}")
            }
        }
        
        // Provide diagnostic summary
        if (apiResolved) {
            Log.d(TAG, "🎯 DNS Diagnosis: API host resolution successful")
        } else {
            Log.w(TAG, "⚠️ DNS Diagnosis: API host resolution failed")
            Log.w(TAG, "⚠️ This suggests DNS server issues or network filtering")
            Log.w(TAG, "⚠️ Consider using mobile data or different WiFi network")
        }
    }

    /**
     * Check mandate status via API
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    suspend fun checkMandateStatus(decentroMandateId: String): MandateStatusResponse {
        return checkMandateStatusReal(decentroMandateId)
    }

    /**
     * Real API call implementation with retry logic for common network failures.
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private suspend fun checkMandateStatusReal(decentroMandateId: String): MandateStatusResponse {
        var lastException: Exception? = null
        val maxRetries = 3 // Standard number of retries

        for (attempt in 0 until maxRetries) {
            val attemptNum = attempt + 1
            Log.i(TAG, "🚀 [API Attempt $attemptNum/$maxRetries] Starting...")
            try {
                // On the first attempt, wait for network stabilization after returning from another app.
                if (attempt == 0) {
                    waitForNetworkStabilization()
                }

                Log.d(TAG, "[API Attempt $attemptNum] Creating fresh API service instance.")
                val freshApiService = DecentroRetrofitClient.createFreshApiService()
                val response = freshApiService.getMandateStatus(
                    decentroMandateId = decentroMandateId,
                    clientId = DecentroRetrofitClient.CLIENT_ID,
                    clientSecret = DecentroRetrofitClient.CLIENT_SECRET
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.i(TAG, "✅ [API Attempt $attemptNum] SUCCESS! Status: ${response.code()}, Body: $body")
                    val mandateStatus = body?.data?.mandateStatus ?: "PENDING"
                    val message = body?.message ?: body?.data?.remarks ?: "Status check completed"
                    return MandateStatusResponse(
                        mandate_status = mandateStatus.uppercase(Locale.getDefault()),
                        decentro_mandate_id = decentroMandateId,
                        message = message
                    )
                } else {
                    val errorCode = response.code()
                    val errorMessage = response.errorBody()?.string() ?: response.message()
                    Log.e(TAG, "❌ [API Attempt $attemptNum] FAILED. HTTP Code: $errorCode, Message: $errorMessage")

                    if (errorCode == 429) {
                        Log.w(TAG, "[API Attempt $attemptNum] Received HTTP 429 (Too Many Requests). Backing off for 5 seconds.")
                        delay(5000)
                        lastException = Exception("HTTP 429: Too Many Requests")
                        continue
                    }

                    if (errorCode in 400..499) {
                        Log.e(TAG, "❌ [API Attempt $attemptNum] Client error ($errorCode). Failing fast, not retrying.")
                        return MandateStatusResponse(
                            mandate_status = "FAILED",
                            decentro_mandate_id = decentroMandateId,
                            message = "API request failed with client error: $errorCode"
                        )
                    }
                    lastException = Exception("API returned HTTP Error: $errorCode")
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "💥 [API Attempt $attemptNum] EXCEPTION: ${e.javaClass.simpleName} - ${e.message}", e)

                if (attemptNum < maxRetries) {
                    // Enhanced delay logic for different exception types
                    val delayMs = when (e) {
                        is java.net.UnknownHostException -> {
                            Log.w(TAG, "🌐 DNS Resolution Failed - using longer delay for network recovery")
                            5000L * attemptNum // Longer delay for DNS issues
                        }
                        is java.net.SocketTimeoutException -> {
                            Log.w(TAG, "⏱️ Socket Timeout - using standard delay")
                            3000L * attemptNum // Medium delay for timeouts
                        }
                        else -> {
                            Log.w(TAG, "🔄 General network error - using standard delay")
                            2000L * attemptNum // Standard delay for other errors
                        }
                    }
                    
                    Log.d(TAG, "🔄 [API Attempt $attemptNum] ${e.javaClass.simpleName} - Waiting ${delayMs}ms before next retry...")
                    delay(delayMs)
                    
                    // For DNS issues, test connectivity again before retry
                    if (e is java.net.UnknownHostException) {
                        Log.d(TAG, "🔍 DNS issue detected - testing connectivity before retry...")
                        testDNSConnectivity()
                        
                        // Provide troubleshooting guidance
                        if (attemptNum == maxRetries - 1) { // Last retry
                            Log.w(TAG, "🚨 === NETWORK TROUBLESHOOTING GUIDE ===")
                            Log.w(TAG, "🚨 DNS resolution is failing consistently")
                            Log.w(TAG, "🚨 Possible solutions:")
                            Log.w(TAG, "   1️⃣ Switch from WiFi to mobile data (or vice versa)")
                            Log.w(TAG, "   2️⃣ Try a different WiFi network")
                            Log.w(TAG, "   3️⃣ Check if corporate/school network blocks external APIs")
                            Log.w(TAG, "   4️⃣ Restart your device's network connection")
                            Log.w(TAG, "   5️⃣ Contact your network administrator")
                        }
                    }
                }
            }
        }

        // If all retries fail, provide detailed error information
        Log.e(TAG, "❌ [FINAL] All API retries failed. Last error: ${lastException?.message}")
        
        // Provide specific error message based on exception type
        val errorMessage = when (lastException) {
            is java.net.UnknownHostException -> {
                Log.e(TAG, "🌐 DNS Resolution consistently failed")
                Log.e(TAG, "💡 Suggestion: Try switching to mobile data or different WiFi network")
                "DNS resolution failed. Please check your internet connection or try switching networks."
            }
            is java.net.SocketTimeoutException -> {
                Log.e(TAG, "⏱️ Network timeout occurred")
                Log.e(TAG, "💡 Suggestion: Check network stability")
                "Network timeout. Please check your internet connection and try again."
            }
            else -> {
                Log.e(TAG, "🔄 General network error occurred")
                "Network error occurred. Please check your connection and try again."
            }
        }
        
        return MandateStatusResponse(
            mandate_status = "FAILED",
            decentro_mandate_id = decentroMandateId,
            message = errorMessage
        )
    }

    /**
     * Generate UPI mandate deep link
     */
    fun generateMandateDeepLink(): String {
        return "upi://mandate?pa=neowisedemo.decfin@ypbiz&pn=Merchant%20onboarding%20account&mn=SDK%20Mandate%20Testing&tid=F90DDBCDDD5543A0B8B4A6804617F94F&validitystart=29012026&validityend=29092026&am=2.00&amrule=MAX&recur=ASPRESENTED&tr=F90DDBCDDD5543A0B8B4A6804617F94F&cu=INR&mc=7392&tn=TPV%20Testing&rev=Y&block=N&txnType=CREATE&purpose=14&mode=13"
    }

    /**
     * Generate mandate ID for demo purposes
     * Using a known valid ID that works with the API
     */
    fun generateMandateId(): String {
        // Use the known working mandate ID from your Postman test
        return "79A329A004C74810988D2190C777520B"
    }
}
