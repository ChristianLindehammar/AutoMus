package com.lindehammarkonsult.automus

import android.app.Application
import android.util.Log
import com.lindehammarkonsult.automus.shared.AppleMusicMediaService
import com.lindehammarkonsult.automus.shared.BuildConfig

/**
 * Application class for AutoMus that handles initialization of the Media3 service and Apple Music SDK
 */
class AutoMusApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Apple Music SDK components here
        try {
            // Prevent OOM false alarms for native libraries
            System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0")
            System.setProperty("org.bytedeco.javacpp.maxbytes", "0")
            
            // Load required native libraries
            System.loadLibrary("c++_shared")
            System.loadLibrary("appleMusicSDK")
            
            Log.d(TAG, "Successfully loaded Apple Music SDK native libraries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load native libraries: ${e.message}", e)
        }
        
        // Log token information (without revealing contents)
        val developerToken = BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN
        if (developerToken == "YOUR_APPLE_MUSIC_DEVELOPER_TOKEN_HERE" || developerToken.isBlank()) {
            Log.w(TAG, "Apple Music developer token is missing or using placeholder. App will run in limited mode.")
        } else {
            Log.d(TAG, "Apple Music developer token is configured (length: ${developerToken.length})")
        }
    }
    
    companion object {
        private const val TAG = "AutoMusApplication"
    }
}
