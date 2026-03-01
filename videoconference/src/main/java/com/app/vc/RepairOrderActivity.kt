package com.app.vc

import android.os.Build
import android.os.Bundle

import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi


import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

import com.app.vc.databinding.ActivityRepairOrderBinding


class RepairOrderActivity : AppCompatActivity() {
    private  val TAG = "RepairOrderActivity:"
    lateinit var binding : ActivityRepairOrderBinding



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRepairOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: FeedbackActivity: ")

        binding.tabParticipants.post {
            moveIndicator(binding.tabParticipants)
        }

        loadFragment(ParticipantsListFragment())
        setupTabs()
        selectParticipantsTab()

    }

    private fun loadFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.FragmentContainer, fragment)
            .commit()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTabs() {

        binding.tabParticipants.setOnClickListener {

            loadFragment(ParticipantsListFragment())

            selectParticipantsTab()
            moveIndicator(binding.tabParticipants)

        }

        binding.tabMedia.setOnClickListener {

            loadFragment(MediaFragment())

            selectMediaTab()
            moveIndicator(binding.tabMedia)

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectParticipantsTab() {

        binding.tabParticipants.setTextColor(
            getColor(R.color.colorPrimary_kia_kandid)
        )

        binding.tabParticipants.setTypeface(
            resources.getFont(R.font.kia_signature_fix_bold)
        )



        binding.tabMedia.setTextColor(
            getColor(R.color.gray_mic_background)
        )

        binding.tabMedia.setTypeface(
            resources.getFont(R.font.kia_signature_fix_regular)
        )

        binding.tabMedia.background = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun selectMediaTab() {

        binding.tabMedia.setTextColor(
            getColor(R.color.colorPrimary_kia_kandid)
        )

        binding.tabMedia.setTypeface(
            resources.getFont(R.font.kia_signature_fix_bold)
        )

        binding.tabParticipants.setTextColor(
            getColor(R.color.gray_mic_background)
        )

        binding.tabParticipants.setTypeface(
            resources.getFont(R.font.kia_signature_fix_regular)
        )

        binding.tabParticipants.background = null
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