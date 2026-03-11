package com.app.vc.virtualroomlist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.network.LoginApiService
import com.app.vc.network.TokenRefreshRequest
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.ConnectivityBannerHandler
import com.app.vc.utils.PreferenceManager
import com.app.vc.virtualchatroom.VirtualChatRoomActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale

class VirtualRoomListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VirtualRoomListAdapter
    private var currentRole: UserRole = UserRole.CUSTOMER
    private var connectivityBannerHandler: ConnectivityBannerHandler? = null

    val TAG="VirtualRoomListActivity"

    private val apiService: LoginApiService by lazy {
        val gson = GsonBuilder().setLenient().create()
        Retrofit.Builder()
            .baseUrl(ApiDetails.APRIK_Kia_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LoginApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vc_activity_virtual_room_list)

        val roleFromIntent = intent.getStringExtra(EXTRA_ROLE)
        currentRole = when (roleFromIntent) {
            UserRole.SERVICE_ADVISOR.name -> UserRole.SERVICE_ADVISOR
            UserRole.MANAGER.name -> UserRole.MANAGER
            else -> UserRole.CUSTOMER
        }


//        setupRoleSelectionIfAvailable()
        setupRecycler()
        applyRoleTitle()
        connectivityBannerHandler = ConnectivityBannerHandler(
            context = this,
            rootViewProvider = { findViewById<View>(android.R.id.content) }
        )

        fetchGroups()
    }

    override fun onStart() {
        super.onStart()
        connectivityBannerHandler?.register()
    }

    override fun onStop() {
        connectivityBannerHandler?.unregister()
        super.onStop()
    }

    private fun setupRecycler() {
        recyclerView = findViewById(R.id.recyclerVirtualRooms)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Initialize with empty list, will update once API returns
        adapter = VirtualRoomListAdapter(emptyList()) { room ->
            openChatRoom(room)
        }
        recyclerView.adapter = adapter
    }

    private fun fetchGroups() {
        val token = PreferenceManager.getAccessToken()
        if (token.isNullOrEmpty()) {
            tryRefreshAndFetch()
            return
        }

        lifecycleScope.launch {
            val result = fetchGroupsWithToken(token)
            when {
                result is GroupsResult.Success -> {
                    adapter.updateRooms(result.rooms)
                }
                result is GroupsResult.Unauthorized -> {
                    tryRefreshAndFetch()
                }
                result is GroupsResult.Error -> {
                    Log.e("VirtualRoomList", "Failed to fetch groups: ${result.message}")
                    Toast.makeText(this@VirtualRoomListActivity, "Failed to load rooms", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun fetchGroupsWithToken(token: String): GroupsResult {
        return try {
            val response = apiService.getGroups("Bearer $token")
            when {
                response.isSuccessful && response.body() != null -> {
                    val groups = response.body()!!
                    Log.d(TAG, "getGroups list API response: ${Gson().toJson(groups)}")
                    val uiModels = groups.map { group ->
                        val serviceStatus = group.currentServiceStatus
                        val (dayLabel, timeLabel) = formatCreatedAt(group.createdAt)
                        Log.d(TAG, "Group ${group.slug}: status_label=${serviceStatus?.statusLabel}, notes=${serviceStatus?.notes}, ro_number=${group.roNumber}, created=${dayLabel} $timeLabel")
                        
                        // Requirement: Check for username which has "customer" in it and fetch that member's first name (when user object is present)
                        val customerMember = group.members.find { it.user?.username?.contains("customer", ignoreCase = true) == true }
                        val customerName = customerMember?.user?.firstName ?: ""

                        VirtualRoomUiModel(
                            roNumber = group.slug,
                            subject = group.name, // Phone view uses this as "groupname"
                            status = serviceStatus?.status ?: "OPEN",
                            dayLabel = dayLabel,
                            timeLabel = timeLabel,
                            unreadCount = 0,
                            customerName = if (customerName.isNotBlank()) customerName else group.description,
                            contactNumber = "",
                            lifecycleStatusLabel = serviceStatus?.statusLabel,
                            roNumberDisplay = group.roNumber, // Tablet view displays this as RO No
                            serviceNotes = serviceStatus?.notes
                        )
                    }
                    GroupsResult.Success(uiModels)
                }
                response.code() == 401 -> GroupsResult.Unauthorized
                else -> GroupsResult.Error(response.message())
            }
        } catch (e: Exception) {
            Log.e("VirtualRoomList", "Error: ${e.localizedMessage}")
            GroupsResult.Error(e.localizedMessage ?: e.message ?: "Unknown error")
        }
    }

    /** Parses ISO 8601 created_at (e.g. 2026-03-05T14:51:03.871701) to "05-03-2026" and "2:51PM". */
    private fun formatCreatedAt(createdAt: String?): Pair<String, String> {
        if (createdAt.isNullOrBlank()) return "" to ""
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            parser.parse(createdAt.take(19))?.let { date ->
                val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(date)
                val timeStr = SimpleDateFormat("h:mma", Locale.US).format(date)
                dateStr to timeStr
            } ?: ("" to "")
        } catch (e: Exception) {
            Log.w(TAG, "formatCreatedAt failed for: $createdAt", e)
            "" to ""
        }
    }

    private fun tryRefreshAndFetch() {
        val refreshToken = PreferenceManager.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.refreshToken(TokenRefreshRequest(refreshToken))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    PreferenceManager.setAccessToken(body.access)
                    if (body.refresh.isNotBlank()) {
                        PreferenceManager.setRefreshToken(body.refresh)
                    }
                    val result = fetchGroupsWithToken(body.access)
                    if (result is GroupsResult.Success) {
                        adapter.updateRooms(result.rooms)
                    } else if (result is GroupsResult.Error) {
                        Log.e("VirtualRoomList", "Failed to fetch groups after refresh: ${result.message}")
                        Toast.makeText(this@VirtualRoomListActivity, "Failed to load rooms", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@VirtualRoomListActivity, "Session expired", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("VirtualRoomList", "Refresh failed: ${e.message}")
                Toast.makeText(this@VirtualRoomListActivity, "Session expired", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private sealed class GroupsResult {
        data class Success(val rooms: List<VirtualRoomUiModel>) : GroupsResult()
        object Unauthorized : GroupsResult()
        data class Error(val message: String) : GroupsResult()
    }

    private fun applyRoleTitle() {
        val titleView = findViewById<TextView?>(R.id.txtTitle)
        titleView?.text = when (currentRole) {
            UserRole.CUSTOMER -> getString(R.string.vc_title_virtual_chat_room_customer)
            UserRole.SERVICE_ADVISOR -> getString(R.string.vc_title_virtual_chat_room_service_advisor)
            UserRole.MANAGER -> getString(R.string.vc_title_virtual_chat_room_manager)
        }
    }

    private fun openChatRoom(room: VirtualRoomUiModel) {
        val gson = Gson()
        val intent = Intent(this, VirtualChatRoomActivity::class.java)
        intent.putExtra(VirtualChatRoomActivity.EXTRA_ROLE, currentRole.name)
        intent.putExtra(VirtualChatRoomActivity.STATUS, room.status)
        Log.d(TAG, "openChatRoom: ${room.status}")
        intent.putExtra(VirtualChatRoomActivity.EXTRA_ROOM_JSON, gson.toJson(room))
        startActivity(intent)
    }

    companion object {
        const val EXTRA_ROLE = "extra_role"
        const val STATUS = "room_status"
    }
}
