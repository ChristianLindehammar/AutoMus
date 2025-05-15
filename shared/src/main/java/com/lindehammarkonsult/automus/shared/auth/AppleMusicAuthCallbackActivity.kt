package com.lindehammarkonsult.automus.shared.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

/**
 * Placeholder activity required for Apple Music SDK authentication.
 * 
 * The Apple Music SDK handles the OAuth flow internally and uses
 * startActivityForResult, so we don't need to implement the callback
 * logic manually anymore.
 * 
 * This activity should still be registered in the manifest with the
 * appropriate intent filter to handle authentication callbacks.
 */
class AppleMusicAuthCallbackActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AppleMusicAuthCallback"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "AppleMusicAuthCallbackActivity created, but not used with SDK authentication approach")
        finish() // Just finish immediately as we use the SDK directly
    }
}
