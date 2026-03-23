package com.app.vc.virtualroomlist

import android.content.Intent
import android.app.DatePickerDialog
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
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
import com.app.vc.websocketconnection.NotificationWebSocketManager
import com.app.vc.websocketconnection.SocketSessionCoordinator
import com.app.vc.virtualchatroom.VirtualChatRoomActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
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
    private var latestRooms: List<VirtualRoomUiModel> = emptyList()
    private var allRooms: List<VirtualRoomUiModel> = emptyList()
    private val gson = Gson()
    private var selectedAppointmentDate: String? = null
    private var selectedServiceStatus: String? = null
    private var selectedReferenceFilter: ReferenceFilter = ReferenceFilter.ALL
    private var searchQuery: String = ""
    private var currentPage: Int = 1
    private var totalPages: Int = 1
    private var hasNextPage: Boolean = false
    private var hasPreviousPage: Boolean = false
    private val unreadListener: (Map<String, Int>) -> Unit = { counts ->
        if (allRooms.isNotEmpty()) {
            val updatedAllRooms = allRooms.map { room ->
                room.copy(unreadCount = counts[room.roNumber] ?: room.unreadCount)
            }
            allRooms = updatedAllRooms
            latestRooms = filterRooms(updatedAllRooms, searchQuery, selectedReferenceFilter)
            runOnUiThread { adapter.updateRooms(latestRooms) }
        }
    }

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

        val btnBack=findViewById<ImageView>(R.id.btnBack)
        val backTv=findViewById<TextView>(R.id.txtBackToHome)
        btnBack?.setOnClickListener { finish() }
        backTv?.setOnClickListener { finish() }
