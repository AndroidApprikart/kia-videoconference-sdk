package com.app.vc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.vc.databinding.FragmentDocumentsBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.vc.utils.ApiDetails
import com.app.vc.virtualchatroom.ChatMediaStore
import com.app.vc.virtualchatroom.ChatMessage
import com.app.vc.virtualchatroom.MediaViewerActivity


class DocumentsFragment : Fragment() {

    private var _binding: FragmentDocumentsBinding? = null
    private val binding get() = _binding!!
    var TAG = "MediaFragment"
    private lateinit var adapter: DocumentMediaAdapter
    private var groupSlug: String? = null

    private val mediaListener: (com.app.vc.virtualchatroom.RoomMediaSnapshot) -> Unit = { snapshot ->
        adapter.updateItems(snapshot.documents)
        binding.progressMediaDocuments.visibility = if (snapshot.isLoading) View.VISIBLE else View.GONE
        binding.recyclerDocuments.visibility =
            if (!snapshot.isLoading && snapshot.documents.isNotEmpty()) View.VISIBLE else View.GONE
        binding.txtEmptyDocuments.visibility =
            if (!snapshot.isLoading && snapshot.documents.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        groupSlug = arguments?.getString(KEY_GROUP_SLUG)
        adapter = DocumentMediaAdapter(emptyList()) { message -> openMedia(message) }
        binding.recyclerDocuments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDocuments.adapter = adapter
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
