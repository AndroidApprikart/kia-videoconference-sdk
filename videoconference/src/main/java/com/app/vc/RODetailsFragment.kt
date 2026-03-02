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


    }




}