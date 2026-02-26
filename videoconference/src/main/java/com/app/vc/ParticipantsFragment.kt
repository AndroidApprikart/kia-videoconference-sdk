package com.app.vc

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.vc.databinding.FragmentDocumentsBinding
import com.app.vc.databinding.FragmentParticipants2Binding
import com.app.vc.databinding.FragmentParticipantsBinding


class ParticipantsFragment : Fragment() {

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


    }




}
