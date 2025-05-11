package com.lindehammarkonsult.automus.ui

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.FragmentBrowseBinding
import com.lindehammarkonsult.automus.model.FeaturedPlaylist
import com.lindehammarkonsult.automus.model.GenreCategory
import com.lindehammarkonsult.automus.ui.adapters.FeaturedPlaylistAdapter
import com.lindehammarkonsult.automus.ui.adapters.GenreCategoryAdapter
import com.lindehammarkonsult.automus.ui.adapters.LikedSongsAdapter
import com.lindehammarkonsult.automus.ui.adapters.RecentlyPlayedAdapter
import com.lindehammarkonsult.automus.ui.adapters.PlaylistAdapter
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment for browsing different music categories
 */
class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MusicViewModel
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var likedSongsAdapter: LikedSongsAdapter
    private lateinit var recentlyPlayedAdapter: RecentlyPlayedAdapter
    private var browseMediaId: String? = null // To store the mediaId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            browseMediaId = it.getString(ARG_MEDIA_ID)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        
        setupRecyclerViews()
        setupObservers()
        
        // Load browse content
        if (browseMediaId != null) {
            // TODO: Implement logic to load content for the specific browseMediaId
            loadBasedOnMediaId(browseMediaId!!)
        } else {
            if (viewModel.isConnected.value == true) {
                loadBrowseContent()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupRecyclerViews() {
        // Playlists adapter and recycler view
        playlistAdapter = PlaylistAdapter { mediaItem ->
            handleMediaItemClick(mediaItem)
        }
        
        binding.playlistsRecyclerView.apply {
            adapter = playlistAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        
        // Liked Songs adapter and recycler view
        likedSongsAdapter = LikedSongsAdapter(
            onItemClick = { mediaItem ->
                handleMediaItemClick(mediaItem)
            },
            onLikeClick = { mediaItem ->
                // Handle like/unlike action
                // For now, just log or show a toast that it was clicked
                // In a real implementation, you'd update the like status in your data source
            }
        )
        
        binding.likedSongsRecyclerView.apply {
            adapter = likedSongsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        
        // Recently played adapter and recycler view
        recentlyPlayedAdapter = RecentlyPlayedAdapter { mediaItem ->
            handleMediaItemClick(mediaItem)
        }
        
        binding.recentlyPlayedRecyclerView.apply {
            adapter = recentlyPlayedAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }
    
    private fun setupObservers() {
        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected && browseMediaId == null) {
                loadBrowseContent()
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            
            // Show/hide empty state based on loading and content availability
            updateEmptyState()
        }
    }
    
    private fun updateEmptyState() {
        val hasContent = playlistAdapter.itemCount > 0 || 
                         likedSongsAdapter.itemCount > 0 || 
                         recentlyPlayedAdapter.itemCount > 0
                         
        val isLoading = viewModel.isLoading.value == true
        
        if (!isLoading && !hasContent) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.signInButton.visibility = View.VISIBLE
            binding.contentScrollView.visibility = View.GONE
            
            // Set up sign in button click listener
            binding.signInButton.setOnClickListener {
                // TODO: Implement actual sign-in logic
                // For demo purposes, just load some sample content
                loadBrowseContent()
            }
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.signInButton.visibility = View.GONE
            binding.contentScrollView.visibility = View.VISIBLE
        }
    }
    
    private fun loadBasedOnMediaId(mediaId: String) {
        viewModel.setLoading(true)
        
        // Clear existing adapters
        playlistAdapter.submitList(emptyList())
        likedSongsAdapter.submitList(emptyList())
        recentlyPlayedAdapter.submitList(emptyList())
        
        // TODO: Implement actual media browsing logic here
        // For now we'll show the empty state
        binding.emptyStateText.text = "Browsing content for $mediaId"
        viewModel.setLoading(false)
        updateEmptyState()

        // Example: Trigger a load in ViewModel
        // viewModel.browseByMediaId(mediaId)
        // Then observe the results via LiveData
    }
    
    private fun loadBrowseContent() {
        viewModel.setLoading(true)
        
        // Create sample playlists
        val playlists = createSamplePlaylists()
        playlistAdapter.submitList(playlists)
        
        // Create sample liked songs
        val likedSongs = createSampleLikedSongs()
        likedSongsAdapter.submitList(likedSongs)
        
        // Create sample recently played
        val recentlyPlayed = createSampleRecentlyPlayed()
        recentlyPlayedAdapter.submitList(recentlyPlayed)
        
        viewModel.setLoading(false)
    }

    private fun createSamplePlaylists(): List<MediaBrowserCompat.MediaItem> {
        return listOf(
            createMediaItem(
                "playlist_1", 
                "Electronic Mix", 
                "Updated weekly",
                true
            ),
            createMediaItem(
                "playlist_2", 
                "Pop Hits", 
                "Top charts",
                true
            ),
            createMediaItem(
                "playlist_3", 
                "My Favorites", 
                "Custom playlist",
                true
            )
        )
    }

    private fun createSampleLikedSongs(): List<MediaBrowserCompat.MediaItem> {
        return listOf(
            createMediaItem(
                "song_1", 
                "Blinding Lights", 
                "The Weeknd",
                false,
                true
            ),
            createMediaItem(
                "song_2", 
                "Don't Start Now", 
                "Dua Lipa",
                false,
                true
            ),
            createMediaItem(
                "song_3", 
                "Circles", 
                "Post Malone",
                false,
                true
            )
        )
    }
    
    private fun createSampleRecentlyPlayed(): List<MediaBrowserCompat.MediaItem> {
        return listOf(
            createMediaItem(
                "album_1", 
                "After Hours", 
                "The Weeknd",
                true
            ),
            createMediaItem(
                "album_2", 
                "Future Nostalgia", 
                "Dua Lipa",
                true
            ),
            createMediaItem(
                "album_3", 
                "Hollywood's Bleeding", 
                "Post Malone",
                true
            ),
            createMediaItem(
                "album_4", 
                "Fine Line", 
                "Harry Styles",
                true
            )
        )
    }
    
    private fun createMediaItem(
        mediaId: String,
        title: String,
        subtitle: String,
        browsable: Boolean = false,
        playable: Boolean = false
    ): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build()
            
        val flags = when {
            browsable -> MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            playable -> MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            else -> 0
        }
        
        return MediaBrowserCompat.MediaItem(description, flags)
    }
    
    private fun handleMediaItemClick(mediaItem: MediaBrowserCompat.MediaItem) {
        if (mediaItem.isPlayable) {
            // Play the media
            val mediaController = MediaControllerCompat.getMediaController(requireActivity())
            mediaController?.transportControls?.playFromMediaId(mediaItem.mediaId, null)
        } else if (mediaItem.isBrowsable) {
            // Browse the item's children
            val browseFragment = newInstance(mediaItem.mediaId)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, browseFragment)
                .addToBackStack(null)
                .commit()
        }
    }
    
    companion object {
        private const val ARG_MEDIA_ID = "media_id"

        fun newInstance(mediaId: String? = null): BrowseFragment {
            val fragment = BrowseFragment()
            mediaId?.let {
                val args = Bundle()
                args.putString(ARG_MEDIA_ID, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}