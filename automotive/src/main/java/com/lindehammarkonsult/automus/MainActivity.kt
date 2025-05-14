package com.lindehammarkonsult.automus

import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.lindehammarkonsult.automus.databinding.ActivityMainBinding
import com.lindehammarkonsult.automus.ui.BrowseFragment
import com.lindehammarkonsult.automus.ui.LibraryFragment
import com.lindehammarkonsult.automus.ui.MediaAwareActivity
import com.lindehammarkonsult.automus.ui.NowPlayingFragment
import com.lindehammarkonsult.automus.ui.SearchFragment
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

private const val TAG = "MainActivity"

/**
 * Main activity for the Apple Music Android Automotive application
 */
@UnstableApi
class MainActivity : AppCompatActivity(), MediaAwareActivity {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MusicViewModel
    
    // Legacy media browser (will be removed once migration is complete)
    private var mediaBrowser: MediaBrowserCompat? = null
    
    // Legacy connection callback
    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.d(TAG, "Legacy MediaBrowser onConnected")
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
        
        // Initialize ViewModel - Media3 version
        viewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        
        // Ensure side navigation is visible first
        findViewById<View>(R.id.sideNavigationContainer)?.visibility = View.VISIBLE
        
        // Set up side navigation
        setupSideNavigation()

        // Setup mini player click listeners
        setupMiniPlayerControls()
        
        // Double check visibility after setup
        findViewById<View>(R.id.sideNavigationContainer)?.visibility = View.VISIBLE
        
