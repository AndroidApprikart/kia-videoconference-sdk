package com.app.vc

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.databinding.FragmentParticipants2Binding
import com.app.vc.models.GroupMemberResponse
import com.app.vc.network.AddGroupMemberRequest
import com.app.vc.network.EmployeeApiService
import com.app.vc.network.EmployeeItem
import com.app.vc.network.EmployeeListRequest
import com.app.vc.network.LoginApiService
import com.app.vc.network.RemoveGroupMemberRequest
import com.app.vc.participants.EmployeeAdvisorAdapter
import com.app.vc.participants.ManageParticipantsAdapter
import com.app.vc.participants.ParticipantsAdapter
import com.app.vc.participants.ParticipantsStorage
import com.app.vc.participants.ParticipantsViewModel
import com.app.vc.presence.PresenceStore
import com.app.vc.utils.ApiDetails
import com.app.vc.utils.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale


class ParticipantsListFragment : Fragment() {

    private var _binding: FragmentParticipants2Binding? = null
    private val binding get() = _binding!!
    var TAG = "ParticipantsFragment"

    private lateinit var viewModel: ParticipantsViewModel
    private lateinit var participantsAdapter: ParticipantsAdapter
    private val chatApiService: LoginApiService by lazy { buildChatApiService() }
    private val employeeApiService: EmployeeApiService by lazy { buildEmployeeApiService() }
    private val presenceListener: (Set<String>) -> Unit = { ids ->
        participantsAdapter.setOnlineUserIds(ids)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParticipants2Binding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupParticipantsList()

        setupManageParticipants()

        viewModel = ViewModelProvider(this)[ParticipantsViewModel::class.java]

        setupObservers()
        val groupSlug = arguments?.getString(KEY_GROUP_SLUG)
        if (!groupSlug.isNullOrBlank()) {
            val cachedMembers = ParticipantsStorage.load(requireContext(), groupSlug)
            if (cachedMembers.isNotEmpty()) {
                participantsAdapter.updateList(cachedMembers)
            }
        }
        val token = PreferenceManager.getAccessToken() ?: ""
        if (token.isNotEmpty()) {
            viewModel.fetchParticipants(token, groupSlug)
        }
        PresenceStore.addListener(presenceListener)
    }

    private fun setupObservers() {
        viewModel.members.observe(viewLifecycleOwner) { members ->
            if (members != null) {
                participantsAdapter.updateList(members)
                arguments?.getString(KEY_GROUP_SLUG)?.let { groupSlug ->
                    ParticipantsStorage.save(requireContext(), groupSlug, members)
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (!isLikelyNetworkError(it)) {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isLikelyNetworkError(message: String): Boolean {
        val text = message.lowercase(Locale.getDefault())
        return text.contains("unable to resolve host") ||
            text.contains("failed to connect") ||
            text.contains("timeout") ||
            text.contains("socket") ||
            text.contains("network") ||
            text.contains("internet") ||
            text.contains("no connection") ||
            text.contains("offline")
    }

    private fun setupParticipantsList() {
        participantsAdapter = ParticipantsAdapter(emptyList())
        binding.rvParticipants.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = participantsAdapter
        }
    }

    private fun setupManageParticipants() {
        val userRole = PreferenceManager.getuserType() ?: ""
        val isManager = userRole.equals("service_manager", ignoreCase = true) ||
                userRole.equals("manager", ignoreCase = true) ||
                userRole.equals("service manager", ignoreCase = true)

        if (isManager) {
            binding.btnManageParticipants.visibility = View.VISIBLE
            binding.btnManageParticipants.setOnClickListener {
                showManageParticipantsSheet()
            }
        } else {
            binding.btnManageParticipants.visibility = View.GONE
        }
    }

    private fun showManageParticipantsSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater
            .inflate(R.layout.vc_bottom_sheet_manage_participants, null)

        dialog.setContentView(view)
        view.findViewById<View>(R.id.crossIcon)?.setOnClickListener { dialog.dismiss() }

        val members = viewModel.members.value ?: emptyList()

        val recyclerView =
            view.findViewById<RecyclerView>(R.id.manageParticipantsList)

        recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerView.adapter =
            ManageParticipantsAdapter(members) { member ->
                dialog.dismiss()
                showChangeAdvisorSheet(member)
            }

        dialog.show()

        val bottomSheet =
            dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            it.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isDraggable = true
        }
    }

    private fun showChangeAdvisorSheet(currentAdvisor: GroupMemberResponse) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.vc_bottom_sheet_change_service_advisor, null)
        dialog.setContentView(view)
        view.findViewById<View>(R.id.crossIcon)?.setOnClickListener { dialog.dismiss() }

        val txtInitial = view.findViewById<TextView>(R.id.txtInitial)
        val txtParticipantName = view.findViewById<TextView>(R.id.tctParticipantName)
        val txtLeftStatus = view.findViewById<TextView>(R.id.txtLeftStatus)
        val recyclerView = view.findViewById<RecyclerView>(R.id.listParticipants)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressEmployees)
        val txtEmpty = view.findViewById<TextView>(R.id.txtEmptyEmployees)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        txtParticipantName.text = currentAdvisor.displayName
        txtInitial.text = currentAdvisor.displayName.firstOrNull()?.uppercase() ?: "?"
        txtLeftStatus.text = "Current"

