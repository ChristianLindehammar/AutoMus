package com.lindehammarkonsult.automus.ui

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.FragmentLibraryBinding
import com.lindehammarkonsult.automus.ui.adapters.MediaCategoryAdapter
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment for browsing different music categories
 */
class BrowseFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MusicViewModel
    private lateinit var adapter: MediaCategoryAdapter
    
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
        
        // Set up title
        binding.libraryTitle.text = getString(R.string.browse_title)
        
        setupRecyclerView()
        setupObservers()
        
        // Check if we're connected to the service
        if (viewModel.isConnected.value == true) {
            loadBrowseCategories()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupRecyclerView() {
        adapter = MediaCategoryAdapter { mediaItem ->
            // Handle item click
            onMediaItemClicked(mediaItem)
        }
        
        binding.libraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BrowseFragment.adapter
        }
    }
    
    private fun setupObservers() {
        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                loadBrowseCategories()
            } else {
                showEmptyState(true)
            }
        }
        
        viewModel.mediaItems.observe(viewLifecycleOwner) { items ->
            binding.progressBar.visibility = View.GONE
            
            if (items.isNotEmpty()) {
                showEmptyState(false)
                adapter.submitList(items)
            } else {
                showEmptyState(true)
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
    
    private fun loadBrowseCategories() {
        viewModel.setLoading(true)
        
        val parentMediaId = "browse_categories"
        
        // Get MediaBrowser from MediaController
        val mediaBrowser = MediaControllerCompat.getMediaController(requireActivity())
        
        // Request children of this browse ID
        mediaBrowser?.transportControls?.sendCustomAction(
            "GET_CHILDREN",
            Bundle().apply {
                putString("parent_id", parentMediaId)
            }
        )
        
        // Register a callback for results using MediaController
        mediaBrowser?.registerCallback(object : MediaControllerCompat.Callback() {
            override fun onSessionEvent(event: String?, extras: Bundle?) {
                super.onSessionEvent(event, extras)
                if (event == "CHILDREN_LOADED") {
                    extras?.let { bundle ->
                        val parentId = bundle.getString("parent_id")
                        if (parentId == parentMediaId) {
                            val mediaItems = bundle.getParcelableArrayList<MediaBrowserCompat.MediaItem>("media_items")
                            if (mediaItems != null) {
                                viewModel.setMediaItems(mediaItems)
                            } else {
                                viewModel.setMediaItems(emptyList())
                            }
                            viewModel.setLoading(false)
                            mediaBrowser.unregisterCallback(this)
                        }
                    }
                }
            }
        })
    }
    
    private fun onMediaItemClicked(mediaItem: MediaBrowserCompat.MediaItem) {
        if (mediaItem.isBrowsable) {
            browseMediaItem(mediaItem)
        } else if (mediaItem.isPlayable) {
            playMediaItem(mediaItem)
        }
    }
    
    private fun browseMediaItem(mediaItem: MediaBrowserCompat.MediaItem) {
        // Navigate to a detail fragment for the selected item
        val mediaId = mediaItem.mediaId ?: return
        
        // Navigate to the appropriate detail fragment based on the media ID pattern
        val fragment = when {
            mediaId.startsWith("playlist_") -> {
                PlaylistDetailFragment.newInstance(mediaId)
            }
            mediaId.startsWith("album_") -> {
                AlbumDetailFragment.newInstance(mediaId)
            }
            mediaId.startsWith("genre_") -> {
                GenreDetailFragment.newInstance(mediaId)
            }
            // For categories within the browse section
            mediaId.startsWith("category_") -> {
                CategoryDetailFragment.newInstance(mediaId)
            }
            else -> null
        }
        
        fragment?.let {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, it)
                .addToBackStack(null)
                .commit()
        }
    }
    
    private fun playMediaItem(mediaItem: MediaBrowserCompat.MediaItem) {
        val mediaController = MediaControllerCompat.getMediaController(requireActivity())
        val transportControls = mediaController?.transportControls
        
        transportControls?.playFromMediaId(mediaItem.mediaId, null)
    }
    
    private fun showEmptyState(show: Boolean) {
        binding.libraryRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        binding.emptyStateText.visibility = if (show) View.VISIBLE else View.GONE
        binding.signInButton.visibility = if (show) View.VISIBLE else View.GONE
        
        // Custom message for browse section
        binding.emptyStateText.text = getString(R.string.browse_empty_state)
    }
    
    companion object {
        fun newInstance() = BrowseFragment()
    }
}