package com.app.vc

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.app.vc.databinding.FragmentMediaBinding
import androidx.core.content.ContextCompat


class MediaFragment: Fragment() {

    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!
    var TAG = "MediaFragment"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tabPhotoVideos.post {
            moveIndicator(binding.tabPhotoVideos)
        }

        loadFragment(PhotosAndVideosFragment())
        setupTabs()
        selectPhotoVideosTab()

    }


    private fun loadFragment(fragment: Fragment) {

        childFragmentManager.beginTransaction()
            .replace(R.id.FragmentContainer, fragment)
            .commit()
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTabs() {

        binding.tabPhotoVideos.setOnClickListener {

            loadFragment(PhotosAndVideosFragment())

            selectPhotoVideosTab()
            moveIndicator(binding.tabPhotoVideos)

        }

        binding.tabDocuments.setOnClickListener {

            loadFragment(DocumentsFragment())

            selectDocsTab()
            moveIndicator(binding.tabDocuments)

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectPhotoVideosTab() {

        binding.tabPhotoVideos.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary_kia_kandid)
        )

        binding.tabPhotoVideos.setTypeface(
            resources.getFont(R.font.kia_signature_fix_bold)
        )



        binding.tabDocuments.setTextColor(
            ContextCompat.getColor(requireContext(),R.color.gray_mic_background)
        )

        binding.tabDocuments.setTypeface(
            resources.getFont(R.font.kia_signature_fix_regular)
        )

        binding.tabDocuments.background = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectDocsTab() {

        binding.tabDocuments.setTextColor(
            ContextCompat.getColor(requireContext(),R.color.colorPrimary_kia_kandid)
        )

        binding.tabDocuments.setTypeface(
            resources.getFont(R.font.kia_signature_fix_bold)
        )

        binding.tabPhotoVideos.setTextColor(
            ContextCompat.getColor(requireContext(),R.color.gray_mic_background)
        )

        binding.tabPhotoVideos.setTypeface(
            resources.getFont(R.font.kia_signature_fix_regular)
        )

        binding.tabPhotoVideos.background = null
    }

    private fun moveIndicator(tab: View) {

        binding.tabIndicator.post {

            val width = tab.width
            val start = tab.left

            binding.tabIndicator.layoutParams.width = width
            binding.tabIndicator.requestLayout()

            binding.tabIndicator.x = start.toFloat()
        }
    }





}
