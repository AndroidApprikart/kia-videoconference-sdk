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
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.getGroups("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val groups = response.body()!!
                    val uiModels = groups.map { group ->
                        VirtualRoomUiModel(
                            roNumber = group.slug, // Using slug as roNumber for WS connection
                            subject = group.name,
                            status = RoomStatus.OPEN,
                            dayLabel = "Today",
                            timeLabel = "",
                            unreadCount = 0,
                            customerName = group.description,
                            contactNumber = ""
                        )
                    }
                    adapter.updateRooms(uiModels)
                } else {
                    Log.e("VirtualRoomList", "Failed to fetch groups: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("VirtualRoomList", "Error: ${e.localizedMessage}")
            }
        }
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
        intent.putExtra(VirtualChatRoomActivity.EXTRA_ROOM_JSON, gson.toJson(room))
        startActivity(intent)
    }

    companion object {
        const val EXTRA_ROLE = "extra_role"
    }
}