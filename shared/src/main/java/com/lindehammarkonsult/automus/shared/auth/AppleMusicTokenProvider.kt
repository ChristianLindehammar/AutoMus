package com.lindehammarkonsult.automus.shared.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.apple.android.music.MusicKit
import com.apple.android.music.TokenProvider
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
     * Get the securely stored developer token
     */
    fun getDeveloperToken(): String? {
        return encryptedPrefs.getString(KEY_DEVELOPER_TOKEN, null)
    }

    /**
     * Get the securely stored user token
     */
    fun getMusicUserToken(): String? {
        return encryptedPrefs.getString(KEY_USER_TOKEN, null)
    }

    // TokenProvider interface implementation

    override fun getDeveloperToken(callback: TokenProvider.TokenProviderCallback?) {
        val token = getDeveloperToken()
        if (token != null) {
            callback?.onSuccess(token)
            Log.d(TAG, "Developer token provided to MusicKit")
        } else {
            callback?.onError("Developer token not available")
            Log.e(TAG, "Developer token requested but not available")
        }
    }

    override fun getMusicUserToken(callback: TokenProvider.TokenProviderCallback?) {
        val token = getMusicUserToken()
        if (token != null) {
            callback?.onSuccess(token)
            Log.d(TAG, "User token provided to MusicKit")
        } else {
            callback?.onError("User token not available")
            Log.e(TAG, "User token requested but not available")
        }
    }
}