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
        val sorted = snapshot.photosVideos.sortedByDescending { it.createdAtMillis ?: 0L }
        adapter.updateItems(toSectionedRows(sorted))
        binding.progressMediaPhotosVideos.visibility = if (snapshot.isLoading) View.VISIBLE else View.GONE
        binding.recyclerPhotosVideos.visibility =
            if (!snapshot.isLoading && sorted.isNotEmpty()) View.VISIBLE else View.GONE
        binding.txtEmptyPhotosVideos.visibility =
            if (!snapshot.isLoading && sorted.isEmpty()) View.VISIBLE else View.GONE
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
        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == 0) 3 else 1
            }
        }
        binding.recyclerPhotosVideos.layoutManager = gridLayoutManager
        binding.recyclerPhotosVideos.adapter = adapter
        groupSlug?.let { ChatMediaStore.addListener(it, mediaListener) }
    }

    private fun toSectionedRows(items: List<ChatMessage>): List<PhotoVideoMediaAdapter.RowItem> {
        if (items.isEmpty()) return emptyList()
        val out = mutableListOf<PhotoVideoMediaAdapter.RowItem>()
        var lastHeader: String? = null
        items.forEach { message ->
            val header = headerLabelFor(message.createdAtMillis ?: 0L)
            if (header != lastHeader) {
                out.add(PhotoVideoMediaAdapter.RowItem.Header(header))
                lastHeader = header
            }
            out.add(PhotoVideoMediaAdapter.RowItem.Media(message))
        }
        return out
    }

    private fun headerLabelFor(timeMs: Long): String {
        val today = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply { timeInMillis = timeMs }
        val dayFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        val targetDay = dayFormat.format(target.time)
        val todayDay = dayFormat.format(today.time)
        if (targetDay == todayDay) return "Today"
        today.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterdayDay = dayFormat.format(today.time)
        if (targetDay == yesterdayDay) return "Yesterday"
        return java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
            .format(target.time)
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