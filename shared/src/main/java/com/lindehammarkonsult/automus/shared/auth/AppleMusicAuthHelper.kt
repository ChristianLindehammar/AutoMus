package com.lindehammarkonsult.automus.shared.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lindehammarkonsult.automus.shared.BuildConfig
import com.lindehammarkonsult.automus.shared.ui.AppleMusicAuthActivity

/**
 * Helper class to manage Apple Music authentication flows
 * from any activity in the application
 */
class AppleMusicAuthHelper(private val context: Context) {

    companion object {
        private const val TAG = "AppleMusicAuthHelper"
        const val REQUEST_APPLE_MUSIC_AUTH = 1001
    }

    // Lazily initialize authentication manager
    private val authManager: AppleMusicAuthManager by lazy {
        AppleMusicAuthManager(
            context = context,
            developerToken = BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN,
            clientId = BuildConfig.APPLE_MUSIC_CLIENT_ID
        )
    }

    /**
     * Check if the user is authenticated with Apple Music
     */
    fun isAuthenticated(): Boolean {
        if (BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN == "YOUR_APPLE_MUSIC_DEVELOPER_TOKEN_HERE" ||
            BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN.isBlank()) {
            Log.w(TAG, "Developer token is missing, authentication will not work")
            return false
        }
        
        return authManager.isAuthenticated()
    }
    
    /**
     * Show login dialog or logout options based on current authentication state
     */
    fun showAuthenticationOptions(activity: Activity) {
        if (isAuthenticated()) {
            // User is logged in - show logout option
            AlertDialog.Builder(activity)
                .setTitle("Apple Music")
                .setMessage("You are currently logged in to Apple Music. Do you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    logout()
                    Toast.makeText(context, "Logged out from Apple Music", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // User not logged in - show login option
            AlertDialog.Builder(activity)
                .setTitle("Apple Music")
                .setMessage("You need to log in to Apple Music to access your library and playlists.")
                .setPositiveButton("Login") { _, _ ->
                    startLogin(activity)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    /**
     * Start the login flow directly
     */
    fun startLogin(activity: Activity) {
        try {
            val intent = AppleMusicAuthActivity.createIntent(activity)
            activity.startActivityForResult(intent, REQUEST_APPLE_MUSIC_AUTH)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting authentication: ${e.message}", e)
            Toast.makeText(context, "Error starting authentication: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle the result from the authentication activity
     * Call this from your activity's onActivityResult
     */
    fun handleAuthResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_APPLE_MUSIC_AUTH) return false
        
        when (resultCode) {
            AppleMusicAuthActivity.RESULT_AUTH_SUCCESS -> {
                Log.d(TAG, "Apple Music authentication successful")
                Toast.makeText(context, "Apple Music login successful", Toast.LENGTH_SHORT).show()
            }
            AppleMusicAuthActivity.RESULT_AUTH_CANCELED -> {
                Log.d(TAG, "Apple Music authentication was canceled")
                Toast.makeText(context, "Apple Music login canceled", Toast.LENGTH_SHORT).show()
            }
            AppleMusicAuthActivity.RESULT_AUTH_ERROR -> {
                val errorMessage = data?.getStringExtra(AppleMusicAuthActivity.EXTRA_AUTH_RESULT)
                Log.e(TAG, "Apple Music authentication error: $errorMessage")
                Toast.makeText(context, "Login error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
        
        return true
    }
    
    /**
     * Initialize the auth manager
     * This should be called during application startup
     */
    fun initialize() {
        authManager.initialize()
        Log.d(TAG, "Apple Music authentication initialized, is authenticated: ${isAuthenticated()}")
    }
    
    /**
     * Log out the current user
     */
    fun logout() {
        authManager.logout()
    }
    
    /**
     * Access to the authentication manager for internal components
     * This is a property, not a function to avoid JVM signature conflicts
     */
    val authManagerInstance: AppleMusicAuthManager
        get() = authManager
}
