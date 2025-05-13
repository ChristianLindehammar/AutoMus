package com.lindehammarkonsult.automus.ui

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.FragmentNowPlayingBinding
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment displaying the now playing screen with full-screen album art
 * and playback controls
 */
class NowPlayingFragment : Fragment() {
    
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MusicViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        
        setupObservers()
        setupClickListeners()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupObservers() {
        // Observe metadata changes to update UI
        viewModel.metadata.observe(viewLifecycleOwner) { metadata ->
            updateMetadataUI(metadata)
        }
        
        // Observe playback state to update controls
        viewModel.playbackState.observe(viewLifecycleOwner) { state ->
            updatePlaybackStateUI(state)
        }
    }
    
    private fun setupClickListeners() {
        // Set click listeners for the playback controls
        binding.apply {
            // Add click listener for the back/close button
            backButton.setOnClickListener {
                // Go back to previous fragment
                if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                    requireActivity().supportFragmentManager.popBackStack()
                } else {
                    // Fallback if for some reason we're not in the back stack
                    (requireActivity() as? MediaAwareActivity)?.let { mediaActivity ->
                        // Return to previously selected tab and show mini player
                        mediaActivity.hideMiniPlayer()
                        mediaActivity.showMiniPlayer()
                    }
                }
            }
            
            playPauseButton.setOnClickListener {
                val controller = MediaControllerCompat.getMediaController(requireActivity())
                val pbState = controller?.playbackState?.state
                
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
            
            skipPreviousButton.setOnClickListener {
                MediaControllerCompat.getMediaController(requireActivity())
                    ?.transportControls?.skipToPrevious()
            }
            
            skipNextButton.setOnClickListener {
                MediaControllerCompat.getMediaController(requireActivity())
                    ?.transportControls?.skipToNext()
            }
        }
    }
    
    private fun updateMetadataUI(metadata: MediaMetadataCompat?) {
        if (metadata == null) return
        
        binding.apply {
            songTitleText.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            artistText.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            albumText.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            
            // Load album art
            val albumArtUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            if (albumArtUri != null) {
                Glide.with(this@NowPlayingFragment)
                    .load(albumArtUri)
                    .placeholder(R.drawable.album_art_placeholder)
                    .error(R.drawable.album_art_placeholder)
                    .fitCenter() // Ensure image is fit properly within bounds
                    .transform(com.bumptech.glide.load.resource.bitmap.RoundedCorners(8)) // Add rounded corners
                    .into(albumArt)
            } else {
                albumArt.setImageResource(R.drawable.album_art_placeholder)
            }
        }
    }
    
    private fun updatePlaybackStateUI(state: PlaybackStateCompat?) {
        if (state == null) return
        
        binding.apply {
            val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
            playPauseButton.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            
            // Enable/disable transport controls based on available actions
            skipPreviousButton.isEnabled = 
                state.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L
                
            skipNextButton.isEnabled = 
                state.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L
                
            playPauseButton.isEnabled = 
                state.actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L
        }
    }
    
    companion object {
        fun newInstance() = NowPlayingFragment()
    }
}