        dialog.show()

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            it.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        loadEmployeeList(
            currentAdvisor = currentAdvisor,
            dialog = dialog,
            recyclerView = recyclerView,
            progressBar = progressBar,
            emptyView = txtEmpty
        )
    }

    private fun loadEmployeeList(
        currentAdvisor: GroupMemberResponse,
        dialog: BottomSheetDialog,
        recyclerView: RecyclerView,
        progressBar: ProgressBar,
        emptyView: TextView
    ) {
        val dealerCode = resolveDealerCode()
        if (dealerCode.isBlank()) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "Dealer code not available."
            recyclerView.visibility = View.GONE
            progressBar.visibility = View.GONE
            return
        }

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = EmployeeListRequest(
                    companyNumber = EMPLOYEE_COMPANY_NUMBER,
                    corporateNumber = EMPLOYEE_CORPORATE_NUMBER,
                    dealerNumber = dealerCode,
                    areaWorkType = EMPLOYEE_AREA_WORK_TYPE
                )

                Log.d(TAG, "Request: $request")

                val response = employeeApiService.getEmployeeList(
                    apiKey = EMPLOYEE_API_KEY,
                    request = request
                )

                Log.d(TAG, "Response Code: ${response.code()}")
                Log.d(TAG, "Response Message: ${response.message()}")

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error Body: $errorBody")
                } else {
                    Log.d(TAG, "Success Body: ${response.body()}")
                }

                val employees = if (response.isSuccessful) {
                    response.body()?.data.orEmpty()
                } else {
                    emptyList()
                }
                    .filter { it.employeeNumber.isNotBlank() && it.employeeName.isNotBlank() }
                    .distinctBy { it.employeeNumber }
                    .filterNot { it.employeeName.equals(currentAdvisor.displayName, ignoreCase = true) }

                progressBar.visibility = View.GONE

                if (employees.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "No service advisors found."

                    if (!response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "API Failed: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE

                recyclerView.adapter = EmployeeAdvisorAdapter(employees) { employee ->
                    confirmAdvisorReplacement(currentAdvisor, employee, dialog)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception: ", e)

                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.text = "Unable to load employees."

                Toast.makeText(
                    requireContext(),
                    e.message ?: "Unable to load employees",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmAdvisorReplacement(
        currentAdvisor: GroupMemberResponse,
        newAdvisor: EmployeeItem,
        dialog: BottomSheetDialog
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("Change Service Advisor")
            .setMessage("Replace ${currentAdvisor.displayName} with ${newAdvisor.employeeName}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Change") { _, _ ->
                replaceServiceAdvisor(currentAdvisor, newAdvisor, dialog)
            }
            .show()
    }

    private fun replaceServiceAdvisor(
        currentAdvisor: GroupMemberResponse,
        newAdvisor: EmployeeItem,
        dialog: BottomSheetDialog
    ) {
        val token = PreferenceManager.getAccessToken().orEmpty()
        val groupSlug = arguments?.getString(KEY_GROUP_SLUG).orEmpty()
        if (token.isBlank() || groupSlug.isBlank()) {
            Toast.makeText(requireContext(), "Missing group details", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {

                    val addResponse = chatApiService.addGroupMember(
                        token = "Bearer $token",
                slug = groupSlug,
                request = AddGroupMemberRequest(
                    uniqueId = newAdvisor.employeeNumber,
                    name = newAdvisor.employeeName,
                    role = "service_advisor"

                )
                    )

                if (!addResponse.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Unable to add advisor: ${addResponse.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val removeResponse = chatApiService.removeGroupMember(
                    token = "Bearer $token",
                    slug = groupSlug,
                    userId = currentAdvisor.userId
                )

                if (!removeResponse.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "New advisor added, but old advisor could not be removed.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                dialog.dismiss()
                viewModel.fetchParticipants(token, groupSlug)
                Toast.makeText(requireContext(), "Service advisor updated", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    e.message ?: "Unable to update service advisor",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun resolveDealerCode(): String {
        if (USE_STATIC_EMPLOYEE_DEALER_CODE) {
            return EMPLOYEE_STATIC_DEALER_NUMBER
        }
        val savedDealerCode = PreferenceManager.getDealerCode().orEmpty().trim()
        if (savedDealerCode.isNotBlank()) {
            return savedDealerCode
        }
        return arguments?.getString(KEY_GROUP_SLUG)
            ?.split("-")
            ?.getOrNull(1)
            ?.uppercase(Locale.ROOT)
            .orEmpty()
    }

    private fun buildChatApiService(): LoginApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(ApiDetails.APRIK_Kia_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(LoginApiService::class.java)
    }

    private fun buildEmployeeApiService(): EmployeeApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(EMPLOYEE_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(EmployeeApiService::class.java)
    }

    companion object {
        const val KEY_GROUP_SLUG = "group_slug"
        private const val EMPLOYEE_BASE_URL = "https://kialinkd-qa.kiaindia.net/"
        private const val EMPLOYEE_API_KEY =
            "APPRIKART-mQ7xK2nP9vR4sT6wY8zA3bC5dE1fG0hJqLp2"
        private const val EMPLOYEE_COMPANY_NUMBER = "K"
        private const val EMPLOYEE_CORPORATE_NUMBER = "A10VA"
        private const val EMPLOYEE_AREA_WORK_TYPE = "SA"

        private const val USE_STATIC_EMPLOYEE_DEALER_CODE = true
        private const val EMPLOYEE_STATIC_DEALER_NUMBER = "UP307"
    }

    override fun onDestroyView() {
        PresenceStore.removeListener(presenceListener)
        super.onDestroyView()
        _binding = null
    }
}
