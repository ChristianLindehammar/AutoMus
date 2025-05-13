package com.lindehammarkonsult.automus.shared.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.apple.android.sdk.authentication.TokenProvider
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "AppleMusicTokenProvider"
private const val PREFS_NAME = "apple_music_auth_prefs"
private const val KEY_DEVELOPER_TOKEN = "developer_token"
private const val KEY_USER_TOKEN = "user_token"
private const val KEY_ALIAS = "apple_music_key"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_LENGTH = 128

/**
 * TokenProvider implementation for Apple Music that securely stores tokens
 * using SharedPreferences with manual encryption via AndroidKeyStore
 */
class AppleMusicTokenProvider(private val context: Context) : TokenProvider {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Creates or retrieves the encryption key from the Android KeyStore
     */
    private fun getOrCreateSecretKey(): SecretKey? {
        try {
            // Check if the key already exists
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            if (keyStore.containsAlias(KEY_ALIAS)) {
                return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            }
            
            // Generate a new key if it doesn't exist
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, 
                ANDROID_KEYSTORE
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
                
            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/retrieving key: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Encrypt data using the AndroidKeyStore key
     */
    private fun encrypt(plaintext: String): String? {
        try {
            val secretKey = getOrCreateSecretKey() ?: return null
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Decrypt data using the AndroidKeyStore key
     */
    private fun decrypt(encryptedData: String): String? {
        try {
            val secretKey = getOrCreateSecretKey() ?: return null
            
            val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
            
            // Extract IV from the beginning of the data
            val iv = ByteArray(GCM_IV_LENGTH)
            val ciphertext = ByteArray(decoded.size - GCM_IV_LENGTH)
            System.arraycopy(decoded, 0, iv, 0, iv.size)
            System.arraycopy(decoded, iv.size, ciphertext, 0, ciphertext.size)
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            
            val decryptedBytes = cipher.doFinal(ciphertext)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error: ${e.message}", e)
            return null
        }
    }

    /**
     * Store the developer token (should be called during app initialization)
     */
    fun setDeveloperToken(token: String?) {
        if (token != null) {
            val encrypted = encrypt(token)
            if (encrypted != null) {
                prefs.edit().putString(KEY_DEVELOPER_TOKEN, encrypted).apply()
                Log.d(TAG, "Developer token stored securely")
            } else {
                // Fallback to plaintext if encryption fails
                prefs.edit().putString(KEY_DEVELOPER_TOKEN, token).apply()
                Log.w(TAG, "Developer token stored in plaintext (encryption failed)")
            }
        } else {
            prefs.edit().remove(KEY_DEVELOPER_TOKEN).apply()
            Log.d(TAG, "Developer token removed")
        }
    }

    /**
     * Store the user token (called after successful authentication)
     */
    fun setUserToken(token: String?) {
        if (token != null) {
            val encrypted = encrypt(token)
            if (encrypted != null) {
                prefs.edit().putString(KEY_USER_TOKEN, encrypted).apply()
                Log.d(TAG, "User token stored securely")
            } else {
                // Fallback to plaintext if encryption fails
                prefs.edit().putString(KEY_USER_TOKEN, token).apply()
                Log.w(TAG, "User token stored in plaintext (encryption failed)")
            }
        } else {
            prefs.edit().remove(KEY_USER_TOKEN).apply()
            Log.d(TAG, "User token removed")
        }
    }

    /**
     * Implementation of TokenProvider interface method
     * Get the securely stored developer token
     */
    override fun getDeveloperToken(): String {
        val encrypted = prefs.getString(KEY_DEVELOPER_TOKEN, "") ?: ""
        if (encrypted.isEmpty()) return ""
        
        return decrypt(encrypted) ?: encrypted // Fallback to encrypted string if decryption fails
    }

    /**
     * Implementation of TokenProvider interface method
     * Get the securely stored user token
     */
    override fun getUserToken(): String? {
        val encrypted = prefs.getString(KEY_USER_TOKEN, null) ?: return null
        return decrypt(encrypted) ?: encrypted // Fallback to encrypted string if decryption fails
    }

    /**
     * Get nullable developer token - for use in initialization checks
     */
    fun getDeveloperTokenOrNull(): String? {
        val encrypted = prefs.getString(KEY_DEVELOPER_TOKEN, null) ?: return null
        return decrypt(encrypted) ?: encrypted // Fallback to encrypted string if decryption fails
    }

    /**
     * Legacy method for compatibility - delegates to getUserToken()
     */
    fun getMusicUserToken(): String? {
        return getUserToken()
    }

    /**
     * Clear the user authentication token
     */
    fun clearUserToken() {
        try {
            Log.d(TAG, "Clearing user token")
            prefs.edit().remove(KEY_USER_TOKEN).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear user token: ${e.message}", e)
        }
    }
}