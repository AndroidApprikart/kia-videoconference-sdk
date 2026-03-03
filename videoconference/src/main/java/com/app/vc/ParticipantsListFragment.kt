package com.app.vc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.databinding.FragmentParticipants2Binding
import com.app.vc.models.AdvisorModel
import com.app.vc.models.ParticipantsModel
import com.app.vc.participants.ChangeAdvisorAdapter
import com.app.vc.participants.ManageParticipantsAdapter
import com.app.vc.participants.ParticipantsAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


class ParticipantsListFragment : Fragment() {

    private var _binding: FragmentParticipants2Binding? = null
    private val binding get() = _binding!!
    var TAG = "ParticipantsFragment"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    }

    private fun setupParticipantsList() {
        val staticParticipants = arrayListOf(
            ParticipantsModel("V", "Vijaykumar", true, true, true, null).apply { displayName = "Vijaykumar (You)" },
            ParticipantsModel("L", "Lata", false, true, true, null).apply { displayName = "Lata (Customer)" },
            ParticipantsModel("A", "Ajay", false, true, true, null).apply { displayName = "Ajay (Service Advisor)" },
            ParticipantsModel("B", "Bhagat", false, true, true, null).apply { displayName = "Bhagat (Service Manager)" },
        )
        binding.rvParticipants.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ParticipantsAdapter(staticParticipants)
        }
    }

    private fun setupManageParticipants() {
        binding.btnManageParticipants.setOnClickListener {
            showManageParticipantsSheet()
        }
    }

    private fun showManageParticipantsSheet() {

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater
            .inflate(R.layout.vc_bottom_sheet_manage_participants, null)

        dialog.setContentView(view)
        val staticParticipants = arrayListOf(
            ParticipantsModel("V", "Vijaykumar", true, true, true, null).apply { displayName = "Vijaykumar" },
            ParticipantsModel("L", "Lata", false, true, true, null).apply { displayName = "Lata" },
            ParticipantsModel("A", "Ajay", false, true, true, null).apply { displayName = "Ajay" },
            ParticipantsModel("B", "Bhagat", false, true, true, null).apply { displayName = "Bhagat" },
        )

        val recyclerView =
            view.findViewById<RecyclerView>(R.id.manageParticipantsList)

        recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerView.adapter =
            ManageParticipantsAdapter(staticParticipants) { participant ->

                showChangeAdvisorSheet()

                // Later you can open another bottom sheet
                // showChangeAdvisorSheet(participant)
            }

//        view.findViewById<View>(R.id.btnChangeAdvisor)?.setOnClickListener {
//            dialog.dismiss()
//            showChangeAdvisorSheet()
//        }

        dialog.show()

        val bottomSheet =
            dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

        bottomSheet?.let {

            val behavior =
                BottomSheetBehavior.from(it)

            it.layoutParams.height =
                ViewGroup.LayoutParams.WRAP_CONTENT

            behavior.state =
                BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isDraggable = true
        }
    }

    private fun setupRecycler(view: View) {

        val recyclerView =
            view.findViewById<RecyclerView>(R.id.listParticipants)

        val staticAdvisors = arrayListOf(
            AdvisorModel("Ajay"),
            AdvisorModel("Bhagat"),
            AdvisorModel("Ramesh"),
            AdvisorModel("Karthik")
        )

        recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerView.adapter =
            ChangeAdvisorAdapter(staticAdvisors) { selectedAdvisor ->


            }
    }


    private fun showChangeAdvisorSheet() {

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater
            .inflate(R.layout.vc_bottom_sheet_change_service_advisor, null)

        dialog.setContentView(view)


        setupRecycler(view)

        dialog.show()

        val bottomSheet =
            dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

        bottomSheet?.let {

            val behavior = BottomSheetBehavior.from(it)

            it.layoutParams.height =
                ViewGroup.LayoutParams.WRAP_CONTENT

            behavior.state =
                BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }
}
