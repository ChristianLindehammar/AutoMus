package com.lindehammarkonsult.automus.shared.auth

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val TAG = "AppleMusicAuthManager"
private const val APPLE_AUTH_URL = "https://authorize.music.apple.com/oauth"
private const val APPLE_TOKEN_URL = "https://api.music.apple.com/oauth/token"
private const val REDIRECT_URI = "automus://oauth-callback"

/**
 * Manager class for handling Apple Music authentication flow
 */
class AppleMusicAuthManager(
    private val context: Context,
    private val developerToken: String,
    private val clientId: String,
    private val clientSecret: String
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
    
    // LiveData for auth state
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState
    
    init {
        // Set the developer token in the token provider
        tokenProvider.setDeveloperToken(developerToken)
        
        // Register token provider with MusicKit
        // Note: The Apple Music SDK has been configured to use this token provider elsewhere
    }
    
    /**
     * Initialize the auth manager and load any cached tokens
     * This is called when the app starts up to prepare the auth state
     */
    fun initialize() {
        Log.d(TAG, "Initializing Apple Music Auth Manager")
        // Validate developer token
        if (developerToken.isEmpty() || developerToken == "YOUR_APPLE_MUSIC_DEVELOPER_TOKEN_HERE") {
            Log.w(TAG, "Developer token is missing or invalid")
            _authState.value = AuthState.Error("Developer token is missing or invalid")
        } else {
            // Set the developer token in the token provider (again to ensure it's set)
            tokenProvider.setDeveloperToken(developerToken)
            
            // Attempt to restore any previously saved user token
            val userToken = tokenProvider.getUserToken()
            if (!userToken.isNullOrEmpty()) {
                Log.d(TAG, "Restored previous user authentication")
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }
    
    /**
     * Initiate the OAuth authentication flow for Apple Music from an Activity
     */
    fun authenticate(activity: Activity) {
        Log.d(TAG, "Starting authentication from activity")
        _authState.value = AuthState.Authenticating
        
        startAuthFlow { success ->
            // This callback will be triggered after the authorization flow completes
            if (success) {
                Log.d(TAG, "Authentication completed successfully")
                _authState.postValue(AuthState.Authenticated)
            } else {
                Log.e(TAG, "Authentication failed")
                _authState.postValue(AuthState.Error("Authentication failed"))
            }
        }
    }
    
    /**
     * Initiate the OAuth authentication flow for Apple Music
     */
    fun startAuthFlow(callback: (Boolean) -> Unit) {
        try {
            // Construct OAuth authorization URL
            val authUrl = Uri.parse(APPLE_AUTH_URL).buildUpon()
                .appendQueryParameter("app_id", context.packageName)
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "musicKit")
                .build()
                
            // Launch Custom Tab for authentication
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                
            customTabsIntent.launchUrl(context, authUrl)
            
            // Note: The auth callback must be handled by an Activity that catches the redirect URI
            // See AppleMusicAuthCallbackActivity for implementation
            Log.d(TAG, "Authentication flow started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting authentication flow: ${e.message}", e)
            callback(false)
        }
    }
    
    /**
     * Handle the authorization code received from OAuth redirect
     * This should be called from the redirect Activity
     */
    suspend fun handleAuthorizationCode(code: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Processing authorization code")
                
                // Exchange authorization code for user token
                val url = URL(APPLE_TOKEN_URL)
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.setRequestProperty("Authorization", "Bearer $developerToken")
                connection.doOutput = true
                
                // Prepare request body
                val requestBody = "grant_type=authorization_code" +
                                 "&code=$code" +
                                 "&client_id=$clientId" +
                                 "&client_secret=$clientSecret" +
                                 "&redirect_uri=$REDIRECT_URI"
                                 
                // Write request body
                val outputStream = connection.outputStream
                outputStream.write(requestBody.toByteArray())
                outputStream.flush()
                outputStream.close()
                
                val responseCode = connection.responseCode
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    // Parse response
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    
                    val userToken = jsonResponse.getString("music_user_token")
                    val expiresIn = jsonResponse.getInt("expires_in")
                    
                    // Store the user token securely
                    tokenProvider.userToken = userToken
                    
                    Log.d(TAG, "Authentication successful, token expires in $expiresIn seconds")
                    
                    // The token provider is already registered with appropriate components
                    true
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "Authentication failed: $responseCode, $errorResponse")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exchanging authorization code: ${e.message}", e)
                false
            }
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
        tokenProvider.userToken = null
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