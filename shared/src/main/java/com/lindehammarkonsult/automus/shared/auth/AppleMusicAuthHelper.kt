package com.lindehammarkonsult.automus.shared.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lindehammarkonsult.automus.shared.BuildConfig
import com.lindehammarkonsult.automus.shared.ui.AppleMusicAuthActivity
import com.lindehammarkonsult.automus.shared.ui.AppleMusicQRAuthActivity

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
            // User is logged in - show account management options
            val options = arrayOf("Account Details", "Sign Out")
            
            AlertDialog.Builder(activity)
                .setTitle("Apple Music Account")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // Show account details
                            AlertDialog.Builder(activity)
                                .setTitle("Account Details")
                                .setMessage("You are signed in to Apple Music.\n\nYour library and playlists are available.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        1 -> {
                            // Log out confirmation
                            AlertDialog.Builder(activity)
                                .setTitle("Sign Out")
                                .setMessage("Are you sure you want to sign out from Apple Music? You'll need to sign in again to access your library and playlists.")
                                .setPositiveButton("Sign Out") { _, _ ->
                                    logout()
                                    Toast.makeText(context, "Signed out from Apple Music", Toast.LENGTH_SHORT).show()
                                    
                                    // Notify activity of logout
                                    if (activity is AuthStateListener) {
                                        (activity as AuthStateListener).onAuthStateChanged(false)
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Check for valid developer token
            if (BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN.isBlank() || 
                BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN == "YOUR_APPLE_MUSIC_DEVELOPER_TOKEN_HERE") {
                
                // Show a different message when token is missing
                AlertDialog.Builder(activity)
                    .setTitle("Apple Music Setup Required")
                    .setMessage("A developer token is required to use Apple Music. Please add your token to the app configuration.")
                    .setPositiveButton("OK", null)
                    .show()
                
                return
            }
            
            // User not logged in - show login option
            AlertDialog.Builder(activity)
                .setTitle("Apple Music")
                .setMessage("Sign in to Apple Music to access your library, playlists, and personalized recommendations.")
                .setPositiveButton("Sign In") { _, _ ->
                    startLogin(activity)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    /**
     * Interface to notify activities of authentication state changes
     */
    interface AuthStateListener {
        fun onAuthStateChanged(isAuthenticated: Boolean)
    }

    /**
     * Start the login flow directly
     */
    fun startLogin(activity: Activity) {
        try {
            // Check developer token first
            if (BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN.isBlank() || 
                BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN == "YOUR_APPLE_MUSIC_DEVELOPER_TOKEN_HERE") {
                Log.e(TAG, "Invalid developer token, cannot start authentication")
                Toast.makeText(context, "Developer token is missing, authentication will not work", Toast.LENGTH_LONG).show()
                return
            }
            
            // Use the simplified authentication activity for Android Automotive
            val intent = AppleMusicQRAuthActivity.createIntent(activity)
            activity.startActivityForResult(intent, REQUEST_APPLE_MUSIC_AUTH)
            Log.d(TAG, "Started Apple Music authentication activity")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting authentication: ${e.message}", e)
            Toast.makeText(context, "Error starting authentication: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle the result from the authentication activity
     * Call this from your activity's onActivityResult
     * 
     * @param requestCode The request code passed to startActivityForResult
     * @param resultCode The result code returned by the child activity
     * @param data The intent data returned by the child activity
     * @param listener Optional AuthStateListener to notify about auth state changes
     * @return true if the result was handled, false otherwise
     */
    fun handleAuthResult(
        requestCode: Int, 
        resultCode: Int, 
        data: Intent?,
        listener: AuthStateListener? = null
    ): Boolean {
        if (requestCode != REQUEST_APPLE_MUSIC_AUTH) return false
        
        try {
            when (resultCode) {
                AppleMusicQRAuthActivity.RESULT_AUTH_SUCCESS, Activity.RESULT_OK -> {
                    Log.d(TAG, "Apple Music authentication successful")
                    
                    // Check if this was a simulated authentication (for testing)
                    val isSimulated = data?.getBooleanExtra("is_simulated_auth", false) ?: false
                    if (isSimulated) {
                        Log.d(TAG, "This was a simulated authentication")
                    }
                    
                    // Verify that we actually have a valid token before continuing
                    if (isAuthenticated()) {
                        Toast.makeText(context, "Apple Music login successful", Toast.LENGTH_SHORT).show()
                        // Notify the provided listener about authentication success
                        try {
                            listener?.onAuthStateChanged(true)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in auth state change listener", e)
                        }
                    } else {
                        Log.w(TAG, "Authentication reported as successful but no valid token found")
                        Toast.makeText(context, "Authentication issue: No valid token", Toast.LENGTH_LONG).show()
                        
                        // For testing purposes, we might still want to notify success
                        if (isSimulated) {
                            Log.d(TAG, "Proceeding with simulated auth despite token validation failure")
                            try {
                                listener?.onAuthStateChanged(true)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in auth state change listener for simulated auth", e)
                            }
                        }
                    }
                }
                AppleMusicQRAuthActivity.RESULT_AUTH_CANCELED, Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "Apple Music authentication was canceled")
                    Toast.makeText(context, "Apple Music login canceled", Toast.LENGTH_SHORT).show()
                }
                AppleMusicQRAuthActivity.RESULT_AUTH_ERROR -> {
                    val errorMessage = data?.getStringExtra(AppleMusicQRAuthActivity.EXTRA_AUTH_RESULT)
                    Log.e(TAG, "Apple Music authentication error: $errorMessage")
                    Toast.makeText(context, "Login error: $errorMessage", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Log.w(TAG, "Unexpected result code: $resultCode")
                    Toast.makeText(context, "Unexpected authentication result", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling auth result", e)
            Toast.makeText(context, "Error handling authentication result", Toast.LENGTH_SHORT).show()
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
