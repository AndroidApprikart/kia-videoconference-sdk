package com.app.vc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.vc.databinding.FragmentPhotosAndVideosBinding
import androidx.recyclerview.widget.GridLayoutManager
import com.app.vc.utils.ApiDetails
import com.app.vc.virtualchatroom.ChatMediaStore
import com.app.vc.virtualchatroom.ChatMessage
import com.app.vc.virtualchatroom.MediaViewerActivity


class PhotosAndVideosFragment : Fragment() {

    private var _binding: FragmentPhotosAndVideosBinding? = null
    private val binding get() = _binding!!
    var TAG = "MediaFragment"
    private lateinit var adapter: PhotoVideoMediaAdapter
    private var groupSlug: String? = null
    private val mediaListener: (com.app.vc.virtualchatroom.RoomMediaSnapshot) -> Unit = { snapshot ->
        adapter.updateItems(snapshot.photosVideos)
        binding.progressMediaPhotosVideos.visibility = if (snapshot.isLoading) View.VISIBLE else View.GONE
        binding.recyclerPhotosVideos.visibility =
            if (!snapshot.isLoading && snapshot.photosVideos.isNotEmpty()) View.VISIBLE else View.GONE
        binding.txtEmptyPhotosVideos.visibility =
            if (!snapshot.isLoading && snapshot.photosVideos.isEmpty()) View.VISIBLE else View.GONE
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotosAndVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        groupSlug = arguments?.getString(KEY_GROUP_SLUG)
        adapter = PhotoVideoMediaAdapter(emptyList()) { message -> openMedia(message) }
        binding.recyclerPhotosVideos.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerPhotosVideos.adapter = adapter
        groupSlug?.let { ChatMediaStore.addListener(it, mediaListener) }
    }

    private fun openMedia(message: ChatMessage) {
        val rawUrl = message.attachmentUri ?: return
        val fullUrl = if (rawUrl.startsWith("http")) rawUrl else ApiDetails.APRIK_Kia_BASE_URL + rawUrl
        startActivity(
            android.content.Intent(requireContext(), MediaViewerActivity::class.java).apply {
                putExtra(MediaViewerActivity.EXTRA_URL, fullUrl)
                putExtra(MediaViewerActivity.EXTRA_TYPE, message.type.name)
                putExtra(MediaViewerActivity.EXTRA_FILE_NAME, message.fileName)
            }
        )
    }

    override fun onDestroyView() {
        groupSlug?.let { ChatMediaStore.removeListener(it, mediaListener) }
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val KEY_GROUP_SLUG = "group_slug"
    }
}