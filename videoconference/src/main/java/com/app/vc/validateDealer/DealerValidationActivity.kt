package com.kia.vc.validateDealer

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION_CODES.P
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.app.vc.R
import com.app.vc.VCDynamicActivity4
import com.app.vc.databinding.ActivityDealerValidationBinding
import com.app.vc.databinding.LayoutDialogConfirmationBinding


class DealerValidationActivity : AppCompatActivity() {
    lateinit var viewModel: DealerValidationViewModel
    lateinit var binding: ActivityDealerValidationBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dealer_validation)
        Log.d("DealerValidation: ", "onCreate: ")

        init()
        initializeObservers()
        initializeApiResponseObservers()
        initializeOnClickListeners()

        getIntentValues()


    }

    private fun getIntentValues() {
        viewModel.roomId = intent.getStringExtra("room")
        viewModel.serviceAdvisorID = intent.getStringExtra("service_person_id")
        viewModel.userType = intent.getStringExtra("user_type")
        viewModel.passcode = intent.getStringExtra("auth_passcode")
        viewModel.customerCode = intent.getStringExtra("customerCode")
        viewModel.dealerCode = intent.getStringExtra("dealerCode")
        viewModel.roNo = intent.getStringExtra("roNo")
        viewModel.displayName = intent.getStringExtra("displayName")
        viewModel.userName = intent.getStringExtra("userName")
        viewModel.vcEndTime = intent.getStringExtra("vcEndTime")
        viewModel.dealerName = intent.getStringExtra("dealerName")

        binding.tvWelcomeText.text = "Welcome to ${viewModel.dealerName} \n Video Conference."


    }

    private fun initializeOnClickListeners() {
        binding.btnContinueToVc.setOnClickListener {
            viewModel.validateDealerCode()
        }
    }

    private fun initializeObservers() {
        viewModel.toastString.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun init() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dealer_validation)
        viewModel = DealerValidationViewModel()
        binding.dealerValidationVM = viewModel
    }

    private fun initializeApiResponseObservers() {
        viewModel.validateDealerCodeResponse.observe(this) {
            if (it != null) {
                when (it.status) {
                    "success" -> {
                        startVCScreen(
                            context = this,
                            meetingCode =viewModel.roomId ,
                            servicePersonId = viewModel.serviceAdvisorID,
                            userType = viewModel.userType,
                            authPassCode = viewModel.passcode,
                            roNo = viewModel.roNo,
                            customerCode =viewModel.customerCode,
                            dealerCode =viewModel.dealerCode ,
                            displayName = viewModel.displayName,
                            userName = viewModel.userName,
                            vcEndTime = viewModel.vcEndTime
                        )
                    }
                    else -> {

                        showConfirmationDialog(
                            this,
                            isCancelable = false,
                            title = "License Expired",
                            message = "Please verify your license with the dealer or \n service advisor to use the VC service.",
                            isCancelButtonVisible = false
                        ) {
                            finish()
                        }
                    }
                }
            } else {
                viewModel.toastString.value = "Something went wrong.NullResponse.DealerValidation"
            }
        }
    }


    fun showConfirmationDialog(
        context: Context,
        isCancelable: Boolean,
        title: String,
        message: String,
        isCancelButtonVisible: Boolean,
        onPositiveButtonClickListener: View.OnClickListener
    ) {
        val binding = LayoutDialogConfirmationBinding.inflate(LayoutInflater.from(context))
        binding.tvDialogTitle.text = title
        binding.tvDialogMessage.text = message

        binding.tvCancelButton.isVisible = isCancelButtonVisible

        val confirmationDialog = Dialog(context)
        confirmationDialog.setContentView(binding.root)
        confirmationDialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        confirmationDialog.setCancelable(isCancelable)

        binding.btnUpdate.setOnClickListener {
            onPositiveButtonClickListener.onClick(it)
            confirmationDialog.dismiss()
        }

        binding.tvCancelButton.setOnClickListener {
            confirmationDialog.dismiss()
        }

        confirmationDialog.show()
    }


    private fun startVCScreen(
        context: Context,
        meetingCode: String?,
        servicePersonId: String?,
        userType: String?,
        authPassCode: String?,
        roNo: String?,
        customerCode: String?,
        dealerCode: String?,
        displayName: String?,
        userName:String?,
        vcEndTime:String?
    ) {
        val intent = Intent(context, VCDynamicActivity4::class.java)
        intent.putExtra("room", meetingCode)
        intent.putExtra("service_person_id", servicePersonId)
        intent.putExtra("user_type", userType)
        intent.putExtra("auth_passcode", authPassCode)
        intent.putExtra("roNo", roNo)
        intent.putExtra("customerCode", customerCode)
        intent.putExtra("dealerCode", dealerCode)
        intent.putExtra("displayName", displayName)
        intent.putExtra("userName",userName)
        intent.putExtra("vcEndTime",vcEndTime)

        finish()
        startActivity(intent)

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DealerValidationActivity", "onDestroy: ")
    }
}