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
import com.app.vc.utils.PreferenceManager
import com.app.vc.virtualchatroom.VirtualChatRoomActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VirtualRoomListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VirtualRoomListAdapter
    private var currentRole: UserRole = UserRole.CUSTOMER

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

        fetchGroups()
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
                    val uiModels = groups.map { group ->
                        VirtualRoomUiModel(
                            roNumber = group.slug,
                            subject = group.name,
                            status = RoomStatus.OPEN,
                            dayLabel = "Today",
                            timeLabel = "",
                            unreadCount = 0,
                            customerName = group.description,
                            contactNumber = ""
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
        intent.putExtra(VirtualChatRoomActivity.STATUS, room.status.name)
        Log.d(TAG, "openChatRoom: ${room.status}")
        intent.putExtra(VirtualChatRoomActivity.EXTRA_ROOM_JSON, gson.toJson(room))
        startActivity(intent)
    }

    companion object {
        const val EXTRA_ROLE = "extra_role"
        const val STATUS = "room_status"
    }
}

private const val DUMMY_ROOMS_JSON = """
[
  {
    "roNumber": "RO R02401012",
    "subject": "About car Engine",
    "status": "OPEN",
    "dayLabel": "Thu",
    "timeLabel": "11:32am",
    "unreadCount": 2,
    "customerName": "Lata",
    "contactNumber": "9876543210"
  },
  {
    "roNumber": "RO R02568783",
    "subject": "About car annual service",
    "status": "IN_PROGRESS",
    "dayLabel": "Mon",
    "timeLabel": "03:12pm",
    "unreadCount": 0,
    "customerName": "Rahul",
    "contactNumber": "9709853542"
  },
  {
    "roNumber": "RO R02568784",
    "subject": "Insurance claim status",
    "status": "CLOSED",
    "dayLabel": "Wed",
    "timeLabel": "09:45am",
    "unreadCount": 0,
    "customerName": "Naveen",
    "contactNumber": "9876500000"
  },
  {
    "roNumber": "RO R02568785",
    "subject": "Re-opened brake issue",
    "status": "REOPENED",
    "dayLabel": "Tue",
    "timeLabel": "05:10pm",
    "unreadCount": 1,
    "customerName": "Anita",
    "contactNumber": "9000012345"
  },
  {
    "roNumber": "RO R02568786",
    "subject": "Booking cancelled",
    "status": "CANCELLED",
    "dayLabel": "Sun",
    "timeLabel": "10:05am",
    "unreadCount": 0,
    "customerName": "Kiran",
    "contactNumber": "9898989898"
  }
]
"""

