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
                
                if (isPlaying) showMiniPlayer() else hideMiniPlayer()
                
                // Update play/pause button
                updatePlayPauseButton(it.state == PlaybackStateCompat.STATE_PLAYING)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.d(TAG, "onMetadataChanged: $metadata")
            viewModel.setMetadata(metadata)
            
            // Update mini player with new metadata
            updateMiniPlayerWithMetadata(metadata)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        
        // Set up top navigation
        setupTopNavigation()
        
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
    
    private fun setupTopNavigation() {
        // Default fragment
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            loadFragment(LibraryFragment())
        }
        
        // Create tabs programmatically
        binding.topNavigation.apply {
            // Remove any existing tabs first
            removeAllTabs()
            
            // Add Library tab
            addTab(newTab().setText("LIBRARY").setTag("nav_library"))
            
            // Add Browse tab
            addTab(newTab().setText("BROWSE").setTag("nav_browse"))
            
            // Add Search tab
            addTab(newTab().setText("SEARCH").setTag("nav_search"))
        }
        
        // Set up top navigation with tab layout
        binding.topNavigation.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadFragment(LibraryFragment())
                    1 -> loadFragment(BrowseFragment())
                    2 -> loadFragment(SearchFragment())
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // No action needed
            }
            
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Optionally refresh the selected fragment
            }
        })
        
        // Set up toolbar buttons
        binding.settingsButton.setOnClickListener {
            // TODO: Implement settings dialog
        }
        
        binding.profileButton.setOnClickListener {
            // TODO: Implement user profile or sign in dialog
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun setupMiniPlayerControls() {
        // Click listeners for the mini player
        binding.miniPlayer.root.setOnClickListener {
            // Open now playing screen
            showNowPlaying()
        }
        
        binding.miniPlayer.btnPlayPause.setOnClickListener {
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
        binding.miniPlayer.btnPlayPause.setImageResource(iconResource)
    }
    
    // MediaAwareActivity implementation
    override fun showMiniPlayer() {
        binding.miniPlayer.root.visibility = View.VISIBLE
    }
    
    override fun hideMiniPlayer() {
        binding.miniPlayer.root.visibility = View.GONE
    }
    
    override fun showNowPlaying() {
        loadFragment(NowPlayingFragment())
        // Don't select any navigation item as "Now Playing" is not part of bottom navigation
    }
    
    // Additional helper methods for handling media updates
    fun updateMiniPlayerWithMetadata(metadata: MediaMetadataCompat?) {
        metadata?.let {
            // Update song title and artist
            binding.miniPlayer.tvTrackTitle.text = it.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            binding.miniPlayer.tvArtist.text = it.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            
            // Load album art
            val artworkUri = it.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            if (artworkUri != null) {
                Glide.with(this)
                    .load(artworkUri)
                    .placeholder(R.drawable.album_art_placeholder)
                    .into(binding.miniPlayer.ivAlbumArt)
            } else {
                // Use default artwork
                binding.miniPlayer.ivAlbumArt.setImageResource(R.drawable.album_art_placeholder)
            }
            
            // Show mini player since we have active media
            showMiniPlayer()
            
            // Ensure fragment container is constrained to the mini player
            val params = binding.fragmentContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.bottomToTop = binding.miniPlayer.root.id
            binding.fragmentContainer.layoutParams = params
        }
    }
    
    /**
     * Updates the selected tab in the top navigation.
     * This method is used by fragments to update the navigation UI.
     */
    fun updateSelectedNavigationItem(itemId: Int) {
        // Convert itemId to tab position
        val tabPosition = when (itemId) {
            R.id.nav_library -> 0
            R.id.nav_browse -> 1
            R.id.nav_search -> 2
            else -> 0 // Default to library
        }
        
        // Make sure we're selecting a valid tab position
        if (tabPosition >= 0 && tabPosition < binding.topNavigation.tabCount) {
            binding.topNavigation.getTabAt(tabPosition)?.select()
        }
    }
}