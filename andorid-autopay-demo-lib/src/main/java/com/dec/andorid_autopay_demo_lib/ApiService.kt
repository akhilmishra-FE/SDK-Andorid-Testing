package com.dec.andorid_autopay_demo_lib

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class AccountRequest(
    @SerializedName("reference_id") val referenceId: String,
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("is_consent_granted") val isConsentGranted: Boolean,
    @SerializedName("fetch_branch_details") val fetchBranchDetails: Boolean,
    @SerializedName("consumer_urn") val consumerUrn: String
)

data class AccountResponse(
    @SerializedName("decentro_txn_id") val decentroTxnId: String,
    @SerializedName("api_status") val status: String,
    val message: String,
    val data: AccountData?,
    @SerializedName("response_key") val responseKey: String
)

data class AccountData(
    @SerializedName("name_as_per_bank") val name: String,
    @SerializedName("account_number") val accountNumber: String,
    val ifsc: String,
    @SerializedName("upi_vpa") val upiVpa: String,
    @SerializedName("payout_amount") val payoutAmount: String?
)

interface ApiService {
    @POST("v3/banking/mobile_to_account")
    suspend fun getAccount(
        @Header("client-id") clientId: String,
        @Header("client-secret") clientSecret: String,
        @Body request: AccountRequest
    ): Response<AccountResponse>
}

