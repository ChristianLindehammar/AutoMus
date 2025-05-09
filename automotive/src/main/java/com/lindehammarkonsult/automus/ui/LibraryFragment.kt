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
import com.lindehammarkonsult.automus.MainActivity
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.FragmentLibraryBinding
import com.lindehammarkonsult.automus.ui.adapters.PlaylistAdapter
import com.lindehammarkonsult.automus.ui.adapters.LikedSongsAdapter
import com.lindehammarkonsult.automus.ui.adapters.RecentlyPlayedAdapter
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment displaying the user\'s music library
 */
class LibraryFragment : Fragment() {
    
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MusicViewModel
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var likedSongsAdapter: LikedSongsAdapter
    private lateinit var recentlyPlayedAdapter: RecentlyPlayedAdapter
    
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
        
        // Setup tabs navigation
        binding.libraryTabs.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { /* Already in Library, do nothing */ }
                    1 -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, BrowseFragment())
                            .commit()
                        (activity as? MainActivity)?.updateSelectedNavigationItem(R.id.nav_browse)
                    }
                    2 -> {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, SearchFragment())
                            .commit()
                        (activity as? MainActivity)?.updateSelectedNavigationItem(R.id.nav_search)
                    }
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) { }
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) { }
        })
        
        setupRecyclerViews() // Renamed from setupRecyclerView
        setupObservers()
        
        // Initial load of library categories - this will need to be updated
        loadAllLibraryData() // Renamed and will be adjusted
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupRecyclerViews() { // Renamed and modified
        // Playlist RecyclerView
        playlistAdapter = PlaylistAdapter { mediaItem ->
            handleMediaItemClick(mediaItem)
        }
        binding.playlistsRecyclerView.apply {
            adapter = playlistAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            // addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL)) // Optional: if you want dividers
        }

        // Liked Songs RecyclerView
        // Update LikedSongsAdapter instantiation and provide the onLikeClick callback
        likedSongsAdapter = LikedSongsAdapter(
            onItemClick = { mediaItem ->
                handleMediaItemClick(mediaItem)
            },
            onLikeClick = { mediaItem -> // Changed: Lambda now only takes mediaItem
                // Call ViewModel to handle like/unlike
                viewModel.toggleLikeStatus(mediaItem) // Changed: Call with only mediaItem
            }
        )
        binding.likedSongsRecyclerView.apply {
            adapter = likedSongsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        // Recently Played RecyclerView
        // Update RecentlyPlayedAdapter instantiation
        recentlyPlayedAdapter = RecentlyPlayedAdapter { mediaItem ->
            handleMediaItemClick(mediaItem)
        }
        binding.recentlyPlayedRecyclerView.apply {
            adapter = recentlyPlayedAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            // addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL)) // Optional: if you want dividers
        }
    }
    
    private fun setupObservers() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoadingValue -> // isLoadingValue might be Any?
            val isActuallyLoading = isLoadingValue as? Boolean // Safe cast to Boolean?

            binding.progressBar.visibility = if (isActuallyLoading == true) View.VISIBLE else View.GONE
            
            // Show/hide main content based on loading state and data availability
            if (isActuallyLoading == true) { // Check against true
                binding.contentScrollView.visibility = View.GONE
                binding.emptyStateText.visibility = View.GONE
            }
            // Note: updateOverallVisibility() is called by other observers, not directly here.
            // If it were, it would need to be after the content/empty text visibility is potentially set by this block.
        }
        
        // Observe Playlists
        viewModel.playlists.observe(viewLifecycleOwner) { items -> 
             if (items.isNullOrEmpty()) {
                // Handle empty state for playlists if needed, e.g., hide the section or show a message
                binding.playlistsRecyclerView.visibility = View.GONE
                binding.playlistsTitle.visibility = View.GONE
            } else {
                binding.playlistsRecyclerView.visibility = View.VISIBLE
                binding.playlistsTitle.visibility = View.VISIBLE
                playlistAdapter.submitList(items) // Submit playlist items
            }
            updateOverallVisibility()
        }

        // Observe Liked Songs
        viewModel.likedSongs.observe(viewLifecycleOwner) { items -> 
            if (items.isNullOrEmpty()) {
                // Handle empty state for liked songs
                binding.likedSongsRecyclerView.visibility = View.GONE
                binding.likedSongsTitle.visibility = View.GONE
            } else {
                binding.likedSongsRecyclerView.visibility = View.VISIBLE
                binding.likedSongsTitle.visibility = View.VISIBLE
                // Ensure the adapter is notified of data changes correctly,
                // especially if the list instance itself doesn't change but its content does.
                // submitList handles this well.
                likedSongsAdapter.submitList(items.toList()) // Use toList() to ensure a new list is submitted if items is mutable
            }
            updateOverallVisibility()
        }
        
        // Observe Recently Played
        viewModel.recentlyPlayedItems.observe(viewLifecycleOwner) { items -> 
            if (items.isNullOrEmpty()) {
                // Handle empty state for recently played
                binding.recentlyPlayedRecyclerView.visibility = View.GONE
                binding.recentlyPlayedTitle.visibility = View.GONE
            } else {
                binding.recentlyPlayedRecyclerView.visibility = View.VISIBLE
                binding.recentlyPlayedTitle.visibility = View.VISIBLE
                // Ensure the adapter is notified of data changes correctly
                recentlyPlayedAdapter.submitList(items.toList()) // Use toList() for consistency
            }
            updateOverallVisibility()
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

    private fun updateOverallVisibility() {
        val currentIsLoadingRaw = viewModel.isLoading.value // This could be Any?
        val currentIsLoading = currentIsLoadingRaw as? Boolean // Safe cast to Boolean?

        // If actively loading (currentIsLoading is true), the isLoading observer handles progress bar 
        // and hides contentScrollView/emptyStateText. This function should only act when not loading.
        if (currentIsLoading == true) {
            // It's important that the isLoading observer has already set
            // contentScrollView and emptyStateText to GONE.
            return 
        }

        // Not actively loading (currentIsLoading is false or null)
        val playlistsVisible = binding.playlistsRecyclerView.visibility == View.VISIBLE
        val likedSongsVisible = binding.likedSongsRecyclerView.visibility == View.VISIBLE
        val recentlyPlayedVisible = binding.recentlyPlayedRecyclerView.visibility == View.VISIBLE

        val anySectionDataVisible = playlistsVisible || likedSongsVisible || recentlyPlayedVisible

        if (anySectionDataVisible) {
            // If not loading AND at least one section has content and is visible
            binding.emptyStateText.visibility = View.GONE
            binding.contentScrollView.visibility = View.VISIBLE
        } else {
            // If not loading AND no sections have content / are visible
            binding.emptyStateText.visibility = View.VISIBLE
            binding.contentScrollView.visibility = View.GONE
        }
    }
    
    private fun loadAllLibraryData() { // Renamed and needs actual implementation
        // viewModel.setLoading(true) // setLoading is now handled by fetchAllLibraryData in ViewModel
        
        // This would normally connect to a MediaBrowser service or repository
        // to fetch playlists, liked songs, and recently played items.
        // For now, we call the new method in the ViewModel that simulates this.
        viewModel.fetchAllLibraryData()
        
        // viewModel.setLoading(false) // setLoading is now handled by fetchAllLibraryData in ViewModel
    }
    
    private fun handleMediaItemClick(mediaItem: MediaBrowserCompat.MediaItem) {
        val mediaController = MediaControllerCompat.getMediaController(requireActivity())
        if (mediaItem.isPlayable) {
            mediaController?.transportControls?.playFromMediaId(mediaItem.mediaId, null)
            mediaAwareActivity?.showMiniPlayer()
        } else if (mediaItem.isBrowsable) {
            // Navigate to a new fragment or activity to browse this item's content
            // For example, if it's an album or playlist
            // This might involve creating a new BrowseFragment instance with the mediaId
            val browseFragment = BrowseFragment.newInstance(mediaItem.mediaId!!)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, browseFragment)
                .addToBackStack(null) // Add to back stack so user can navigate back
                .commit()
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