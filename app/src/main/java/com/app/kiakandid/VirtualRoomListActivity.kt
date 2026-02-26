package com.app.kiakandid

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VirtualRoomListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VirtualRoomListAdapter

    private var currentRole: UserRole = UserRole.CUSTOMER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_virtual_room_list)

        val roleFromIntent = intent.getStringExtra(EXTRA_ROLE)
        currentRole = when (roleFromIntent) {
            UserRole.SERVICE_ADVISOR.name -> UserRole.SERVICE_ADVISOR
            UserRole.MANAGER.name -> UserRole.MANAGER
            else -> UserRole.CUSTOMER
        }

        setupRoleSelectionIfAvailable()
        setupRecycler()
        applyRoleTitle()
    }

    private fun setupRoleSelectionIfAvailable() {
        val roleContainer = findViewById<View?>(R.id.roleSelectionContainer)
        val btnServiceAdvisor = findViewById<View?>(R.id.btnServiceAdvisor)
        val btnManager = findViewById<View?>(R.id.btnManager)

        if (roleContainer != null && btnServiceAdvisor != null && btnManager != null) {
            roleContainer.visibility = View.VISIBLE

            btnServiceAdvisor.setOnClickListener {
                currentRole = UserRole.SERVICE_ADVISOR
                roleContainer.visibility = View.GONE
                applyRoleTitle()
                adapter.updateRooms(createStaticRooms(currentRole))
            }

            btnManager.setOnClickListener {
                currentRole = UserRole.MANAGER
                roleContainer.visibility = View.GONE
                applyRoleTitle()
                adapter.updateRooms(createStaticRooms(currentRole))
            }
        } else {
            currentRole = UserRole.CUSTOMER
        }
    }

    private fun setupRecycler() {
        recyclerView = findViewById(R.id.recyclerVirtualRooms)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = VirtualRoomListAdapter(createStaticRooms(currentRole))
        recyclerView.adapter = adapter
    }

    private fun applyRoleTitle() {
        val titleView = findViewById<android.widget.TextView?>(R.id.txtTitle)
        titleView?.text = when (currentRole) {
            UserRole.CUSTOMER -> getString(R.string.title_virtual_chat_room_customer)
            UserRole.SERVICE_ADVISOR -> getString(R.string.title_virtual_chat_room_service_advisor)
            UserRole.MANAGER -> getString(R.string.title_virtual_chat_room_manager)
        }
    }

    private fun createStaticRooms(role: UserRole): List<VirtualRoom> {
        return listOf(
            VirtualRoom(
                customerName = "Lata",
                roNumber = "RO20240101",
                complaint = "Car in the inspection stage",
                contactNumber = "9876543210",
                status = if (role == UserRole.CUSTOMER) "Assigned" else "Open"
            ),
            VirtualRoom(
                customerName = "Rahul",
                roNumber = "RO20240102",
                complaint = "Request for quick service",
                contactNumber = "9876501234",
                status = "Open"
            ),
            VirtualRoom(
                customerName = "Naveen",
                roNumber = "RO20240103",
                complaint = "Need update on estimation",
                contactNumber = "9000012345",
                status = "Closed"
            )
        )
    }

    companion object {
        const val EXTRA_ROLE = "extra_role"
    }
}

enum class UserRole {
    CUSTOMER,
    SERVICE_ADVISOR,
    MANAGER
}

data class VirtualRoom(
    val customerName: String,
    val roNumber: String,
    val complaint: String,
    val contactNumber: String,
    val status: String
)

