package com.app.vc

import android.os.Build
import android.os.Bundle

import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi


import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

import com.app.vc.databinding.ActivityRepairOrderBinding
import com.app.vc.utils.ConnectivityBannerHandler


class RepairOrderActivity : AppCompatActivity() {
    private  val TAG = "RepairOrderActivity:"
    lateinit var binding : ActivityRepairOrderBinding

    private var groupSlug: String? = null
    private var connectivityBannerHandler: ConnectivityBannerHandler? = null

    companion object {
        const val EXTRA_GROUP_SLUG = "extra_group_slug"
        const val EXTRA_APPOINTMENTID = "extra_appointmentId"
        const val EXTRA_RO_NUMBER = "extra_ro_number"
        const val EXTRA_STATUS_LABEL = "extra_status_label"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_DAY_LABEL = "extra_day_label"
        const val EXTRA_TIME_LABEL = "extra_time_label"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRepairOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: FeedbackActivity: ")

        groupSlug = intent.getStringExtra(EXTRA_GROUP_SLUG)
        bindRoomDetailsFromIntent()
        connectivityBannerHandler = ConnectivityBannerHandler(
            context = this,
            rootViewProvider = { findViewById<View>(android.R.id.content) }
        )

        binding.btnBack.setOnClickListener { finish() }

        binding.tabParticipants.post {
            moveIndicator(binding.tabParticipants)
        }

        loadFragment(ParticipantsListFragment().apply {
            arguments = Bundle().apply {
                putString(ParticipantsListFragment.KEY_GROUP_SLUG, groupSlug)
            }
        })
        setupTabs()
        selectParticipantsTab()

    }

    override fun onStart() {
        super.onStart()
        connectivityBannerHandler?.register()
    }

    override fun onStop() {
        connectivityBannerHandler?.unregister()
        super.onStop()
    }

    private fun bindRoomDetailsFromIntent() {

        val roNumber = intent.getStringExtra(EXTRA_RO_NUMBER)
        val appointmentNumber = intent.getStringExtra(EXTRA_APPOINTMENTID)

//        intent.getStringExtra(EXTRA_RO_NUMBER)?.takeIf { it.isNotBlank() }?.let {
//            binding.orderId.text = it
//        }

        binding.orderId.text = when {
            !roNumber.isNullOrBlank() -> roNumber
            !appointmentNumber.isNullOrBlank() -> appointmentNumber
            else -> ""
        }

        intent.getStringExtra(EXTRA_STATUS_LABEL)?.takeIf { it.isNotBlank() }?.let {
            binding.status.text = it
        }
        intent.getStringExtra(EXTRA_DESCRIPTION)?.takeIf { it.isNotBlank() }?.let {
            binding.repairDescription.text = it
        }
        intent.getStringExtra(EXTRA_DAY_LABEL)?.takeIf { it.isNotBlank() }?.let {
            binding.createdDayTv.text = it
        }
        intent.getStringExtra(EXTRA_TIME_LABEL)?.takeIf { it.isNotBlank() }?.let {
            binding.timeTv.text = it
        }
    }

    private fun loadFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.FragmentContainer, fragment)
            .commit()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTabs() {

        binding.tabParticipants.setOnClickListener {

            loadFragment(ParticipantsListFragment().apply {
                arguments = Bundle().apply {
                    putString(ParticipantsListFragment.KEY_GROUP_SLUG, groupSlug)
                }
            })

            selectParticipantsTab()
            moveIndicator(binding.tabParticipants)

        }

        binding.tabMedia.setOnClickListener {

            loadFragment(MediaFragment().apply {
                arguments = Bundle().apply {
                    putString(MediaFragment.KEY_GROUP_SLUG, groupSlug)
                }
            })

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