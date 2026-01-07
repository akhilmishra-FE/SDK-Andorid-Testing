package com.dec.andorid_autopay_demo_lib

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit API interface for Decentro mandate status endpoint
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
 * Response data class for Decentro mandate status API
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
 * Retrofit client singleton for Decentro API
 */
object DecentroRetrofitClient {
    
    private const val BASE_URL = "https://api.decentro.tech/"
    const val CLIENT_ID = "neowise_prepod"
    const val CLIENT_SECRET = "BITUm29bx9DpVZK26gZ73DA8lXfkfH2u"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)  // Increased from 30s
        .readTimeout(60, TimeUnit.SECONDS)     // Increased from 30s  
        .writeTimeout(60, TimeUnit.SECONDS)    // Increased from 30s
        .retryOnConnectionFailure(true)        // Enable automatic retry
        .build()
    
    val instance: DecentroApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DecentroApiService::class.java)
    }
}
