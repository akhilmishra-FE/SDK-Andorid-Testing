package com.dec.andorid_autopay_demo_lib

import android.content.Context

/**
 * Main UPI AutoPay SDK class
 * Entry point for merchant applications to integrate UPI AutoPay functionality
 */
object UPIAutoPaySDK {
    
    /**
     * SDK Configuration class
     */
    data class Configuration(
        val clientId: String,
        val clientSecret: String,
        val providerId: String,
        val environment: Environment = Environment.SANDBOX
    )
    
    /**
     * Mandate details for creating UPI AutoPay mandate
     */
    data class MandateDetails(
        val amount: String,                    // Maximum debit amount
        val recurrence: String,               // DAILY, WEEKLY, MONTHLY, YEARLY
        val startDate: String,                // Start date (YYYY-MM-DD)
        val endDate: String,                  // End date (YYYY-MM-DD)
        val purpose: String,                  // Purpose description
        val merchantReferenceId: String,      // Your unique transaction ID
        val customerVpa: String? = null       // Customer's UPI ID (optional)
    )
    
    /**
     * Payment details for executing payment against mandate
     */
    data class PaymentDetails(
        val mandateId: String,
        val amount: String,                   // Amount to debit (must be <= mandate amount)
        val referenceId: String
    )
    
    /**
     * Callback interface for mandate creation
     */
    interface MandateCallback {
        fun onSuccess(mandateId: String)
        fun onFailure(error: String)
        fun onCancelled()
    }
    
    /**
     * Callback interface for payment execution
     */
    interface PaymentCallback {
        fun onSuccess(transactionId: String)
        fun onFailure(error: String)
    }
    
    /**
     * Callback interface for status checking
     */
    interface StatusCallback {
        fun onStatusReceived(status: MandateStatus)
        fun onError(error: String)
    }
    
    /**
     * Callback interface for mandate revocation
     */
    interface RevocationCallback {
        fun onSuccess()
        fun onFailure(error: String)
    }
    
    /**
     * Mandate status enum
     */
    enum class MandateStatus {
        ACTIVE,
        PENDING,
        EXPIRED,
        REVOKED
    }
    
    /**
     * Environment enum for SDK configuration
     */
    enum class Environment {
        SANDBOX,
        PRODUCTION
    }
    
    // Legacy methods for backward compatibility
    /**
     * @deprecated Use UPIAutoPaySDKManager.startMandateCreation() instead
     */
    @Deprecated("Use UPIAutoPaySDKManager.startMandateCreation() instead")
    fun launchLoginScreen(context: Context) {
        UPIAutoPaySDKManager.launchSDK(context)
    }
    
    /**
     * @deprecated Use UPIAutoPaySDKManager.startMandateCreation() instead
     */
    @Deprecated("Use UPIAutoPaySDKManager.startMandateCreation() instead")
    fun launchDetailsScreen(
        context: Context,
        name: String? = null,
        accountNumber: String? = null,
        ifsc: String? = null,
        upiVpa: String? = null,
        txnId: String? = null,
        amount: String? = null
    ) {
        val accountDetails = AccountDetails(
            name = name ?: "",
            accountNumber = accountNumber ?: "",
            ifsc = ifsc ?: "",
            upiVpa = upiVpa ?: "",
            txnId = txnId ?: "",
            amount = amount ?: ""
        )
        UPIAutoPaySDKManager.launchSDKWithAccountDetails(context, accountDetails = accountDetails)
    }
}