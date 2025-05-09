package com.lindehammarkonsult.automus.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.lindehammarkonsult.automus.databinding.FragmentLibraryBinding
import com.lindehammarkonsult.automus.ui.adapters.MediaCategoryAdapter
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment displaying the user's music library
 */
class LibraryFragment : Fragment() {
    
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MusicViewModel
    private lateinit var mediaAdapter: MediaCategoryAdapter
    
    private var mediaAwareActivity: MediaAwareActivity? = null
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MediaAwareActivity) {
            mediaAwareActivity = context
        } else {
            throw IllegalStateException("Host activity must implement MediaAwareActivity")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        
        setupRecyclerView()
        setupObservers()
        
        // Initial load of library categories
        loadLibraryCategories()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupRecyclerView() {
        mediaAdapter = MediaCategoryAdapter { mediaItem ->
            handleMediaItemClick(mediaItem)
        }
        
        binding.libraryRecyclerView.apply {
            adapter = mediaAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }
    
    private fun setupObservers() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe media items
        viewModel.mediaItems.observe(viewLifecycleOwner) { items ->
            if (items.isNullOrEmpty()) {
                binding.emptyStateText.visibility = View.VISIBLE
                binding.libraryRecyclerView.visibility = View.GONE
            } else {
                binding.emptyStateText.visibility = View.GONE
                binding.libraryRecyclerView.visibility = View.VISIBLE
                mediaAdapter.submitList(items)
            }
        }
        
        // Observe playback state to show/hide mini player
        viewModel.playbackState.observe(viewLifecycleOwner) { state ->
            if (state != null) {
                mediaAwareActivity?.showMiniPlayer()
            } else {
                mediaAwareActivity?.hideMiniPlayer()
            }
        }
    }
    
    private fun loadLibraryCategories() {
        viewModel.setLoading(true)
        
        // This would normally connect to a MediaBrowser service
        // For now, we'll create some dummy data
        val dummyCategories = listOf(
            createMediaItem("library_artists", "Artists", "Browse your favorite artists"),
            createMediaItem("library_albums", "Albums", "Browse your albums"),
            createMediaItem("library_playlists", "Playlists", "Browse your playlists"),
            createMediaItem("library_songs", "Songs", "Browse all songs")
        )
        
        viewModel.setMediaItems(dummyCategories)
        viewModel.setLoading(false)
    }
    
    private fun handleMediaItemClick(mediaItem: MediaBrowserCompat.MediaItem) {
        if (mediaItem.isBrowsable) {
            // Navigate to the browsable content
            // In a real implementation, we would browse the media item children
            // and update the UI with the new content
        } else if (mediaItem.isPlayable) {
            // Play the media
            val mediaController = MediaControllerCompat.getMediaController(requireActivity())
            mediaController?.transportControls?.playFromMediaId(mediaItem.mediaId, null)
            
            // Show the mini player
            mediaAwareActivity?.showMiniPlayer()
        }
    }
    
    private fun createMediaItem(
        mediaId: String,
        title: String,
        subtitle: String,
        browsable: Boolean = true,
        playable: Boolean = false
    ): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build()
            
        val flags = if (browsable) {
            MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        } else {
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        }
        
        return MediaBrowserCompat.MediaItem(description, flags)
    }
    
    companion object {
        fun newInstance() = LibraryFragment()
    }
}