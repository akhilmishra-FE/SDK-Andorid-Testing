package com.dec.andorid_autopay_demo_lib

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.delay
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
        // Control flag to easily switch between real and dummy flows
        private const val USE_REAL_API = true
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
        } else {
            Log.w(TAG, "⚠️ [Stabilization] Network is still not ready after initial delay.")
        }
    }

    /**
     * Simulates an API call and returns dummy data. Useful for testing and as a fallback.
     */
    private suspend fun checkMandateStatusDummy(decentroMandateId: String): MandateStatusResponse {
        Log.d(TAG, "🎭 [Dummy Flow] Using DUMMY API.")
        delay(2000) // Simulate network latency
        return MandateStatusResponse(
            mandate_status = MandateStatus.SUCCESS.toString(),
            decentro_mandate_id = decentroMandateId,
            message = "Mandate status checked successfully (Dummy Data)"
        )
    }

    /**
     * Checks mandate status by calling the real Decentro API with a robust retry strategy.
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    suspend fun checkMandateStatus(decentroMandateId: String): MandateStatusResponse {
        Log.i(TAG, "🔍 === CHECKING MANDATE STATUS for ID: $decentroMandateId ===")
        return if (USE_REAL_API) {
            Log.d(TAG, "🌐 [Real Flow] Starting...")
            checkMandateStatusReal(decentroMandateId)
        } else {
            checkMandateStatusDummy(decentroMandateId)
        }
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
                    val delayMs = 2000L * attemptNum
                    Log.d(TAG, "🔄 [API Attempt $attemptNum] Waiting ${delayMs}ms before next retry...")
                    delay(delayMs)
                }
            }
        }

        // If all retries fail, return a FAILED response
        Log.e(TAG, "❌ [FINAL] All API retries failed. Last error: ${lastException?.message}")
        return MandateStatusResponse(
            mandate_status = "FAILED",
            decentro_mandate_id = decentroMandateId,
            message = "Failed to get status after $maxRetries attempts. Last error: ${lastException?.javaClass?.simpleName}"
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
