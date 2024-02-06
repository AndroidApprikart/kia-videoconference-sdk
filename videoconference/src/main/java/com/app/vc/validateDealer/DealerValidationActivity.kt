package com.kia.vc.validateDealer

import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.app.vc.R
import com.app.vc.VCDynamicActivity4
import com.app.vc.baseui.BaseActivity
import com.app.vc.databinding.ActivityDealerValidationBinding
import com.app.vc.databinding.LayoutDialogConfirmationBinding
import com.app.vc.databinding.LayoutUniversalDialogBinding
import com.app.vc.utils.AndroidUtils


class DealerValidationActivity : BaseActivity() {
    lateinit var viewModel: DealerValidationViewModel
    lateinit var binding: ActivityDealerValidationBinding
    private var isScreenLargeOrXlarge: Boolean = false
    private var isScreenSmallOrNormal: Boolean = false

    private lateinit var dealerValidationDialog: Dialog
    private var isPowerSavingModeOn:Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dealer_validation)
        Log.d("DealerValidation: ", "onCreate: ")

        checkIfPowerSavingModeIsOn()
        init()
        initializeObservers()
        initializeApiResponseObservers()
        initializeOnClickListeners()

        getIntentValues()


    }
    private fun checkIfPowerSavingModeIsOn() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        isPowerSavingModeOn = powerManager.isPowerSaveMode
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

        viewModel.callType = intent.getStringExtra("callType")
        viewModel.customerName = intent.getStringExtra("customerName")
        viewModel.customerPhoneNumber = intent.getStringExtra("customerPhoneNumber")

        binding.tvWelcomeText.text = "Welcome to ${viewModel.dealerName} \n Video Conference."


    }

    private fun initializeOnClickListeners() {
        binding.btnContinueToVc.setOnClickListener {
            viewModel.isProgressBarVisible.value = true
            viewModel.isContinueButtonClickable.value = false
            checkIfPowerSavingModeIsOn()
            if(!isPowerSavingModeOn) {
                if(AndroidUtils.isNetworkOnLine(this)) {
                    viewModel.validateDealerCode()
                }else {
                    viewModel.isProgressBarVisible.value = false
                    viewModel.isContinueButtonClickable.value = true
                    viewModel.toastString.value = "No Internet Connection"
                }

            }else {
                viewModel.isProgressBarVisible.value = false
                viewModel.isContinueButtonClickable.value = true
                viewModel.toastString.value = "Please turn off powerSaving mode."

            }

        }
    }

    private fun initializeObservers() {
        viewModel.toastString.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        viewModel.isProgressBarVisible.observe(this) {
            if(it) {
                showProgressDialog()
            }else {
                dismissProgressDialog()
            }
        }
        viewModel.isContinueButtonClickable.observe(this) {
            if(true) {
                binding.btnContinueToVc.isEnabled = true
                binding.btnContinueToVc.isClickable= true
            }else {
                binding.btnContinueToVc.isEnabled = false
                binding.btnContinueToVc.isClickable= false
            }
        }
    }

    private fun init() {
        progressDialog = AndroidUtils.progressDialog(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dealer_validation)
        viewModel = DealerValidationViewModel()
        binding.dealerValidationVM = viewModel
        isScreenLargeOrXlarge = resources.getBoolean(R.bool.is_device_xlarge) or resources.getBoolean(R.bool.is_device_large)
        isScreenSmallOrNormal = resources.getBoolean(R.bool.is_device_normal) or resources.getBoolean(R.bool.is_device_small)
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
                        viewModel.isProgressBarVisible.value = false
                        viewModel.isContinueButtonClickable.value = true
                        showDealerValidationResponseDialog()
//                        showConfirmationDialog(
//                            this,
//                            isCancelable = false,
//                            title = "License Expired",
//                            message = "Please verify your license with the dealer or \n service advisor to use the VC service.",
//                            isCancelButtonVisible = true
//                        ) {
//                            finish()
//                        }
                    }
                }
            } else {
                viewModel.isProgressBarVisible.value = false
                viewModel.isContinueButtonClickable.value = true
                viewModel.toastString.value = "Something went wrong.NullResponse.DealerValidation"
            }
        }
    }

    private fun showDealerValidationResponseDialog() {
        dealerValidationDialog = Dialog(this)
        dealerValidationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = LayoutUniversalDialogBinding.inflate(LayoutInflater.from(this))
        dealerValidationDialog.setContentView(dialogBinding.root)
        dealerValidationDialog.setCancelable(false)
        dealerValidationDialog.setCanceledOnTouchOutside(false)

        dealerValidationDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogBinding.tvDialogTitle.text = "License Expired"
        dialogBinding.tvDialogMessage.text = "Please verify your license with the dealer or \n service advisor to use the VC service."
        dialogBinding.btnNegative.visibility = View.GONE
        dialogBinding.btnPositive.text = "OK"


        if(isScreenLargeOrXlarge) {
            dialogBinding.tvDialogMessage.text = "Please verify your license with the dealer or \n service advisor to use the VC service."
        }else {
            dialogBinding.tvDialogMessage.text = "Please verify your license with the dealer or service advisor to use the VC service."
        }

        dialogBinding.btnNegative.setOnClickListener {
            dealerValidationDialog.dismiss()
//                viewModel.isEndVcEnabled.value = true

        }
        dialogBinding.btnPositive.setOnClickListener {
            dealerValidationDialog.dismiss()
            //show progress dialog
            // check if the userType is customer if yes , then make api call to update vc status else endVC
            // based on the response if success show rate us dialog if failed then, show show errorr message with okay button
            // on click of okay button, open feedback screen

            finish()
        }
        //dialog alignment and size code.
//        val lp = WindowManager.LayoutParams()
//        lp.copyFrom(endVCDialog.window?.attributes)
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT
//        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
//        lp.gravity = Gravity.CENTER
//        endVCDialog.window?.attributes = lp
//        endVCDialog.getWindow()?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));

        dealerValidationDialog.show()
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



        if(isCancelButtonVisible) {
            val cancelLayoutParams = binding.tvCancelButton.layoutParams as LinearLayout.LayoutParams
            cancelLayoutParams.width = 0  // Set width to 0 to allow the weight to take effect
            cancelLayoutParams.weight = 1f  // Set weight for the "Cancel" button
            binding.tvCancelButton.layoutParams = cancelLayoutParams

            val updateLayoutParams = binding.btnUpdate.layoutParams as LinearLayout.LayoutParams
            updateLayoutParams.width = 0  // Set width to 0 to allow the weight to take effect
            updateLayoutParams.weight = 1f  // Set weight for the "Update" button
            binding.btnUpdate.layoutParams = updateLayoutParams
        }else {
            // Change the layout parameters for the "Update" button
            val layoutParams = binding.btnUpdate.layoutParams as LinearLayout.LayoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            binding.btnUpdate.layoutParams = layoutParams
        }

        val confirmationDialog = Dialog(context)
        confirmationDialog.setContentView(binding.root)

        if(!isScreenLargeOrXlarge) {
            confirmationDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }



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

        intent.putExtra("callType",viewModel.callType)
        intent.putExtra("customerName", viewModel.customerName)
        intent.putExtra("customerPhoneNumber",viewModel.customerPhoneNumber)

        viewModel.isProgressBarVisible.value = false
        finish()
        startActivity(intent)

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DealerValidationActivity", "onDestroy: ")
    }
}