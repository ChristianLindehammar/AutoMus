package com.lindehammarkonsult.automus.ui

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.FragmentBrowseNewBinding
import com.lindehammarkonsult.automus.model.FeaturedPlaylist
import com.lindehammarkonsult.automus.model.GenreCategory
import com.lindehammarkonsult.automus.ui.adapters.FeaturedPlaylistAdapter
import com.lindehammarkonsult.automus.ui.adapters.GenreCategoryAdapter
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment for browsing different music categories
 */
class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseNewBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MusicViewModel
    private lateinit var featuredPlaylistAdapter: FeaturedPlaylistAdapter
    private lateinit var genreCategoryAdapter: GenreCategoryAdapter
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
        _binding = FragmentBrowseNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        
        setupRecyclerViews()
        setupObservers()
        
        // Load featured playlists and genre categories
        // Adjust loading based on browseMediaId
        if (browseMediaId != null) {
            // TODO: Implement logic to load content for the specific browseMediaId
            // For now, it might load the same content or specific content if your ViewModel supports it
            // Example: viewModel.loadChildren(browseMediaId)
            // For now, let's assume it might still load default or you'll adapt this
            loadBasedOnMediaId(browseMediaId!!)
        } else {
            if (viewModel.isConnected.value == true) {
                loadFeaturedPlaylists()
                loadGenreCategories()
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
        
        binding.featuredPlaylistsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = featuredPlaylistAdapter
        }
        
        // Genre categories adapter and recycler view
        genreCategoryAdapter = GenreCategoryAdapter { genre ->
            onGenreCategoryClicked(genre)
        }
        
        binding.categoriesRecycler.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = genreCategoryAdapter
        }
    }
    
    private fun setupObservers() {
        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected && browseMediaId == null) { // Only load default if no specific mediaId
                loadFeaturedPlaylists()
                loadGenreCategories()
            }
            // If browseMediaId is not null, loading is handled in onViewCreated or by observing specific LiveData
        }
        // TODO: Add observer for viewModel.getChildren(browseMediaId) if you implement that
    }
    
    private fun loadBasedOnMediaId(mediaId: String) {
        // Placeholder for logic to load items based on the mediaId
        // This would typically involve a call to the ViewModel, e.g.,
        // viewModel.loadMediaChildren(mediaId)
        // And then observing LiveData that holds these children.
        // For now, we can log or show a toast, or load default as a fallback.
        // For demonstration, let's assume it might still try to load general categories
        // or you have a way to adapt this.
        // If this fragment is meant to be generic, it should subscribe to a parentId
        // in the MediaBrowserService.

        // For now, let's clear existing adapters or show a specific state
        featuredPlaylistAdapter.submitList(emptyList())
        genreCategoryAdapter.submitList(emptyList())
        // You would replace this with actual data loading for mediaId
        binding.browseEmptyViewText.text = "Browsing content for $mediaId (implementation pending)"
        binding.browseEmptyViewText.visibility = View.VISIBLE
        binding.featuredPlaylistsRecycler.visibility = View.GONE
        binding.categoriesRecycler.visibility = View.GONE
        binding.featuredTitle.visibility = View.GONE
        binding.categoriesTitle.visibility = View.GONE

        // Example: Trigger a load in ViewModel
        // viewModel.browseByMediaId(mediaId)
        // Then observe the results via LiveData
    }
    
    private fun loadFeaturedPlaylists() {
        // Create sample featured playlists
        val featuredPlaylists = listOf(
            FeaturedPlaylist(
                id = "playlist_1",
                title = "Electronic Mix",
                subtitle = "Updated weekly",
                coverArtUrl = null, // Will use placeholder
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
        
        featuredPlaylistAdapter.submitList(featuredPlaylists)
    }
    
    private fun loadGenreCategories() {
        // Create sample genre categories
        val genreCategories = listOf(
            GenreCategory(
                id = "genre_1",
                name = "Pop",
                mediaId = "genre_pop"
            ),
            GenreCategory(
                id = "genre_2",
                name = "Rock",
                mediaId = "genre_rock"
            ),
            GenreCategory(
                id = "genre_3",
                name = "Hip-Hop",
                mediaId = "genre_hiphop"
            ),
            GenreCategory(
                id = "genre_4",
                name = "Electronic",
                mediaId = "genre_electronic"
            ),
            GenreCategory(
                id = "genre_5",
                name = "Jazz",
                mediaId = "genre_jazz"
            ),
            GenreCategory(
                id = "genre_6",
                name = "Classical",
                mediaId = "genre_classical"
            )
        )
        
        genreCategoryAdapter.submitList(genreCategories)
    }
    
    private fun onFeaturedPlaylistClicked(playlist: FeaturedPlaylist) {
        // Navigate to playlist detail
        val fragment = PlaylistDetailFragment.newInstance(playlist.mediaId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun onGenreCategoryClicked(genre: GenreCategory) {
        // Navigate to genre detail
        val fragment = GenreDetailFragment.newInstance(genre.mediaId) // Ensure this is correct
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