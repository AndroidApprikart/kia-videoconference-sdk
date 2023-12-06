package com.app.vc

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast

import com.kia.vc.validateDealer.DealerValidationActivity


public class VCLaunchConfiguration(
    private val activity: Activity?,
    private val meetingCode: String?,
    private var servicePersonId: String?,
    private val userType: String?,
    private val authPassCode: String?,
    private val roNo: String?,
    private val customerCode: String?,
    private val dealerCode: String?,
    private val displayName: String?,
    private val estimateToken: String?,
    private val userName: String?,
    private val vcEndTime: String?,
    private val dealerName:String?,
    private val baseUrl:String?
) {

    fun launchVCScreen() {
        if (activity != null) {
            if (!meetingCode.isNullOrEmpty()) {
                if (!servicePersonId.isNullOrEmpty()) {
                    if (!userType.isNullOrEmpty()) {
                        if (!authPassCode.isNullOrEmpty()) {
                            if (!customerCode.isNullOrEmpty()) {
                                if (!dealerCode.isNullOrEmpty()) {
                                    if (!displayName.isNullOrEmpty()) {
                                        if (!estimateToken.isNullOrEmpty()) {
                                            if (!userName.isNullOrEmpty()) {
                                                if (!vcEndTime.isNullOrEmpty()) {
                                                    if(!dealerName.isNullOrEmpty()) {
                                                        if(!baseUrl.isNullOrEmpty()) {
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: activity : ${activity.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: meetingCode : ${meetingCode.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: servicePersonId : ${servicePersonId.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: userType : ${userType.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: authPassCode : ${authPassCode.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: roNo : ${roNo.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: customerCode : ${customerCode.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: dealerCode : ${dealerCode.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: displayName : ${displayName.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: estimateToken : ${estimateToken.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: userName : ${userName.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: vcEndTime : ${vcEndTime.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: dealerName : ${dealerName.toString()} ")
                                                            Log.d("VCLaunchConfiguration", "launchVCScreen: baseUrl : ${baseUrl.toString()} ")

                                                            saveEstimateToken(
                                                                context = activity,
                                                                token = estimateToken
                                                            )
                                                            saveBaseUrl(
                                                                context = activity,
                                                                baseUrl = baseUrl
                                                            )

                                                            startValidationScreen(
                                                                activity = activity,
                                                                meetingCode = meetingCode,
                                                                servicePersonId = servicePersonId,
                                                                userType = userType,
                                                                authPassCode = authPassCode,
                                                                roNo = roNo,
                                                                customerCode = customerCode,
                                                                dealerCode = dealerCode,
                                                                displayName = displayName,
                                                                userName = userName,
                                                                vcEndTime = vcEndTime,
                                                                dealerName = dealerName
                                                            )
                                                        }else {
                                                            Toast.makeText(activity,"Base Url cannot be empty",Toast.LENGTH_SHORT).show()
                                                        }


                                                    }else {
                                                        Toast.makeText(
                                                            activity,
                                                            "Dealer name cannot be empty.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        activity,
                                                        "vcEndTime(HH:mm) cannot be null or empty",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }

//                                              startVCScreen(
//                                                  activity = activity,
//                                                  meetingCode = meetingCode,
//                                                  servicePersonId = servicePersonId,
//                                                  userType = userType,
//                                                  authPassCode = authPassCode,
//                                                  roNo = roNo,
//                                                  customerCode = customerCode,
//                                                  dealerCode = dealerCode,
//                                                  displayName = displayName
//                                              )


                                            } else {
                                                Toast.makeText(
                                                    activity,
                                                    "userName cannot be null or empty",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } else {
                                            Toast.makeText(
                                                activity,
                                                "estimateToken cannot be null or empty",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            activity,
                                            "displayName cannot be null or empty",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        activity,
                                        "dealerCode cannot be null or empty",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    activity,
                                    "customerCode cannot be null or empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                activity,
                                "authPassCode cannot be null or empty",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            activity,
                            "userType cannot be null or empty",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        activity,
                        "servicePersonId cannot be null or empty",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(activity, "meetingCode cannot be null or empty", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(activity, "activity cannot be null or empty", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun startVCScreen(
//        activity: Activity?,
//        meetingCode: String?,
//        servicePersonId: String?,
//        userType: String?,
//        authPassCode: String?,
//        roNo:String?,
//        customerCode:String?,
//        dealerCode:String?,
//        displayName:String?,
//    ) {
//        val intent = Intent(activity, VCScreen::class.java)
//        intent.putExtra("room", meetingCode)
//        intent.putExtra("service_person_id", servicePersonId)
//        intent.putExtra("user_type", userType)
//        intent.putExtra("auth_passcode", authPassCode)
//        intent.putExtra("roNo", roNo)
//        intent.putExtra("customerCode", customerCode)
//        intent.putExtra("dealerCode", dealerCode)
//        intent.putExtra("displayName", displayName)
//        activity?.startActivity(intent)
//    }

    private fun startValidationScreen(
        activity: Activity?,
        meetingCode: String?,
        servicePersonId: String?,
        userType: String?,
        authPassCode: String?,
        roNo: String?,
        customerCode: String?,
        dealerCode: String?,
        displayName: String?,
        userName: String?,
        vcEndTime: String?,
        dealerName: String?
    ) {
        val intent = Intent(activity, DealerValidationActivity::class.java)
        intent.putExtra("room", meetingCode)
        intent.putExtra("service_person_id", servicePersonId)
        intent.putExtra("user_type", userType)
        intent.putExtra("auth_passcode", authPassCode)
        intent.putExtra("roNo", roNo)
        intent.putExtra("customerCode", customerCode)
        intent.putExtra("dealerCode", dealerCode)
        intent.putExtra("displayName", displayName)
        intent.putExtra("userName", userName)
        intent.putExtra("vcEndTime",vcEndTime)
        intent.putExtra("dealerName",dealerName)


        activity?.startActivity(intent)
    }

    private fun saveEstimateToken(context: Activity, token: String) {
        PreferenceManager.init(context = context)
        PreferenceManager.setEstimateToken(token)
    }

    private fun saveBaseUrl(context: Activity,baseUrl:String) {
        PreferenceManager.init(context = context)
        PreferenceManager.setBaseUrl(baseUrl)
    }
}