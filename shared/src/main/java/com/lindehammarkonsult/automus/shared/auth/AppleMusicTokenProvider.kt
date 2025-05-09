package com.lindehammarkonsult.automus.shared.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.apple.android.sdk.authentication.TokenProvider
import java.io.IOException
import java.security.GeneralSecurityException

private const val TAG = "AppleMusicTokenProvider"
private const val PREFS_NAME = "apple_music_auth_prefs"
private const val KEY_DEVELOPER_TOKEN = "developer_token"
private const val KEY_USER_TOKEN = "user_token"

/**
 * TokenProvider implementation for Apple Music that securely stores tokens
 * using EncryptedSharedPreferences and provides them to the MusicKit
 */
class AppleMusicTokenProvider(private val context: Context) : TokenProvider {

    private val encryptedPrefs: SharedPreferences

    init {
        val masterKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKeyAlias,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Error creating encrypted shared preferences: ${e.message}", e)
            // Fallback to regular shared preferences if encryption fails
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: IOException) {
            Log.e(TAG, "I/O error creating encrypted shared preferences: ${e.message}", e)
            // Fallback to regular shared preferences if encryption fails
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Store the developer token (should be called during app initialization)
     */
    fun setDeveloperToken(token: String?) {
        if (token != null) {
            encryptedPrefs.edit().putString(KEY_DEVELOPER_TOKEN, token).apply()
            Log.d(TAG, "Developer token stored")
        } else {
            encryptedPrefs.edit().remove(KEY_DEVELOPER_TOKEN).apply()
            Log.d(TAG, "Developer token removed")
        }
    }

    /**
     * Store the user token (called after successful authentication)
     */
    fun setUserToken(token: String?) {
        if (token != null) {
            encryptedPrefs.edit().putString(KEY_USER_TOKEN, token).apply()
            Log.d(TAG, "User token stored")
        } else {
            encryptedPrefs.edit().remove(KEY_USER_TOKEN).apply()
            Log.d(TAG, "User token removed")
        }
    }

    /**
     * Implementation of TokenProvider interface method
     * Get the securely stored developer token
     */
    override fun getDeveloperToken(): String {
        return encryptedPrefs.getString(KEY_DEVELOPER_TOKEN, "") ?: ""
    }

    /**
     * Implementation of TokenProvider interface method
     * Get the securely stored user token
     */
    override fun getUserToken(): String? {
        return encryptedPrefs.getString(KEY_USER_TOKEN, null)
    }

    /**
     * Get nullable developer token - for use in initialization checks
     */
    fun getDeveloperTokenOrNull(): String? {
        return encryptedPrefs.getString(KEY_DEVELOPER_TOKEN, null)
    }

    /**
     * Legacy method for compatibility - delegates to getUserToken()
     */
    fun getMusicUserToken(): String? {
        return getUserToken()
    }
}