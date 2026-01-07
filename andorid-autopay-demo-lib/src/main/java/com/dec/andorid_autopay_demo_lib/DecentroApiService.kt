package com.dec.andorid_autopay_demo_lib

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response as OkHttpResponse
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Retrofit API interface for Decentro mandate status endpoint.
 */
interface DecentroApiService {

    @GET("v3/payments/upi/autopay/mandate/status")
    suspend fun getMandateStatus(
        @Query("decentro_mandate_id") decentroMandateId: String,
        @Header("client_id") clientId: String,
        @Header("client_secret") clientSecret: String
    ): Response<DecentroMandateStatusResponse>
}

/**
 * Response data classes for the Decentro mandate status API.
 */
data class DecentroMandateStatusResponse(
    @SerializedName("decentroTxnId")
    val decentroTxnId: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("responseCode")
    val responseCode: String? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: MandateData? = null
)

data class MandateData(
    @SerializedName("mandate_status")
    val mandateStatus: String? = null,
    @SerializedName("decentro_mandate_id")
    val decentroMandateId: String? = null,
    @SerializedName("umn")
    val umn: String? = null,
    @SerializedName("txnId")
    val txnId: String? = null,
    @SerializedName("npci_txn_id")
    val npciTxnId: String? = null,
    @SerializedName("customer_vpa")
    val customerVpa: String? = null,
    @SerializedName("amount")
    val amount: String? = null,
    @SerializedName("remarks")
    val remarks: String? = null
)

/**
 * Manages the creation of Retrofit and OkHttp clients with robust, industry-standard
 * resilience for mobile networking challenges.
 */
object DecentroRetrofitClient {

    private const val BASE_URL = "https://api.decentro.tech/"
    const val CLIENT_ID = "neowise_prepod"
    const val CLIENT_SECRET = "BITUm29bx9DpVZK26gZ73DA8lXfkfH2u"

    /**
     * A single, resilient OkHttp client instance.
     * This client is configured to handle common mobile network issues like connection loss
     * after an app returns from the background.
     */
    private val resilientHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            // FINAL STRATEGY: Long timeout for slow networks/servers.
            .readTimeout(30, TimeUnit.SECONDS)    // Give server plenty of time to respond.
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS) // Standard connection timeout.

            // Retry interceptor is still valuable for instant connection failures.
            .addInterceptor(ResilientInterceptor(maxRetries = 2, initialDelayMs = 500))

            // Add the network-level interceptor (logging) last.
            .addInterceptor(loggingInterceptor)

            // CRITICAL: Disable OkHttp's default retry mechanism.
            .retryOnConnectionFailure(false)

            .build()
    }

    /**
     * Creates a fresh Retrofit service instance.
     */
    fun createFreshApiService(): DecentroApiService {
        Log.d("DecentroRetrofitClient", "Creating fresh Retrofit service with the resilient OkHttp client.")

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(resilientHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DecentroApiService::class.java)
    }

    /**
     * An interceptor that retries on specific, recoverable network connection failures.
     */
    private class ResilientInterceptor(
        private val maxRetries: Int,
        private val initialDelayMs: Long
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
            var lastException: IOException? = null
            var currentDelay = initialDelayMs

            for (attempt in 0..maxRetries) {
                try {
                    return chain.proceed(chain.request())
                } catch (e: IOException) {
                    lastException = e
                    Log.w(
                        "ResilientInterceptor",
                        "Attempt ${attempt + 1}/${maxRetries + 1} failed.", e
                    )

                    // If the error is recoverable and we have retries left, wait and retry.
                    if (isRecoverable(e) && attempt < maxRetries) {
                        Log.d("ResilientInterceptor", "Recoverable error detected. Waiting ${currentDelay}ms before retry ${attempt + 2}...")
                        try {
                            Thread.sleep(currentDelay)
                        } catch (ie: InterruptedException) {
                            Thread.currentThread().interrupt()
                            throw IOException("Retry attempt was interrupted", ie)
                        }
                        currentDelay *= 2
                    } else {
                        Log.e("ResilientInterceptor", "Unrecoverable error or max retries reached. Failing.")
                        throw lastException
                    }
                }
            }
            throw lastException ?: IOException("All retry attempts failed.")
        }

        /**
         * Determines if an IOException is a transient connection failure.
         * IMPORTANT: We do NOT retry on SocketTimeoutException anymore.
         * A timeout means the connection was made but the server is slow.
         * Retrying a slow request will likely just result in another timeout.
         */
        private fun isRecoverable(e: IOException): Boolean {
            return when (e) {
                is SocketException,         // "Socket closed", "Connection reset"
                is UnknownHostException,    // DNS lookup failure
                is ConnectException,        // Connection refused
                is SSLException -> true     // SSL handshake issue

                // A timeout is NOT a recoverable connection error in this strategy.
                is SocketTimeoutException -> false
                else -> false
            }
        }
    }
}
