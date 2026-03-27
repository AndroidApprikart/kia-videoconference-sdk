package com.app.vc

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.app.vc.databinding.FragmentMediaBinding
import androidx.core.content.ContextCompat
import com.app.vc.network.ApiAttachmentResponse
import com.app.vc.network.ApiMessageResponse
import com.app.vc.network.LoginApiService
import com.app.vc.utils.PreferenceManager
import com.app.vc.virtualchatroom.ChatMediaStore
import com.app.vc.virtualchatroom.ChatMessage
import com.app.vc.virtualchatroom.ChatMessageType
import com.app.vc.virtualchatroom.ChatMessageStorage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MediaFragment: Fragment() {

    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!
    var TAG = "MediaFragment"
    private var groupSlug: String? = null
    private val gson = Gson()
    private val apiService: LoginApiService by lazy {
        val gsonBuilder = GsonBuilder().setLenient().create()
        Retrofit.Builder()
            .baseUrl(com.app.vc.utils.ApiDetails.APRIK_Kia_BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(gsonBuilder))
            .build()
            .create(LoginApiService::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        groupSlug = arguments?.getString(KEY_GROUP_SLUG)

        binding.tabPhotoVideos.post {
            moveIndicator(binding.tabPhotoVideos)
        }

        loadFragment(PhotosAndVideosFragment().apply {
            arguments = Bundle().apply { putString(PhotosAndVideosFragment.KEY_GROUP_SLUG, groupSlug) }
        })
        setupTabs()
        selectPhotoVideosTab()
        seedMediaFromLocalCache()
        loadCompleteMediaHistory()

    }


    private fun loadFragment(fragment: Fragment) {

        childFragmentManager.beginTransaction()
            .replace(R.id.FragmentContainer, fragment)
            .commit()
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTabs() {

        binding.tabPhotoVideos.setOnClickListener {

            loadFragment(PhotosAndVideosFragment().apply {
                arguments = Bundle().apply { putString(PhotosAndVideosFragment.KEY_GROUP_SLUG, groupSlug) }
            })

            selectPhotoVideosTab()
            moveIndicator(binding.tabPhotoVideos)

        }

        binding.tabDocuments.setOnClickListener {

            loadFragment(DocumentsFragment().apply {
                arguments = Bundle().apply { putString(DocumentsFragment.KEY_GROUP_SLUG, groupSlug) }
            })

            selectDocsTab()
            moveIndicator(binding.tabDocuments)

        }
    }

    companion object {
        const val KEY_GROUP_SLUG = "group_slug"
    }

    private fun loadCompleteMediaHistory() {
        val slug = groupSlug ?: return
        val token = PreferenceManager.getAccessToken().orEmpty()
        if (token.isBlank()) return
        ChatMediaStore.setLoading(slug, true)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allMessages = mutableListOf<ChatMessage>()
                var beforeId: Int? = null
                var previousBeforeId: Int? = null

                while (true) {
                    val response = apiService.getMessages("Bearer $token", slug, beforeId)
                    if (!response.isSuccessful || response.body() == null) break
                    val apiMessages = parseApiMessagesResponse(response.body())
                    if (apiMessages.isEmpty()) break

                    allMessages.addAll(apiMessages.map { apiMessageToChatMessage(it) })
                    previousBeforeId = beforeId
                    beforeId = apiMessages.map { it.id }.minOrNull()
                    if (beforeId == null || beforeId == previousBeforeId) break
                }

                val merged = allMessages
                    .distinctBy { it.messageId ?: "${it.type}:${it.createdAtMillis}:${it.attachmentUri}:${it.text}" }
                    .sortedBy { it.createdAtMillis ?: Long.MAX_VALUE }

                withContext(Dispatchers.Main) {
                    ChatMediaStore.replaceMessages(slug, merged)
                    ChatMediaStore.setLoading(slug, false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load full media history: ${e.message}")
                withContext(Dispatchers.Main) {
                    ChatMediaStore.setLoading(slug, false)
                }
            }
        }
    }

    private fun seedMediaFromLocalCache() {
        val slug = groupSlug ?: return
        val cached = ChatMessageStorage.loadMessages(requireContext(), slug)
        if (cached.isNotEmpty()) {
            ChatMediaStore.replaceMessages(slug, cached)
        }
    }

    private fun parseApiMessagesResponse(body: JsonElement?): List<ApiMessageResponse> {
        if (body == null || body.isJsonNull) return emptyList()
        return try {
            when {
                body.isJsonArray -> body.asJsonArray.mapNotNull {
                    gson.fromJson(it, ApiMessageResponse::class.java)
                }
                body.isJsonObject -> {
                    val obj = body.asJsonObject
                    val array = when {
                        obj.get("results")?.isJsonArray == true -> obj.getAsJsonArray("results")
                        obj.get("data")?.isJsonArray == true -> obj.getAsJsonArray("data")
                        obj.get("messages")?.isJsonArray == true -> obj.getAsJsonArray("messages")
                        else -> null
                    }
                    array?.mapNotNull { gson.fromJson(it, ApiMessageResponse::class.java) } ?: emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse media messages response: ${e.message}")
            emptyList()
        }
    }

    private fun apiMessageToChatMessage(apiMsg: ApiMessageResponse): ChatMessage {
        val attachment = apiMsg.attachments?.firstOrNull() ?: return ChatMessage(
            messageId = apiMsg.id.toString(),
            text = apiMsg.content,
            isSender = false,
            timeLabel = formatApiDate(apiMsg.createdAt),
            createdAtMillis = parseApiCreatedAtMillis(apiMsg.createdAt),
            type = ChatMessageType.TEXT
        )
        val type = attachment.toChatMessageType(apiMsg.messageType)
        return ChatMessage(
            messageId = apiMsg.id.toString(),
            text = if (type == ChatMessageType.TEXT) apiMsg.content else "",
            isSender = false,
            timeLabel = formatApiDate(apiMsg.createdAt),
            createdAtMillis = parseApiCreatedAtMillis(apiMsg.createdAt),
            type = type,
            attachmentUri = attachment.fileUrl.let { if (it.startsWith("http")) it else com.app.vc.utils.ApiDetails.APRIK_Kia_BASE_URL + it },
            fileName = attachment.fileName,
            caption = if (type != ChatMessageType.TEXT) apiMsg.content else null,
            thumbnailUrl = attachment.thumbnailUrl?.let { if (it.startsWith("http")) it else com.app.vc.utils.ApiDetails.APRIK_Kia_BASE_URL + it },
            mimeType = attachment.mimeType
        )
    }

    private fun ApiAttachmentResponse.toChatMessageType(messageType: String): ChatMessageType {
        return when {
            mimeType.startsWith("image") -> ChatMessageType.IMAGE
            mimeType.startsWith("video") -> ChatMessageType.VIDEO
            mimeType.startsWith("audio") -> ChatMessageType.VOICE_NOTE
            else -> when (messageType) {
                "image" -> ChatMessageType.IMAGE
                "video" -> ChatMessageType.VIDEO
                "document" -> ChatMessageType.FILE
                else -> ChatMessageType.FILE
            }
        }
    }

    private fun formatApiDate(dateStr: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateStr.take(19))
            if (date != null) {
                SimpleDateFormat("hh:mma", Locale.getDefault()).format(date).lowercase()
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseApiCreatedAtMillis(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(dateStr.take(19))?.time
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectPhotoVideosTab() {

        binding.tabPhotoVideos.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary_kia_kandid)
        )

        binding.tabPhotoVideos.setTypeface(
            resources.getFont(R.font.kia_signature_fix_bold)
        )



        binding.tabDocuments.setTextColor(
            ContextCompat.getColor(requireContext(),R.color.gray_mic_background)
        )

        binding.tabDocuments.setTypeface(
            resources.getFont(R.font.kia_signature_fix_regular)
        )

        binding.tabDocuments.background = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectDocsTab() {

        binding.tabDocuments.setTextColor(
            ContextCompat.getColor(requireContext(),R.color.colorPrimary_kia_kandid)
        )

        binding.tabDocuments.setTypeface(
            resources.getFont(R.font.kia_signature_fix_bold)
        )

        binding.tabPhotoVideos.setTextColor(
            ContextCompat.getColor(requireContext(),R.color.gray_mic_background)
        )

        binding.tabPhotoVideos.setTypeface(
            resources.getFont(R.font.kia_signature_fix_regular)
        )

        binding.tabPhotoVideos.background = null
    }

    private fun moveIndicator(tab: View) {

        binding.tabIndicator.post {

            val width = tab.width
            val start = tab.left

            binding.tabIndicator.layoutParams.width = width
            binding.tabIndicator.requestLayout()

            binding.tabIndicator.x = start.toFloat()
        }
    }





}
