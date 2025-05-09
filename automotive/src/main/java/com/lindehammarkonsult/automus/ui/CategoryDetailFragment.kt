package com.lindehammarkonsult.automus.ui

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.lindehammarkonsult.automus.R
import com.lindehammarkonsult.automus.databinding.FragmentDetailBinding
import com.lindehammarkonsult.automus.ui.adapters.MediaCategoryAdapter
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment for displaying browse category details and subcategories
 */
class CategoryDetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MusicViewModel
    private lateinit var adapter: MediaCategoryAdapter
    private var mediaId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mediaId = it.getString(ARG_MEDIA_ID)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        
        setupRecyclerView()
        setupObservers()
        
        // Extract category name from media ID for display
        mediaId?.let { id ->
            val categoryName = id.substringAfter("category_").replace("_", " ").capitalizeWords()
            binding.detailTitle.text = categoryName
            
            loadCategoryContent(id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupRecyclerView() {
        adapter = MediaCategoryAdapter { mediaItem ->
            onMediaItemClicked(mediaItem)
        }
        
        binding.detailRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CategoryDetailFragment.adapter
        }
    }
    
    private fun setupObservers() {
        viewModel.mediaItems.observe(viewLifecycleOwner) { items ->
            binding.progressBar.visibility = View.GONE
            
            if (items.isNotEmpty()) {
                adapter.submitList(items)
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
    
    private fun loadCategoryContent(mediaId: String) {
        viewModel.setLoading(true)
        
        // Get MediaController
        val mediaController = MediaControllerCompat.getMediaController(requireActivity())
        
        // Request children of this category ID
        mediaController?.transportControls?.sendCustomAction(
            "GET_CHILDREN",
            Bundle().apply {
                putString("parent_id", mediaId)
            }
        )
        
        // Register a callback for results
        mediaController?.registerCallback(object : MediaControllerCompat.Callback() {
            override fun onSessionEvent(event: String?, extras: Bundle?) {
                super.onSessionEvent(event, extras)
                if (event == "CHILDREN_LOADED") {
                    extras?.let { bundle ->
                        val parentId = bundle.getString("parent_id")
                        if (parentId == mediaId) {
                            val mediaItems = bundle.getParcelableArrayList<MediaBrowserCompat.MediaItem>("media_items")
                            if (mediaItems != null) {
                                viewModel.setMediaItems(mediaItems)
                            }
                            viewModel.setLoading(false)
                            mediaController.unregisterCallback(this)
                        }
                    }
                }
            }
        })
    }
    
    private fun onMediaItemClicked(mediaItem: MediaBrowserCompat.MediaItem) {
        if (mediaItem.isPlayable) {
            // Play the item if it's playable
            val mediaController = MediaControllerCompat.getMediaController(requireActivity())
            mediaController?.transportControls?.playFromMediaId(mediaItem.mediaId, null)
        } else if (mediaItem.isBrowsable) {
            // Browse to the appropriate detail fragment based on media ID
            val mediaId = mediaItem.mediaId ?: return
            
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
    }
    
    private fun String.capitalizeWords(): String = split(" ")
        .map { it.capitalize() }
        .joinToString(" ")
    
    companion object {
        private const val ARG_MEDIA_ID = "media_id"
        
        fun newInstance(mediaId: String) = 
            CategoryDetailFragment().apply {
                arguments = bundleOf(ARG_MEDIA_ID to mediaId)
            }
    }
}