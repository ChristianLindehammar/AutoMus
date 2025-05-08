package com.lindehammarkonsult.automus.shared.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.apple.android.music.AuthenticationFactory
import com.apple.android.music.MusicAuthenticationManager
import com.apple.android.music.TokenError
import com.apple.android.music.TokenResult
import com.lindehammarkonsult.automus.shared.model.AppleMusicToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "AppleMusicAuthManager"

/**
 * Manager class for handling Apple Music authentication using the MusicKit SDK
 */
class AppleMusicAuthManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Token provider that will be used by media player
    val tokenProvider = AppleMusicTokenProvider(context)
    
    // Auth state
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState
    
    /**
     * Initialize with developer token
     */
    fun initialize(developerToken: String) {
        tokenProvider.setDeveloperToken(developerToken)
        
        // Check if we already have a user token
        if (tokenProvider.getMusicUserToken() != null) {
            _authState.value = AuthState.Authenticated
            Log.d(TAG, "Already authenticated with stored token")
        }
    }
    
    /**
     * Request user authentication
     */
    fun authenticate(activity: Activity) {
        _authState.value = AuthState.Authenticating
        
        try {
            // Create authentication instance
            val musicAuthenticator = AuthenticationFactory.createMusicAuthenticator()
            
            // Start authentication flow
            musicAuthenticator.authenticate(activity) { result ->
                when (result) {
                    is TokenResult.Success -> {
                        // Save the user token
                        tokenProvider.setUserToken(result.userToken)
                        _authState.value = AuthState.Authenticated
                        Log.d(TAG, "Authentication successful")
                    }
                    is TokenResult.Error -> {
                        val errorMsg = when (result.error) {
                            TokenError.NETWORK_ERROR -> "Network error during authentication"
                            TokenError.TOKEN_ERROR -> "Token error during authentication"
                            TokenError.AUTHENTICATION_ERROR -> "Authentication error"
                            TokenError.AUTHORIZATION_ERROR -> "Authorization error"
                            else -> "Unknown authentication error"
                        }
                        _authState.value = AuthState.Error(errorMsg)
                        Log.e(TAG, errorMsg)
                    }
                    is TokenResult.Canceled -> {
                        _authState.value = AuthState.Canceled
                        Log.d(TAG, "Authentication canceled by user")
                    }
                    else -> {
                        _authState.value = AuthState.Error("Unknown result")
                        Log.e(TAG, "Unknown authentication result type")
                    }
                }
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Authentication failed: ${e.message}")
            Log.e(TAG, "Authentication failed", e)
        }
    }
    
    /**
     * Check if the user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return tokenProvider.getMusicUserToken() != null
    }
    
    /**
     * Log out the user
     */
    fun logout() {
        // Clear tokens from storage
        tokenProvider.clearTokens()
        
        // Also clear MusicKit session state 
        try {
            // Note: There's no direct logout API in the SDK, but resetting the token is sufficient
            MusicAuthenticationManager.getInstance().resetDeveloperToken()
            Log.d(TAG, "Logged out user")
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: ${e.message}")
        }
        
        _authState.value = AuthState.Idle
    }
    
    /**
     * Authentication state
     */
    sealed class AuthState {
        object Idle : AuthState()
        object Authenticating : AuthState()
        object Authenticated : AuthState()
        data class Error(val message: String) : AuthState()
        object Canceled : AuthState()
    }
}