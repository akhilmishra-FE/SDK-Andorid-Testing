package com.dec.andorid_autopay_demo_lib

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Main SDK Manager for UPI AutoPay functionality
 * This is the entry point for merchant apps to integrate the SDK
 * 
 * IMPORTANT: To receive results from the SDK, use registerForActivityResult in your calling app:
 * 
 * Example usage in your MainActivity:
 * ```
 * private val sdkLauncher = registerForActivityResult(
 *     ActivityResultContracts.StartActivityForResult()
 * ) { result ->
 *     if (result.resultCode == Activity.RESULT_OK) {
 *         val status = result.data?.getStringExtra("MANDATE_STATUS")
 *         val message = result.data?.getStringExtra("MESSAGE")
 *         // Handle success
 *     } else {
 *         val error = result.data?.getStringExtra("ERROR_MESSAGE")
 *         // Handle failure
 *     }
 * }
 * 
 * // Launch SDK
 * UPIAutoPaySDKManager.startMandateCreation(this, mandateDetails, sdkLauncher)
 * ```
 */
object UPIAutoPaySDKManager {
    
    // Store callback for returning results
    private var mandateCallback: UPIAutoPaySDK.MandateCallback? = null
    
    /**
     * Initialize the SDK with configuration
     */
    fun initialize(context: Context, config: UPIAutoPaySDK.Configuration) {
        // Store configuration for SDK use
        // This can be expanded to store config in SharedPreferences or singleton
    }
    
    /**
     * Start mandate creation flow
     * This is the main entry point for merchant apps
     */
    fun startMandateCreation(
        context: Context,
        mandateDetails: UPIAutoPaySDK.MandateDetails,
        callback: UPIAutoPaySDK.MandateCallback
    ) {
        // Store callback for later use
        mandateCallback = callback
        
        val intent = Intent(context, LoginActivity::class.java).apply {
            // Pass mandate details
            putExtra("AMOUNT", mandateDetails.amount)
            putExtra("RECURRENCE", mandateDetails.recurrence)
            putExtra("START_DATE", mandateDetails.startDate)
            putExtra("END_DATE", mandateDetails.endDate)
            putExtra("PURPOSE", mandateDetails.purpose)
            putExtra("MERCHANT_REF_ID", mandateDetails.merchantReferenceId)
            putExtra("CUSTOMER_VPA", mandateDetails.customerVpa)
            
            // Store merchant package for return navigation
            putExtra("MERCHANT_PACKAGE", context.packageName)
            
            // CRITICAL: Don't use NEW_TASK flag - stay in same task as merchant app
            // This ensures proper return navigation
        }
        
        // Launch as activity for result if context is Activity
        if (context is Activity) {
            context.startActivityForResult(intent, SDK_REQUEST_CODE)
        } else {
            // Fallback for non-Activity context
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Handle activity result from SDK
     * Call this from merchant app's onActivityResult
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SDK_REQUEST_CODE) {
            mandateCallback?.let { callback ->
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val mandateId = data?.getStringExtra("DECENTRO_MANDATE_ID") ?: ""
                        callback.onSuccess(mandateId)
                    }
                    Activity.RESULT_CANCELED -> {
                        val errorMessage = data?.getStringExtra("ERROR_MESSAGE") ?: "Operation cancelled"
                        callback.onFailure(errorMessage)
                    }
                    else -> {
                        callback.onCancelled()
                    }
                }
                // Clear callback after use
                mandateCallback = null
            }
        }
    }
    
    // Constants for SDK
    const val SDK_REQUEST_CODE = 1001
    
    /**
     * Launch the UPI AutoPay SDK from merchant app
     * 
     * @param context The calling activity context
     * @param merchantPackageName Package name of merchant app (for return navigation)
     * @param mobileNumber Mobile number for account lookup
     */
    fun launchSDK(
        context: Context,
        merchantPackageName: String? = null,
        mobileNumber: String? = null
    ) {
        val intent = Intent(context, LoginActivity::class.java).apply {
            // Pass merchant package name for return navigation
            merchantPackageName?.let { 
                putExtra("MERCHANT_PACKAGE", it) 
            }
            
            // Pre-fill mobile number if provided
            mobileNumber?.let { 
                putExtra("MOBILE_NUMBER", it) 
            }
            
            // Ensure SDK starts as new task
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        context.startActivity(intent)
    }
    
    /**
     * Launch SDK with account details pre-filled
     * 
     * @param context The calling activity context
     * @param merchantPackageName Package name of merchant app
     * @param accountDetails Pre-filled account information
     */
    fun launchSDKWithAccountDetails(
        context: Context,
        merchantPackageName: String? = null,
        accountDetails: AccountDetails
    ) {
        val intent = Intent(context, DetailsActivity::class.java).apply {
            // Pass merchant package name for return navigation
            merchantPackageName?.let { 
                putExtra("MERCHANT_PACKAGE", it) 
            }
            
            // Pre-fill account details
            putExtra("NAME", accountDetails.name)
            putExtra("ACCOUNT_NUMBER", accountDetails.accountNumber)
            putExtra("IFSC", accountDetails.ifsc)
            putExtra("UPI_VPA", accountDetails.upiVpa)
            putExtra("TXN_ID", accountDetails.txnId)
            putExtra("AMOUNT", accountDetails.amount)
            
            // Ensure SDK starts as new task
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        context.startActivity(intent)
    }
    
    /**
     * Check if UPI apps are available on device
     */
    fun isUPIAvailable(context: Context): Boolean {
        val upiIntent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("upi://pay")
        }
        
        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(upiIntent, 0)
        return activities.isNotEmpty()
    }
}

/**
 * Data class for account details
 */
data class AccountDetails(
    val name: String,
    val accountNumber: String,
    val ifsc: String,
    val upiVpa: String,
    val txnId: String,
    val amount: String
)
