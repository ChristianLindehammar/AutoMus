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
import com.lindehammarkonsult.automus.ui.adapters.MediaItemAdapter
import com.lindehammarkonsult.automus.viewmodel.MusicViewModel

/**
 * Fragment for displaying genre details and related media items
 */
class GenreDetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MusicViewModel
    private lateinit var adapter: MediaItemAdapter
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
        
        // Load genre details if we have a valid media ID
        mediaId?.let { id ->
            loadGenreDetails(id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupRecyclerView() {
        adapter = MediaItemAdapter { mediaItem ->
            onMediaItemClicked(mediaItem)
        }
        
        binding.detailRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GenreDetailFragment.adapter
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
    
    private fun loadGenreDetails(mediaId: String) {
        viewModel.setLoading(true)
        
        // Get MediaController
        val mediaController = MediaControllerCompat.getMediaController(requireActivity())
        
        // Request children of this genre ID
        mediaController?.transportControls?.sendCustomAction(
            "GET_CHILDREN",
            Bundle().apply {
                putString("parent_id", mediaId)
            }
        )
        
        // Extract genre name from media ID for display
        val genreName = mediaId.substringAfter("genre_").replace("_", " ").capitalizeWords()
        binding.detailTitle.text = genreName
        
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
            val mediaController = MediaControllerCompat.getMediaController(requireActivity())
            mediaController?.transportControls?.playFromMediaId(mediaItem.mediaId, null)
        } else if (mediaItem.isBrowsable) {
            // Navigate to album or playlist if browsable
            val fragment = when {
                mediaItem.mediaId?.startsWith("playlist_") == true -> {
                    PlaylistDetailFragment.newInstance(mediaItem.mediaId!!)
                }
                mediaItem.mediaId?.startsWith("album_") == true -> {
                    AlbumDetailFragment.newInstance(mediaItem.mediaId!!)
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
            GenreDetailFragment().apply {
                arguments = bundleOf(ARG_MEDIA_ID to mediaId)
            }
    }
}