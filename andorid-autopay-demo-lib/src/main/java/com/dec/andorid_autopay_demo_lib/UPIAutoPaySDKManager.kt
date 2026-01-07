package com.dec.andorid_autopay_demo_lib

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Main SDK Manager for UPI AutoPay functionality
 * This is the entry point for merchant apps to integrate the SDK
 */
object UPIAutoPaySDKManager {
    
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
