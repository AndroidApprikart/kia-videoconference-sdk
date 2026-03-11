package com.app.vc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.databinding.FragmentParticipants2Binding
import com.app.vc.models.GroupMemberResponse
import com.app.vc.participants.ChangeAdvisorAdapter
import com.app.vc.participants.ManageParticipantsAdapter
import com.app.vc.participants.ParticipantsAdapter
import com.app.vc.participants.ParticipantsViewModel
import com.app.vc.presence.PresenceStore
import com.app.vc.utils.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


class ParticipantsListFragment : Fragment() {

    private var _binding: FragmentParticipants2Binding? = null
    private val binding get() = _binding!!
    var TAG = "ParticipantsFragment"

    private lateinit var viewModel: ParticipantsViewModel
    private lateinit var participantsAdapter: ParticipantsAdapter
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

        // Fetch participants using token and group slug from arguments
        val token = PreferenceManager.getAccessToken() ?: ""
        val groupSlug = arguments?.getString(KEY_GROUP_SLUG)
        if (token.isNotEmpty()) {
            viewModel.fetchParticipants(token, groupSlug)
        } else {
            Toast.makeText(requireContext(), "No access token found", Toast.LENGTH_SHORT).show()
        }
        PresenceStore.addListener(presenceListener)
    }

    private fun setupObservers() {
        viewModel.members.observe(viewLifecycleOwner) { members ->
            if (members != null) {
                participantsAdapter.updateList(members)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
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
        
        val members = viewModel.members.value ?: emptyList()

        val recyclerView =
            view.findViewById<RecyclerView>(R.id.manageParticipantsList)

        recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerView.adapter =
            ManageParticipantsAdapter(members) { member ->
                showChangeAdvisorSheet()
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

    private fun setupRecycler(view: View, otherMembers: List<GroupMemberResponse>) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.listParticipants)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = ChangeAdvisorAdapter(otherMembers) { selectedAdvisor -> 
            // Handle selection if needed
        }
    }


    private fun showChangeAdvisorSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.vc_bottom_sheet_change_service_advisor, null)
        dialog.setContentView(view)

        val members = viewModel.members.value ?: emptyList()
        val adminMember = members.find { it.chatRole.equals("admin", ignoreCase = true) }
        val otherMembers = members.filter { !it.chatRole.equals("admin", ignoreCase = true) }

        // Update Admin info in the header of the bottom sheet
        adminMember?.let { admin ->
            val txtInitial = view.findViewById<TextView>(R.id.txtInitial)
            val tctParticipantName = view.findViewById<TextView>(R.id.tctParticipantName)
            val txtLeftStatus = view.findViewById<TextView>(R.id.txtLeftStatus)

            val displayName = admin.displayName
            tctParticipantName.text = displayName
            txtInitial.text = displayName.firstOrNull()?.uppercase() ?: "?"
            txtLeftStatus.text = admin.participantRole ?: ""
        }

        setupRecycler(view, otherMembers)

        dialog.show()

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            it.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    companion object {
        const val KEY_GROUP_SLUG = "group_slug"
    }

    override fun onDestroyView() {
        PresenceStore.removeListener(presenceListener)
        super.onDestroyView()
        _binding = null
    }
}
