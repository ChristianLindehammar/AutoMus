package com.lindehammarkonsult.automus.mobile

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.lindehammarkonsult.automus.mobile.databinding.ActivityMainBinding
import com.lindehammarkonsult.automus.shared.AppleMusicService
import com.lindehammarkonsult.automus.shared.ui.AppleMusicAuthActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaBrowser: MediaBrowserCompat
    
    // Register for auth activity result
    private val appleAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            AppleMusicAuthActivity.RESULT_AUTH_SUCCESS -> {
                Toast.makeText(this, "Successfully authenticated with Apple Music", Toast.LENGTH_SHORT).show()
                // Reconnect to media browser to refresh content
                if (!mediaBrowser.isConnected) {
                    mediaBrowser.connect()
                }
            }
            AppleMusicAuthActivity.RESULT_AUTH_CANCELED -> {
                Toast.makeText(this, "Authentication canceled", Toast.LENGTH_SHORT).show()
            }
            AppleMusicAuthActivity.RESULT_AUTH_ERROR -> {
                val error = result.data?.getStringExtra(AppleMusicAuthActivity.EXTRA_AUTH_RESULT)
                Toast.makeText(this, "Authentication error: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        
        // Set up media browser
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, AppleMusicService::class.java),
            ConnectionCallback(),
            null
        )
        
        // Set up action buttons
        setupButtons()
    }
    
    override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }
    
    override fun onStop() {
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(mediaControllerCallback)
        mediaBrowser.disconnect()
        super.onStop()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_login -> {
                launchAppleMusicAuth()
                true
            }
            R.id.action_logout -> {
                logoutFromAppleMusic()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupButtons() {
        // Set up playback control buttons
        binding.content.btnPlay.setOnClickListener {
            val controller = MediaControllerCompat.getMediaController(this)
            if (controller?.playbackState?.isPlaying == true) {
                controller.transportControls.pause()
            } else {
                controller?.transportControls?.play()
            }
        }
        
        binding.content.btnNext.setOnClickListener {
            MediaControllerCompat.getMediaController(this)?.transportControls?.skipToNext()
        }
        
        binding.content.btnPrevious.setOnClickListener {
            MediaControllerCompat.getMediaController(this)?.transportControls?.skipToPrevious()
        }
    }
    
    /**
     * Launch Apple Music authentication activity
     */
    private fun launchAppleMusicAuth() {
        val intent = AppleMusicAuthActivity.createIntent(this)
        appleAuthLauncher.launch(intent)
    }
    
    /**
     * Logout from Apple Music
     */
    private fun logoutFromAppleMusic() {
        val controller = MediaControllerCompat.getMediaController(this)
        if (controller != null) {
            // Use custom action for logout
            val bundle = Bundle()
            controller.sendCommand(
                AppleMusicService.ACTION_LOGOUT, 
                bundle,
                object : MediaControllerCompat.CommandCallback() {
                    override fun onResult(resultCode: Int, result: Bundle?) {
                        val success = result?.getBoolean("success", false) ?: false
                        if (success) {
                            Toast.makeText(this@MainActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to logout", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
    
    /**
     * Media browser connection callback
     */
    private inner class ConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.d(TAG, "onConnected: connected to media browser service")
            
            val mediaController = MediaControllerCompat(
                this@MainActivity,
                mediaBrowser.sessionToken
            )
            MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
            
            // Register controller callback
            mediaController.registerCallback(mediaControllerCallback)
            
            // Update UI based on current state
            updatePlaybackState(mediaController.playbackState.isPlaying)
        }
        
        override fun onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended: connection suspended")
        }
        
        override fun onConnectionFailed() {
            Log.e(TAG, "onConnectionFailed: connection failed")
        }
    }
    
    /**
     * Media controller callback to receive updates
     */
    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: android.support.v4.media.session.PlaybackStateCompat) {
            updatePlaybackState(state.isPlaying)
        }
        
        override fun onMetadataChanged(metadata: android.support.v4.media.MediaMetadataCompat) {
            // Update UI with track info
            val title = metadata.getString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE)
            val artist = metadata.getString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST)
            updateTrackInfo(title, artist)
        }
    }
    
    /**
     * Update playback state UI
     */
    private fun updatePlaybackState(isPlaying: Boolean) {
        binding.content.btnPlay.text = if (isPlaying) "Pause" else "Play"
    }
    
    /**
     * Update track information UI
     */
    private fun updateTrackInfo(title: String?, artist: String?) {
        binding.content.textTrackTitle.text = title ?: "No track playing"
        binding.content.textTrackArtist.text = artist ?: ""
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}