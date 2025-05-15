package com.lindehammarkonsult.automus.shared.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.apple.android.sdk.authentication.AuthenticationFactory
import com.apple.android.sdk.authentication.AuthenticationManager
import com.apple.android.sdk.authentication.TokenResult

private const val TAG = "AppleMusicAuthManager"
private const val REQUEST_CODE_APPLE_MUSIC_AUTH = 3456

/**
 * Manager class for handling Apple Music authentication flow using the official Apple SDK
 */
class AppleMusicAuthManager(
    private val context: Context,
    private val developerToken: String,
    private val clientId: String = "", // Not needed with the SDK approach
    private val clientSecret: String = "" // Not needed with the SDK approach
) {
    /**
     * Authentication state representation
     */
    sealed class AuthState {
        object Idle : AuthState()
        object Authenticating : AuthState()
        object Authenticated : AuthState()
        object Canceled : AuthState()
        class Error(val message: String) : AuthState()
    }
    
    // Token provider to store and provide tokens
    val tokenProvider = AppleMusicTokenProvider(context)
    
    // Apple's Authentication Manager instance
    private val authenticationManager: AuthenticationManager by lazy {
        AuthenticationFactory.createAuthenticationManager(context)
    }
    
    // LiveData for auth state
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState
    
    init {
        // The token provider now automatically initializes with the BuildConfig token
        // We don't need to explicitly set the developer token here
        
        // Register token provider with MusicKit
        // Note: The Apple Music SDK has been configured to use this token provider elsewhere
    }
    
    /**
     * Initialize the auth manager and load any cached tokens
     * This is called when the app starts up to prepare the auth state
     */
    fun initialize() {
        Log.d(TAG, "Initializing Apple Music Auth Manager")
        
        // Validate that a developer token is available (either from prefs or BuildConfig)
        if (!tokenProvider.hasDeveloperToken()) {
            Log.w(TAG, "Developer token is missing or invalid")
            _authState.value = AuthState.Error("Developer token is missing or invalid")
            return
        }
            
        // Attempt to restore any previously saved user token
        val userToken = tokenProvider.getUserToken()
        if (!userToken.isNullOrEmpty()) {
            Log.d(TAG, "Restored previous user authentication")
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Idle
        }
    }
    
    /**
     * Initiate the authentication flow for Apple Music using the Apple SDK
     */
    fun authenticate(activity: Activity) {
        Log.d(TAG, "Starting authentication from activity using Apple SDK")
        _authState.value = AuthState.Authenticating
        
        try {
            // Create the authentication intent using the Apple SDK's AuthenticationManager
            val intent = authenticationManager.createIntentBuilder(developerToken)
                .setHideStartScreen(false) // Show the Apple Music authentication start screen
                .setStartScreenMessage("Connect AutoMus to your Apple Music account")
                .build()
            
            // Start the authentication activity
            activity.startActivityForResult(intent, REQUEST_CODE_APPLE_MUSIC_AUTH)
            Log.d(TAG, "Started Apple Music authentication activity")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Apple Music authentication: ${e.message}", e)
            _authState.postValue(AuthState.Error("Failed to start authentication: ${e.message}"))
        }
    }
    
    /**
     * Handle the authentication result from the Apple SDK
     * Call this from your activity's onActivityResult
     */
    fun handleAuthResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_CODE_APPLE_MUSIC_AUTH) return false
        
        if (data != null) {
            val tokenResult = authenticationManager.handleTokenResult(data)
            if (!tokenResult.isError) {
                // Authentication successful
                val musicUserToken = tokenResult.musicUserToken
                
                // Store the user token securely
                tokenProvider.setUserToken(musicUserToken)
                
                Log.d(TAG, "Authentication successful, received music user token")
                _authState.value = AuthState.Authenticated
                return true
            } else {
                // Authentication failed
                val error = tokenResult.error
                Log.e(TAG, "Authentication error: ${error.name}")
                _authState.value = AuthState.Error("Authentication failed: ${error.name}")
                return false
            }
        } else {
            // User canceled authentication
            Log.d(TAG, "Authentication was canceled or failed (no data returned)")
            _authState.value = AuthState.Canceled
            return false
        }
    }
    
    /**
     * Check if user is currently authenticated with Apple Music
     */
    fun isAuthenticated(): Boolean {
        // Use getUserToken() to avoid overload resolution ambiguity
        val userToken: String? = tokenProvider.getUserToken()
        return !userToken.isNullOrEmpty()
    }
    
    /**
     * Logout the current user
     */
    fun logout() {
        tokenProvider.setUserToken(null)
        Log.d(TAG, "User logged out")
        _authState.value = AuthState.Idle
    }
    
    /**
     * Clear the authentication token and reset the auth state
     */
    fun clearToken() {
        Log.d(TAG, "Clearing authentication token")
        tokenProvider.clearUserToken()
        _authState.value = AuthState.Idle
    }
}