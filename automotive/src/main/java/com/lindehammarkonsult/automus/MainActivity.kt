package com.lindehammarkonsult.automus

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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
        
        // Clear existing navigation buttons
        binding.topNavigation.removeAllViews()
        
        // Add navigation buttons programmatically
        val navItems = listOf(
            Triple(R.id.nav_library, "Library", LibraryFragment::class.java),
            Triple(R.id.nav_browse, "Browse", BrowseFragment::class.java),
            Triple(R.id.nav_search, "Search", SearchFragment::class.java)
        )
        
        // Create each pill-shaped button and add to the LinearLayout
        navItems.forEachIndexed { index, (id, title, fragmentClass) ->
            val button = createNavigationButton(title, index == 0)
            button.id = id
            
            // Set click listener
            button.setOnClickListener {
                // Update button states with animation
                for (i in 0 until binding.topNavigation.childCount) {
                    val navButton = binding.topNavigation.getChildAt(i)
                    val shouldBeSelected = (i == index)
                    
                    if (shouldBeSelected && !navButton.isSelected) {
                        // Apply scale-up animation when selecting
                        navButton.isSelected = true
                        android.animation.AnimatorInflater.loadAnimator(this, R.animator.nav_button_scale_up).apply {
                            setTarget(navButton)
                            start()
                        }
                    } else if (!shouldBeSelected && navButton.isSelected) {
                        // Apply scale-down animation when deselecting
                        navButton.isSelected = false
                        android.animation.AnimatorInflater.loadAnimator(this, R.animator.nav_button_scale_down).apply {
                            setTarget(navButton)
                            start()
                        }
                    }
                }
                
                // Load the fragment
                try {
                    val fragment = fragmentClass.getDeclaredConstructor().newInstance() as Fragment
                    loadFragment(fragment)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating fragment", e)
                }
            }
            
            // Add to the layout with proper spacing matching the design
            val layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.nav_button_margin)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            binding.topNavigation.addView(button, layoutParams)
        }
        
        // Select the first button by default
        binding.topNavigation.getChildAt(0).isSelected = true
        
        // Set up toolbar buttons
        binding.settingsButton.setOnClickListener {
            // TODO: Implement settings dialog
        }
        
        binding.profileButton.setOnClickListener {
            // TODO: Implement user profile or sign in dialog
        }
        
        // Make the Apple logo function as a home button
        findViewById<ImageView>(R.id.apple_music_logo).setOnClickListener {
            // Navigate to library screen and select the library tab
            loadFragment(LibraryFragment())
            updateSelectedNavigationItem(R.id.nav_library)
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fade_slide_in,
                R.anim.fade_slide_out
            )
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
     * Updates the selected button in the top navigation.
     * This method is used by fragments to update the navigation UI.
     */
    fun updateSelectedNavigationItem(itemId: Int) {
        // Find the button with the given ID and select it with animation
        for (i in 0 until binding.topNavigation.childCount) {
            val navButton = binding.topNavigation.getChildAt(i)
            val shouldBeSelected = (navButton.id == itemId)
            
            if (shouldBeSelected && !navButton.isSelected) {
                // Apply scale-up animation when selecting
                navButton.isSelected = true
                navButton.elevation = resources.getDimension(R.dimen.nav_button_elevation)
                navButton.translationZ = 1f
                android.animation.AnimatorInflater.loadAnimator(this, R.animator.nav_button_scale_up).apply {
                    setTarget(navButton)
                    start()
                }
            } else if (!shouldBeSelected && navButton.isSelected) {
                // Apply scale-down animation when deselecting
                navButton.isSelected = false
                navButton.elevation = 0f
                navButton.translationZ = 0f
                android.animation.AnimatorInflater.loadAnimator(this, R.animator.nav_button_scale_down).apply {
                    setTarget(navButton)
                    start()
                }
            }
        }
    }
    
    /**
     * Creates a pill-shaped navigation button matching Apple Music design
     */
    private fun createNavigationButton(text: String, isSelected: Boolean = false): android.widget.Button {
        return android.widget.Button(this).apply {
            this.text = text
            textSize = resources.getDimension(R.dimen.nav_button_text_size) / resources.displayMetrics.density
            isAllCaps = false
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            
            // Remove default button styling
            stateListAnimator = null
            
            // Set elevation only for selected state (in XML it's handled via layer-list)
            elevation = if (isSelected) resources.getDimension(R.dimen.nav_button_elevation) else 0f
            translationZ = if (isSelected) 1f else 0f
            
            // Set the background drawable 
            background = getDrawable(R.drawable.nav_button_background)
            
            // Set the text color using selector for smooth state transitions
            setTextColor(resources.getColorStateList(R.color.nav_button_text_color, theme))
            
            // Set padding to match design
            setPadding(
                resources.getDimensionPixelSize(R.dimen.nav_button_padding_horizontal),
                resources.getDimensionPixelSize(R.dimen.nav_button_padding_vertical),
                resources.getDimensionPixelSize(R.dimen.nav_button_padding_horizontal),
                resources.getDimensionPixelSize(R.dimen.nav_button_padding_vertical)
            )
            
            // Set minimum width to zero to prevent default button width
            minimumWidth = 0
            minimumHeight = resources.getDimensionPixelSize(R.dimen.nav_button_padding_vertical) * 2
            
            // Set initially selected state
            this.isSelected = isSelected
        }
    }
}