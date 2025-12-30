package com.dec.andorid_autopay_demo_lib

import android.content.Context
import android.content.Intent

/**
 * Public SDK entry point for UPIAutoPay SDK
 * External applications can use this class to launch the SDK screens
 */
object UPIAutoPaySDK {
    
    /**
     * Launches the login screen where users can enter their mobile number
     * and fetch their bank account details
     * 
     * @param context The application context
     */
    fun launchLoginScreen(context: Context) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    /**
     * Launches the details screen with account information
     * 
     * @param context The application context
     * @param name Account holder name
     * @param accountNumber Bank account number
     * @param ifsc IFSC code
     * @param upiVpa UPI VPA
     * @param txnId Transaction ID
     * @param amount Transaction amount
     */
    fun launchDetailsScreen(
        context: Context,
        name: String? = null,
        accountNumber: String? = null,
        ifsc: String? = null,
        upiVpa: String? = null,
        txnId: String? = null,
        amount: String? = null
    ) {
        val intent = Intent(context, DetailsActivity::class.java).apply {
            putExtra("NAME", name)
            putExtra("ACCOUNT_NUMBER", accountNumber)
            putExtra("IFSC", ifsc)
            putExtra("UPI_VPA", upiVpa)
            putExtra("TXN_ID", txnId)
            putExtra("AMOUNT", amount)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

