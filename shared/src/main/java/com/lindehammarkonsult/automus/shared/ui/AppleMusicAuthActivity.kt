package com.lindehammarkonsult.automus.shared.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.Observer
import com.lindehammarkonsult.automus.shared.BuildConfig
import com.lindehammarkonsult.automus.shared.R
import com.lindehammarkonsult.automus.shared.auth.AppleMusicAuthManager
import com.lindehammarkonsult.automus.shared.auth.AppleMusicAuthManager.AuthState

/**
 * Activity for Apple Music authentication using the native MusicKit SDK
 * This can be launched from either mobile or automotive app to handle auth
 */
class AppleMusicAuthActivity : ComponentActivity() {

    private lateinit var authManager: AppleMusicAuthManager
    private lateinit var loginButton: Button
    private lateinit var cancelButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "AppleMusicAuthActivity"
        
        const val EXTRA_AUTH_RESULT = "auth_result"
        const val RESULT_AUTH_SUCCESS = Activity.RESULT_FIRST_USER + 1
        const val RESULT_AUTH_CANCELED = Activity.RESULT_FIRST_USER + 2
        const val RESULT_AUTH_ERROR = Activity.RESULT_FIRST_USER + 3
        
        /**
         * Create an intent to launch this activity
         */
        fun createIntent(activity: Activity): Intent {
            return Intent(activity, AppleMusicAuthActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apple_music_auth)
        
        // Initialize UI components
        loginButton = findViewById(R.id.btn_login)
        cancelButton = findViewById(R.id.btn_cancel)
        statusText = findViewById(R.id.text_status)
        progressBar = findViewById(R.id.progress_bar)
        
        // Initialize auth manager with required parameters
        authManager = AppleMusicAuthManager(
            context = this,
            developerToken = BuildConfig.APPLE_MUSIC_DEVELOPER_TOKEN,
            clientId = BuildConfig.APPLE_MUSIC_CLIENT_ID,
            clientSecret = BuildConfig.APPLE_MUSIC_CLIENT_SECRET
        )
        
        // Set up login button
        loginButton.setOnClickListener {
            startAuthentication()
        }
        
        // Set up cancel button
        cancelButton.setOnClickListener {
            finishWithCanceled()
        }
        
        // Observe auth state changes
        authManager.authState.observe(this, Observer { state ->
            updateUI(state)
        })
        
        // Check if already authenticated
        if (authManager.isAuthenticated()) {
            finishWithSuccess()
        }
    }
    
    /**
     * Start Apple Music authentication flow
     */
    private fun startAuthentication() {
        try {
            authManager.authenticate(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting authentication: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finishWithError("Authentication error: ${e.message}")
        }
    }
    
    /**
     * Update the UI based on authentication state
     */
    private fun updateUI(state: AuthState) {
        when (state) {
            is AuthState.Idle -> {
                loginButton.isEnabled = true
                cancelButton.isEnabled = true
                statusText.text = "Please sign in to Apple Music"
                progressBar.visibility = View.INVISIBLE
            }
            is AuthState.Authenticating -> {
                loginButton.isEnabled = false
                cancelButton.isEnabled = false
                statusText.text = "Authenticating with Apple Music..."
                progressBar.visibility = View.VISIBLE
            }
            is AuthState.Authenticated -> {
                loginButton.isEnabled = false
                cancelButton.isEnabled = false
                statusText.text = "Authentication successful!"
                progressBar.visibility = View.INVISIBLE
                finishWithSuccess()
            }
            is AuthState.Error -> {
                loginButton.isEnabled = true
                cancelButton.isEnabled = true
                statusText.text = "Error: ${state.message}"
                progressBar.visibility = View.INVISIBLE
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
            is AuthState.Canceled -> {
                loginButton.isEnabled = true
                cancelButton.isEnabled = true
                statusText.text = "Authentication canceled"
                progressBar.visibility = View.INVISIBLE
            }
        }
    }
    
    /**
     * Finish activity with success result
     */
    private fun finishWithSuccess() {
        val resultIntent = Intent()
        setResult(RESULT_AUTH_SUCCESS, resultIntent)
        finish()
    }
    
    /**
     * Finish activity with error result
     */
    private fun finishWithError(error: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_AUTH_RESULT, error)
        }
        setResult(RESULT_AUTH_ERROR, resultIntent)
        
        // Don't finish immediately to allow retry
        // User needs to manually exit or retry
    }
    
    /**
     * Finish activity with canceled result
     */
    private fun finishWithCanceled() {
        setResult(RESULT_AUTH_CANCELED)
        finish()
    }
}