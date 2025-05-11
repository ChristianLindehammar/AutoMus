package com.lindehammarkonsult.automus.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexboxLayoutManager
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.FragmentBrowseBinding
import com.lindehammarkonsult.automus.model.FeaturedPlaylist
import com.lindehammarkonsult.automus.ui.adapters.FeaturedPlaylistAdapter
import com.lindehammarkonsult.automus.ui.adapters.GenrePillAdapter
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment for browsing different music categories
 */
class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MusicViewModel
    private lateinit var featuredPlaylistAdapter: FeaturedPlaylistAdapter
    private lateinit var genrePillAdapter: GenrePillAdapter
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
        // Featured playlists adapter and recycler view
        featuredPlaylistAdapter = FeaturedPlaylistAdapter { playlist ->
            onFeaturedPlaylistClicked(playlist)
        }
        
        binding.featuredPlaylistsRecyclerView.apply {
            adapter = featuredPlaylistAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        
        // Genre Pills adapter and recycler view
        genrePillAdapter = GenrePillAdapter { genrePill ->
            onGenrePillClicked(genrePill)
        }
        
        binding.genrePillsRecyclerView.apply {
            adapter = genrePillAdapter
            layoutManager = FlexboxLayoutManager(requireContext())
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
        val hasContent = featuredPlaylistAdapter.itemCount > 0 ||
                         genrePillAdapter.itemCount > 0
                         
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
        featuredPlaylistAdapter.submitList(emptyList())
        genrePillAdapter.submitList(emptyList())
        
        // TODO: Implement actual media browsing logic here
        // For now we'll show the empty state
        binding.emptyStateText.text = "Browsing content for $mediaId"
        viewModel.setLoading(false)
        updateEmptyState()
    }
    
    private fun loadBrowseContent() {
        viewModel.setLoading(true)
        
        // Create featured playlists
        val playlists = createFeaturedPlaylists()
        featuredPlaylistAdapter.submitList(playlists)
        
        // Create genre pills
        val genrePills = createGenrePills()
        genrePillAdapter.submitList(genrePills)
        
        viewModel.setLoading(false)
    }

    /* 
     * Category cards have been moved to another fragment
     * Keeping the method commented out for reference when implementing the new location
     */
    /*
    private fun createCategoryCards(): List<CategoryCardAdapter.CategoryCard> {
        return listOf(
            CategoryCardAdapter.CategoryCard(
                id = "liked_songs",
                title = "Liked Songs",
                subtitle = "Access your favorite tracks",
                iconResId = R.drawable.ic_favorite,
                mediaId = "media_liked_songs"
            ),
            CategoryCardAdapter.CategoryCard(
                id = "recently_played",
                title = "Recently Played",
                subtitle = "Continue where you left off",
                iconResId = R.drawable.ic_history,
                mediaId = "media_recently_played"
            ),
            CategoryCardAdapter.CategoryCard(
                id = "downloads",
                title = "Downloads",
                subtitle = "Available offline",
                iconResId = R.drawable.ic_download,
                mediaId = "media_downloads"
            )
        )
    }
    */

    private fun createFeaturedPlaylists(): List<FeaturedPlaylist> {
        return listOf(
            FeaturedPlaylist(
                id = "playlist_1",
                title = "Electronic Mix",
                subtitle = "Updated weekly",
                coverArtUrl = null,
                mediaId = "playlist_electronic_mix"
            ),
            FeaturedPlaylist(
                id = "playlist_2",
                title = "Pop Hits",
                subtitle = "Top charts",
                coverArtUrl = null,
                mediaId = "playlist_pop_hits"
            ),
            FeaturedPlaylist(
                id = "playlist_3",
                title = "Hip-Hop Essentials",
                subtitle = "Fresh beats",
                coverArtUrl = null,
                mediaId = "playlist_hiphop_essentials"
            ),
            FeaturedPlaylist(
                id = "playlist_4",
                title = "Rock Classics",
                subtitle = "All-time favorites",
                coverArtUrl = null,
                mediaId = "playlist_rock_classics"
            )
        )
    }
    
    private fun createGenrePills(): List<GenrePillAdapter.GenrePill> {
        val context = requireContext()
        return listOf(
            GenrePillAdapter.GenrePill(
                id = "genre_1",
                name = "Pop",
                mediaId = "genre_pop",
                color = context.getColor(android.R.color.holo_purple)
            ),
            GenrePillAdapter.GenrePill(
                id = "genre_2",
                name = "Rock",
                mediaId = "genre_rock",
                color = context.getColor(android.R.color.holo_blue_light)
            ),
            GenrePillAdapter.GenrePill(
                id = "genre_3",
                name = "Hip-Hop",
                mediaId = "genre_hiphop",
                color = context.getColor(android.R.color.holo_green_light)
            ),
            GenrePillAdapter.GenrePill(
                id = "genre_4",
                name = "Electronic",
                mediaId = "genre_electronic",
                color = context.getColor(android.R.color.holo_red_light)
            ),
            GenrePillAdapter.GenrePill(
                id = "genre_5",
                name = "Jazz",
                mediaId = "genre_jazz",
                color = context.getColor(android.R.color.holo_orange_light)
            ),
            GenrePillAdapter.GenrePill(
                id = "genre_6",
                name = "Classical",
                mediaId = "genre_classical",
                color = context.getColor(android.R.color.holo_purple)
            )
        )
    }
    
    /*
     * Category cards click handling has been moved to another fragment
     * Keeping the method commented out for reference when implementing the new location
     */
    /*
    private fun handleCategoryCardClick(categoryCard: CategoryCardAdapter.CategoryCard) {
        when (categoryCard.id) {
            "liked_songs" -> {
                // Navigate to liked songs screen or expand section
            }
            "recently_played" -> {
                // Navigate to recently played screen or expand section
            }
            "downloads" -> {
                // Navigate to downloads screen or expand section
            }
        }
    }
    */
    
    private fun onFeaturedPlaylistClicked(playlist: FeaturedPlaylist) {
        // Navigate to playlist detail
        val fragment = PlaylistDetailFragment.newInstance(playlist.mediaId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun onGenrePillClicked(genrePill: GenrePillAdapter.GenrePill) {
        // Navigate to genre detail
        val fragment = GenreDetailFragment.newInstance(genrePill.mediaId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
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