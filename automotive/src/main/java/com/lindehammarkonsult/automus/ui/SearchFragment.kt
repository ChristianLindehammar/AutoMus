package com.lindehammarkonsult.automus.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.lindehammarkonsult.automus.databinding.FragmentSearchBinding
import com.lindehammarkonsult.automus.ui.adapters.MediaCategoryAdapter
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment allowing users to search for music content
 */
class SearchFragment : Fragment() {
    
    private var _binding: FragmentSearchBinding? = null
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
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        
        setupSearchView()
        setupRecyclerView()
        setupObservers()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupSearchView() {
        binding.searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (!query.isNullOrBlank()) {
                        performSearch(query)
                        hideKeyboard()
                    }
                    return true
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    // We could implement search suggestions here
                    return false
                }
            })
            
            // Request focus and show keyboard when fragment becomes visible
            requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    private fun setupRecyclerView() {
        mediaAdapter = MediaCategoryAdapter { mediaItem ->
            handleMediaItemClick(mediaItem)
        }
        
        binding.searchResultsRecyclerView.apply {
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
        
        // Observe search results
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (results.isNullOrEmpty()) {
                binding.noResultsText.visibility = View.VISIBLE
                binding.searchResultsRecyclerView.visibility = View.GONE
            } else {
                binding.noResultsText.visibility = View.GONE
                binding.searchResultsRecyclerView.visibility = View.VISIBLE
                mediaAdapter.submitList(results)
            }
        }
    }
    
    private fun performSearch(query: String) {
        viewModel.setLoading(true)
        
        // This would normally connect to a MediaBrowser service to search
        // For now, we'll create some dummy search results
        val searchResults = listOf(
            createMediaItem(
                "song_1", 
                "\"${query}\" Song Result 1", 
                "Artist Name", 
                false, 
                true
            ),
            createMediaItem(
                "song_2", 
                "Another ${query} Song", 
                "Different Artist", 
                false, 
                true
            ),
            createMediaItem(
                "album_1", 
                "Album with ${query}", 
                "Various Artists", 
                true, 
                false
            )
        )
        
        viewModel.setSearchResults(searchResults)
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
        browsable: Boolean = false,
        playable: Boolean = true
    ): MediaBrowserCompat.MediaItem {
        val description = MediaBrowserCompat.MediaDescriptionCompat.Builder()
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
    
    private fun hideKeyboard() {
        val view = activity?.currentFocus ?: return
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    
    companion object {
        fun newInstance() = SearchFragment()
    }
}