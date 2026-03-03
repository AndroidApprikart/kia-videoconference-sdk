package com.app.vc.virtualroomlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.virtualchatroom.VirtualChatRoomActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VirtualRoomListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VirtualRoomListAdapter

    private var currentRole: UserRole = UserRole.CUSTOMER

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
    }

//    private fun setupRoleSelectionIfAvailable() {
//        val roleContainer = findViewById<View?>(R.id.roleSelectionContainer)
//        val btnServiceAdvisor = findViewById<View?>(R.id.btnServiceAdvisor)
//        val btnManager = findViewById<View?>(R.id.btnManager)
//
//        if (roleContainer != null && btnServiceAdvisor != null && btnManager != null) {
//            roleContainer.visibility = View.VISIBLE
//
//            btnServiceAdvisor.setOnClickListener {
//                currentRole = UserRole.SERVICE_ADVISOR
//                roleContainer.visibility = View.GONE
//                applyRoleTitle()
//                adapter.updateRooms(loadRoomsFromJson())
//            }
//
//            btnManager.setOnClickListener {
//                currentRole = UserRole.MANAGER
//                roleContainer.visibility = View.GONE
//                applyRoleTitle()
//                adapter.updateRooms(loadRoomsFromJson())
//            }
//        } else {
//            currentRole = UserRole.CUSTOMER
//        }
//    }

    private fun setupRecycler() {
        recyclerView = findViewById(R.id.recyclerVirtualRooms)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = VirtualRoomListAdapter(loadRoomsFromJson()) { room ->
            openChatRoom(room)
        }
        recyclerView.adapter = adapter
    }

    private fun applyRoleTitle() {
        val titleView = findViewById<TextView?>(R.id.txtTitle)
        titleView?.text = when (currentRole) {
            UserRole.CUSTOMER -> getString(R.string.vc_title_virtual_chat_room_customer)
            UserRole.SERVICE_ADVISOR -> getString(R.string.vc_title_virtual_chat_room_service_advisor)
            UserRole.MANAGER -> getString(R.string.vc_title_virtual_chat_room_manager)
        }
    }

    private fun loadRoomsFromJson(): List<VirtualRoomUiModel> {
        val gson = Gson()
        val listType = object : TypeToken<List<VirtualRoomDto>>() {}.type
        val dtoList: List<VirtualRoomDto> = gson.fromJson(DUMMY_ROOMS_JSON, listType)
        return dtoList.map { it.toUiModel() }
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

