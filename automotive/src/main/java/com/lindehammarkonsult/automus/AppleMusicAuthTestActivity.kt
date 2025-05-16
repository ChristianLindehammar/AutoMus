package com.lindehammarkonsult.automus

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lindehammarkonsult.automus.shared.auth.AppleMusicAuthHelper

/**
 * Simple activity to test Apple Music authentication in isolation
 */
class AppleMusicAuthTestActivity : AppCompatActivity(), AppleMusicAuthHelper.AuthStateListener {

    private lateinit var authHelper: AppleMusicAuthHelper
    private lateinit var statusText: TextView
    private lateinit var authButton: Button
    private lateinit var logoutButton: Button
    
    companion object {
        private const val TAG = "AppleMusicAuthTest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_test)
        
        // Initialize UI components
        statusText = findViewById(R.id.status_text)
        authButton = findViewById(R.id.auth_button)
        logoutButton = findViewById(R.id.logout_button)
        
        // Initialize auth helper
        authHelper = AppleMusicAuthHelper(this)
        
        // Set up UI
        updateUI()
        
        // Set up click listeners
        authButton.setOnClickListener {
            if (authHelper.isAuthenticated()) {
                authHelper.showAuthenticationOptions(this)
            } else {
                authHelper.startLogin(this)
            }
        }
        
        logoutButton.setOnClickListener {
            if (authHelper.isAuthenticated()) {
                authHelper.logout()
                updateUI()
            }
        }
    }
    
    private fun updateUI() {
        val isAuthenticated = authHelper.isAuthenticated()
        
        statusText.text = if (isAuthenticated) {
            "Authenticated with Apple Music"
        } else {
            "Not authenticated with Apple Music"
        }
        
        authButton.text = if (isAuthenticated) {
            "Manage Account"
        } else {
            "Connect Apple Music"
        }
        
        logoutButton.isEnabled = isAuthenticated
    }
    
    override fun onAuthStateChanged(isAuthenticated: Boolean) {
        Log.d(TAG, "Auth state changed: isAuthenticated=$isAuthenticated")
        updateUI()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Let the auth helper handle the result
        if (authHelper.handleAuthResult(requestCode, resultCode, data, this)) {
            // Auth helper handled the result
            return
        }
    }
}
