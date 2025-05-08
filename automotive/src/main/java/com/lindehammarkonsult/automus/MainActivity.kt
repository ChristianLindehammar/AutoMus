package com.lindehammarkonsult.automus

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.lindehammarkonsult.automus.databinding.ActivityMainBinding
import com.lindehammarkonsult.automus.ui.BrowseFragment
import com.lindehammarkonsult.automus.ui.LibraryFragment
import com.lindehammarkonsult.automus.ui.MediaAwareActivity
import com.lindehammarkonsult.automus.ui.NowPlayingFragment
import com.lindehammarkonsult.automus.ui.SearchFragment
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel
import com.lindehammarkonsult.automus.shared.AppleMusicService

private const val TAG = "MainActivity"
private const val SERVICE_PACKAGE_NAME = "com.lindehammarkonsult.automus"

/**
 * Main activity for the Apple Music Android Automotive application
 */
class MainActivity : AppCompatActivity(), MediaAwareActivity {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MusicViewModel
    
    private var mediaBrowser: MediaBrowserCompat? = null
    
    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.d(TAG, "onConnected")
            mediaBrowser?.let {
                val mediaController = MediaControllerCompat(
                    this@MainActivity,
                    it.sessionToken
                )
                MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
                
                // Register controller callbacks to update UI
                mediaController.registerCallback(controllerCallback)
                
                // Update connection state in ViewModel
                viewModel.setIsConnected(true)
            }
        }

        override fun onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended")
            viewModel.setIsConnected(false)
        }

        override fun onConnectionFailed() {
            Log.e(TAG, "onConnectionFailed")
            viewModel.setIsConnected(false)
        }
    }
    
    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Log.d(TAG, "onPlaybackStateChanged: $state")
            viewModel.setPlaybackState(state)
            
            // Update mini player visibility based on playback state
            state?.let {
                val isPlaying = it.state == PlaybackStateCompat.STATE_PLAYING ||
                        it.state == PlaybackStateCompat.STATE_BUFFERING ||
                        it.state == PlaybackStateCompat.STATE_PAUSED
                
                showMiniPlayer(isPlaying)
                
                // Update play/pause button
                updatePlayPauseButton(it.state == PlaybackStateCompat.STATE_PLAYING)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.d(TAG, "onMetadataChanged: $metadata")
            viewModel.setMetadata(metadata)
            
            // Update mini player with new metadata
            updateMiniPlayer(metadata)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Set up media browser connection
        mediaBrowser = MediaBrowserCompat(
            this,
            android.content.ComponentName(
                SERVICE_PACKAGE_NAME,
                AppleMusicService::class.java.name
            ),
            connectionCallback,
            null
        )
        
        // Setup mini player click listeners
        setupMiniPlayerControls()
    }

    override fun onStart() {
        super.onStart()
        mediaBrowser?.connect()
    }

    override fun onStop() {
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
        mediaBrowser?.disconnect()
        super.onStop()
    }
    
    private fun setupBottomNavigation() {
        // Default fragment
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            loadFragment(LibraryFragment())
        }
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_library -> {
                    loadFragment(LibraryFragment())
                    true
                }
                R.id.nav_browse -> {
                    loadFragment(BrowseFragment())
                    true
                }
                R.id.nav_search -> {
                    loadFragment(SearchFragment())
                    true
                }
                R.id.nav_now_playing -> {
                    loadFragment(NowPlayingFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun setupMiniPlayerControls() {
        // Click listeners for the mini player
        binding.nowPlayingMini.root.setOnClickListener {
            // Open now playing screen
            loadFragment(NowPlayingFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_now_playing
        }
        
        binding.nowPlayingMini.miniPlayPause.setOnClickListener {
            val controller = MediaControllerCompat.getMediaController(this)
            val pbState = controller?.playbackState?.state
            
            if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        }
    }
    
    private fun updatePlayPauseButton(isPlaying: Boolean) {
        val iconResource = if (isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        binding.nowPlayingMini.miniPlayPause.setImageResource(iconResource)
    }
    
    // MediaAwareActivity implementation
    override fun updateMiniPlayer(metadata: MediaMetadataCompat?) {
        metadata?.let {
            // Update song title and artist
            binding.nowPlayingMini.miniSongTitle.text = it.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            binding.nowPlayingMini.miniArtistName.text = it.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            
            // Load album art
            val artworkUri = it.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            if (artworkUri != null) {
                Glide.with(this)
                    .load(artworkUri)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(binding.nowPlayingMini.miniAlbumArt)
            } else {
                // Use default artwork
                binding.nowPlayingMini.miniAlbumArt.setImageResource(R.drawable.ic_launcher_background)
            }
            
            // Show mini player since we have active media
            showMiniPlayer(true)
        }
    }
    
    override fun showMiniPlayer(show: Boolean) {
        binding.nowPlayingMini.root.visibility = if (show) View.VISIBLE else View.GONE
    }
}