        // Update the settings and profile button actions from side navigation
        findViewById<View>(R.id.sideNavigationContainer)?.let { sideNavContainer ->
            sideNavContainer.findViewById<ImageButton>(R.id.settings_button)?.setOnClickListener {
                // TODO: Implement settings dialog
                Log.d(TAG, "Settings button clicked")
            }
            
            // Changed from ImageButton to ImageView to match the XML layout
            sideNavContainer.findViewById<ImageView>(R.id.profile_button)?.setOnClickListener {
                // TODO: Implement user profile dialog
                Log.d(TAG, "Profile button clicked")
            }
        }
    }

    private fun setupSideNavigation() {
        // Default fragment
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            loadFragment(LibraryFragment())
        }
        
        // Get reference to the side navigation container
        val sideNavContainer = findViewById<View>(R.id.sideNavigationContainer)
        if (sideNavContainer == null) {
            Log.e(TAG, "Side navigation container not found!")
            return
        }
        
        // Ensure side navigation container is visible
        sideNavContainer.visibility = View.VISIBLE
        
        // Setup navigation item click listeners
        setupNavigationItems(sideNavContainer)
        
        // Update the settings and voice button actions
        sideNavContainer.findViewById<ImageButton>(R.id.settings_button)?.setOnClickListener {
            // TODO: Implement settings dialog
            Log.d(TAG, "Settings button clicked")
        }
        
        sideNavContainer.findViewById<ImageButton>(R.id.voice_button)?.setOnClickListener {
            // TODO: Implement voice search
            Log.d(TAG, "Voice search button clicked")
        }
        
        sideNavContainer.findViewById<ImageView>(R.id.profile_button)?.setOnClickListener {
            // TODO: Implement user profile dialog
            Log.d(TAG, "Profile button clicked")
        }
        
        // Select the Library item by default
        updateSelectedNavigationItem(R.id.nav_library)
    }

    private fun setupNavigationItems(container: View) {
        // Library navigation
        container.findViewById<LinearLayout>(R.id.nav_library)?.setOnClickListener {
            loadFragment(LibraryFragment())
            updateSelectedNavigationItem(R.id.nav_library)
        }
        
        // Browse navigation
        container.findViewById<LinearLayout>(R.id.nav_browse)?.setOnClickListener {
            loadFragment(BrowseFragment())
            updateSelectedNavigationItem(R.id.nav_browse)
        }
        
        // Search navigation
        container.findViewById<LinearLayout>(R.id.nav_search)?.setOnClickListener {
            loadFragment(SearchFragment())
            updateSelectedNavigationItem(R.id.nav_search)
        }
    }
    
    /**
     * Updates the selected state of navigation items
     */
    fun updateSelectedNavigationItem(itemId: Int) {
        val sideNavContainer = findViewById<View>(R.id.sideNavigationContainer) ?: return
        
        val navItems = mapOf(
            R.id.nav_library to sideNavContainer.findViewById<LinearLayout>(R.id.nav_library),
            R.id.nav_browse to sideNavContainer.findViewById<LinearLayout>(R.id.nav_browse),
            R.id.nav_search to sideNavContainer.findViewById<LinearLayout>(R.id.nav_search)
        )
        
        // Update each navigation item
        navItems.forEach { (id, layout) ->
            val isSelected = id == itemId
            updateNavigationItemAppearance(layout, isSelected)
        }
    }
    
    /**
     * Updates the appearance of a navigation item based on selection state
     */
    private fun updateNavigationItemAppearance(layout: LinearLayout?, isSelected: Boolean) {
        layout?.let {
            // Find child views
            val iconView = layout.getChildAt(0) as? ImageView
            val textView = layout.getChildAt(1) as? TextView
            
            if (isSelected) {
                // Selected state
                layout.setBackgroundColor(ContextCompat.getColor(this, R.color.navigation_selected_bg))
                iconView?.setColorFilter(ContextCompat.getColor(this, R.color.navigation_selected_tint))
                textView?.setTextColor(ContextCompat.getColor(this, R.color.navigation_selected_text))
            } else {
                // Unselected state
                layout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                iconView?.setColorFilter(ContextCompat.getColor(this, R.color.navigation_unselected_tint))
                textView?.setTextColor(ContextCompat.getColor(this, R.color.navigation_unselected_text))
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_slide_in,
                R.anim.fade_slide_out
            )
            .replace(R.id.fragmentContainer, fragment)
        
        // Add to back stack only if it's the now playing fragment
        if (fragment is NowPlayingFragment) {
            transaction.addToBackStack("nowPlaying")
        }
        
        transaction.commit()
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
        
        // When showing mini player, ensure side navigation is visible
        findViewById<View>(R.id.sideNavigationContainer)?.visibility = View.VISIBLE
        
        // Adjust fragment container constraints
        val params = binding.fragmentContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.bottomToTop = binding.miniPlayer.root.id
        binding.fragmentContainer.layoutParams = params
    }
    
    override fun hideMiniPlayer() {
        binding.miniPlayer.root.visibility = View.GONE
    }
    
    override fun showNowPlaying() {
        // Hide mini player before showing the full player
        hideMiniPlayer()
        
        // Hide side navigation when showing now playing
        findViewById<View>(R.id.sideNavigationContainer)?.visibility = View.GONE
        
        // Adjust fragment container to use full width
        val fragmentParams = binding.fragmentContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        fragmentParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        binding.fragmentContainer.layoutParams = fragmentParams
        
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
                    .error(R.drawable.album_art_placeholder)
                    .centerCrop() // For mini player, centerCrop is appropriate as we want to fill the small square
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
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Check if current fragment is NowPlayingFragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        
        if (currentFragment is NowPlayingFragment) {
            // If in now playing screen, show the mini player and restore side navigation
            showMiniPlayer()
            val sideNavContainer = findViewById<View>(R.id.sideNavigationContainer) ?: return
            sideNavContainer.visibility = View.VISIBLE
            
            // Restore original fragment container constraints
            val fragmentParams = binding.fragmentContainer.layoutParams as ConstraintLayout.LayoutParams
            fragmentParams.startToStart = ConstraintLayout.LayoutParams.UNSET
            fragmentParams.startToEnd = sideNavContainer.id
            binding.fragmentContainer.layoutParams = fragmentParams
            
            supportFragmentManager.popBackStack()
        } else if (supportFragmentManager.backStackEntryCount > 0) {
            // If there are entries in back stack, pop them
            supportFragmentManager.popBackStack()
        } else {
            // Otherwise, perform default back behavior
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}