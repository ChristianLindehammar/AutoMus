package com.lindehammarkonsult.automus.shared.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.apple.android.music.TokenProvider

private const val TAG = "AppleMusicTokenProvider"
private const val PREF_FILE_NAME = "apple_music_tokens"
private const val KEY_DEVELOPER_TOKEN = "developer_token"
private const val KEY_USER_TOKEN = "user_token"

/**
 * Implementation of Apple Music TokenProvider interface for managing authentication tokens
 * This class securely stores and provides the tokens needed by the MusicKit SDK
 */
class AppleMusicTokenProvider(context: Context) : TokenProvider {

    // Use encrypted shared preferences for secure token storage
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Set the developer token provided by Apple
     */
    fun setDeveloperToken(token: String) {
        encryptedPrefs.edit().putString(KEY_DEVELOPER_TOKEN, token).apply()
        Log.d(TAG, "Developer token saved")
    }
    
    /**
     * Set the user token received after successful authentication
     */
    fun setUserToken(token: String?) {
        if (token != null) {
            encryptedPrefs.edit().putString(KEY_USER_TOKEN, token).apply()
            Log.d(TAG, "User token saved")
        } else {
            encryptedPrefs.edit().remove(KEY_USER_TOKEN).apply()
            Log.d(TAG, "User token removed")
        }
    }
    
    /**
     * Clear all saved tokens
     */
    fun clearTokens() {
        encryptedPrefs.edit()
            .remove(KEY_USER_TOKEN)
            .apply()
        Log.d(TAG, "Tokens cleared")
    }
    
    //
    // TokenProvider interface implementation
    //
    
    override fun getDeveloperToken(): String? {
        return encryptedPrefs.getString(KEY_DEVELOPER_TOKEN, null)
    }

    override fun getMusicUserToken(): String? {
        return encryptedPrefs.getString(KEY_USER_TOKEN, null)
    }

    override fun getUsersStoreFrontCountryCode(): String? {
        // This could be fetched from user preferences or API
        // For now we'll return null to let the SDK determine it
        return null
    }

    override fun getUsersStoreFrontIdentifier(): String? {
        // Similarly, return null to let the SDK determine it
        return null
    }
}