//        setupRoleSelectionIfAvailable()
        setupRecycler()
        applyRoleTitle()
        setupFilters()
        setupPaginationControls()
        GroupUnreadStore.addListener(unreadListener)
        connectivityBannerHandler = ConnectivityBannerHandler(
            context = this,
            rootViewProvider = { findViewById<View>(android.R.id.content) }
        )

        fetchGroups(page = 1)
        NotificationWebSocketManager.getInstance().connectWithToken(PreferenceManager.getAccessToken())
    }

    override fun onStart() {
        super.onStart()
        connectivityBannerHandler?.register()
        NotificationWebSocketManager.getInstance().setActiveGroupSlug(null)
        NotificationWebSocketManager.getInstance().connectWithToken(PreferenceManager.getAccessToken())
        if (latestRooms.isNotEmpty()) {
            val counts = GroupUnreadStore.snapshot()
            val updatedAllRooms = allRooms.map { room ->
                room.copy(unreadCount = counts[room.roNumber] ?: room.unreadCount)
            }
            allRooms = updatedAllRooms
            latestRooms = filterRooms(updatedAllRooms, searchQuery, selectedReferenceFilter)
            adapter.updateRooms(latestRooms)
        }
    }

    override fun onStop() {
        connectivityBannerHandler?.unregister()
        super.onStop()
    }

    override fun onDestroy() {
        GroupUnreadStore.removeListener(unreadListener)
        super.onDestroy()
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

    private fun fetchGroups(page: Int = currentPage) {
        val token = PreferenceManager.getAccessToken()
        if (token.isNullOrEmpty()) {
            tryRefreshAndFetch()
            return
        }

        lifecycleScope.launch {
            val result = fetchGroupsWithToken(token, page)
            when {
                result is GroupsResult.Success -> {
                    currentPage = result.currentPage
                    totalPages = result.totalPages
                    hasNextPage = result.hasNext
                    hasPreviousPage = result.hasPrevious
                    allRooms = result.rooms
                    latestRooms = filterRooms(result.rooms, searchQuery, selectedReferenceFilter)
                    adapter.updateRooms(latestRooms)
                    updatePaginationUi()
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

    private suspend fun fetchGroupsWithToken(token: String, page: Int): GroupsResult {
        return try {
            val response = apiService.getGroups(
                token = "Bearer $token",
                page = page,
                pageSize = resolvePageSize(),
                appointmentDate = selectedAppointmentDate,
                serviceStatus = selectedServiceStatus
            )
            when {
                response.isSuccessful && response.body() != null -> {
                    val pageResponse = parseGroupsPageResponse(response.body())
                    val groups = pageResponse.groups
                    Log.d(TAG, "getGroups list API response: ${gson.toJson(groups)}")
                    GroupUnreadStore.replaceAll(groups.associate { it.slug to (it.unreadCount ?: 0) })
                    val uiModels = groups.map { group ->
                        val serviceStatus = group.currentServiceStatus
                        val (dayLabel, timeLabel) = formatCreatedAt(group.createdAt)
                        Log.d(TAG, "Group ${group.slug}: status_label=${serviceStatus?.statusLabel}, notes=${serviceStatus?.notes}, ro_number=${group.roNumber}, created=${dayLabel} $timeLabel")

                        VirtualRoomUiModel(
                            roNumber = group.slug,
                            slug = group.slug,
                            subject = group.name, // Phone view uses this as "groupname"
                            status = serviceStatus?.status ?: "OPEN",
                            dayLabel = dayLabel,
                            timeLabel = timeLabel,
                            unreadCount = GroupUnreadStore.getUnreadCount(group.slug),
                            customerName = resolveCustomerName(group),
                            contactNumber = "",
                            lifecycleStatusLabel = serviceStatus?.statusLabel,
                            roNumberDisplay = group.roNumber, // Tablet view displays this as RO No
                            appointmentIdDisplay = resolveAppointmentId(group),
                            serviceNotes = serviceStatus?.notes
                        )
                    }
                    GroupsResult.Success(
                        rooms = uiModels,
                        currentPage = pageResponse.currentPage,
                        totalPages = pageResponse.totalPages,
                        hasNext = pageResponse.hasNext,
                        hasPrevious = pageResponse.hasPrevious
                    )
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
            SocketSessionCoordinator.getInstance().clearSession()
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.refreshToken(TokenRefreshRequest(refreshToken))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "Refresh token API response: ${Gson().toJson(body)}")
                    PreferenceManager.setAccessToken(body.access)
                    if (body.refresh.isNotBlank()) {
                        PreferenceManager.setRefreshToken(body.refresh)
                    }
                    val result = fetchGroupsWithToken(body.access, currentPage)
                    if (result is GroupsResult.Success) {
                        currentPage = result.currentPage
                        totalPages = result.totalPages
                        hasNextPage = result.hasNext
                        hasPreviousPage = result.hasPrevious
                        allRooms = result.rooms
                        latestRooms = filterRooms(result.rooms, searchQuery, selectedReferenceFilter)
                        adapter.updateRooms(latestRooms)
                        updatePaginationUi()
                    } else if (result is GroupsResult.Error) {
                        Log.e("VirtualRoomList", "Failed to fetch groups after refresh: ${result.message}")
                        Toast.makeText(this@VirtualRoomListActivity, "Failed to load rooms", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    SocketSessionCoordinator.getInstance().clearSession()
                    Toast.makeText(this@VirtualRoomListActivity, "Session expired", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("VirtualRoomList", "Refresh failed: ${e.message}")
                SocketSessionCoordinator.getInstance().clearSession()
                Toast.makeText(this@VirtualRoomListActivity, "Session expired", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private sealed class GroupsResult {
        data class Success(
            val rooms: List<VirtualRoomUiModel>,
            val currentPage: Int,
            val totalPages: Int,
            val hasNext: Boolean,
            val hasPrevious: Boolean
        ) : GroupsResult()
        object Unauthorized : GroupsResult()
        data class Error(val message: String) : GroupsResult()
    }

    private data class ParsedGroupsPage(
        val groups: List<GroupResponse>,
        val currentPage: Int,
        val totalPages: Int,
        val hasNext: Boolean,
        val hasPrevious: Boolean
    )

    private fun parseGroupsPageResponse(body: JsonElement?): ParsedGroupsPage {
        if (body == null || body.isJsonNull) {
            return ParsedGroupsPage(emptyList(), 1, 1, false, false)
        }
        return try {
            when {
                body.isJsonArray -> ParsedGroupsPage(
                    groups = body.asJsonArray.mapNotNull { gson.fromJson(it, GroupResponse::class.java) },
                    currentPage = 1,
                    totalPages = 1,
                    hasNext = false,
                    hasPrevious = false
                )
                body.isJsonObject -> {
                    val obj = body.asJsonObject
                    val array = when {
                        obj.get("results")?.isJsonArray == true -> obj.getAsJsonArray("results")
                        obj.get("data")?.isJsonArray == true -> obj.getAsJsonArray("data")
                        obj.get("groups")?.isJsonArray == true -> obj.getAsJsonArray("groups")
                        else -> null
                    }
                    val groups = array?.mapNotNull { gson.fromJson(it, GroupResponse::class.java) } ?: emptyList()
                    val next = obj.get("next")?.takeIf { !it.isJsonNull }?.asString
                    val previous = obj.get("previous")?.takeIf { !it.isJsonNull }?.asString
                    val count = obj.get("count")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt
                    val parsedCurrentPage = parsePageFromUrl(next)?.minus(1)
                        ?: parsePageFromUrl(previous)?.plus(1)
                        ?: currentPage
                    val pageSize = resolvePageSize().coerceAtLeast(1)
                    val parsedTotalPages = count?.let { kotlin.math.ceil(it / pageSize.toDouble()).toInt().coerceAtLeast(1) }
                        ?: maxOf(parsedCurrentPage, if (next != null) parsedCurrentPage + 1 else parsedCurrentPage)
                    ParsedGroupsPage(
                        groups = groups,
                        currentPage = parsedCurrentPage,
                        totalPages = parsedTotalPages,
                        hasNext = next != null,
                        hasPrevious = previous != null
                    )
                }
                else -> ParsedGroupsPage(emptyList(), 1, 1, false, false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse groups response: ${e.message}")
            ParsedGroupsPage(emptyList(), 1, 1, false, false)
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
        GroupUnreadStore.markRead(room.roNumber)
        allRooms = allRooms.map { existing ->
            if (existing.roNumber == room.roNumber) existing.copy(unreadCount = 0) else existing
        }
        latestRooms = filterRooms(allRooms, searchQuery, selectedReferenceFilter)
        adapter.updateRooms(latestRooms)
        NotificationWebSocketManager.getInstance().setActiveGroupSlug(room.roNumber)
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

    private fun setupFilters() {
        findViewById<EditText?>(R.id.edtSearch)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString().orEmpty().trim()
                latestRooms = filterRooms(allRooms, searchQuery, selectedReferenceFilter)
                adapter.updateRooms(latestRooms)
            }
        })

        findViewById<LinearLayout?>(R.id.filterDateLayout)?.setOnClickListener {
            showAppointmentDatePicker()
        }
        findViewById<LinearLayout?>(R.id.filterReferenceLayout)?.setOnClickListener {
            showReferenceMenu()
        }
        findViewById<LinearLayout?>(R.id.filterStatusLayout)?.setOnClickListener {
            showServiceStatusMenu()
        }
    }

    private fun setupPaginationControls() {
        findViewById<View?>(R.id.btnPrev)?.setOnClickListener {
            if (hasPreviousPage && currentPage > 1) {
                fetchGroups(currentPage - 1)
            }
        }
        findViewById<View?>(R.id.btnNext)?.setOnClickListener {
            if (hasNextPage) {
                fetchGroups(currentPage + 1)
            }
        }
    }

    private fun showAppointmentDatePicker() {
        val input = EditText(this).apply {
            hint = "YYYY-MM-DD"
            inputType = InputType.TYPE_CLASS_DATETIME
            setText(selectedAppointmentDate.orEmpty())
            setSelection(text.length)
            setPadding(40, 24, 40, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Appointment Date")
            .setView(input)
            .setPositiveButton("Apply") { _, _ ->
                val value = input.text?.toString()?.trim().orEmpty()
                if (value.isEmpty()) {
                    selectedAppointmentDate = null
                    findViewById<TextView?>(R.id.txtFilterDate)?.text = "YYYY-MM-DD"
                } else if (isValidFilterDate(value)) {
                    selectedAppointmentDate = value
                    findViewById<TextView?>(R.id.txtFilterDate)?.text = value
                } else {
                    Toast.makeText(this, "Use YYYY-MM-DD", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                fetchGroups(page = 1)
            }
            .setNeutralButton("Pick") { _, _ ->
                showNativeDatePicker()
            }
            .setNegativeButton("Clear") { _, _ ->
                selectedAppointmentDate = null
                findViewById<TextView?>(R.id.txtFilterDate)?.text = "YYYY-MM-DD"
                fetchGroups(page = 1)
            }
            .show()
    }

    private fun showNativeDatePicker() {
        val calendar = java.util.Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedAppointmentDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                findViewById<TextView?>(R.id.txtFilterDate)?.text = selectedAppointmentDate
                fetchGroups(page = 1)
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showReferenceMenu() {
        val anchor = findViewById<View>(R.id.filterReferenceLayout) ?: return
        val options = ReferenceFilter.values().toList()
        PopupMenu(this, anchor).apply {
            options.forEachIndexed { index, option ->
                menu.add(0, index, index, option.label)
            }
            setOnMenuItemClickListener { item ->
                val selected = options[item.itemId]
                selectedReferenceFilter = selected
                findViewById<TextView?>(R.id.txtFilterReference)?.text = selected.label
                latestRooms = filterRooms(allRooms, searchQuery, selectedReferenceFilter)
                adapter.updateRooms(latestRooms)
                true
            }
        }.show()
    }

    private fun showServiceStatusMenu() {
        val anchor = findViewById<View>(R.id.filterStatusLayout) ?: return
        val options = listOf(
            "All Rooms" to null,
            "Active" to "ACTIVE",
            "Disabled" to "DISABLED",
            "Closed" to "CLOSED",
            "Re-opened" to "RE_OPENED",
            "Cancelled" to "CANCELLED",
            "No Show" to "NO_SHOW"
        )
        PopupMenu(this, anchor).apply {
            options.forEachIndexed { index, option ->
                menu.add(0, index, index, option.first)
            }
            setOnMenuItemClickListener { item ->
                val selected = options[item.itemId]
                selectedServiceStatus = selected.second
                findViewById<TextView?>(R.id.txtFilterStatus)?.text = selected.first
                fetchGroups(page = 1)
                true
            }
        }.show()
    }

    private fun filterRooms(
        rooms: List<VirtualRoomUiModel>,
        query: String,
        referenceFilter: ReferenceFilter
    ): List<VirtualRoomUiModel> {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        return rooms.filter { room ->
            val referenceMatches = when (referenceFilter) {
                ReferenceFilter.ALL -> true
                ReferenceFilter.RO_NUMBER -> !room.roNumberDisplay.isNullOrBlank()
                ReferenceFilter.APPOINTMENT_ID -> room.roNumberDisplay.isNullOrBlank()
            }
            if (!referenceMatches) return@filter false
            if (normalizedQuery.isBlank()) return@filter true
            listOf(
                room.customerName,
                room.subject,
                room.roNumberDisplay,
                room.appointmentIdDisplay,
                room.status
            ).any { value ->
                value?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true
            }
        }
    }

    private fun resolveAppointmentId(group: GroupResponse): String? {
        if (!group.appointmentId.isNullOrBlank()) return group.appointmentId
        val subjectMatch = Regex("(SA\\d+|#\\d+)", RegexOption.IGNORE_CASE).find(group.name)?.value
        if (!subjectMatch.isNullOrBlank()) return subjectMatch
        val slugMatch = Regex("sa\\d+", RegexOption.IGNORE_CASE).find(group.slug)?.value
        return slugMatch?.uppercase(Locale.getDefault())
    }

    private fun resolveCustomerName(group: GroupResponse): String {
        val customerMember = group.members.find { member ->
            member.participantRole.equals("customer", ignoreCase = true) ||
                member.user?.username?.contains("customer", ignoreCase = true) == true
        }
        return customerMember?.displayName?.takeIf { it.isNotBlank() }
            ?: customerMember?.user?.firstName?.takeIf { it.isNotBlank() }
            ?: group.description
    }

    private fun resolvePageSize(): Int {
        return if (findViewById<View?>(R.id.layoutPagination) != null) 20 else 100
    }

    private fun parsePageFromUrl(url: String?): Int? {
        if (url.isNullOrBlank()) return null
        return try {
            Uri.parse(url).getQueryParameter("page")?.toIntOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun updatePaginationUi() {
        val paginationLayout = findViewById<View?>(R.id.layoutPagination) ?: return
        paginationLayout.visibility = View.VISIBLE
        findViewById<TextView?>(R.id.txtPageInfo)?.text = String.format(
            Locale.US,
            "%02d Of %02d Pages",
            currentPage,
            totalPages
        )

        findViewById<View?>(R.id.btnPrev)?.alpha = if (hasPreviousPage) 1f else 0.4f
        findViewById<View?>(R.id.btnNext)?.alpha = if (hasNextPage) 1f else 0.4f

        val page1 = findViewById<TextView?>(R.id.page1)
        val page2 = findViewById<TextView?>(R.id.page2)
        val page3 = findViewById<TextView?>(R.id.page3)
        val pageDots = findViewById<TextView?>(R.id.pageDots)
        val pageLast = findViewById<TextView?>(R.id.pageLast)

        listOf(page1, page2, page3, pageDots, pageLast).forEach { it?.visibility = View.GONE }

        val visiblePages = buildVisiblePages()
        bindPageText(page1, visiblePages.getOrNull(0))
        bindPageText(page2, visiblePages.getOrNull(1))
        bindPageText(page3, visiblePages.getOrNull(2))

        val showDots = totalPages > 4 && visiblePages.lastOrNull() != totalPages
        pageDots?.visibility = if (showDots) View.VISIBLE else View.GONE
        pageLast?.visibility = if (showDots) View.VISIBLE else View.GONE
        if (showDots) {
            bindPageText(pageLast, totalPages)
        }
    }

    private fun buildVisiblePages(): List<Int> {
        return when {
            totalPages <= 3 -> (1..totalPages).toList()
            currentPage <= 2 -> listOf(1, 2, 3)
            currentPage >= totalPages - 1 -> listOf(totalPages - 2, totalPages - 1, totalPages)
            else -> listOf(currentPage - 1, currentPage, currentPage + 1)
        }.filter { it in 1..totalPages }
    }

    private fun bindPageText(view: TextView?, page: Int?) {
        if (view == null) return
        if (page == null) {
            view.visibility = View.GONE
            view.setOnClickListener(null)
            return
        }
        view.visibility = View.VISIBLE
        view.text = page.toString()
        val isSelected = page == currentPage
        if (isSelected) {
            view.setBackgroundResource(R.drawable.bg_page_selected)
            view.setTextColor(getColor(android.R.color.white))
        } else {
            view.background = null
            view.setTextColor(android.graphics.Color.parseColor("#444444"))
        }
        view.setOnClickListener {
            if (page != currentPage) {
                fetchGroups(page)
            }
        }
    }

    private fun formatFilterDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "All Dates"
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(rawDate)
            SimpleDateFormat("ddMMMyyyy", Locale.US).format(input ?: return "All Dates")
        } catch (_: Exception) {
            rawDate
        }
    }

    private fun isValidFilterDate(value: String): Boolean {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            parser.isLenient = false
            parser.parse(value)
            true
        } catch (_: Exception) {
            false
        }
    }

    private enum class ReferenceFilter(val label: String) {
        ALL("All Types"),
        RO_NUMBER("RO Number"),
        APPOINTMENT_ID("Appointment ID")
    }
}
