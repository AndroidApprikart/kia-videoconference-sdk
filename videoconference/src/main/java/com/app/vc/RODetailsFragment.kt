package com.app.vc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.vc.databinding.FragmentPhotosAndVideosBinding
import com.app.vc.databinding.FragmentRODetailsBinding

class RODetailsFragment : Fragment() {

    private var _binding: FragmentRODetailsBinding? = null
    private val binding get() = _binding!!
    var TAG = "RODetailsFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRODetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(KEY_JOB_NOTES)?.let { setJobNotes(it) }
        arguments?.getString(KEY_STATUS_LABEL)?.let { setStatusLabel(it) }
    }

    fun setJobNotes(notes: String?) {
        binding.txtJobNotes.text = notes?.takeIf { it.isNotBlank() } ?: "--"
    }

    fun setStatusLabel(label: String?) {
        binding.statusDescription.text = label?.takeIf { it.isNotBlank() } ?: "Car is in the inspection stage"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val KEY_JOB_NOTES = "job_notes"
        const val KEY_STATUS_LABEL = "status_label"
    }
}